package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.{ChangeSnippet, ChangeType}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.{ExactlySnippetCondition, SameCodeSnippetCondition, Searcher}
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{ASTNode, Block}

/**
 * To revert the modified statement/expression to old ones in previous version:
 * Change actions covered:
 * - remove ADDED (already supported by Delete Mutation)
 * - add REMOVED
 * - revert MODIFIED
 * - re-swap MOVED
 * @param faultStatement
 * @param projectData
 */
case class RevertMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  // the whole faulty statement is added, moved
  private def revertStatementAsWhole(faultNode: Identifier, cs: ChangeSnippet): Boolean = {
    cs.changeType match {
      case ChangeType.MOVED =>
        ASTUtils.replaceNode(this.astRewrite, faultNode.getJavaNode(), this.astRewrite.getAST.createInstance(classOf[Block]))

        // identify the related node to the faulty node in the previous version
        val faultNodeInPrev = cs.srcSource
        var sibNode: ASTNode = ASTUtils.getSiblingNode(faultNodeInPrev.getJavaNode(), after = true)
        if (sibNode != null) { // try to insert before the pos-sibling node
          if (ASTUtils.isExistedNode(this.document.cu, sibNode)) {
            ASTUtils.insertNode(this.astRewrite, sibNode, faultNode.getJavaNode(), insertAfter = false)
            return true
          }
        }

        sibNode = ASTUtils.getSiblingNode(faultNodeInPrev.getJavaNode(), after = false)
        if (sibNode != null) { // try to insert after the pre-sibling node
          if (ASTUtils.isExistedNode(this.document.cu, sibNode)) {
            ASTUtils.insertNode(this.astRewrite, sibNode, faultNode.getJavaNode(), insertAfter = true)
            return true
          }
        }

        val parentNode = faultNodeInPrev.getJavaNode().getParent
        if (parentNode != null) { // try to insert inside the parent node
          if (ASTUtils.isExistedNode(this.document.cu, parentNode)) {
            ASTUtils.appendNode(this.astRewrite, parentNode, faultNode.getJavaNode())
            return true
          }
        }
        true

      case ChangeType.ADDED =>
        ASTUtils.replaceNode(this.astRewrite, faultNode.getJavaNode(), this.astRewrite.getAST.createInstance(classOf[Block]))
        true

      case _ =>
        logger.debug("Not support statement level revert action for change type: " + cs.changeType)
        false
    }
  }

  override def mutate(conditionExpr: Identifier = null): Boolean = {
    if (isParameterizable) assert(conditionExpr != null)
    var applied = false
    val faultLineNumber = faultStatement.getLine()

//    val faultFile = faultStatement.getFileName()
//    val cs = Searcher.searchChangeSnippets(
//      projectData.changedSourcesMap(faultFile),
//      ExactlySnippetCondition(faultStatement))
//
//    if (cs.size == 1) {
//      applied = revertStatementAsWhole(faultStatement, cs(0))
//    }

    var changedSnippet = DiffUtils.searchChangeSnippetOutside(projectData.changedSourcesMap, faultStatement)
    if (changedSnippet != null && changedSnippet.changeType != ChangeType.ADDED && changedSnippet.changeType != ChangeType.REMOVED) {
      val prevCode = changedSnippet.srcSource
      val currCode = changedSnippet.dstSource
      assert(prevCode != null)
      assert(currCode != null)
      if (changedSnippet.changeType == ChangeType.MODIFIED) {
        val currASTNodeOnDocument = ASTUtils.searchNodeByIdentifier(document.cu, currCode)
        ASTUtils.replaceNode(this.astRewrite, currASTNodeOnDocument, prevCode.getJavaNode())
        applied = true
      }
      else if (changedSnippet.changeType == ChangeType.MOVED && currCode.getBeginLine() == faultLineNumber) {
        val prevLine = prevCode.getBeginLine()
        val currentNodeAtPrevLine = ASTUtils.searchNodeByLineNumber(document.cu, prevLine)
        if (currentNodeAtPrevLine == null) return false
        val currentNode = ASTUtils.searchNodeByIdentifier(document.cu, currCode)

        // Step 1: Remove the current block of code
        ASTUtils.removeNode(document.rewriter, currentNode)

        // Step 2: Put it again at the previous line number
        if (prevLine < currCode.getBeginLine()) { // move down
          ASTUtils.insertNode(document.rewriter, currentNodeAtPrevLine, currCode.getJavaNode(), insertAfter = false)
        }
        else { // move up
          ASTUtils.insertNode(document.rewriter, currentNodeAtPrevLine, currCode.getJavaNode())
        }

        applied = true
      }
    }
    else {
      changedSnippet = DiffUtils.searchChangeSnippetOutside(projectData.changedSourcesMap, faultStatement, MAX_LINE_DISTANCE)
      // TODO: Support removed minor expression in a statement
      if (changedSnippet != null && changedSnippet.changeType == ChangeType.REMOVED) {
        val prevCode = changedSnippet.srcSource
        assert(prevCode != null)

        if (changedSnippet.srcRange.beginLine > faultStatement.getBeginLine()) {
          // insert after fault statement
          ASTUtils.insertNode(this.astRewrite, faultStatement.getJavaNode(), prevCode.getJavaNode())
        }
        else {
          // insert before fault statement
          ASTUtils.insertNode(this.astRewrite, faultStatement.getJavaNode(), prevCode.getJavaNode(), insertAfter = false)
        }
        applied = true
      }
    }

    if (applied) {
      doMutating()
      true
    }
    else false
  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = false
}
