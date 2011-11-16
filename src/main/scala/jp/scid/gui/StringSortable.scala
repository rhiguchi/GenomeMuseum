package jp.scid.gui

import java.util.Comparator

/**
 * 文字列で並び替えをする機能を持たせるモジュールトレイト。
 */
trait StringSortable[A] {
  this: DataListModel[A] =>
  
  /**
   * 文字列で並び替え。
   * @param orderStatement 並び替えの記述
   */
  def sortWith(orderStatement: String) {
    sortWith(comparatorFor(orderStatement))
  }
  
  /**
   * 並び替えの {@code Compaator} を、文字列から作成する。
   */
  protected def comparatorFor(orderStatement: String): Comparator[_ >: A]
}
