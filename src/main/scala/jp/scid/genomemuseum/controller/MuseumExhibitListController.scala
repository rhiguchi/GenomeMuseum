package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField, JComponent, TransferHandler}

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.{ValueChange, DataListSelectionChanged}
import jp.scid.gui.table.DataTableModel
import jp.scid.genomemuseum.{view, model, gui}
import gui.ExhibitTableModel
import model.{MuseumExhibit, MuseumExhibitService}

/**
 * 展示物のテーブル表示と、フィルタリング、テーブルに表示されている項目の
 * 情報を表示する機能を提供する操作クラス。
 */
class MuseumExhibitListController(
  private[controller] val dataTable: JTable,
  private[controller] val quickSearchField: JTextField
) extends DataListController(dataTable, quickSearchField) {
  // モデル
  /** テーブルの選択項目 */
  val tableSelection = new ValueHolder(List.empty[MuseumExhibit])
  /** ローカルデータのテーブルモデル */
  private[controller] val tableModel = {
    val model = createTableModel
    model.reactions += {
      // 選択項目変化
      case DataListSelectionChanged(_, false, selections) =>
        tableSelection := selections.collect { case e: MuseumExhibit => e }
        tableSelectionChanged()
    }
    
    /** テーブルフィルタリング */
    searchTextModel.reactions += {
      case ValueChange(_, _, text: String) =>
        model filterWith text
    }
    model
  }
  
  // コントローラ
  /** 転送ハンドラ */
  override private[controller] val tableTransferHandler =
    new MuseumExhibitListTransferHandler(tableModel)
  /** ローカルソースの選択項目を除去するアクション */
  override val removeSelectionAction = swing.Action("Remove") {
    tableModel.removeSelections()
  }
  removeSelectionAction.enabled = false
  
  // プロパティ
  /** 現在のデータサービスを取得 */
  def dataService = tableModel.dataService
  
  /** テーブルのデータサービスを設定 */
  def dataService_=(newService: MuseumExhibitService) {
    tableModel.dataService = newService
  }
  
  /** 現在の読み込み管理オブジェクトを取得 */
  def loadManager = tableTransferHandler.loadManager.get
  
  /** 転送ハンドラに読み込み管理オブジェクトを設定 */
  def loadManager_=(newManager: MuseumExhibitLoadManager) {
    tableTransferHandler.loadManager = Option(newManager)
  }
  
  /** 選択項目が変化した際の処理 */
  private def tableSelectionChanged() {
    // 行が選択されているときのみ削除アクションが有効化
    removeSelectionAction.enabled = tableSelection().nonEmpty
  }
  
  private def museumExhibitStorage = tableTransferHandler.loadManager
    .flatMap(_.museumExhibitStorage)
  
  /** テーブルモデル作成メソッド */
  private[controller] def createTableModel = new ExhibitTableModel
}
