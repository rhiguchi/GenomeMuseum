package jp.scid.gui.table

import java.awt.Cursor
import java.awt.event.{MouseEvent, MouseListener, MouseAdapter}
import javax.swing.table.{JTableHeader, TableColumnModel, TableColumn}
import collection.mutable
import java.util.Comparator

/**
 * テーブルヘッダクリックソートハンドラ
 */
class TableSortingMouseHandler(var tableModel: DataTableModel[_]) {
  import TableSortingMouseHandler.makeSortableTableHeader
  
  /** ヘッダーのクリックを認識するハンドラ */
  val mouseListener: MouseListener = new MouseAdapter {
    override def mouseClicked(e: MouseEvent) {
      e.getComponent match {
        case tableHeader: JTableHeader => 
          if (!isResizing(tableHeader)) {
            val clickecColumn = tableHeader.getColumnModel.getColumnIndexAtX(e.getX)
            headerClick(clickecColumn, tableHeader.getColumnModel)
            tableHeader.resizeAndRepaint()
          }
        case _ =>
      }
    }
  }
  
  /**
   * ハンドラをヘッダに設定する
   */
  def installTo(header: JTableHeader) {
    header addMouseListener mouseListener
    makeSortableTableHeader(header)
  }
  
  /** 列整列情報が列オブジェクトに保存できない時の情報格納場所 */
  private val currentOrderMap = mutable.Map.empty[String, String]
  
  /**
   * 識別子取得ショートカット
   * @return null でない識別子 String 値。
   */
  private def columnIdentifierFor(column: TableColumn): String =
    Some(column.getIdentifier).getOrElse("").toString
  
  /**
   * 現在の列情報の取得
   */
  def currentOrder(column: TableColumn): String = {
    val stmt = column match {
      case column: SortableColumn => column.orderStatement
      case _ => currentOrderMap.getOrElse(columnIdentifierFor(column), "")
    }
    if (stmt.nonEmpty) stmt
    else ordersFor(column).headOption.getOrElse("")
  }
  
  def updateOrderFor(column: TableColumn, newOrder: String) = column match {
    case column: SortableColumn => column.orderStatement = newOrder
    case _ => currentOrderMap(columnIdentifierFor(column)) = newOrder
  }
  
  /**
   * カラムヘッダをクリックした動作を実行する
   * @param columnIndex 列番号
   * @param columnModel クリックされたヘッダの列モデル
   */
  def headerClick(columnIndex: Int, columnModel: TableColumnModel) {
    val tableColumn = columnModel.getColumn(columnIndex)
    val newOrder = if (columnModel.getSelectionModel.isSelectedIndex(columnIndex)) {
      val orders = ordersFor(tableColumn)
      orders.indexOf(currentOrder(tableColumn)) match {
        case -1 => orders.headOption.getOrElse("")
        case index => orders.drop(index + 1).headOption.getOrElse(orders.head)
      }
    }
    else {
      currentOrder(tableColumn)
    }
    // 並べ替え記述の更新
    updateOrderFor(tableColumn, newOrder)
    tableModel sortWith newOrder
    
    // カラム選択
    columnModel.getSelectionModel.setSelectionInterval(columnIndex, columnIndex)
  }
  
  /**
   * 列整列情報の取得
   * @return 列が {@code SortableColumn} の時は、そのオブジェクから取得。
   *         それ以外は {@code TableColumn#getIdentifier() } の文字列から生成される
   */
  protected[table] def ordersFor(column: TableColumn): List[String] = column match {
    case column: SortableColumn => column.orderStatements
    case _ => List("", " desc").map(columnIdentifierFor(column) + _)
  }
  
  /** クリックした場所がリサイズ領域であるか */
  private def isResizing(header: JTableHeader) =
    header.getCursor == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
}

object TableSortingMouseHandler {
  import javax.swing.{table, JTable, JLabel, Icon, SwingConstants}
  import table.{TableCellRenderer, TableColumn}
  import ca.odell.glazedlists.swing.SortableRenderer
  import ca.odell.glazedlists.impl.SortIconFactory
  
  private lazy val icons = SortIconFactory.loadIcons
  
  private def makeSortableTableHeader(header: JTableHeader) {
    val renderer = toSortArrowHeaderRenderer(header.getDefaultRenderer, icons(1), icons(2))
    header.setDefaultRenderer(renderer)
  }
  
  /**
   * セルレンダラを整列矢印を付与するレンダラに変換する
   */
  def toSortArrowHeaderRenderer(renderer: TableCellRenderer, ascending: Icon,
      descending: Icon) = new TableCellRenderer {
    def getTableCellRendererComponent(table: JTable, value: AnyRef, isSelected: Boolean,
        hasFocus: Boolean, row: Int, column: Int) = {
      val selected = table.getColumnModel.getSelectionModel.isSelectedIndex(column)
      // 方向アイコン
      val iconOp =
        if (!selected) None
        else if (table.getColumnCount <= column) None
        else sortingStateAscending(table.getColumnModel.getColumn(column)) map
          { if (_) ascending else descending }
      
      // レンダリング
      val label = renderer match {
        case renderer: SortableRenderer =>
          renderer.setSortIcon(iconOp.getOrElse(null))
          renderer.getTableCellRendererComponent(table, value, selected, hasFocus, row, column)
        case _ => renderer.getTableCellRendererComponent(table, value, selected,
            hasFocus, row, column) match {
          case rendered: JLabel =>
            rendered.setIcon(iconOp.getOrElse(null))
            rendered.setHorizontalTextPosition(SwingConstants.LEADING)
            rendered
          case rendered =>
            rendered
        }
      }
      
      label
    }
    
    protected def sortingStateAscending(column: TableColumn) = column match {
      case column: SortableColumn =>
        if (column.orderStatement.split("\\s+").contains("desc"))
          Some(false)
        else
          Some(true)
      case _ => Some(true)
    }
  }
}
