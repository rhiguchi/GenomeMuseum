package jp.scid.gui.table

import java.util.Comparator
import javax.swing.{JTable, JTextField, ListSelectionModel}
import javax.swing.table.{TableModel, TableColumnModel, DefaultTableColumnModel,
  TableColumn}

import collection.mutable.Buffer

import ca.odell.glazedlists.{swing => glswing, gui => glgui,
  GlazedLists}
import glswing.EventTableModel
import glgui.TableFormat

import jp.scid.gui.DataListModel

class DataTableModel[A](tableFormat: TableFormat[A]) extends DataListModel[A] {
  /** テーブルモデル */
  private val eventTableModel = new EventTableModel(viewSource, tableFormat)
  
  /** テーブルカラムモデル */
  private val myColumnModel = {
    val columnModel = new DefaultTableColumnModel
    Range(0, tableFormat.getColumnCount) map createTableColumn foreach
      columnModel.addColumn
    columnModel
  }
  
  /** Comparators */
  private lazy val comparatorFactory = new TableFormatComparatorFactory(tableFormat)
  
  /** ヘッダーソートハンドラ */
  private val tableHeaderSortHandler = new TableSortingMouseHandler(this)
  
  /** ソート記述からソート */
  def sortWith(orderStatement: String) {
    sortWith(getComparatorFor(orderStatement))
  }
  
  /** テーブルモデルの取得 */
  def tableModel: TableModel = eventTableModel
  
  /** テーブルカラムモデルの取得 */
  def columnModel: TableColumnModel = myColumnModel
  
  /** JTable ビューにモデルを設定する */
  def installTo(table: JTable) {
    table setAutoCreateColumnsFromModel false
    table setModel tableModel
    table setColumnModel columnModel
    table setSelectionModel selectionModel
    // ヘッダーソーター
    tableHeaderSortHandler installTo table.getTableHeader
  }
  
  /** テーブルカラムを作成 */
  protected def createTableColumn(modelIndex: Int): TableColumn = {
    val columName = tableFormat.getColumnName(modelIndex)
    val column = new TableColumn(modelIndex) with SortableColumn {
      val orderStatements = List(columName, columName + " desc")
    }
    column setHeaderValue columName
    column setIdentifier columName
    column
  }
  
  /** 文字列から比較器を取得 */
  protected def getComparatorFor(orderStatement: String): Comparator[_ >: A] = {
    comparatorFactory(orderStatement)
  }
}
