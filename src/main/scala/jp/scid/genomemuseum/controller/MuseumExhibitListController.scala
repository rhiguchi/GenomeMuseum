package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.{JTable, JTextField, JComponent, TransferHandler}

import org.jdesktop.application.Action

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.{ValueChange, DataListSelectionChanged}
import jp.scid.gui.table.DataTableModel
import jp.scid.genomemuseum.{view, model, gui}
import gui.{ExhibitTableModel, PublisherScheduleTaskAdapter}
import model.{UserExhibitRoom, MuseumExhibit, MuseumExhibitService, DefaultMuseumExhibitTransferData}
import DataListController.View

/**
 * 展示物のテーブル表示と、フィルタリング、テーブルに表示されている項目の
 * 情報を表示する機能を提供する操作クラス。
 */
class MuseumExhibitListController(
  exhibitService: MuseumExhibitService, view: View
) extends DataListController(view) {
  /**
   * 読み込みマネージャを利用してクラスを作成する
   */
  def this(exhibitService: MuseumExhibitService, view: View, loadManager: MuseumExhibitLoadManager) {
    this(exhibitService, view)
    this.loadManager = Option(loadManager)
  }
  
  // モデル
  /** 表に表示する展示物の部屋。{@code exhibitService} よりから取得に利用される。 */
  val userExhibitRoom = new ValueHolder[Option[UserExhibitRoom]](None)
  /** テーブルの選択項目 */
  val tableSelection = new ValueHolder(List.empty[MuseumExhibit])
  /** テーブルモデル */
  val tableModel = new ExhibitTableModel(exhibitService)
  
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
  
  /** 転送ハンドラ実装 */
  private class MyTransferHandler extends MuseumExhibitListTransferHandler {
    import TransferHandler.TransferSupport
    
    override def importFiles(files: Seq[File], targetRoom: Option[UserExhibitRoom]) = {
      files foreach (f => loadExhibit(f, targetRoom))
      true
    }
    
    override def importExhibits(exhibits: Seq[MuseumExhibit], targetRoom: UserExhibitRoom) = {
      exhibits map (_.asInstanceOf[exhibitService.ElementClass]) foreach
        (e => exhibitService.addElement(targetRoom, e))
      true
    }
    
    override def getTargetRooom(ts: TransferSupport): Option[UserExhibitRoom] =
      userExhibitRoom()
    
    override def createTransferable(c: JComponent) = {
      new DefaultMuseumExhibitTransferData(tableSelection(), userExhibitRoom())
    }
  }
  
  /** モデルの結合を行う */
  private def bindModels() {
    // データサービスの更新をテーブルに適用する
    userExhibitRoom.reactions += {
      case ValueChange(_, _, _) => tableModel.userExhibitRoom = userExhibitRoom()
    }
    
    // 選択行の保持と削除アクションの有効性の変化
    tableModel.reactions += {
      case DataListSelectionChanged(_, false, selections) =>
        tableSelection := selections.collect { case e: MuseumExhibit => e }
        updateRemoveActionEnabled()
    }
    
    // テーブルフィルタリングのための結合
    searchTextModel.reactions += {
      case ValueChange(_, _, text: String) => tableModel.filterText = text
    }
  }
  
  /** 選択項目が変化した際の処理 */
  private def updateRemoveActionEnabled() {
    // 行が選択されているときのみ削除アクションが有効化
    removeSelectionAction.enabled = tableSelection().nonEmpty
  }
  
  bind()
  bindModels()
}
