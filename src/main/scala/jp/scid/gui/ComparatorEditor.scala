package jp.scid.gui

import swing.{Reactions, Reactor}
import java.util.Comparator

/**
 * Comparator が変化するオブジェクト
 */
trait ComparatorEditor[E] {
  /**
   * 現在の比較器を取得
   * @return {@code Comparator}
   */
  def comparator: Comparator[E]
  def reactions: Reactions
}

object ComparatorEditor {
  def empty[E]: ComparatorEditor[E] = new ComparatorEditor[E] with Reactor {
    val comparator = new Comparator[E] {
      def compare(o1: E, o2: E) = 0
    }
  }
  
  abstract class Event extends swing.event.Event
  
  case class ComparatorChanged[E] (
    source: ComparatorEditor[E]
  ) extends Event
  
  private[gui] def noOrder[E] = new Comparator[E] {
    def compare(o1: E, o2: E) = 0
  }
}
