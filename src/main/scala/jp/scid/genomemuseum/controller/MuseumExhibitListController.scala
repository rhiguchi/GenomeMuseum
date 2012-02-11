package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.{JTable, JTextField, JComponent, TransferHandler}

import org.jdesktop.application.Action

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.{ValueChange, DataListSelectionChanged}
import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.genomemuseum.{view, model, gui}
import gui.{ExhibitTableModel, PublisherScheduleTaskAdapter}
import model.{UserExhibitRoom, MuseumExhibit,
  MutableMuseumExhibitListModel, MuseumExhibitListModel}

/**
 * 展示物のテーブル表示と、フィルタリング、テーブルに表示されている項目の
 * 情報を表示する機能を提供する操作クラス。
 */
class MuseumExhibitListController extends MuseumExhibitController {
  private val ctrl = GenomeMuseumController(this);
  
  // モデル
  
  // コントローラ
  /** 項目削除アクション */
  def tableDeleteAction = Some(removeSelectionAction.peer)
  /** 転送ハンドラ */
  val tableTransferHandler = new MuseumExhibitListTransferHandler(this)
  /** ローカルソースの選択項目を除去するアクション */
  val removeSelectionAction = ctrl.getAction("removeSelections")
  
  /**
   * 選択項目を削除する
   */
  @Action(name="removeSelections")
  def removeSelections() {
    import collection.JavaConverters._
    
    getModel match {
      case model: MutableMuseumExhibitListModel => 
        getSelectionModel.getSelected.asScala.toList foreach model.remove
      case _ =>
    }
  }
  
  /** 読み込みマネージャを返す */
  def loadManager = tableTransferHandler.exhibitLoadManager
  
  /** 読み込みマネージャを設定する */
  def loadManager_=(manager: Option[MuseumExhibitLoadManager]) =
    tableTransferHandler.exhibitLoadManager = manager
}
