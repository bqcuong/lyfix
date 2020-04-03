package net.bqc.jrelifix.identifier.node

import net.bqc.jrelifix.identifier.PositionBasedIdentifier

class ExpressionIdentifier(beginLine: Int,
                           endLine: Int,
                           beginColumn: Int,
                           endColumn: Int)
  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn) {

  private var bool = false

  def isBool(): Boolean = bool
  def setBool(bool: Boolean): Unit = this.bool = bool
}
