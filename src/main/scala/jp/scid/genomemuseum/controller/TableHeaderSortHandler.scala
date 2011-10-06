package jp.scid.genomemuseum.controller

import java.awt.Cursor
import java.awt.event.{MouseEvent, MouseAdapter}
import javax.swing.table.JTableHeader
import collection.mutable
import jp.scid.genomemuseum.gui.{StringComparatorEditor, SortableColumn}
import java.util.Comparator

/**
 * テーブルヘッダクリックソートハンドラ
 */
class TableHeaderSortHandler[E] (tableHeader: JTableHeader,
    comparatorFactory: String => Comparator[E]) {
  /** ヘッダーのクリックを認識するハンドラ */
  tableHeader addMouseListener new MouseAdapter {
    override def mouseClicked(e: MouseEvent) {
      e.getComponent match {
        case tableHeader: JTableHeader => 
          if (!isResizing(tableHeader)) {
            val clickecColumn = tableHeader.getColumnModel.getColumnIndexAtX(e.getX)
            headerClick(clickecColumn)
          }
        case _ =>
      }
    }
  }
  
  val comparatorEditor = new StringComparatorEditor[E](comparatorFactory)
  
  private val currentOrderMap = mutable.Map.empty[String, String]
  
  private def columnSelectionModel = columnModel.getSelectionModel
  private def columnModel = tableHeader.getColumnModel
  private def columnIdentifierFor(columnIndex: Int): String =
    Some(columnModel.getColumn(columnIndex).getIdentifier).getOrElse("").toString
  
  protected[controller] def ordersFor(columnIndex: Int): List[String] =
    columnModel.getColumn(columnIndex) match {
      case column: SortableColumn => column.orderStatements
      case _ => List(" asc", " desc").map(columnIdentifierFor(columnIndex) + _)
    }
  
  def currentOrderFor(columnIndex: Int): String = {
    val stmt = columnModel.getColumn(columnIndex) match {
      case column: SortableColumn => column.orderStatement
      case _ => currentOrderMap.getOrElse(columnIdentifierFor(columnIndex).toString, "")
    }
    if (stmt.nonEmpty) stmt
    else ordersFor(columnIndex).headOption.getOrElse("")
  }
  
  def updateOrderFor(columnIndex: Int, newOrder: String) =
    columnModel.getColumn(columnIndex) match {
      case column: SortableColumn => column.orderStatement = newOrder
      case _ => currentOrderMap(columnIdentifierFor(columnIndex)) = newOrder
    }
  
  /**
   * カラムヘッダをクリックした動作を実行する
   */
  def headerClick(columnIndex: Int) {
    val newOrder = if (columnSelectionModel.isSelectedIndex(columnIndex)) {
      val orders = ordersFor(columnIndex)
      orders.indexOf(currentOrderFor(columnIndex)) match {
        case -1 => orders.headOption.getOrElse("")
        case index => orders.drop(index + 1).headOption.getOrElse(orders.head)
      }
    }
    else {
      currentOrderFor(columnIndex)
    }
    // 並べ替え記述の更新
    updateOrderFor(columnIndex, newOrder)
    comparatorEditor.orderStatement = newOrder
    
    // カラム選択
    columnSelectionModel.setSelectionInterval(columnIndex, columnIndex)
  }
  
  /** クリックした場所がリサイズ領域であるか */
  private def isResizing(header: JTableHeader) =
    header.getCursor == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
}
