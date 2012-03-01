package jp.scid.genomemuseum.controller

import java.net.{URL, URI}
import java.text.ParseException
import java.io.{File, IOException}
import java.beans.{PropertyChangeListener, PropertyChangeEvent}
import java.util.concurrent.{Executors, Future}
import javax.swing.{SwingWorker, JComponent, JProgressBar, JLabel}
import SwingWorker.StateValue

import collection.mutable.{ListBuffer, SynchronizedQueue}

import jp.scid.genomemuseum.view.MainView
import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitLoader, MuseumExhibitFileLibrary,
  MuseumExhibitService, FreeExhibitRoomModel}
import jp.scid.gui.model.ValueModels
import jp.scid.gui.control.{ComponentPropertyConnector, OptionPaneController}

import MuseumExhibit.FileType._

/**
 * ファイルから展示物を作成するマネージャ
 * 
 * ファイルライブラリが指定されている時は、読み込んだファイルはライブラリ指定ディレクトリに保管される。
 */
class MuseumExhibitLoadManager {
  private val ctrl = GenomeMuseumController(this)
  
  import MuseumExhibitLoadManager._
  
  // モデル
  /** 読み込み処理 */
  var museumExhibitLoader: MuseumExhibitLoader = new MuseumExhibitLoader
  
  /** 展示物保管サービス */
  var museumExhibitService: Option[MuseumExhibitService] = None
  
  /** ファイル保管ライブラリ */
  var fileLibrary: Option[MuseumExhibitFileLibrary] = None
  
  /** タスク実行器 */
  private val loadingTaskExecutor = Executors.newSingleThreadExecutor
  
  /** タスク実行結果キュー */
  private val loadingTaskResults = new SynchronizedQueue[TaskResult]
  
  // リソース
  /** 読み込み時の形式不良メッセージ */
  lazy val invalidFormatMessage = ctrl.getResource("alertInvalidFormat.message")
  /** 読み込み時の形式不良メッセージ */
  lazy val failToLoadMessage = ctrl.getResource("alertFailToLoad.message")
  /** 読み込み進行中メッセージ */
  lazy val progressMessageInProgress = ctrl.getResource("progressMessage.inprogress")
  
  // モデル
  /** タスクが実行中であるか */
  val isRunning = ValueModels.newBooleanModel(false)
  
  /** 連続実行中のタスクの最大値 */
  val maxmumTaskCount = ValueModels.newIntegerModel(0)
  
  /** 連続実行中の完了タスクの数 */
  val finishedTaskCount = ValueModels.newIntegerModel(0)
  
  /** タスクメッセージ */
  val progressMessage = ValueModels.newValueModel("Message")
  
  /** 現在読み込み中のファイル */
  private var currentLoadingFile = ""
  
  // コントローラ
  /** メッセージ出力 */
  var optionDialogManager = new OptionPaneController
  
  /** 現在実行中のタスク数やメッセージを更新する */
  private[controller] object TaskPropertyChangeHandler extends PropertyChangeListener {
    def propertyChange(e: PropertyChangeEvent) = synchronized {
      e.getPropertyName match {
        case "state" => e.getNewValue match {
          case StateValue.STARTED =>
            isRunning := true
            e.getSource match {
              case task: MuseumExhibitLoadingTask =>
                currentLoadingFile = task.source.getFile
              case _ =>
            }
          case StateValue.DONE =>
            finishedTaskCount := (finishedTaskCount() + 1)
            if (maxmumTaskCount() == finishedTaskCount()) {
              isRunning := false
              maxmumTaskCount := 0
              finishedTaskCount := 0
            }
          case _ =>
        }
        case _ =>
      }
      updateProgressMessage()
    }
    
    def listenTo(task: SwingWorker[_, _]) = synchronized {
      task addPropertyChangeListener this
      maxmumTaskCount := (maxmumTaskCount() + 1)
    }
  }
  
  /**
   * 展示物の読み込みタスク
   * 
   * @param service 展示物の作成サービス
   * @param source 読み込む URL
   */
  private class MuseumExhibitLoadingTask(service: MuseumExhibitService, val source: URL) extends SwingWorker[Option[MuseumExhibit], Unit] {
    def this(service: MuseumExhibitService, source: URL, destModel: FreeExhibitRoomModel) {
      this(service, source)
      this.destModel = Option(destModel)
    }
    
    /** 読み込み完了後に追加されるモデル */
    var destModel: Option[FreeExhibitRoomModel] = None
    
    private lazy val exhibit = {
      val exhibit = service.create
      service.add(exhibit)
      destModel match {
        case Some(room) if room != service => room.add(exhibit)
        case _ =>
      }
      exhibit
    }
    
