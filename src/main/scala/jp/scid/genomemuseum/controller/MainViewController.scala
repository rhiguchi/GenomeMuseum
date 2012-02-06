package jp.scid.genomemuseum.controller

import java.net.{URI, URL}
import javax.swing.{JFrame, JTree, JTextField, JComponent, JProgressBar, JLabel}

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.ValueChange
import jp.scid.gui.DataModel.Connector
import jp.scid.genomemuseum.{view, model, gui, GenomeMuseumGUI}
import view.MainView
import model.{MuseumSchema, ExhibitRoom, UserExhibitRoom, MuseumExhibit,
  UserExhibitRoomService, MuseumExhibitService}
import jp.scid.motifviewer.controller.MotifViewerController

/**
 * 主画面の操作を受け付け、操作反応を実行するオブジェクト。
 * 
 * @param sourceListCtrl ソースリスト 表示コントローラ
 * @param museumExhibitListCtrl MuseumExhibit 表示コントローラ
 * @param webServiceResultCtrl WebService 表示ントローラ
 * @param application データやメッセージを取り扱うアプリケーションオブジェクト。
 * @param mainView 表示と入力を行う画面。
 */
class MainViewController(
  userExhibitRoomService: UserExhibitRoomService,
  museumExhibitService: MuseumExhibitService,
  exhibitLoadManager: MuseumExhibitLoadManager
) extends GenomeMuseumController {
  // コントローラ
  /** 部屋リスト操作 */
  protected[controller] val exhibitRoomListController = {
    val ctrl = new ExhibitRoomListController(userExhibitRoomService, exhibitLoadManager)
    ctrl.selectedRoom.addNewValueReaction(updateRoomContents)
    ctrl
  }
  
  /** 展示物リスト操作 */
  protected[controller] val museumExhibitListController = {
    val ctrl = new MuseumExhibitListController(museumExhibitService, exhibitLoadManager)
    // ファイルソース表示
    ctrl.tableSelection.addNewValueReaction(updateExhibitViewContent)
    ctrl
  }  
  /** ウェブ検索操作 */
  protected[controller] val webServiceResultController = new WebServiceResultController
  webServiceResultController.loadManager = Some(exhibitLoadManager)
  
    /** コンテントビューワー */
//    val contentViewer = new FileContentViewer(mainView.fileContentView)
    /** ファイル読み込み表示 */
//    fileLoadingProgressHandler.updateViews()
    //  読み込み管理オブジェクトの進行表示
//    fileLoadingProgressHandler.listenTo(loadManager)
  
  /** 俯瞰図 */
  val motifViewerController = new MotifViewerController
  
  // モデル
  /** データテーブルの現在適用するコントローラ */
  lazy protected[controller] val dataListController = new ValueHolder[DataListController](museumExhibitListController)
  
  /** このコントローラを表すタイトル */
  val title = new ValueHolder("")
  
  /** 検索モチーフ */
//  val searchMotif = new ValueHolder("")
//  searchMotif.addNewValueReaction(sequenceOverviewController.setSearchMotif)
  
  /** 進捗パネルの表示モデル */
  private[controller] lazy val progressViewVisibled = new ValueHolder(false)
  private[controller] lazy val progressMessage = new ValueHolder("")
  private[controller] lazy val progressMaximum = new ValueHolder(0)
  private[controller] lazy val progressValue = new ValueHolder(0)
  private[controller] lazy val progressIndeterminate = new ValueHolder(false)
  
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
  
  /**
   * データテーブル領域に表示するコンテンツを設定する
   * 通常は、ソースリストの選択項目となる
   */
  private def updateRoomContents(newRoom: ExhibitRoom) {
    newRoom match {
      case exhibitRoomListController.sourceStructure.webSource =>
        dataListController := webServiceResultController
      case _ =>
        //ソースリスト項目選択
        museumExhibitListController.userExhibitRoom = newRoom match {
          case newRoom: UserExhibitRoom => Some(newRoom)
          case _ =>  None
        }
        dataListController := museumExhibitListController
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
    
    motifViewerController.setSequence(source.mkString)
//    contentViewer.source = source
//    if (mainView.isContentViewerClosed)
//      mainView.openContentViewer(200)
  }
  
  def bind(view: MainView) {
    // ソースリスト結合
    exhibitRoomListController.bindTree(view.sourceList)
    
    // ボタンアクションの結合
    bindAction(view.addListBox -> exhibitRoomListController.addBasicRoomAction,
      view.addSmartBox -> exhibitRoomListController.addSamrtRoomAction,
      view.addBoxFolder -> exhibitRoomListController.addGroupRoomAction,
      view.removeBoxButton -> exhibitRoomListController.removeSelectedUserRoomAction)
    
    var currentConnectors: List[Connector] = Nil
    
    /** データテーブルの表示状態を変更する */
    // TODO プライベートクラスにリファクタリング
    def setTableSource(ctrl: DataListController) {
      // 結合解除
      currentConnectors.foreach(_.release())
      
      currentConnectors = ValueHolder.connect(ctrl.statusTextModel, title) ::
        ctrl.bindSearchField(view.quickSearchField) ::
        ctrl.bindTable(view.dataTable) ::: Nil
    }
    
    // データリストの表示モードの変更
    dataListController.reactions += {
      case ValueChange(_, _, ctrl) => setTableSource(ctrl.asInstanceOf[DataListController])
    }
    setTableSource(dataListController())
    // 進捗画面
    bindProgressView(view.fileLoadingActivityPane, view.fileLoadingProgress, view.fileLoadingStatus)
    
    // 俯瞰図
    motifViewerController.bindOverviewPane(view.overviewMotifView.overviewPane);
    motifViewerController.bindSearchMotifField(view.overviewMotifView.searchMotifField)
  }
  
  /** 進捗ビューのモデル結合 */
  protected[controller] def bindProgressView(contentPane: JComponent,
      progressBar: JProgressBar, statusLabel: JLabel) {
    progressViewVisibled.addNewValueReaction(contentPane.setVisible).update()
    progressMaximum.addNewValueReaction(progressBar.setMaximum).update()
    progressValue.addNewValueReaction(progressBar.setValue).update()
    progressMessage.addNewValueReaction(statusLabel.setText).update()
    progressIndeterminate.addNewValueReaction(progressBar.setIndeterminate).update()
  }
}

object MainViewController {
}
