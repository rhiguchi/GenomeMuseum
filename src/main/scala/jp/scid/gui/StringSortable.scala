package jp.scid.gui

import java.util.Comparator
import jp.scid.gui.event.OrderStatementChanged

object StringSortable {
  /**
   * 要素値で並び替えを行う Comparator
   */
  private class ElementPropertyComparator[A, B <: Comparable[B]](converter: A => B) extends Comparator[A] {
    def compare(o1: A, o2: A) = (o1, o2) match {
      case (null, null) => 0
      case (null, _) => 1
      case (_, null) => -1
      case (o1, o2) => valueOf(o1) compareTo valueOf(o2)
    }
    
    /** 要素の値を取得 */
    private def valueOf(element: A) = converter(element)
  }
  
  object ElementPropertyComparator {
    def apply[A, B <: Comparable[B]](converter: (A) => B): Comparator[A] =
      new ElementPropertyComparator(converter)
  }
}

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
