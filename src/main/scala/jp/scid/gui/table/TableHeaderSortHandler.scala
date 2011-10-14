package jp.scid.gui.table

import java.awt.Cursor
import java.awt.event.{MouseEvent, MouseAdapter}
import javax.swing.table.JTableHeader
import collection.mutable
import java.util.Comparator
import jp.scid.gui.{ComparatorEditor, StringComparatorEditor}

/**
 * テーブルヘッダクリックソートハンドラ
 */
class TableHeaderSortHandler[E](tableHeader: JTableHeader,
    comparatorFactory: String => Comparator[E]) {
  /** このコンストラクタで作成したときは comparatorEditor は比較器を変更しない */
  def this(tableHeader: JTableHeader) =
    this(tableHeader, (_: String) => ComparatorEditor.noOrder)
  
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
  
  /** 列整列情報が列オブジェクトに保存できない時の情報格納場所 */
  private val currentOrderMap = mutable.Map.empty[String, String]
  
  /** クリックでの値が適用される比較器エディタ */
  lazy val comparatorEditor = new StringComparatorEditor[E](comparatorFactory)
  
  /** カラムモデル取得ショートカット */
  private def columnModel = tableHeader.getColumnModel
  
  /** 選択モデル取得ショートカット */
  private def columnSelectionModel = columnModel.getSelectionModel
  
  /**
   * 識別子取得ショートカット
   * @return null でない識別子 String 値。
   */
  private def columnIdentifierFor(columnIndex: Int): String =
    Some(columnModel.getColumn(columnIndex).getIdentifier).getOrElse("").toString
  
  /**
   * 現在の列情報の取得
   */
  def currentOrder(columnIndex: Int): String = {
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
      orders.indexOf(currentOrder(columnIndex)) match {
        case -1 => orders.headOption.getOrElse("")
        case index => orders.drop(index + 1).headOption.getOrElse(orders.head)
      }
    }
    else {
      currentOrder(columnIndex)
    }
    // 並べ替え記述の更新
    updateOrderFor(columnIndex, newOrder)
    comparatorEditor.orderStatement = newOrder
    
    // カラム選択
    columnSelectionModel.setSelectionInterval(columnIndex, columnIndex)
    tableHeader.resizeAndRepaint()
  }
  
  /**
   * 列整列情報の取得
   * @return 列が {@code SortableColumn} の時は、そのオブジェクから取得。
   *         それ以外は {@code TableColumn#getIdentifier() } の文字列から生成される
   */
  protected[table] def ordersFor(columnIndex: Int): List[String] =
    columnModel.getColumn(columnIndex) match {
      case column: SortableColumn => column.orderStatements
      case _ => List("", " desc").map(columnIdentifierFor(columnIndex) + _)
    }
  
  /** クリックした場所がリサイズ領域であるか */
  private def isResizing(header: JTableHeader) =
    header.getCursor == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
}
