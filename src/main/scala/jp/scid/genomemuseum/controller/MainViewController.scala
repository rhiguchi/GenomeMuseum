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
import jp.scid.gui.control.{ViewValueConnector, StringPropertyBinder, DocumentTextController}
import model.{MuseumSchema, ExhibitRoom, UserExhibitRoom, MuseumExhibit, ExhibitRoomModel,
  UserExhibitRoomService, MuseumExhibitService, MuseumStructure}
import jp.scid.motifviewer.gui.MotifViewerController

/**
 * 主画面の操作を担うクラスです。
 * 
 * 主画面は [[jp.scid.genomemuseum.controller.ExhibitRoomListController]] でソースリストを、
 * [[jp.scid.genomemuseum.controller.MuseumExhibitListController]] でデータリストをそれぞれ管理します。
 * `bind()` によって、[[jp.scid.genomemuseum.view.MainView]] はこれらコントローラとも結合されます。
 * 
 * ソースリストの選択項目に応じて、データリストの表示が変化します。
 * ローカルに保存された展示物を表示する `LOCAL` モードと、ウェブから検索を行う `NCBI` モードがあります。
 * これら表示モードは `setContentsMode(MainView.ContentsMode)` で変更できます。
 */
class MainViewController extends GenomeMuseumController {
  import MainViewController._
  
  // プロパティ
  /** 検索フィールド文字列 */
  val searchText = new ProxyValueModel[String]
  
  /** このコントローラの表題 */
  val title = new ProxyValueModel[String]
  
  /** 現在のデータビューの表示モード */
  val contentsMode = ValueModels.newValueModel(ContentsMode.LOCAL)
  
  /** ソースリストモデル */
  private val museumStructure = new MuseumStructure()
  
  /** データモデル */
  private var currentMuseumSchema: Option[MuseumSchema] = None
  
  // コントローラ
  /** 検索フィールドコントローラ */
  protected[controller] val searchTextController = new DocumentTextController(searchText)
  
  /** 部屋リスト操作 */
  protected[controller] val exhibitRoomListController = new ExhibitRoomListController(museumStructure)
  
  /** 展示物リスト操作 */
  protected[controller] val museumExhibitController = new MuseumExhibitListController
  
  /** ウェブ検索操作 */
  protected[controller] val webServiceResultController = new WebServiceResultController
  
  /** データテーブルの現在適用するモデル */
  private val sourceSelectionHandler = EventListHandler(exhibitRoomListController.getSelectedPathList) {
    case Seq(path, _*) => 
      import collection.JavaConverters._
      updateRoomContents(path.asScala.last)
    case _ => exhibitRoomListController.selectLocalSource()
  }
  
  /** スキーマの取得 */
  def museumSchema = currentMuseumSchema.get
  
  /** スキーマの設定 */
  def museumSchema_=(schema: MuseumSchema) {
    currentMuseumSchema = Option(schema)
    museumStructure.userExhibitRoomService = Some(schema.userExhibitRoomService)
    exhibitRoomListController.selectLocalSource()
  }
  
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
  def updateRoomContents(newRoom: ExhibitRoom) {
    implicit def roomService = museumStructure.userExhibitRoomService.get
    
    val webSource = museumStructure.webSource
    newRoom match {
      case `webSource` => setContentsMode(ContentsMode.NCBI)
      case _ =>
        //ソースリスト項目選択
        currentMuseumSchema foreach { museumSchema =>
          val exhibits = newRoom match {
            case room: ExhibitRoomModel => museumExhibitController setModel room
            case _ =>
          }
        }
        setContentsMode(ContentsMode.LOCAL)
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
    searchTextController.setModel(view.quickSearchField.getDocument)
    searchTextController.bindTextComponent(view.quickSearchField)
    
    // データリストの表示モードの変更
    val contentsModeHandler = new ContentsModeHandler(view)
    contentsModeHandler.setModel(contentsMode)
    
    // ボタンアクションの結合
    bindAction(view.addListBox -> exhibitRoomListController.addBasicRoomAction,
      view.addSmartBox -> exhibitRoomListController.addSamrtRoomAction,
      view.addBoxFolder -> exhibitRoomListController.addGroupRoomAction,
      view.removeBoxButton -> exhibitRoomListController.removeSelectedUserRoomAction)
  }
  
  /**
   * コンテンツモードを変更する。
   * 
   * 関連するモデルやデータビューの表示が変化する。
   */
  def setContentsMode(newMode: ContentsMode) {
    // 検索フィールドのモデル変更
    val newSearchTextModel = newMode match {
      case ContentsMode.LOCAL => museumExhibitController.searchText
      case ContentsMode.NCBI => webServiceResultController.searchTextModel
    }
    searchText.setSubject(newSearchTextModel)
    
    // 表題のモデル変更
    val titleModel = newMode match {
      case ContentsMode.LOCAL => museumExhibitController.title
      case ContentsMode.NCBI => webServiceResultController.taskMessage
    }
    title.setSubject(titleModel)
    
    // プロパティへ適用
    contentsMode.setValue(newMode)
  }
}

object MainViewController {
  /**
   * コンテンツモードを更新するハンドラ
   */
  class ContentsModeHandler(view: MainView) extends ViewValueConnector[MainView, ContentsMode](view) {
    def updateView(view: MainView, mode: ContentsMode) {
      view setContentsMode mode
    }
  }
}
