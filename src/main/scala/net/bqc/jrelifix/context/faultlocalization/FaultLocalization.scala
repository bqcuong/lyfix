package net.bqc.jrelifix.context.faultlocalization

import net.bqc.jrelifix.model.Identifier

import scala.collection.mutable.ArrayBuffer

abstract class FaultLocalization {
  var rankedList: ArrayBuffer[Identifier] = new ArrayBuffer[Identifier]()
  def run(): Unit
}