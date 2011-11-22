package jp.scid.genomemuseum.controller

import javax.swing.{JFrame, JTree, JTextField}

import jp.scid.gui.event.ValueChange
import jp.scid.gui.DataModel.Connector
import jp.scid.genomemuseum.{view, model, gui, GenomeMuseumGUI}
import view.MainView
import model.{MuseumSchema, ExhibitRoom, UserExhibitRoom, MuseumExhibit}

class MainViewController(
  private[controller] val mainView: MainView
) {
  import MainViewController.TableSource._
  // ビュー
  /** ソースリストショートカット */
  private def sourceList = mainView.sourceList
  private def contentViewerSplit = mainView.dataListContentSplit
  private def dataTable = mainView.dataTable
  private def quickSearchField = mainView.quickSearchField
  private def statusField = mainView.statusLabel
  private def progressView = mainView.loadingIconLabel
  private def fileContentView = mainView.fileContentView
  
  // コントローラ
  /** ソースリスト用 */
  val sourceListCtrl = new ExhibitRoomListController(mainView.sourceList)
  
  // データリスト用
  /** MuseumExhibit 表示用 */
  private[controller] val museumExhibitListCtrl = new MuseumExhibitListController(dataTable,
    quickSearchField, statusField, fileContentView)
  /** WebService 表示用 */
  private[controller] val webServiceResultCtrl = new WebServiceResultController(dataTable,
    quickSearchField, statusField, progressView)
  
  // モデル
  /** 現在のスキーマ */
  private var currentSchema: Option[MuseumSchema] = None
  /** データテーブルの現在の表示モード */
  private var currentTableSource: TableSource = LocalSource
  /** データリストテーブルの結合 */
  private var currentConnectors: List[Connector] = Nil
  
  // モデルバインド
  /** ソースリスト項目選択 */
  sourceListCtrl.selectedRoom.reactions += {
    case ValueChange(_, _, newRoom: ExhibitRoom) =>
      setRoomContentsTo(newRoom)
  }
  // データリストコントローラと結合
  updateTableSource()
  
  // アクションバインディング
  setActionTo(mainView.addListBox -> sourceListCtrl.addBasicRoomAction,
    mainView.addSmartBox -> sourceListCtrl.addSamrtRoomAction,
    mainView.addBoxFolder -> sourceListCtrl.addGroupRoomAction,
    mainView.removeBoxButton -> sourceListCtrl.removeSelectedUserRoomAction)
  
  private def setActionTo(binds: (javax.swing.AbstractButton, swing.Action)*) {
    binds foreach { pair => pair._1 setAction pair._2.peer }
  }
  
  // プロパティ
  /** 現在のデータモデルを取得設定 */
  def dataSchema = currentSchema.get
  
  /** ソースリストやデータリストの表示に使用するデータモデルを設定 */
  def dataSchema_=(newSchema: MuseumSchema) {
    currentSchema = Option(newSchema)
    currentSchema foreach { dataSchema =>
      // ソースリスト
      sourceListCtrl.userExhibitRoomService = dataSchema.userExhibitRoomService
    }
  }
  
  /** 読み込み管理オブジェクトを取得 */
  def loadManager = museumExhibitListCtrl.loadManager
  
  /** 読み込み管理オブジェクトを設定 */
  def loadManager_=(newManager: MuseumExhibitLoadManager) {
    museumExhibitListCtrl.loadManager = newManager
  }
  
  /** 表示モードを取得 */
  private def tableSource = currentTableSource
  
  /** 表示モードを設定 */
  private def tableSource_=(newSource: TableSource) {
    if (currentTableSource != newSource) {
      currentTableSource = newSource
      updateTableSource()
    }
  }
  
  /** データテーブルの表示状態を変更する */
  private def updateTableSource() {
    // 結合解除
    currentConnectors.foreach(_.release())
    
    currentConnectors = tableSource match {
      case LocalSource => museumExhibitListCtrl.bind()
      case WebSource => webServiceResultCtrl.bind()
    }
  }
  
  /**
   * データテーブル領域に表示するコンテンツを設定する
   * 通常は、ソースリストの選択項目となる
   */
  private def setRoomContentsTo(newRoom: ExhibitRoom) {
    if (newRoom == sourceListCtrl.sourceStructure.webSource) {
      tableSource = WebSource
    }
    else {
      // データテーブルに指定
      museumExhibitListCtrl.dataService = newRoom match {
        case newRoom: UserExhibitRoom => dataSchema.roomExhibitService(newRoom)
        case _ => dataSchema.museumExhibitService
      }
      tableSource = LocalSource
    }
  }
}

object MainViewController {
  /**
   * ビューに表示するデータソースの種類
   */
  private object TableSource extends Enumeration {
    type TableSource = Value
    val LocalSource = Value
    val WebSource = Value
  }
}
