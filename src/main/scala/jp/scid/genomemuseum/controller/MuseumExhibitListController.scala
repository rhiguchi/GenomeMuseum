package jp.scid.genomemuseum.controller

import java.io.File

import javax.swing.{JTable, JTextField, JComponent, TransferHandler, DropMode}

import org.jdesktop.application.Action

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.{ValueChange, DataListSelectionChanged}
import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.genomemuseum.{view, model, gui}
import model.{UserExhibitRoom, MuseumExhibit, FreeExhibitRoomModel,
  MutableMuseumExhibitListModel, MuseumExhibitListModel}
import view.ExhibitListView

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
  
  /** 読み込みマネージャ */
  var loadManager: Option[MuseumExhibitLoadManager] = None
  
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
  
  /** ビューに結合処理を追加するため */
  override def bind(view: ExhibitListView) {
    super.bind(view)
    bindTable(view.dataTable)
  }
  
  /** 転送ハンドラを作成 */
  override def getTransferHandler() = tableTransferHandler
  
  /** ファイルを読み込む */
  def importFile(files: List[File]) = getModel match {
//    case model: FreeExhibitRoomModel =>
//      import collection.JavaConverters._
//      files.asScala foreach (file => loadManager.get.load(model, file))
//      true
    case  _ => false
  }
  
  def addElements(sourceRows: List[MuseumExhibit]) = getModel match {
//    case model: FreeExhibitRoomModel => sourceRows map mode.addContent contains true
    case _ => false
  }
}