    def doInBackground() = {
      // 読み込み処理
      loadMuseumExhibit(exhibit, source) match {
        case true =>
          // ファイルのコピーとライブラリ登録
          val dataSourceUri = fileLibrary map (_.store(exhibit, source)) getOrElse source.toURI
          exhibit.dataSourceUri = dataSourceUri.toString
          service.save(exhibit)
          
          Some(exhibit)
        case false =>
          None
      }
    }
    
    override def done() {
      import java.util.concurrent.ExecutionException
      import util.control.Exception.catching
      
      val result = isCancelled match {
        case false => catching(classOf[ExecutionException]) either get() match {
          case Right(e @ Some(_)) => e
          case Right(None) =>
            loadingTaskResults += InvalidFormat(source)
            None
          case Left(e: ExecutionException) =>
            e.getCause match {
              case e: IOException => loadingTaskResults += ThrowedIOException(e, source)
              case e: ParseException => loadingTaskResults += ThrowedParseException(e, source)
              case e => e.printStackTrace
            }
            None
        }
        case true => false
      }
      
      result match {
        case None => service.remove(exhibit)
        case _ =>
      }
      
      // アラート表示中に次のタスクをブロックさせないために次のイベントで実行
      java.awt.EventQueue.invokeLater(new Runnable {
        def run() {
          processResultMessages()
        }
      })
    }
  }
  
  /**
   * ソースから展示物を作成する。
   * @return ファイルから読み込みが成功したら {@code true}
   */
  @throws(classOf[IOException])
  @throws(classOf[ParseException])
  protected[controller] def loadMuseumExhibit(exhibit: MuseumExhibit, source: URL) = {
    museumExhibitLoader.findFormat(source) match {
      case Unknown => false
      case format =>
        museumExhibitLoader.loadMuseumExhibit(exhibit, source, format)
        true
    }
  }
  
  // 読み込み実行
  private type TaskFuture = Future[Option[MuseumExhibit]]
  /**
   * URLから展示物を読み込み、コピーしたファイルをライブラリへコピーして追加する。
   */
  def loadExhibit(source: URL): TaskFuture = {
    val task = new MuseumExhibitLoadingTask(museumExhibitService.get, source)
    execute(task)
    task
  }
  
  /**
   * ファイルから展示物を読み込み、リストに追加する。
   */
  def loadExhibit(targetRoom: FreeExhibitRoomModel, file: File): TaskFuture = {
    val task = new MuseumExhibitLoadingTask(museumExhibitService.get, file.toURI.toURL, targetRoom)
    execute(task)
    task
  }
  
  /**
   * ファイルから展示物を読み込み、ファイルをライブラリへコピーして追加する。
   * 
   * @param file 読み込みもとのファイル
   */
  def loadExhibit(file: File): TaskFuture = loadExhibit(file.toURI.toURL)
  
  
  /**
   * ファイルから展示物を読む
   */
  def loadExhibit(file: List[File]): List[TaskFuture] = getAllFiles(file) map loadExhibit
  
  /**
   * ファイルから展示物を読む
   */
  def loadExhibit(file: List[File], destRoom: FreeExhibitRoomModel): List[TaskFuture] =
    getAllFiles(file) map (f => loadExhibit(destRoom, f))
  
  /**
   * タスクを実行する
   */
  protected[controller] def execute(task: SwingWorker[_, _]) {
    TaskPropertyChangeHandler listenTo task
    loadingTaskExecutor execute task
  }
  
  // バインディング
  def bind(view: MainView) {
    // 進捗画面
    bindProgressView(view.fileLoadingActivityPane, view.fileLoadingProgress, view.fileLoadingStatus)
  }
  
  /** 進捗ビューのモデル結合 */
  def bindProgressView(contentPane: JComponent,
      progressBar: JProgressBar, statusLabel: JLabel) {
    new ComponentPropertyConnector(contentPane, "visible").setModel(isRunning)
    
    // progressBar
    progressBar.setMinimum(0)
    new ComponentPropertyConnector(progressBar, "maximum").setModel(maxmumTaskCount)
    new ComponentPropertyConnector(progressBar, "value").setModel(finishedTaskCount)
    
    // message
    new ComponentPropertyConnector(statusLabel, "text").setModel(progressMessage)
    
    // owner for option dialog
    optionDialogManager.setParentComponent(contentPane)
  }

  
  // 読み込み通知
  /**
   * 処理メッセージの更新
   */
  def updateProgressMessage() {
    val message = Boolean unbox isRunning() match {
      case true => 
        "Loading %s [%s / %s]".format(currentLoadingFile, finishedTaskCount(), maxmumTaskCount())
      case false => ""
    }
    progressMessage := message
  }
  
