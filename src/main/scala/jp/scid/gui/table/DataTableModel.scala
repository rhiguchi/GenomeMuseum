package jp.scid.gui.table

import javax.swing.table.{TableModel, TableColumnModel, TableColumn}

import ca.odell.glazedlists.{swing => glswing, gui => glgui,
  GlazedLists, EventList, FunctionList}
import glswing.{EventTableModel, EventTableColumnModel}
import glgui.TableFormat

import jp.scid.gui.DataListModel

class DataTableModel[A](val tableFormat: TableFormat[A]) extends DataListModel[A] {
  import DataTableModel._
  import DataListModel.{withWriteLock, withReadLock}
  
  /** 全テーブルカラム */
  private val allTableColumns = Range(0, tableFormat.getColumnCount) map { index =>
    val name = tableFormat getColumnName(index)
    val column = createTableColumn(index)
    name -> column
  } toIndexedSeq
  
  /** テーブルカラム名前マップ */
  private[table] val tableColumnMap = allTableColumns.toMap
  
  /** 表示されているテーブルカラム名の EventList */
  private val visibledColumnList: EventList[String] =
    GlazedLists.eventListOf(allTableColumns map (_._1): _*)
  
  /** 表示されているカラム名と同期したテーブルカラム EventList */
  private[table] val tableColumnSource = new FunctionList(visibledColumnList,
      new IdentifierTableColumnConvertFunction(allTableColumns toMap),
      new TableColumnIdentifierConvertFunction(tableFormat))
  
  /** テーブルカラムモデル */
  private val eventColumnModel = new EventTableColumnModel(tableColumnSource)
  
  /** テーブルモデル */
  private val eventTableModel = new EventTableModel(viewEventList, tableFormat)
  
  /** テーブルモデルの取得 */
  def tableModel: TableModel = eventTableModel
  
  /** テーブルカラムモデルの取得 */
  def columnModel: TableColumnModel = eventColumnModel
  
  /** 現在表示されているカラムの識別子を取得 */
  def visibledColumns: IndexedSeq[String] = {
    import collection.JavaConverters._
    
    withReadLock(visibledColumnList) { list =>
      list.asScala.toIndexedSeq
    }
  }
  
  /** 表示するカラムを指定 */
  def visibledColumns_=(newIdentifiers: Seq[String]) {
    import collection.JavaConverters._
    val jIdentifiers = newIdentifiers.asJava
    
    withWriteLock(visibledColumnList) { list =>
      GlazedLists.replaceAll(list, jIdentifiers, true)
    }
  }
  
  /** テーブルカラムを作成 */
  protected def createTableColumn(modelIndex: Int): TableColumn = {
    val columName = tableFormat.getColumnName(modelIndex)
    val column = new TableColumn(modelIndex)
    column setHeaderValue columName
    column setIdentifier columName
    column
  }
}

object DataTableModel {
  import javax.swing.JTable
  import FunctionList.Function
  
  /** JTable ビューとモデルを接続する */
  def connect(model: DataTableModel[_], table: JTable) {
    table setAutoCreateColumnsFromModel false
    table setModel model.tableModel
    table setColumnModel model.columnModel
    table setSelectionModel model.selectionModel
  }
  
  /**
   * {@code TableColumn} から識別子に変換する関数。
   */
  private class TableColumnIdentifierConvertFunction(tableFormat: TableFormat[_])
      extends Function[TableColumn, String] {
    def evaluate(column: TableColumn): String = {
      tableFormat.getColumnName(column.getModelIndex)
    }
  }
  
  /**
   * 識別子 から {@code TableColumn} に変換する関数。
   */
  private class IdentifierTableColumnConvertFunction(
    identifierTableColumnMap: Map[String, TableColumn]
  ) extends Function[String, TableColumn] {
    def evaluate(identifier: String): TableColumn = {
      identifierTableColumnMap(identifier)
    }
  }
}
