package jp.scid.genomemuseum.gui

import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.gui.TableFormat
import java.util.Comparator
import jp.scid.gui.ComparatorEditor

/**
 * SortingColumnModel と接続し、Comparator の変化をするエディタ
 */
class TableFormatComparatorFactory[E] (tableFormat: TableFormat[E])
    extends (String => Comparator[E]) {
  import TableFormatComparatorFactory._
  import ComparatorEditor.ComparatorChanged
  
  /** ソート記述の区切り文字 */
  val tokenSepalator = ","
  
  /** 比較を行わない比較器 */
  private lazy val noOrder = new Comparator[E] { def compare(o1: E, o2: E) = 0 }
  
  /** 指定する識別子を列名としてモデルインデックスを取得 */
  private def findModelIndex(columnName: String) = {
    Range(0, tableFormat.getColumnCount) find { index => 
      columnName == tableFormat.getColumnName(index) }
  }
  
  /** 並び替え記述で Comparator を作成 */
  private def createComparator(sort: SortState): Comparator[E] = {
    val index = findModelIndex(sort.column).get
    val c = new TableValueComparator(index)
    sort.order match {
      case SortOrder.Descending => GlazedLists reverseComparator c
      case _ => c
    }
  }
  
  /** SortState を 文字列から作成 */
  def apply(statement: String): Comparator[E] =
      statement.trim split tokenSepalator map SortState.parse match {
    case Array(sortState, _*) => createComparator(sortState)
    case _ => noOrder
  }
  
  /**
   * テーブル値で並び替えを行う Comparator
   */
  private class TableValueComparator(columnIndex: Int) extends Comparator[E] {
    def compare(o1: E, o2: E) = (o1, o2) match {
      case (null, null) => 0
      case (null, _) => 1
      case (_, null) => -1
      case (o1, o2) => compareValues(o1, o2)
    }
    
    /** 要素の値を比較 */
    private def compareValues(o1: E, o2: E) = (valueOf(o1), valueOf(o2)) match {
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
    private def valueOf(element: E) =
      tableFormat.getColumnValue(element, columnIndex)
  }
}

object TableFormatComparatorFactory {
  private object SortOrder extends Enumeration {
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
  private case class SortState (
    column: String,
    order: SortOrder.Value
  )

  private object SortState {
    /** SortState 作成時の単語の分割条件 */
    private val wordSepalator = "\\s+"
    
    /**
     * 文字列から SortState を作成
     */
    def parse(token: String) = token.trim split wordSepalator match {
      case Array(columnName, tail @ _*) =>
        val order = tail.headOption match {
          case None => SortOrder.Ascending
          case Some(SortOrder(order)) => order
          case unknown => throw new IllegalArgumentException("Unknown order " + unknown)
        }
        SortState(columnName, order)
      case _ => throw new IllegalArgumentException("Unknown token " + token)
    }
  }
}