  /**
   * 読み込み処理が正常に終わらなかった時の処理
   */
  private[controller] def processResultMessages() = synchronized {
    var invalidfResults = ListBuffer.empty[InvalidFormat]
    var ioeResults = ListBuffer.empty[ThrowedIOException]
    var parseeResults = ListBuffer.empty[ThrowedParseException]
    
    def collect(): Unit = loadingTaskResults.isEmpty match {
      case true =>
      case false =>
        loadingTaskResults.dequeue match {
          case r: InvalidFormat => invalidfResults += r
          case r: ThrowedIOException => ioeResults += r
          case r: ThrowedParseException => parseeResults += r
        }
        collect()
    }
    
    collect()
    
    if (invalidfResults.nonEmpty)
      alertInvalidFormat(invalidfResults)
    if (ioeResults.nonEmpty)
      alertIOException(ioeResults)
    if (parseeResults.nonEmpty)
      alertParseException(parseeResults)
  }
  
  /**
   * 読み込み中の失敗を通知する。
   */
  protected[controller] def alertInvalidFormat(results: Seq[InvalidFormat]) {
    results.foreach(result => logger.info("Loading failed because invalid format {}", result.source))
    
    val description = results.map(_.source.toString).mkString("<br>\n")
    optionDialogManager.showMessage(invalidFormatMessage(), description)
  }
  
  /**
   * 読み込み中の失敗を通知する。
   */
  protected[controller] def alertIOException(results: Seq[ThrowedIOException]) {
    results.foreach(result =>
      logger.info("Loading failed with thrown %s".format(result.source), result.cause))
    
    val description = results.map(_.source.toString).mkString("<br>\n")
    optionDialogManager.showMessage(failToLoadMessage(), description)
  }
  
  /**
   * 読み込み中の失敗を通知する。
   */
  protected[controller] def alertParseException(results: Seq[ThrowedParseException]) {
    results.foreach(result =>
      logger.info("Loading failed with thrown %s".format(result.source), result.cause))
    
    val description = results.map(_.source.toString).mkString("<br>\n")
    optionDialogManager.showMessage(failToLoadMessage(), description)
  }
  
  /** 読み込み処理の完了状態を表すクラスの抽象定義 */
  private sealed abstract class TaskResult
  
  /** 読み込みができないファイル形式の結果表現 */
  private case class InvalidFormat(source: URL) extends TaskResult
  
  /** 読み込み中にIO例外が発生した時の結果表現 */
  private case class ThrowedIOException(cause: IOException, source: URL) extends TaskResult
  
  /** 読み込み中に解釈例外が発生した時の結果表現 */
  private case class ThrowedParseException(cause: ParseException, source: URL) extends TaskResult
}

object MuseumExhibitLoadManager {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[MuseumExhibitLoadManager])
  
  sealed abstract class TaskEvent extends swing.event.Event {
    def task: SwingWorker[_, _]
  }
  case class Started(task: SwingWorker[_, _]) extends TaskEvent
  case class ProgressChange(task: SwingWorker[_, _], max: Int, value: Int) extends TaskEvent
  case class SubjectChange(task: SwingWorker[_, _], subject: String) extends TaskEvent
  case class MessageChange(task: SwingWorker[_, _], message: String) extends TaskEvent
  case class Done(task: SwingWorker[_, _]) extends TaskEvent
  
  /**
   * ディレクトリ内に含まれる全てのファイルを探索し、取得する。
   */
  private[controller] def getAllFiles(files: Seq[File]) = {
    import collection.mutable.{Buffer, ListBuffer, HashSet}
    // 探索済みディレクトリ
    val checkedDirs = HashSet.empty[String]
    // ディレクトリがすでに探索済みであるか
    def alreadyChecked(dir: File) = checkedDirs contains dir.getCanonicalPath
    // 探索済みに追加
    def addCheckedDir(dir: File) = checkedDirs += dir.getCanonicalPath
    
    @annotation.tailrec
    def collectFiles(files: List[File], accume: Buffer[File] = ListBuffer.empty[File]): List[File] = {
      files match {
        case head :: tail =>
          if (head.isHidden) {
            collectFiles(tail, accume)
          }
          else if (head.isDirectory && !alreadyChecked(head)) {
            addCheckedDir(head)
            collectFiles(head.listFiles.toList ::: tail, accume)
          }
          else {
            accume += head
            collectFiles(tail, accume)
          }
        case Nil => accume.toList
      }
    }
    
    collectFiles(files.toList)
  }
}


