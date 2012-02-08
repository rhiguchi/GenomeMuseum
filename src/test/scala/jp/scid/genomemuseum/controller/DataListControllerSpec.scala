package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField}

import org.specs2._

class DataListControllerSpec extends Specification with mock.Mockito {
  def is = "DataListController" ^ end
  
  def canBindToTable(ctrl: => DataListController) =
    "tableModel の適用" ! bindTableModel(ctrl).tableModel ^
    "tableColumnModel の適用" ! bindTableModel(ctrl).tableColumnModel ^
    "selectionModel の適用" ! bindTableModel(ctrl).selectionModel ^
    "ドラッグ可能" ! bindTableModel(ctrl).setDragEnabled ^
    "行上ドロップ" ! bindTableModel(ctrl).setDropMode ^
    "転送ハンドラ" ! bindTableModel(ctrl).setTransferHandler ^
    "親コンポーネントがある時は親へ転送ハンドラ" ! bindTableModel(ctrl).setTransferHandlerToParent ^
    "削除アクション設定" ! bindTableModel(ctrl).actionMapDelete ^
    "テーブルヘッダにクリックソート" ! todo ^
    bt
  
  // dataTable 結合
  def bindTableModel(ctrl: DataListController) = new {
    val parent = mock[javax.swing.JComponent]
    val table = spy(new JTable)
    table.getParent returns parent
    ctrl.bindTable(table)
    
    private def tm = ctrl.tableModel
    
    def tableModel = there was one(table).setModel(tm.tableModel)
    def tableColumnModel = there was one(table).setColumnModel(tm.columnModel)
    def selectionModel = there was one(table).setSelectionModel(tm.selectionModel)
    def setDragEnabled = there was one(table).setDragEnabled(ctrl.isTableDraggable)
    def setDropMode = there was one(table).setDropMode(javax.swing.DropMode.INSERT_ROWS)
    def setTransferHandler = there was one(table).setTransferHandler(ctrl.tableTransferHandler)
    def setTransferHandlerToParent = there was one(parent)
      .setTransferHandler(ctrl.tableTransferHandler)
    def actionMapDelete = table.getActionMap.get("delete") must_==
      ctrl.tableDeleteAction.getOrElse(null)
  }
}
