package jp.scid.gui.table

import java.util.Comparator
import ca.odell.glazedlists.gui.AdvancedTableFormat
import ca.odell.glazedlists.GlazedLists

/**
 * TableFormat を利用して文字列指定で要素の並び替えをするモジュールトレイト。
 */
trait StringSortable[A] extends jp.scid.gui.StringSortable[A] {
  this: DataTableModel[A] =>
  import StringSortable._
  
  /** ソート記述の区切り文字 */
  val sortTokenSepalator = ","
  
  /** 記述の指定が無効のときの標準の比較器 */
  lazy val defaultComparator = new Comparator[A] { def compare(o1: A, o2: A) = 0 }
  
  /**
   * 並び替え記述から比較を作成する。
   * 列名と空白文字を挟んで「asc」「desc」を加えることで、それぞれ順方向、逆方向を
   * 指定することができる。
   * {@code sortTokenSepalator} で区切ることで、複数の列からなる比較器を作成できる。
   * @param orderStatement 並び替え記述。
   * @return {@code tableFormat} に基づいた比較器。
   */
  protected def comparatorFor(orderStatement: String) = {
    logger.debug("比較器の取得 {}", orderStatement)
    val comps = orderStatement split sortTokenSepalator map (_.trim) map
      SortModel.fromStatement map comparatorFor collect { case Some(c) => c }
    GlazedLists.chainComparators(comps: _*)
  }
  
  /** {@code tableFormat} に基づいて比較器を作成する。 */
  private def comparatorFor(model: SortModel) = {
    val colIndex = findColumnIndexOf(model.column)
    
    val comparator = colIndex match {
      case -1 => None
      case colIndex =>
        val comparator = tableFormat match {
          case format: AdvancedTableFormat[_] =>
            format.getColumnComparator(colIndex).asInstanceOf[Comparator[A]]
          case _ => new TableValueComparator(colIndex)
        }
        Option(comparator)
    }
      
    model.order match {
      case SortOrder.Descending =>
        comparator.map{ c => GlazedLists reverseComparator c }
      case _ => comparator
    }
  }
  
  /** 列名から列モデルインデックスを取得。 */
  private def findColumnIndexOf(columnName: String) =
    Range(0, tableFormat.getColumnCount) find { index => 
      columnName == tableFormat.getColumnName(index) } getOrElse(-1)
  
  /**
   * テーブル値で並び替えを行う Comparator
   */
  private class TableValueComparator(columnIndex: Int) extends Comparator[A] {
    def compare(o1: A, o2: A) = (o1, o2) match {
      case (null, null) => 0
      case (null, _) => 1
      case (_, null) => -1
      case (o1, o2) => compareValues(o1, o2)
    }
    
    /** 要素の値を比較 */
    private def compareValues(o1: A, o2: A) = (valueOf(o1), valueOf(o2)) match {
      case (v1: Comparable[_], v2: Comparable[_]) =>
        try {
          v1.asInstanceOf[Comparable[AnyRef]] compareTo v2.asInstanceOf[Comparable[AnyRef]]
        }
        catch {
          case e: ClassCastException =>
            e.printStackTrace
            0
        }
      case _ => 0
    }
    
    /** 要素の値を取得 */
    private def valueOf(element: A) =
      tableFormat.getColumnValue(element, columnIndex)
  }
}

private[table] object StringSortable {
  import ca.odell.glazedlists.gui.TableFormat
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[StringSortable[_]])
  
  object SortOrder extends Enumeration {
    type SortOrder = Value
    val Ascending = Value
    val Descending = Value
    
    def unapply(name: String): Option[SortOrder.Value] = name.toLowerCase match {
      case "asc" => Some(Ascending)
      case "desc" => Some(Descending)
      case "ascending" => Some(Ascending)
      case "descending" => Some(Descending)
      case _ => None
    }
  }

  /**
   * ならびかえの定義
   */
  case class SortModel (
    column: String,
    order: SortOrder.Value
  )

  object SortModel {
    /** SortState 作成時の単語の分割条件 */
    private val wordSepalator = "\\s+"
    
    /**
     * 文字列から SortState を作成
     */
    def fromStatement(token: String) = token.trim split wordSepalator match {
      case Array(columnName, tail @ _*) =>
        val order = tail.headOption match {
          case None => SortOrder.Ascending
          case Some(SortOrder(order)) => order
          case unknown => throw new IllegalArgumentException("Unknown order " + unknown)
        }
        SortModel(columnName, order)
      case _ => throw new IllegalArgumentException("Unknown token " + token)
    }
  }
}
