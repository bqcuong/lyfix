package net.bqc.jrelifix.identifier

import java.util.Objects

import net.bqc.jrelifix.context.diff.SourceRange
import net.bqc.jrelifix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom._

abstract class Identifier {
  private val logger: Logger = Logger.getLogger(this.getClass)
  protected var javaNode: ASTNode = _

  def getBeginLine(): Int
  def getEndLine(): Int
  def getLine(): Int
  def getBeginColumn(): Int
  def getEndColumn(): Int
  def getFileName(): String

  def getJavaNode(): ASTNode = javaNode
  def setJavaNode(javaNode: ASTNode): Unit = this.javaNode = javaNode

  def toSourceRange(): SourceRange = {
    new SourceRange(getBeginLine(), getEndLine(), getBeginColumn(), getEndColumn())
  }

  def sameLocation(node: ASTNode): Boolean = {
    val id = ASTUtils.createFaultIdentifierNoClassName(node)
     this.getBeginLine() == id.getBeginLine() &&
       this.getBeginColumn() == id.getBeginColumn() &&
       this.getEndLine() == id.getEndLine() &&
       this.getEndColumn() == id.getEndColumn()
  }

  def sameLocation(id: Identifier): Boolean = {
    this.getBeginLine() == id.getBeginLine() &&
      this.getBeginColumn() == id.getBeginColumn() &&
      this.getEndLine() == id.getEndLine() &&
      this.getEndColumn() == id.getEndColumn()
  }

  def after(id: Identifier): Boolean = {
    val c1 = this.getBeginLine() - id.getEndLine()
    val c2 = this.getBeginColumn() - id.getEndColumn()
    c1 > 0 || (c1 == 0 && c2 > 0)
  }

  /**
   * The same source code string, AND same location
   * @param obj
   * @return
   */
  override def equals(obj: Any): Boolean =
    obj match {
      case that: Identifier => {
        (that.getJavaNode() != null && this.getJavaNode() != null && that.getJavaNode().toString.equals(this.getJavaNode().toString) && that.sameLocation(this)) ||
        that.sameLocation(this)
      }
      case _ => false
    }

  override def hashCode(): Int = {
    var result = locationHashCode()
    if (javaNode != null) {
      result = 31 * result + javaNode.toString.hashCode
    }
    result
  }

  protected def locationHashCode(): Int = {
    val prime = 31
    var result = 1
    result = prime * result + getBeginLine()
    result = prime * result + getEndLine()
    result = prime * result + getBeginColumn()
    result = prime * result + getEndColumn()
    result
  }

  def isConditionalStatement(): Boolean = {
    ASTUtils.isConditionalStatement(javaNode)
  }

  def isIfStatement(): Boolean = {
    if (javaNode == null) return false
    javaNode.isInstanceOf[IfStatement]
  }

  def isVariableDeclarationStatement(): Boolean = {
    if (javaNode == null) return false
    javaNode.isInstanceOf[VariableDeclarationStatement]
  }

  def isSwappableStatement(): Boolean = {
    !isConditionalStatement() && !javaNode.isInstanceOf[ConstructorInvocation] &&
      !javaNode.isInstanceOf[ReturnStatement] && !javaNode.isInstanceOf[VariableDeclaration]
  }
}