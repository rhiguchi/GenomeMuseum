package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField, JLabel, JComponent, TransferHandler}

import jp.scid.gui.ValueHolder
import jp.scid.gui.table.DataTableModel
import jp.scid.gui.event.ValueChange

abstract class DataListController(
  dataTable: JTable,
  quickSearchField: JTextField,
  statusField: JLabel
) {
  /** テーブルモデル */
  private[controller] def tableModel: DataTableModel[_]
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
    DataTableModel.connect(tableModel, dataTable)
    dataTable setTransferHandler tableTransferHandler
    dataTable.getActionMap.put("delete", removeSelectionAction.peer)
    
    val searchConn = ValueHolder.connect(searchTextModel, quickSearchField)
    val statConn = ValueHolder.connect(statusTextModel, statusField, "text")
    List(searchConn, statConn)
  }
}

