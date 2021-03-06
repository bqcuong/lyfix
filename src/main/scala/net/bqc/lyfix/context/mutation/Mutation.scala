package net.bqc.lyfix.context.mutation

import net.bqc.lyfix.context.ProjectData
import net.bqc.lyfix.context.compiler.DocumentASTRewrite
import net.bqc.lyfix.identifier.Identifier
import org.eclipse.jdt.core.dom.rewrite.{ASTRewrite, ListRewrite}
import org.eclipse.jdt.core.dom.{ASTNode, Block}
import org.eclipse.text.edits.{TextEdit, UndoEdit}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

abstract class Mutation(faultStatement: Identifier, projectData: ProjectData) {
  /**
   * Line Margin for Fault Localization (±3)
   */
  protected val MAX_LINE_DISTANCE: Int = 3
  /**
   * If no document is provided, use the original document in sourceFileContents map
   */
  protected val document: DocumentASTRewrite = projectData.sourceFileContents.get(faultStatement.getFileName())
  protected val astRewrite: ASTRewrite = document.generateASTRewrite
  protected val patches: ArrayBuffer[Patch] = ArrayBuffer[Patch]()

  protected def addPatch(patch: Patch): Unit = {
    patches.append(patch)
  }

  protected def doMutating(): Unit = {

  }

  def unmutate(): Unit = {

  }

  def getPatches(): ArrayBuffer[Patch] = patches

  def isParameterizable: Boolean

  /**
   * Handle the mutating actions
   * @param paramSeeds if not null, this operator is parameterizable
   */
  def mutate(paramSeeds: ArrayBuffer[Identifier]): Boolean
  def applicable(): Boolean
  def getRewriter: ASTRewrite = this.astRewrite
}
