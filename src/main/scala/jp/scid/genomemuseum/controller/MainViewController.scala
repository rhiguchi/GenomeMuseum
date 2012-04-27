package jp.scid.genomemuseum.controller

import java.net.{URI, URL}
import javax.swing.{JDialog, AbstractButton}

import jp.scid.gui.model.TransformValueModel
import jp.scid.genomemuseum.{view, model, gui, GenomeMuseumGUI}
import view.{MainView, ColumnVisibilitySetting}
import MainView.ContentsMode
import jp.scid.gui.model.{ProxyValueModel, ValueModels}
import jp.scid.gui.control.{ViewValueConnector, StringPropertyBinder, DocumentTextController,
  TableColumnEditor}
import model.{MuseumSchema, ExhibitRoom, MuseumExhibit, ExhibitRoomModel, MuseumSpace,
  MuseumExhibitService, MuseumStructure}
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
  
  /** データテーブルに表示する展示室 */
  private val exhibitRoom = new TransformValueModel[MuseumSpace, ExhibitRoomModel](RoomModelTransformer)
  
  // コントローラ
  /** 検索フィールドコントローラ */
  protected[controller] val searchTextController = new DocumentTextController(searchText)
  
  /** 部屋リスト操作 */
  protected[controller] val exhibitRoomListController: ExhibitRoomListController = new ExhibitRoomListController(museumStructure)
  
  /** 展示物リスト操作 */
  protected[controller] val museumExhibitController = new MuseumExhibitListController
  
  /** ウェブ検索操作 */
  protected[controller] val webServiceResultController = new WebServiceResultController
  
  /** 列設定ダイアログ操作 */
  private[controller] val exhibitTableColumnEditor = new TableColumnEditor(museumExhibitController.columnModel)
  
  // モデル
  /** データテーブルの現在適用するモデル */
  private val sourceSelectionHandler = ValueChangeHandler(exhibitRoomListController.getSelection)(updateRoomContents)
  
  /** スキーマの取得 */
  def museumSchema = currentMuseumSchema.get
  
  /** スキーマの設定 */
  def museumSchema_=(schema: MuseumSchema) {
    currentMuseumSchema = Option(schema)
    museumStructure.localManagedPavilion = Some(schema.museumExhibitService)
    museumStructure.freeExhibitPavilion = Some(schema.freeExhibitPavilion)
    
    import collection.JavaConverters._
    // museumExhibitService を標準選択項目に
    val mainPavilionPath = museumStructure.pathToRoot(schema.museumExhibitService)
    exhibitRoomListController.setDefaultSelection(mainPavilionPath.asJava)
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
  def updateRoomContents(newRoom: ExhibitRoom) = newRoom match {
    case museumStructure.webSource => setContentsMode(ContentsMode.NCBI)
    case _ =>
      //ソースリスト項目選択
      newRoom match {
        case room: ExhibitRoomModel => museumExhibitController setModel room
        case _ =>
      }
      setContentsMode(ContentsMode.LOCAL)
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
    bindFreeRoomAddingButton(view.addListBox)
    bindSmartRoomAddingButton(view.addSmartBox)
    bindGroupRoomAddingButton(view.addBoxFolder)
    bindRoomRemovingButton(view.removeBoxButton)
  }
  
  def bindFreeRoomAddingButton(button: AbstractButton) =
    button.setAction(exhibitRoomListController.addBasicRoomAction)
  
  def bindSmartRoomAddingButton(button: AbstractButton) =
    button.setAction(exhibitRoomListController.addSamrtRoomAction)
  
  def bindGroupRoomAddingButton(button: AbstractButton) =
    button.setAction(exhibitRoomListController.addGroupRoomAction)
  
  def bindRoomRemovingButton(button: AbstractButton) {
    val icon = button.getIcon
    button.setAction(exhibitRoomListController.getDeleteAction)
    button.setText(null)
    button.setIcon(icon)
  }
  
  /** 列設定ビューと結合 */
  def bindColumnVisibilitySettingView(view: ColumnVisibilitySetting) {
    
  }
  
  /** 列設定ビューダイアログと結合 */
  def bindColumnVisibilitySettingDialog(dialog: JDialog) {
    exhibitTableColumnEditor.bindDialog(dialog)
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
  import TransformValueModel.Transformer
  /**
   * コンテンツモードを更新するハンドラ
   */
  class ContentsModeHandler(view: MainView) extends ViewValueConnector[MainView, ContentsMode](view) {
    def updateView(view: MainView, mode: ContentsMode) {
      view setContentsMode mode
    }
  }
  
  /** MuseumSpace が展示室の時に変換。 */
  object RoomModelTransformer extends Transformer[MuseumSpace, ExhibitRoomModel] {
    def apply(space: MuseumSpace) = space match {
      case room: ExhibitRoomModel => room
      case _ => null
    }
  }
}


object ValueChangeHandler {
  import jp.scid.gui.model.ValueModel
  import jp.scid.gui.control.ValueChangeHandler
  
  def apply[A](model: ValueModel[A])(function: A => Unit): ValueChangeHandler[A] = {
    val handler = new ValueChangeHandler[A] {
      override def valueChanged(newValue: A) {
        function apply newValue
      }
    }
    handler.setModel(model)
    handler
  }
}

