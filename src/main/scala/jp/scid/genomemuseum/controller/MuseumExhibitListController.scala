package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.{JTable, JTextField, JComponent, TransferHandler}

import org.jdesktop.application.Action

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.{ValueChange, DataListSelectionChanged}
import jp.scid.gui.table.DataTableModel
import jp.scid.genomemuseum.{view, model, gui}
import gui.{ExhibitTableModel, PublisherScheduleTaskAdapter}
import model.{UserExhibitRoom, MuseumExhibit, MuseumExhibitService, MuseumExhibitTransferData}

/**
 * 展示物のテーブル表示と、フィルタリング、テーブルに表示されている項目の
 * 情報を表示する機能を提供する操作クラス。
 */
class MuseumExhibitListController(
  application: ApplicationActionHandler,
  view: DataListController.View
) extends DataListController(application, view) {
  // モデル
  /** 展示物サービス */
  private def museumExhibitService = museumSchema.museumExhibitService
  /** 現在設定されている部屋 */
  val userExhibitRoom = new ValueHolder[Option[UserExhibitRoom]](None)
  /** テーブルの選択項目 */
  val tableSelection = new ValueHolder(List.empty[MuseumExhibit])
  /** テーブルモデル */
  val tableModel = new ExhibitTableModel(museumExhibitService)
  
  // コントローラ
  /** 転送ハンドラ */
  override val tableTransferHandler: MuseumExhibitListTransferHandler = new MyTransferHandler
  /** ローカルソースの選択項目を除去するアクション */
  override val removeSelectionAction = getAction("removeSelections")
  removeSelectionAction.enabled = false
  
  @Action(name="removeSelections")
  def removeSelections() {
//    tableModel.selections foreach dataService.remove
  }
  
  /** 選択項目が変化した際の処理 */
  private def tableSelectionChanged() {
    // 行が選択されているときのみ削除アクションが有効化
    removeSelectionAction.enabled = tableSelection().nonEmpty
  }
  
  /** 転送ハンドラ実装 */
  private class MyTransferHandler extends MuseumExhibitListTransferHandler {
    import TransferHandler.TransferSupport
    
    override def importFiles(files: Seq[File], targetRoom: Option[UserExhibitRoom]) = {
      files foreach loadManager.loadExhibit
      true
    }
    
    override def importExhibits(exhibits: Seq[MuseumExhibit], targetRoom: UserExhibitRoom) = {
      val service = museumExhibitService
      exhibits map (_.asInstanceOf[service.ElementClass]) foreach
        (e => service.addElement(targetRoom, e))
      true
    }
    
    override def getTargetRooom(ts: TransferSupport): Option[UserExhibitRoom] =
      userExhibitRoom()
    
    override def createTransferable(c: JComponent) = {
      MuseumExhibitTransferData(tableSelection(), userExhibitRoom(), fileStorage)
    }
  }
  
  /** モデルの結合を行う */
  private def bindModels() {
    // データサービスの更新をテーブルに適用する
    userExhibitRoom.reactions += {
      case ValueChange(_, _, _) => tableModel.userExhibitRoom = userExhibitRoom()
    }
    
    // 選択項目変化
    tableModel.reactions += {
      case DataListSelectionChanged(_, false, selections) =>
        tableSelection := selections.collect { case e: MuseumExhibit => e }
        tableSelectionChanged()
    }
    
    /** テーブルフィルタリング */
    searchTextModel.reactions += {
      case ValueChange(_, _, text: String) => tableModel.filterText = text
    }
  }
  
  bindModels()
}
