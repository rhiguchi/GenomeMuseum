package jp.scid.genomemuseum.controller

import java.net.URL
import java.io.{File, FileOutputStream, IOException, Reader, FileReader, InputStreamReader}
import java.beans.{PropertyChangeListener, PropertyChangeEvent}
import java.util.concurrent.{Callable, Executors}
import javax.swing.SwingWorker
import SwingWorker.StateValue

import swing.Publisher
import util.control.Exception.allCatch

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitLoader, MuseumExhibitFileLibrary,
  MuseumExhibitService}

import SwingTaskService._

/**
 * ファイルから展示物を作成するマネージャ
 * @param loader ファイルから MuseumExhibit を読み込む処理を行う処理の移譲先。
 * @param museumExhibitStorage ファイル格納管理オブジェクト
 */
class MuseumExhibitLoadManager(
  val dataService: MuseumExhibitService,
  loader: MuseumExhibitLoader,
  var fileLibrary: Option[MuseumExhibitFileLibrary] = None
) extends GenomeMuseumController with Publisher {
  import MuseumExhibitLoadManager._
  
  def this(dataService: MuseumExhibitService) {
    this(dataService, new MuseumExhibitLoader)
  }
  
  // リソース
  /** 読み込み時の形式不良メッセージ */
  def invalidFormatMessage = getResource("alertInvalidFormat.message")
  /** 読み込み時の形式不良メッセージ */
  def failToLoadMessage = getResource("alertFailToLoad.message")
  
  // コントローラ
  /** メッセージ出力 */
  var optionDialogManager: Option[OptionDialogManager] = None
  
  /** 現在実行中のタスク */
  private[controller] var currentLoadTask = createLoadTask()
  
  /** SwingWorker の PCL と scala.swing.Event の接続 */
  private object TaskPropertyChangeHandler extends PropertyChangeListener {
    def propertyChange(e: PropertyChangeEvent) = {
      val task = e.getSource.asInstanceOf[SwingTaskService[_, _]]
      
      e.getPropertyName match {
        case "state" => e.getNewValue match {
          case StateValue.STARTED => publish(Started(task))
          case StateValue.DONE => publish(Done(task))
          case _ =>
        }
        case "target" =>
          publish(ProgressChange(task, task.getFinishedCount, task.getTaskCount))
          
          val loadTask = e.getNewValue.asInstanceOf[LoadTask]
          val message = "%s 読み込み中... [%d / %d]"
            .format(loadTask.fileName, task.getFinishedCount, task.getTaskCount)
          
          publish(MessageChange(task, message))
        case _ =>
      }
    }
  }
  
  /**
   * ファイルから展示物の読み込みを非同期で行う
   * 
   * {@code fileLibrary} が設定されているとき、このファイルはライブラリへ保管される。
   * @param file 読み込みもとのファイル
   */
  def loadExhibit(file: File) = {
    val task = new FileLoadingTask(file)
    addQuery(task)
  }
  
  /**
   * ソースから展示物の読み込みを非同期で行う
   * 
   * {@code #loadExhibit(File)} と異なり、{@code fileLibrary} が設定されていても
   * ファイルはライブラリに保管されない。
   * @param file 読み込みもとのファイル
   */
  def loadExhibit(source: URL) = {
    val task = new SourceLoadingTask(source)
    addQuery(task)
  }
  
  /**
   * 読み込みタスクを追加する。タスクサービスが停止しているときは、実行する。
   */
  protected def addQuery(task: LoadTask) = {
    val feature = currentLoadTask.trySubmit(task) match {
      case Some(f) => f
      case None =>
        allCatch(currentLoadTask.get)
        currentLoadTask = createLoadTask
        currentLoadTask.trySubmit(task).get
    }
    
    if (currentLoadTask.getState == StateValue.PENDING) {
      currentLoadTask.addPropertyChangeListener(TaskPropertyChangeHandler)
      currentLoadTask.execute()
    }
    
    feature
  }
  
  /**
   * 展示物データの読み込みタスク
   */
  abstract class LoadTask extends Callable[Option[MuseumExhibit]] {
    def fileName: String
  }
  
  /**
   * 展示物読み込みの処理を行う。
   * ファイル格納管理オブジェクトが有効の時、ファイルはコピーされ
   * 管理オブジェクトの指定する位置に移動される。
   */
  private class SourceLoadingTask(source: URL) extends LoadTask {
    def call() = {
      val exhibitOpt = using(getReader(source)) { reader =>
        loader.makeMuseumExhibit(dataService.create, reader)
      }
      exhibitOpt.foreach { exhibit =>
        exhibit.dataSourceUri = source.toURI.toString
        dataService.save(exhibit)
      }
      exhibitOpt
    }
    
    def fileName = source.toString
  }
  
  /**
   * 展示物読み込みの処理を行う。
   * ファイル格納管理オブジェクトが有効の時、ファイルはコピーされ
   * 管理オブジェクトの指定する位置に移動される。
   */
  private class FileLoadingTask(file: File) extends LoadTask {
    def call() = {
      val exhibitOpt = using(getReader(file)) { reader =>
        loader.makeMuseumExhibit(dataService.create, reader)
      }
      exhibitOpt.foreach { exhibit =>
        exhibit.dataSourceUri = file.toURI.toString
        // ファイルをライブラリに保管
        fileLibrary foreach { lib => exhibit.sourceFile = Some(lib.store(file, exhibit)) }
        dataService.save(exhibit)
      }
      exhibitOpt
    }
    
    def fileName = file.toString
  }
  
  /** ファイルから読み込みオブジェクトを作成 */
  protected[controller] def getReader(file: File): Reader =
    new FileReader(file)
  
  /** URL から読み込みオブジェクトを作成 */
  protected[controller] def getReader(url: URL): Reader =
    new InputStreamReader(url.openStream)
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
  
  /**
   * 読み込みを行う Swing タスクを作成
   */
  private def createLoadTask() = new SwingTaskService[Option[MuseumExhibit], LoadTask](
      Executors.newSingleThreadExecutor) {
    /**
     * 警告の出力
     */
    override def process(chunks: java.util.List[ResultChunk]) {
      import scala.collection.JavaConverters._
      val chunkList = chunks.asScala.toList
      // エラーファイルを取得
      val invalidFiles = chunkList.collect{case ExceptionThrown(q, c) => (q, c)}
      if (invalidFiles.nonEmpty)
        alertFailToLoad(invalidFiles)
      
      // 形式不正の警告表示
      val invalidFormatQueries = chunkList.collect{case Success(q, None) => q}
      if (invalidFormatQueries.nonEmpty)
        alertInvalidFormat(invalidFormatQueries)
    }
    
    /**
     * 全読み込み完了
     */
    override def done {
    }
  }
  
  /**
   * 読み込み中の失敗を通知する。
   */
  protected[controller] def alertInvalidFormat(tasks: Seq[LoadTask]) {
    if (logger.isDebugEnabled)
      tasks.foreach(task => logger.debug("invalid format", task.fileName))
    
    optionDialogManager foreach { manager =>
      val description = tasks.map(_.fileName).mkString("<br>\n")
      manager.showMessage(invalidFormatMessage(), Some(description))
    }
  }
  
  /**
   * 読み込み中の失敗を通知する。
   */
  protected[controller] def alertFailToLoad(results: Seq[(LoadTask, Exception)]) {
    results.foreach{ case (task, exp) => logger.info("fail to load: " + task.fileName, exp) }
    
    optionDialogManager foreach { manager =>
      val description = results.map(_._1.fileName).mkString("<br>\n")
      manager.showMessage(failToLoadMessage(), Some(description))
    }
  }
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
}


