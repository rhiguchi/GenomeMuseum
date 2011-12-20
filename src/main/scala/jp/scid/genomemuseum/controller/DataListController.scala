package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField, JLabel, JComponent, TransferHandler, DropMode}

import jp.scid.gui.ValueHolder
import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.gui.event.ValueChange

object DataListController {
  case class View(dataTable: JTable, quickSearchField: JTextField)
}

/**
 * テーブルと検索フィールドの操作反応基底クラス。
 */
abstract class DataListController(
  application: ApplicationActionHandler,
  view: DataListController.View
) extends GenomeMuseumController(application) {
  // ビューショートカット
  private def dataTable = view.dataTable
  private def quickSearchField = view.quickSearchField
  
  /** テーブルモデル */
  def tableModel: DataTableModel[_] with TableColumnSortable[_]
  /** 検索文字列モデル */
  val searchTextModel: ValueHolder[String] = new ValueHolder("")
  /** 状態文字列モデル */
  val statusTextModel: ValueHolder[String] = new ValueHolder("")
  /** 転送ハンドラ */
  val tableTransferHandler = new TransferHandler("")
  
  // アクション
  /** 項目削除アクション */
  val removeSelectionAction = new swing.Action("Remove") {
    def apply() {}
    enabled = false
  }
  
  /**
   * ビューとモデルの結合を行う。
   */
  def bind() = {
    dataTable.setDragEnabled(true)
    dataTable.setDropMode(DropMode.INSERT_ROWS)
    DataTableModel.connect(tableModel, dataTable)
    dataTable setTransferHandler tableTransferHandler
    dataTable.getParent match {
      case scrollPane: JComponent => scrollPane setTransferHandler tableTransferHandler
      case _ =>
    }
    dataTable.getActionMap.put("delete", removeSelectionAction.peer)
    
    val headerConn = TableColumnSortable.connect(tableModel, dataTable.getTableHeader)
    val searchConn = ValueHolder.connect(searchTextModel, quickSearchField)
    List(headerConn, searchConn)
  }
}

