package jp.scid.genomemuseum.controller

import java.util.ResourceBundle
import java.io.{File, FileInputStream}
import javax.swing.{JFrame, JTree, JTextField}

import org.jdesktop.application.{Application, Action, ResourceMap}

import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor

import jp.scid.gui.event.{DataListSelectionChanged, DataTreePathsSelectionChanged}
import jp.scid.gui.tree.DataTreeModel
import jp.scid.gui.event.ValueChange
import DataTreeModel.Path
import jp.scid.genomemuseum.{view, model, gui, GenomeMuseumGUI}
import view.{MainView, MainViewMenuBar}
import model.{MuseumSchema, ExhibitRoom, UserExhibitRoom, MuseumStructure, MuseumExhibit}
import gui.{ExhibitTableModel, MuseumSourceModel, WebServiceResultsModel}
import MuseumExhibitListController.TableSource._

class MainViewController(
  mainView: MainView
) {
  // ビュー
  /** ソースリストショートカット */
  private def sourceList = mainView.sourceList
  private def contentViewerSplit = mainView.dataListContentSplit
  private def dataTable = mainView.dataTable
  
  // コントローラ
  /** ソースリスト用 */
  val sourceListCtrl = new ExhibitRoomListController(mainView.sourceList)
  /** データテーブル用 */
  private[controller] val dataTableCtrl = new MuseumExhibitListController(dataTable,
    mainView.quickSearchField)
  
  /** コンテントビューワー */
  private val contentViewer = new FileContentViewer(mainView.fileContentView)
  
  // モデル
  /** 現在のスキーマ */
  private var currentSchema: Option[MuseumSchema] = None
  
  // ソースリスト項目選択
  sourceListCtrl.selectedRoom.reactions += {
    case ValueChange(_, _, newRoom: ExhibitRoom) =>
      setRoomContentsTo(newRoom)
  }
  
  // アクションバインディング
  // ファイルのドラッグ＆ドロップ追加ハンドラ
//  protected val transferHandler = new ExhibitTransferHandler(this)
//  mainView.dataTableScroll.setTransferHandler(transferHandler)
  
  setActionTo(mainView.addListBox -> sourceListCtrl.addBasicRoomAction,
    mainView.addSmartBox -> sourceListCtrl.addSamrtRoomAction,
    mainView.addBoxFolder -> sourceListCtrl.addGroupRoomAction,
    mainView.removeBoxButton -> sourceListCtrl.removeSelectedUserRoomAction)
  
  dataTable.getActionMap.put("delete", dataTableCtrl.removeSelectedExhibitAction.peer)
  
  private def setActionTo(binds: (javax.swing.AbstractButton, swing.Action)*) {
    binds foreach { pair => pair._1 setAction pair._2.peer }
  }
  
  
  /** 現在のデータモデルを取得設定 */
  def dataSchema = currentSchema.get
  
  /** ソースリストやデータリストの表示に使用するデータモデルを設定 */
  def dataSchema_=(newSchema: MuseumSchema) {
    currentSchema = Option(newSchema)
    reloadSchema()
  }
  
  /**
   * データテーブル領域に表示するコンテンツを設定する
   * 通常は、ソースリストの選択項目となる
   */
  private def setRoomContentsTo(newRoom: ExhibitRoom) {
    if (newRoom == sourceListCtrl.sourceStructure.webSource) {
      dataTableCtrl.tableSource = WebSource
    }
    else {
      dataTableCtrl.tableSource = LocalSource
      currentSchema map { dataSchema =>
        dataTableCtrl.localDataService = newRoom match {
          case newRoom: UserExhibitRoom => dataSchema.roomExhibitService(newRoom)
          case _ => dataSchema.museumExhibitService
        }
      }
    }
  }
  
  /** データスキーマからモデルの再設定 */
  private def reloadSchema() {
    dataTableCtrl.localDataService = dataSchema.museumExhibitService
    sourceListCtrl.userExhibitRoomService = dataSchema.userExhibitRoomService
  }
}
