package jp.scid.gui

import java.util.Comparator
import jp.scid.gui.event.OrderStatementChanged

/**
 * 文字列で並び替えをする機能を持たせるモジュールトレイト。
 */
trait StringSortable[A] {
  this: DataListModel[A] =>
  
  private var currentOrderStatement = ""
  
  /**
   * 文字列で並び替え。
   * @param orderStatement 並び替えの記述
   */
  @deprecated("use orderStatement")
  def sortWith(newValue: String) {
    orderStatement = newValue
  }
  
  /**
   * 文字列で並び替え記述の取得
   */
  def orderStatement = currentOrderStatement
  
  /**
   * 文字列で並び替え。
   * @param orderStatement 並び替えの記述
   */
  def orderStatement_=(newValue: String) {
    currentOrderStatement = newValue
    sortWith(comparatorFor(newValue))
    publish(OrderStatementChanged(this, newValue))
  }
  
  /**
   * 並び替えの {@code Compaator} を、文字列から作成する。
   */
  protected def comparatorFor(orderStatement: String): Comparator[_ >: A]
}
