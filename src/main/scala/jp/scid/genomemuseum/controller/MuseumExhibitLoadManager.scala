package jp.scid.genomemuseum.controller

import java.net.URL
import java.io.{File, FileOutputStream, IOException}
import java.beans.{PropertyChangeListener, PropertyChangeEvent}
import java.util.concurrent.{Callable, Executors}
import javax.swing.SwingWorker
import SwingWorker.StateValue

import swing.Publisher
import util.control.Exception.allCatch

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitLoader, MuseumExhibitStorage,
  MuseumExhibitService}

import SwingTaskService._

/**
 * ファイルから展示物を作成するマネージャ
 * @param loader ファイルから MuseumExhibit を読み込む処理を行う処理の移譲先。
 * @param museumExhibitStorage ファイル格納管理オブジェクト
 */
class MuseumExhibitLoadManager(
  val dataService: MuseumExhibitService,
  val loader: MuseumExhibitLoader,
  var museumExhibitStorage: Option[MuseumExhibitStorage] = None
) extends Publisher {
  import MuseumExhibitLoadManager._
  
  def this(dataService: MuseumExhibitService, storage: Option[MuseumExhibitStorage]) {
    this(dataService, new MuseumExhibitLoader, storage)
  }
  
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
   * @param file 読み込みもとのファイル
   */
  def loadExhibit(file: File) = {
    val task = new FileLoadingTask(file)
    addQuery(task)
  }
  
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
  
  abstract class LoadTask extends Callable[Option[MuseumExhibit]] {
    def fileName: String
  }
  
  /**
   * 展示物読み込みの処理を行う。
   * ファイル格納管理オブジェクトが有効の時、ファイルはコピーされ
   * 管理オブジェクトの指定する位置に移動される。
   */
  class SourceLoadingTask(source: URL) extends LoadTask {
    def call() = {
      val exhibit = dataService.create
      val result = loader.makeMuseumExhibit(exhibit, source) match {
        case true => Some(exhibit)
        case false =>  None
      }
      result foreach setFilePathTo
      result foreach dataService.save
      result
    }
    
    def fileName = source.getFile
    
    protected def setFilePathTo(exhibit: MuseumExhibit) {
      exhibit.filePath = source.toURI.toString
    }
  }
  
  /**
   * 展示物読み込みの処理を行う。
   * ファイル格納管理オブジェクトが有効の時、ファイルはコピーされ
   * 管理オブジェクトの指定する位置に移動される。
   */
  class FileLoadingTask(file: File) extends SourceLoadingTask(file.toURI.toURL) {
    override protected def setFilePathTo(exhibit: MuseumExhibit) {
      museumExhibitStorage match {
        case Some(storage) =>
          storage.saveSource(exhibit, copyToTempFile(file))
        case None =>
          super.setFilePathTo(exhibit)
      }
    }
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
    // TODO
  }
  
  /**
   * 読み込み中の失敗を通知する。
   */
  protected[controller] def alertFailToLoad(results: Seq[(LoadTask, Exception)]) {
    // TODO
  }
  
  /**
   * 取り込み中に起きた例外を通知する。
   */
  protected[controller] def alertFailToTransfer(cause: Exception) {
    cause.printStackTrace
    // TODO
  }
}

object MuseumExhibitLoadManager {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[MuseumExhibitLoadManager])
  
  /**
   * 一時ファイルを作成し、そこへファイルを複製する。
   * @throws IOException 入出力エラー
   */
  private def copyToTempFile(src: File) = {
    import java.io.{FileInputStream, FileOutputStream}
    import java.nio.channels.FileChannel
    
    val dest = File.createTempFile("MuseumExhibitLoadManager", "txt")
    val srcChannel = new FileInputStream(src).getChannel()
    val destChannel = new FileOutputStream(dest).getChannel()
    try {
      srcChannel.transferTo(0, srcChannel.size(), destChannel)
    }
    finally {
      try {
        srcChannel.close()
      }
      finally {
        destChannel.close()
      }
    }
    
    dest
  }
  
  sealed abstract class TaskEvent extends swing.event.Event {
    def task: SwingWorker[_, _]
  }
  case class Started(task: SwingWorker[_, _]) extends TaskEvent
  case class ProgressChange(task: SwingWorker[_, _], max: Int, value: Int) extends TaskEvent
  case class SubjectChange(task: SwingWorker[_, _], subject: String) extends TaskEvent
  case class MessageChange(task: SwingWorker[_, _], message: String) extends TaskEvent
  case class Done(task: SwingWorker[_, _]) extends TaskEvent
}


