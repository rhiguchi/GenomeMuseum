package jp.scid.genomemuseum.controller

import javax.swing.{JFrame, JTree, JTextField, JComponent, JProgressBar, JLabel}

import jp.scid.gui.ValueHolder
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
  
  private def loadingView = mainView.fileLoadingActivityPane
  
  // コントローラ
  /** ソースリスト用 */
  val sourceListCtrl = new ExhibitRoomListController(mainView.sourceList)
  
  // データリスト用
  /** MuseumExhibit 表示用 */
  private[controller] val museumExhibitListCtrl = new MuseumExhibitListController(dataTable,
    quickSearchField)
  /** WebService 表示用 */
  private[controller] val webServiceResultCtrl = new WebServiceResultController(dataTable,
    quickSearchField)
  /** ファイル読み込み表示 */
  private[controller] val fileLoadingProgressHandler = new FileLoadingProgressViewHandler(loadingView,
      mainView.fileLoadingProgress, mainView.fileLoadingStatus)
  /** コンテントビューワー */
  private val contentViewer = new FileContentViewer(fileContentView)
  
  // モデル
  /** 現在のスキーマ */
  private var currentSchema: Option[MuseumSchema] = None
  /** 現在の読み込みマネージャ */
  private var currentLoadManager: Option[MuseumExhibitLoadManager] = None
  /** データテーブルの現在の表示モード */
  private var currentTableSource: TableSource = LocalSource
  /** データリストテーブルの結合 */
  private var currentConnectors: List[Connector] = Nil
  /** ソースリストの選択項目 */
  private[controller] def selectedRoom = sourceListCtrl.selectedRoom
  /** データテーブルの選択項目 */
  private var contentViewItem: Option[MuseumExhibit] = None
  /** このコントローラを表すタイトル */
  val title = new ValueHolder("")
  
  // モデルバインド
  /** ソースリスト項目選択 */
  selectedRoom.reactions += {
    case ValueChange(_, _, _) => updateRoomContents()
  }
  /** ファイルソース表示 */
  museumExhibitListCtrl.tableSelection.reactions += {
    case ValueChange(_, _, newValue: Seq[_]) =>
      contentViewItem = newValue.headOption.collect { case e: MuseumExhibit => e }
      updateContentView()
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
      // データテーブル
      updateRoomContents()
    }
  }
  
  /** 読み込み管理オブジェクトを取得 */
  def loadManager = currentLoadManager.get
  
  /** 読み込み管理オブジェクトを設定 */
  def loadManager_=(newManager: MuseumExhibitLoadManager) {
    currentLoadManager = Option(newManager)
    fileLoadingProgressHandler.listenTo(newManager)
    fileLoadingProgressHandler.updateViews()
    museumExhibitListCtrl.loadManager = newManager
    webServiceResultCtrl.loadManager = currentLoadManager
  }
  
  /** ファイルストレージ取得 */
  private def exhibitStorage = currentLoadManager.flatMap(_.museumExhibitStorage)
  
  class FileLoadingProgressViewHandler(contentPane: JComponent, progressBar: JProgressBar, statusLabel: JLabel) extends swing.Reactor {
    import MuseumExhibitLoadManager._
    import swing.Reactions.Reaction
    import swing.event.Event
    import java.io.File
    
    private var currentLoader: Option[MuseumExhibitLoadManager] = None
    
    private var inProgress = false
    private var currentFile: Option[File] = None
    private var finishedCount = 0
    private var totalCount = 0
    
    reactions += {
      case Started() =>
        inProgress = true
        updateViews()
      case ProgressChange(file, finished, total) =>
        currentFile = Option(file)
        finishedCount = finished
        totalCount = total
        updateViews()
      case Done() =>
        currentFile = None
        inProgress = false
        updateViews()
    }
    
    def updateViews() {
      contentPane.setVisible(inProgress)
      statusLabel.setText(statusLabelText)
      
      progressBar.setMaximum(totalCount)
      progressBar.setValue(finishedCount)
      progressBar.setIndeterminate(isIndeterminate)
    }
    
    protected def statusLabelText = {
      val fileNameLabel = currentFile.map(_.getName + " を").getOrElse("")
      "%s読み込み中... [%d / %d]".format(fileNameLabel, finishedCount, totalCount)
    }
    
    protected def isIndeterminate = {
      inProgress && totalCount <= finishedCount
    }
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
      case LocalSource =>
        ValueHolder.connect(museumExhibitListCtrl.statusTextModel, title) ::
        museumExhibitListCtrl.bind()
      case WebSource =>
        ValueHolder.connect(webServiceResultCtrl.statusTextModel, title) ::
        webServiceResultCtrl.bind()
    }
  }
  
  /**
   * データテーブル領域に表示するコンテンツを設定する
   * 通常は、ソースリストの選択項目となる
   */
  private def updateRoomContents() {
    if (selectedRoom() == sourceListCtrl.sourceStructure.webSource) {
      tableSource = WebSource
    }
    else {
      // データテーブルに指定
      museumExhibitListCtrl.dataService = selectedRoom() match {
        case newRoom: UserExhibitRoom => dataSchema.roomExhibitService(newRoom)
        case _ => dataSchema.museumExhibitService
      }
      tableSource = LocalSource
    }
  }
  
  /**
   * コンテンツビューのモデルを変更する。
   */
  private def updateContentView() {
    // ビューワー表示
    val source = (contentViewItem, exhibitStorage) match {
      case (Some(exhibit), Some(storage)) => storage.getSource(exhibit) match {
        case None => Iterator.empty
        case Some(source) => io.Source.fromURL(source).getLines
      }
      case _ => Iterator.empty
    }
    
    contentViewer.source = source
    if (mainView.isContentViewerClosed)
      mainView.openContentViewer(200)
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
