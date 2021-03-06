package net.bqc.lyfix.context.mutation

import java.util.Objects

import net.bqc.lyfix.context.ProjectData
import net.bqc.lyfix.context.diff.ChangeType
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.identifier.seed.VariableSeedIdentifier
import net.bqc.lyfix.utils.ASTUtils
import org.apache.log4j.Logger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class NullCheckerMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def isParameterizable: Boolean = false

  /**
   * Handle the mutating actions
   *
   * @param paramSeeds if not null, this operator is parameterizable
   */
  override def mutate(paramSeeds: ArrayBuffer[Identifier]): Boolean = {
    val faultFile = faultStatement.getFileName()
    val seedSet = projectData.seedsMap(faultFile)
    if (Objects.isNull(seedSet)) return false
    val variableSet = new mutable.HashSet[Identifier]()

    // collect all the changed variables in the faulty file
    for (originalSeed <- seedSet) {
      originalSeed match {
        case s: VariableSeedIdentifier =>
          if (s.getChangeTypes().nonEmpty && s.getChangeTypes().contains(ChangeType.ADDED)) {
            variableSet.addOne(s)
          }
        case _ =>
      }
    }

    // if statement, for statement, while statement
    if (faultStatement.isConditionalStatement()) {
      val conExpr = ASTUtils.getConditionalNode(faultStatement.getJavaNode())
      val conExprStr = conExpr.toString.trim
      for (vSeed <- variableSet) {
        val vSeedName = vSeed.getJavaNode().toString.trim
        if (conExprStr.contains(vSeedName)) {
          // patch 1
          val newConExprStr1 = "%s != null && %s".format(vSeedName, conExprStr)
          val newConExpr1 = ASTUtils.createExprNodeFromString(newConExprStr1)
          val patch1 = new Patch(this.document)
          val replaceAction1 = ASTActionFactory.generateReplaceAction(conExpr, newConExpr1)
          patch1.addAction(replaceAction1)
          patch1.addUsingSeed(vSeed)
          addPatch(patch1)

          // patch 2
          val newConExprStr2 = "%s == null || %s".format(vSeedName, conExprStr)
          val newConExpr2 = ASTUtils.createExprNodeFromString(newConExprStr2)
          val patch2 = new Patch(this.document)
          val replaceAction2 = ASTActionFactory.generateReplaceAction(conExpr, newConExpr2)
          patch2.addAction(replaceAction2)
          patch2.addUsingSeed(vSeed)
          addPatch(patch2)
        }
      }
    }
    else { // normal statement, add if-precondition to check null
      val stmtStr = faultStatement.getJavaNode().toString.trim
      for (vSeed <- variableSet) {
       val vSeedName = vSeed.getJavaNode().toString.trim
        if (stmtStr.contains(vSeedName)) {
          val newIfCode = "if (%s != null) {%s}".format(vSeedName, stmtStr)
          val newIfNode = ASTUtils.createStmtNodeFromString(newIfCode)
          val patch = new Patch(this.document)
          val replaceAction = ASTActionFactory.generateReplaceAction(faultStatement.getJavaNode(), newIfNode)
          patch.addAction(replaceAction)
          patch.addUsingSeed(vSeed)
          addPatch(patch)
        }
      }
    }
    true
  }

  override def applicable(): Boolean = ???
}
