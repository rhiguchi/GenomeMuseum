package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField, JLabel, JComponent, TransferHandler, DropMode}

import jp.scid.gui.ValueHolder
import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.gui.event.ValueChange

abstract class DataListController(
  dataTable: JTable,
  quickSearchField: JTextField,
  statusField: JLabel
) {
  /** テーブルモデル */
  private[controller] def tableModel: DataTableModel[_] with TableColumnSortable[_]
  /** 検索文字列モデル */
  private[controller] val searchTextModel: ValueHolder[String] = new ValueHolder("")
  /** 状態文字列モデル */
  private[controller] val statusTextModel: ValueHolder[String] = new ValueHolder("")
  /** 転送ハンドラ */
  private[controller] val tableTransferHandler = new TransferHandler("")
  
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
    val statConn = ValueHolder.connect(statusTextModel, statusField, "text")
    List(headerConn, searchConn, statConn)
  }
}

