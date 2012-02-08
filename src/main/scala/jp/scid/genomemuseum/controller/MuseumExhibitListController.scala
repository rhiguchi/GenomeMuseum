package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.{JTable, JTextField, JComponent, TransferHandler}

import org.jdesktop.application.Action

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.{ValueChange, DataListSelectionChanged}
import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.genomemuseum.{view, model, gui}
import gui.{ExhibitTableModel, PublisherScheduleTaskAdapter}
import model.{UserExhibitRoom, MuseumExhibit, DefaultMuseumExhibitTransferData,
  MutableMuseumExhibitListModel, MuseumExhibitListModel}

/**
 * 展示物のテーブル表示と、フィルタリング、テーブルに表示されている項目の
 * 情報を表示する機能を提供する操作クラス。
 */
class MuseumExhibitListController extends MuseumExhibitController {
  private val ctrl = GenomeMuseumController(this);
  
  /**
   * 読み込みマネージャを利用してクラスを作成する
   */
  var loadManager: Option[MuseumExhibitLoadManager] = None
  
  // モデル
  
  // コントローラ
  /** 項目削除アクション */
  def tableDeleteAction = Some(removeSelectionAction.peer)
  /** 転送ハンドラ */
  val tableTransferHandler: MuseumExhibitListTransferHandler = new MyTransferHandler
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
  
  /** 転送ハンドラ実装 */
  private class MyTransferHandler extends MuseumExhibitListTransferHandler {
    import TransferHandler.TransferSupport
    
    override def importFiles(files: Seq[File], targetRoom: Option[UserExhibitRoom]) = {
//      files foreach (f => loadExhibit(f, targetRoom))
      true
    }
    
    override def importExhibits(exhibits: Seq[MuseumExhibit], targetRoom: UserExhibitRoom) = {
//      exhibits map (_.asInstanceOf[exhibitService.ElementClass]) foreach
//        (e => exhibitService.addElement(targetRoom, e))
      true
    }
    
    override def getTargetRooom(ts: TransferSupport): Option[UserExhibitRoom] = getModel.getRoom
    
    override def createTransferable(c: JComponent) = {
      import collection.JavaConverters._
      new DefaultMuseumExhibitTransferData(getSelectionModel.getSelected.asScala, getModel.getRoom)
    }
  }
}
