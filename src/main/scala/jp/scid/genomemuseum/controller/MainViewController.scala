package jp.scid.genomemuseum.controller

import java.net.{URI, URL}
import javax.swing.{JFrame, JTree, JTextField, JComponent, JProgressBar, JLabel}

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.ValueChange
import jp.scid.gui.DataModel.Connector
import jp.scid.genomemuseum.{view, model, gui, GenomeMuseumGUI}
import view.MainView
import MainView.ContentsMode
import jp.scid.gui.model.{ProxyValueModel, ValueModels}
import jp.scid.gui.control.{ViewValueConnector, StringPropertyBinder}
import model.{MuseumSchema, ExhibitRoom, UserExhibitRoom, MuseumExhibit,
  UserExhibitRoomService, MuseumExhibitService, MuseumStructure}
import jp.scid.motifviewer.gui.MotifViewerController

/**
 * 主画面の操作を受け付け、操作反応を実行するオブジェクト。
 * 
 * @param sourceListCtrl ソースリスト 表示コントローラ
 * @param museumExhibitListCtrl MuseumExhibit 表示コントローラ
 * @param webServiceResultCtrl WebService 表示ントローラ
 * @param application データやメッセージを取り扱うアプリケーションオブジェクト。
 * @param mainView 表示と入力を行う画面。
 */
class MainViewController extends GenomeMuseumController {
  // コントローラ
  /** 部屋リスト操作 */
  protected[controller] val exhibitRoomListController = new ExhibitRoomListController
  
  /** 展示物リスト操作 */
  val museumExhibitController = new MuseumExhibitListController
  
  // ファイルソース表示
  val contentsMode = ValueModels.newValueModel(ContentsMode.LOCAL)
  
  /** ウェブ検索操作 */
  protected[controller] val webServiceResultController = new WebServiceResultController
  
  // モデル
  /** データテーブルの現在適用するモデル */
  private val sourceSelectionHandler = EventListHandler(exhibitRoomListController.getSelectedNodes) {
    case Seq(room, _*) => updateRoomContents(room)
    case _ =>
  }
  
  /** このコントローラを表すタイトル */
  val title = new ProxyValueModel(museumExhibitController.getTitleModel)
  
  /** 検索フィールド */
  val searchTextModel = new ProxyValueModel(museumExhibitController.getFilterTextModel)
  
  val searchFildBinder = new StringPropertyBinder(searchTextModel)
  
  /** ソースリストモデルを取得 */
  def museumStructure: MuseumStructure = exhibitRoomListController.getModel
  
  /** ソースリストモデルを設定 */
  def museumStructure_=(newModel: MuseumStructure) = exhibitRoomListController setModel newModel
  
  /** 読み込みマネージャの設定 */
  def setExhibitLoadManager(manager: MuseumExhibitLoadManager) {
    exhibitRoomListController.exhibitLoadManager = Some(manager)
    museumExhibitController.loadManager = Some(manager)
    webServiceResultController.loadManager = Some(manager)
  }

  /**
   * データテーブル領域に表示するコンテンツを設定する
   * 通常は、ソースリストの選択項目となる
   */
  private def updateRoomContents(newRoom: ExhibitRoom) {
    implicit def roomService = museumStructure.userExhibitRoomService.get
    
    val webSource = museumStructure.webSource
    newRoom match {
      case `webSource` => contentsMode.setValue(ContentsMode.NCBI)
      case _ =>
        //ソースリスト項目選択
        val exhibits = newRoom match {
          case room: UserExhibitRoom => museumStructure getContent room
          case _ => museumStructure.museumExhibitService.getOrElse(null)
        }
        museumExhibitController setModel exhibits
        contentsMode.setValue(ContentsMode.LOCAL)
    }
  }
  
  /**
   * コンテンツビューのモデルを変更する。
   */
  private def updateExhibitViewContent(newItem: Seq[MuseumExhibit]) {
    val source = newItem match {
      case Seq(exhibit) => exhibit.sourceFile match {
        case Some(file) if file.isFile => io.Source.fromFile(file)
        case _ => exhibit.dataSourceUri.startsWith("jar") match {
          case true => io.Source.fromURL(new URI(exhibit.dataSourceUri).toURL).getLines
          case false => Iterator.empty
        }
      }
      case _ => Iterator.empty
    }
  }
  
  def bind(view: MainView) {
    // ソースリストと結合
    exhibitRoomListController.bindTree(view.sourceList)
    // コンテンツビューと結合
    museumExhibitController.bind(view.exhibitListView)
    // NCBI 検索ビューと結合
    webServiceResultController.bindTable(view.websearchTable)
    // 検索フィールドと結合
    searchFildBinder.bindTextField(view.quickSearchField, view.quickSearchField.getDocument)
    
    // データリストの表示モードの変更
    val contentsModeHandler = new ContentsModeHandler(view)
    contentsModeHandler.setModel(contentsMode)
    
    // ボタンアクションの結合
    bindAction(view.addListBox -> exhibitRoomListController.addBasicRoomAction,
      view.addSmartBox -> exhibitRoomListController.addSamrtRoomAction,
      view.addBoxFolder -> exhibitRoomListController.addGroupRoomAction,
      view.removeBoxButton -> exhibitRoomListController.removeSelectedUserRoomAction)
    
    var currentConnectors: List[Connector] = Nil
  }
  
  /**
   * コンテンツモードを更新するハンドラ
   */
  class ContentsModeHandler(view: MainView) extends ViewValueConnector[MainView, ContentsMode](view) {
    def updateView(view: MainView, mode: ContentsMode) {
      view setContentsMode mode
      // 検索フィールドのモデル変更
      val newSearchTextModel = mode match {
        case ContentsMode.LOCAL => museumExhibitController.getFilterTextModel()
        case ContentsMode.NCBI => webServiceResultController.searchTextModel
      }
      searchTextModel.setSubject(newSearchTextModel)
      
      // 表題のモデル変更
      val titleModel = mode match {
        case ContentsMode.LOCAL => museumExhibitController.getTitleModel
        case ContentsMode.NCBI => webServiceResultController.taskMessage
      }
      title.setSubject(titleModel)
    }
  }
}

object MainViewController {
}
