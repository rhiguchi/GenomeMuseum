package jp.scid.gui

import swing.Publisher
import java.util.Comparator

/**
 * 並び替え記述による比較器エディタ
 */
class StringComparatorEditor[E](var factory: String => Comparator[E])
    extends ComparatorEditor[E] with Publisher {
  import ComparatorEditor.ComparatorChanged
  
  /** 現在の並び替え記述 */
  private var orderStmt = ""
  
  /** 現在の Comparator */
  private var currentComparator: Comparator[E] = ComparatorEditor.noOrder
  
  /** 現在のソート記述をモデルから取得 */
  def orderStatement = orderStmt
  
  def orderStatement_=(newOrder: String) {
    orderStmt = newOrder
    updateComparator()
  }
  
  def comparator = currentComparator
  
  /**
   * Comparator を更新する
   */
  def updateComparator() {
    currentComparator = factory(orderStatement)
    publish(ComparatorChanged(this))
  }
}
