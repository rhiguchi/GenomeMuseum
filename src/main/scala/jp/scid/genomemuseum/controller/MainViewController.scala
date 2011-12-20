package jp.scid.genomemuseum.controller

import javax.swing.{JFrame, JTree, JTextField, JComponent, JProgressBar, JLabel}

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.ValueChange
import jp.scid.gui.DataModel.Connector
import jp.scid.genomemuseum.{view, model, gui, GenomeMuseumGUI}
import view.MainView
import model.{MuseumSchema, ExhibitRoom, UserExhibitRoom, MuseumExhibit}

/**
 * 主画面の操作を受け付け、操作反応を実行するオブジェクト。
 * 
 * @param application データやメッセージを取り扱うアプリケーションオブジェクト。
 * @param mainView 表示と入力を行う画面。
 */
class MainViewController(
  application: ApplicationActionHandler,
  mainView: MainView
) extends GenomeMuseumController(application) {
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
  
  /** データリストコントローラ用ビュー */
  private val dataListView = DataListController.View(dataTable, quickSearchField)
  
  // コントローラ
  /** ソースリスト用 */
  val sourceListCtrl = new ExhibitRoomListController(
    application, mainView.sourceList)
  
  // データリスト用
  /** MuseumExhibit 表示用 */
  val museumExhibitListCtrl = new MuseumExhibitListController(
    application, dataListView)
  /** WebService 表示用 */
  val webServiceResultCtrl = new WebServiceResultController(
    application, dataListView)
  /** ファイル読み込み表示 */
  val fileLoadingProgressHandler = new FileLoadingProgressViewHandler(loadingView,
      mainView.fileLoadingProgress, mainView.fileLoadingStatus)
  /** コンテントビューワー */
  private val contentViewer = new FileContentViewer(fileContentView)
  
  // モデル
  /** データテーブルの現在の表示モード */
  private val tableSource = new ValueHolder[TableSource](LocalSource)
  /** データリストテーブルの結合 */
  private var currentConnectors: List[Connector] = Nil
  /** このコントローラを表すタイトル */
  val title = new ValueHolder("")
  /** ソースリストの選択項目 */
  def selectedRoom = sourceListCtrl.selectedRoom
  /** データテーブルの選択項目 */
  def contentViewItem = museumExhibitListCtrl.tableSelection
  
  class FileLoadingProgressViewHandler(contentPane: JComponent, progressBar: JProgressBar, statusLabel: JLabel) extends swing.Reactor {
    import MuseumExhibitLoadManager._
    import swing.Reactions.Reaction
    import swing.event.Event
    import java.io.File
    
    private var inProgress = false
    private var progressMax = 0
    private var progressValue = 0
    private var subject = ""
    private var message = ""
    
    reactions += {
      case Started(task) =>
        progressMax = 0
        progressValue = 0
        subject = ""
        message = ""
        inProgress = true
        updateViews()
      case ProgressChange(task, max, value) =>
        progressMax = max
        progressValue = value
        updateViews()
      case MessageChange(task, message) =>
        this.message = message
        updateViews()
      case SubjectChange(task, subject) =>
        this.subject = subject
        updateViews()
      case Done(task) =>
        inProgress = false
        updateViews()
    }
    
    def updateViews() {
      contentPane.setVisible(inProgress)
      statusLabel.setText(message)
      
      progressBar.setMaximum(progressMax)
      progressBar.setValue(progressValue)
      progressBar.setIndeterminate(isIndeterminate)
    }
    
    protected def isIndeterminate =  inProgress && progressMax <= progressValue
  }
  
  /** データテーブルの表示状態を変更する */
  private def setTableSource(newTableSource: TableSource) {
    // 結合解除
    currentConnectors.foreach(_.release())
    
    currentConnectors = newTableSource match {
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
  private def updateRoomContents(newRoom: ExhibitRoom) {
    if (newRoom == sourceListCtrl.sourceStructure.webSource) {
      tableSource := WebSource
    }
    else {
      val room = newRoom match {
        case newRoom: UserExhibitRoom => Some(newRoom)
        case _ =>  None
      }
      museumExhibitListCtrl.userExhibitRoom := room
      tableSource := LocalSource
    }
  }
  
  /**
   * コンテンツビューのモデルを変更する。
   */
  private def setContentViewItem(newItem: Option[MuseumExhibit]) {
    // ビューワー表示
    val source = newItem match {
      case Some(exhibit) => fileStorage.getSource(exhibit) match {
        case None => Iterator.empty
        case Some(source) => io.Source.fromURL(source).getLines
      }
      case _ => Iterator.empty
    }
    
    contentViewer.source = source
    if (mainView.isContentViewerClosed)
      mainView.openContentViewer(200)
  }
  
  /** モデルの結合を行う */
  private def bindModels() {
    // ソースリスト項目選択
    sourceListCtrl.selectedRoom.reactions += {
      case ValueChange(_, _, newRoom) => updateRoomContents(newRoom.asInstanceOf[ExhibitRoom])
    }
    
    // データリストの表示モードの変更
    tableSource.reactions += {
      case ValueChange(_, _, newMode) => setTableSource(newMode.asInstanceOf[TableSource])
    }
    
    // ファイルソース表示
    contentViewItem.reactions += {
      case ValueChange(_, _, newValue: Seq[_]) =>
        val newItem = newValue.headOption.collect { case e: MuseumExhibit => e }
        setContentViewItem(newItem)
    }
    
    //  読み込み管理オブジェクトの進行表示
    fileLoadingProgressHandler.listenTo(loadManager)
    fileLoadingProgressHandler.updateViews()
  }
  
  /** アクションの結合を行う */
  private def bindActions() {
    // ボタン
    bindAction(mainView.addListBox -> sourceListCtrl.addBasicRoomAction,
      mainView.addSmartBox -> sourceListCtrl.addSamrtRoomAction,
      mainView.addBoxFolder -> sourceListCtrl.addGroupRoomAction,
      mainView.removeBoxButton -> sourceListCtrl.removeSelectedUserRoomAction)
  }
  
  bindModels()
  bindActions()
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
