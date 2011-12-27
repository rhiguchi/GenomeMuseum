package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.{JTable, JTextField, JComponent, TransferHandler}

import org.jdesktop.application.Action

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.{ValueChange, DataListSelectionChanged}
import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.genomemuseum.{view, model, gui}
import gui.{ExhibitTableModel, PublisherScheduleTaskAdapter}
import model.{UserExhibitRoom, MuseumExhibit, MuseumExhibitService, DefaultMuseumExhibitTransferData}

/**
 * 展示物のテーブル表示と、フィルタリング、テーブルに表示されている項目の
 * 情報を表示する機能を提供する操作クラス。
 */
class MuseumExhibitListController(
  val exhibitService: MuseumExhibitService
) extends DataListController {
  /**
   * 読み込みマネージャを利用してクラスを作成する
   */
  def this(exhibitService: MuseumExhibitService, loadManager: MuseumExhibitLoadManager) {
    this(exhibitService)
    this.loadManager = Option(loadManager)
  }
  
  // モデル
  /** テーブルがドラッグ可能設定 */
  override protected[controller] def isTableDraggable = true
  
  /** テーブルの選択項目 */
  val tableSelection = new ValueHolder(List.empty[MuseumExhibit])
  
  /** テーブルモデル */
  lazy val tableModel: ExhibitTableModel = new ExhibitTableModel(exhibitService) {
    // 選択行の保持と削除アクションの有効性の変化
    reactions += {
      case DataListSelectionChanged(_, false, selections) =>
        tableSelection := selections.collect { case e: MuseumExhibit => e }
        updateRemoveActionEnabled()
    }
  }
  
  // コントローラ
  /** 項目削除アクション */
  override def tableDeleteAction = Some(removeSelectionAction.peer)
  /** 転送ハンドラ */
  override val tableTransferHandler: MuseumExhibitListTransferHandler = new MyTransferHandler
  /** ローカルソースの選択項目を除去するアクション */
  val removeSelectionAction = {
    val action = getAction("removeSelections")
    // 最初は選択項目が無いため
    action.enabled = false
    action
  }
  
  // プロパティ
  /** 現在設定されている部屋を取得する */
  def userExhibitRoom = tableModel.userExhibitRoom
  
  /** テーブルに表示する部屋を設定する */
  def userExhibitRoom_=(newRoom: Option[UserExhibitRoom]) {
    tableModel.userExhibitRoom = newRoom
  }
  
  /**
   * 選択項目を削除する
   */
  @Action(name="removeSelections")
  def removeSelections() {
    tableSelection() map (_.asInstanceOf[exhibitService.ElementClass]) foreach exhibitService.remove
  }
  
  /** テーブルフィルタリングを行う */
  protected def searchTextChange(newValue: String) = tableModel.filterText = newValue
  
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
      userExhibitRoom
    
    override def createTransferable(c: JComponent) = {
      new DefaultMuseumExhibitTransferData(tableSelection(), userExhibitRoom)
    }
  }
  
  /** 選択項目が変化した際の処理 */
  private def updateRemoveActionEnabled() {
    // TODO バインディング化
    // 行が選択されているときのみ削除アクションが有効化
    removeSelectionAction.enabled = tableSelection().nonEmpty
  }
}
