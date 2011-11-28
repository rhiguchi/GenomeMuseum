package jp.scid.gui.table

import javax.swing.table.{TableColumnModel, TableColumn}

/**
 * TableColumn に並び替え特性を持たせ、列の識別子でテーブル行の並び替えを実装するモジュール
 */
trait TableColumnSortable[A] extends StringSortable[A] {
  this: DataTableModel[A] =>
  import collection.mutable
  import TableColumnSortable._
  
  /** 列整列情報が列オブジェクトに保存できない時の情報格納場所 */
  private val currentOrderMap = mutable.Map.empty[String, String]
  
  /** 現在指定されている並び替え列の識別子 */
  private var currentSortColumn: String = ""
  
  /**
   * 列が持つ現在の比較記述の取得する。
   */
  def orderStatement(identifier: String): String = {
    logger.debug("並び替え記述の取得 {}", identifier)
    currentOrderMap.getOrElse(identifier,
      columnOrderStatementsFor(identifier).headOption.getOrElse(""))
  }
  
  /**
   * 列の比較記述を更新する。
   */
  def updateOrderStatement(identifier: String, newOrder: String) {
    currentOrderMap(identifier) = newOrder
  }
  
  /**
   * 並び替えをしている列を取得する。
   */
  def sortColumn = currentSortColumn
  
  /**
   * 並び替えを行う列を指定する。
   */
  def sortColumn_=(identifier: String) {
    logger.debug("並び替え {}", identifier)
    currentSortColumn = identifier
    updateColumnSorting()
  }
  
  /**
   * 列並び替えを再度行う。
   */
  protected[table] def updateColumnSorting() {
    sortWith(orderStatement(currentSortColumn))
  }
  
  /**
   * このモデルに使用される列モデルを取得する。
   */
  def columnModel: TableColumnModel
  
  /**
   * 列整列情報の取得
   * @return 列が {@code SortableColumn} の時は、そのオブジェクから取得。
   *         それ以外か、列が存在しない時は {@code Nil} 。
   */
  private[table] def columnOrderStatementsFor(identifier: String): List[String] = {
    tableColumnMap.get(identifier) match {
      case Some(column: SortableColumn) => column.orderStatements
      case _ => Nil
    }
  }
  
  /**
   * TableColumn を作成する。
   */
  override protected def createTableColumn(modelIndex: Int): TableColumn with SortableColumn = {
    val columName = tableFormat.getColumnName(modelIndex)
    val column = new TableColumn(modelIndex) with SortableColumn {
      val orderStatements = orderStatementsFor(columName)
    }
    column setHeaderValue columName
    column setIdentifier columName
    column
  }
  
  protected def orderStatementsFor(columName: String) = {
    List(columName, columName + " desc")
  }
}

import javax.swing.table.JTableHeader
import jp.scid.gui.DataModel.Connector

object TableColumnSortable {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[TableColumnSortable[_]])
  
  def connect(model: TableColumnSortable[_], tableHeader: JTableHeader) = {
    new SortableTableHeaderHandler(model, tableHeader)
  }
}

/**
 * テーブルヘッダクリックソートハンドラ
 */
private[table] class SortableTableHeaderHandler(model: TableColumnSortable[_],
    tableHeader: JTableHeader) extends Connector {
  import java.awt.Cursor
  import java.awt.event.{MouseEvent, MouseAdapter}
  import javax.swing.table.TableCellRenderer
  import ca.odell.glazedlists.impl.SortIconFactory
  
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[SortableTableHeaderHandler])
  
  private val icons = SortIconFactory.loadIcons
  /** 順方向を示すアイコン */
  def ascendingIcon = icons(1)
  /** 逆方向を示すアイコン */
  def descendingIcon = icons(2)
  
  /** ヘッダーのクリックを認識するハンドラ */
  private val clickHandler = new MouseAdapter {
    override def mouseClicked(e: MouseEvent) {
      logger.debug("TableHeader クリック")
      e.getComponent match {
        case tableHeader: JTableHeader => 
          if (!isResizing(tableHeader)) {
            val clickecColumn = model.columnModel.getColumnIndexAtX(e.getX)
            headerClick(clickecColumn)
          }
          tableHeader.resizeAndRepaint()
        case _ =>
      }
    }
  }
  
  /** ヘッダーレンダラ */
  private val sortableRenderer = new ProxySortableRenderer(tableHeader.getDefaultRenderer)
  
  /**
   * カラムヘッダをクリックした動作を実行する
   */
  def headerClick(columnIndex: Int) {
    logger.debug("列ヘッダークリック {}", columnIndex)
    val identifier = getIdentifierFrom(model.columnModel, columnIndex)
    
    // 同じ列をクリックした時はその列の並び替え記述を更新
    if (identifier == model.sortColumn) {
      val orders = model.columnOrderStatementsFor(identifier)
      val currentOrder = model.orderStatement(identifier)
      val rest = orders.dropWhile(currentOrder.!=)
      if (rest.nonEmpty) {
        val newOrder = rest.tail.headOption.getOrElse(orders.head)
        model.updateOrderStatement(identifier, newOrder)
      }
    }
    
    model.sortColumn = identifier
  }
  
  /**
   * 結合を解放する。
   */
  def release() {
    tableHeader removeMouseListener clickHandler
    tableHeader setDefaultRenderer sortableRenderer.originalRenderer
  }
  
  /** クリックした場所がリサイズ領域であるか */
  private def isResizing(header: JTableHeader) =
    header.getCursor == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
  
  // ハンドラのインストール
  tableHeader setReorderingAllowed true
  tableHeader addMouseListener clickHandler
  // レンダラのインストール
  tableHeader.setDefaultRenderer(sortableRenderer)
  
  /**
   * 列モデルと列番号から識別子を取得
   */
  private def getIdentifierFrom(columnModel: TableColumnModel, column: Int) = {
    if (column < 0 || columnModel.getColumnCount <= column) {
      ""
    }
    else {
      columnModel.getColumn(column).getIdentifier.toString
    }
  }
  
  /**
   * テーブル列ヘッダに矢印を表示するレンダラ。
   */
  private class ProxySortableRenderer(val originalRenderer: TableCellRenderer) extends TableCellRenderer {
    import javax.swing.{table, JTable, JLabel, Icon, SwingConstants}
    import ca.odell.glazedlists.swing.SortableRenderer
    import StringSortable.{SortModel, SortOrder}
    import SortOrder._
    
    def getTableCellRendererComponent(table: JTable, value: AnyRef, isSelected: Boolean,
        hasFocus: Boolean, row: Int, column: Int) = {
      val identifier = getIdentifierFrom(table.getColumnModel, column)
      val isSortingColumn = identifier == model.sortColumn
      
      // 方向アイコン
      val iconOp = isSortingColumn match {
        case true => Some(orderIconOf(model.orderStatement(identifier)))
        case false => None
      }
      
      // レンダリング
      val label = originalRenderer match {
        case renderer: SortableRenderer =>
          renderer.setSortIcon(iconOp.getOrElse(null))
          renderer.getTableCellRendererComponent(table, value, isSortingColumn, hasFocus, row, column)
        case _ => originalRenderer.getTableCellRendererComponent(table, value, isSortingColumn,
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
    
    /**
     * 並び替え方向のアイコンを並び替え記述から取得する。
     */
    private def orderIconOf(orderStatement: String) = {
      SortModel.fromStatement(orderStatement).order match {
        case Ascending => ascendingIcon
        case Descending => descendingIcon
      }
    }
  }
}
