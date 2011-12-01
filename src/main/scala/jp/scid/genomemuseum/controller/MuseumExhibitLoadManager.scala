package jp.scid.genomemuseum.controller

import java.net.URL
import java.io.{File, BufferedOutputStream, BufferedInputStream,
  FileOutputStream, IOException}
import java.beans.{PropertyChangeListener, PropertyChangeEvent}
import java.util.concurrent.ExecutionException
import javax.swing.SwingWorker
import SwingWorker.StateValue

import actors.{Future, Futures}
import swing.Publisher
import util.control.Exception.catching

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitLoader, MuseumExhibitStorage,
  MuseumExhibitService}
import jp.scid.genomemuseum.gui.ExhibitTableModel

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
  private var currentLoadTask: Option[LoadTask] = None
  
  /** SwingWorker の PCL と scala.swing.Event の接続 */
  private object TaskPropertyChangeHandler extends PropertyChangeListener {
    def propertyChange(e: PropertyChangeEvent) = e.getPropertyName match {
      case "state" => e.getNewValue match {
        case StateValue.STARTED => publish(Started())
        case StateValue.DONE => publish(Done())
        case _ =>
      }
      case "target" =>
        val next = e.getNewValue.asInstanceOf[File]
        val task = e.getSource.asInstanceOf[LoadTask]
        publish(ProgressChange(next, task.getFinishedCount, task.getQueuedCount))
      case _ =>
    }
  }
  
  def loadExhibits(files: Seq[File]): Future[Option[_]] = {
    loadExhibits(None, files)
  }
  
  /**
   * ファイルから展示物の読み込みを非同期で行う
   * @param tableModel 追加先のテーブルモデル。
   * @param file 読み込みもとのファイル
   * @return ファイルの読み込みに失敗したときは原因を含んだ Left 値。
   *         読み込みに成功し、サービスに正常に追加されたときはエンティティの Right 値。
   */
  def loadExhibits(tableModel: Option[ExhibitTableModel],
      files: Seq[File]): Future[Option[_]] = {
    import util.control.Exception.catching
    
    logger.debug("ファイルの読み込み {}", files)
    
    // 読み込み処理を行うタスクを取得
    // 実行中タスクが指定されているが終了フラグとなっている時は新しいタスクを作成
    val task = currentLoadTask.filter(_.getState != StateValue.DONE)
      .getOrElse(createLoadTask())
    
    logger.debug("task {}", task)
    // クエリを追加する
    task.tryQueue(tableModel, files) match {
      case true =>
        logger.trace("クエリの追加 State {}", task.getState)
        // タスクを実行状態にする。
        if (task.getState == StateValue.PENDING)
          execute(task)
        // このクエリが完了することを通知する Future を返す。
        Futures.future {
          catching(classOf[InterruptedException],
            classOf[ExecutionException]) opt task.get
        }
      case false => {
        logger.trace("クエリの追加失敗", files)
        // 現在の処理が終了するのを待ち、再度クエリ追加を行う。
        currentLoadTask = None
        catching(classOf[InterruptedException],
          classOf[ExecutionException]) opt task.get
        
        loadExhibits(tableModel, files)
      }
    }
  }
  
  /**
   * 展示物読み込みの処理を行う。
   * ファイル格納管理オブジェクトが有効の時、ファイルはコピーされ
   * 管理オブジェクトの指定する位置に移動される。
   */
  protected def loadExhibit(entity: MuseumExhibit, file: File) = {
    loader.makeMuseumExhibit(entity, file.toURI.toURL) match {
      case true =>
        museumExhibitStorage foreach { storage =>
          val storeFile = copyToTempFile(file)
          storage.saveSource(entity, storeFile)
        }
        true
      case false => false
    }
  }
  
  /**
   * 展示物読み込みの処理を行う。
   * ファイル格納管理オブジェクトが有効の時、ファイルはコピーされ
   * 管理オブジェクトの指定する位置に移動される。
   */
  protected def loadExhibit(entity: MuseumExhibit, source: URL) = {
    val result = loader.makeMuseumExhibit(entity, source)
    if (result)
      entity.filePath = source.toString
    result
  }
  
  /**
   * タスクの実行を行う。現在実行中のタスクはキャンセルされる。
   */
  private def execute(task: LoadTask) {
    import util.control.Exception.catching
    logger.debug("ファイル追加タスク起動")
    
    currentLoadTask match {
      case Some(task) => if (!task.isDone) {
        logger.trace("起動済みタスクの停止")
        task.cancel(false)
      }
      case _ =>
    }
    task.addPropertyChangeListener(TaskPropertyChangeHandler)
    task.execute()
    currentLoadTask = Some(task)
  }
  
  /**
   * 読み込みを行う Swing タスクを作成
   */
  private def createLoadTask() = new LoadTask(dataService) {
    import LoadTask.FailToLoad
    
    def loadExhibitFromFile(entity: MuseumExhibit, file: File) = loadExhibit(entity, file)
    
    def loadExhibitFromUrl(entity: MuseumExhibit, source: URL) = loadExhibit(entity, source)
    
    /**
     * 警告の出力
     */
    override def process(chunks: java.util.List[LoadTask.Chunk]) {
      import scala.collection.JavaConverters._
      
      val chunkList = chunks.asScala.toList
      // エラーファイルを取得
      val invalidFiles = chunkList.collect{case FailToLoad(file) => file}
      // 形式不正の警告表示
      alertFailToLoad(invalidFiles)
    }
    
    /**
     * 全読み込み完了
     */
    override def done {
      currentLoadTask = None
    }
  }
  
  /**
   * 読み込み中の失敗を通知する。
   */
  protected[controller] def alertFailToLoad(causes: List[File]) {
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
  
  sealed abstract class LoadingEvent extends swing.event.Event
  case class Started() extends LoadingEvent
  case class ProgressChange(next: File, finishedCount: Int, queuedCount: Int) extends LoadingEvent
  case class Done() extends LoadingEvent
}

/**
 * データ読み込み Swing タスク
 */
abstract class LoadTask(dataService: MuseumExhibitService) extends SwingWorker[Unit, LoadTask.Chunk] {
  import LoadTask._
  import collection.mutable.Queue
  import java.text.ParseException
  
  private val fileQueue = Queue.empty[Query]
  private var shutdown = false
  private var queuedCount = 0
  private var finishedCount = 0
  
  def doInBackground() {
    load()
  }
  
  @annotation.tailrec
  private def load() {
    import util.control.Exception.catching
    
    // ファイルキューの先頭を取得
    fileQueue.synchronized {
      if (fileQueue.isEmpty) {
        // キューが空のときは終了処理
        shutdown = true
        None
      }
      else {
        Some(fileQueue.dequeue)
      }
    }
    .foreach { case Query(entity, tableModel, file) =>
      // イベント発行
      firePropertyChange("target", null, file)
      
      // 読み込み処理
      catching(classOf[IOException], classOf[ParseException]) either {
        loadExhibitFromFile(entity, file)
      } match {
        case Right(true) =>
          dataService.save(entity)
          tableModel.map(_.reloadSource())
          tableModel.map(_.updateElement(entity))
        case Right(false) =>
          // 形式未対応
          dataService.remove(entity)
          tableModel.map(_.removeElement(entity))
        case Left(thrown) =>
          // 例外
          dataService.remove(entity)
          tableModel.map(_.removeElement(entity))
          publish(FailToLoad(file))
      }
      // 処理完了数の増加
      finishedCount += 1
    }
    
    if (!shutdown)
      load()
  }
  
  /**
   * ファイルから読み込み処理を行う。
   * @return ファイルが形式に対応し、読み込みが行われたときは {@code true} 。
   */
  protected def loadExhibitFromFile(entity: MuseumExhibit, file: File): Boolean
  
  /**
   * リモートリソースから読み込み処理を行う。
   * @return ファイルが形式に対応し、読み込みが行われたときは {@code true} 。
   */
  protected def loadExhibitFromUrl(entity: MuseumExhibit, source: URL): Boolean
  
  /**
   * このタスクに追加されたファイル数
   */
  def getQueuedCount = queuedCount
  
  /**
   * 処理が完了したファイル数
   */
  def getFinishedCount = finishedCount
  
  /**
   * 読み込むファイルを追加する。このタスクが終了処理に入った時はファイルは追加されない。
   */
  def tryQueue(tableModel: Option[ExhibitTableModel], files: Seq[File]) = {
    fileQueue.synchronized {
      logger.debug("クエリの追加 isShutdonw: {}", shutdown)
      // タスクが終了していなければ追加
      if (!shutdown) {
        val queries = files.map(file =>
          Query(dataService.create(), tableModel, file))
        fileQueue.enqueue(queries: _*)
        queuedCount += queries.size
        
        true
      }
      else {
        files.isEmpty
      }
    }
  }
  
  /** クエリ情報オブジェクト */
  private case class Query(entity: dataService.ElementClass,
    tableModel: Option[ExhibitTableModel], file: File)
}

private[controller] object LoadTask {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[LoadTask])
  
  sealed abstract class Chunk
  case class FailToLoad(file: File) extends Chunk
}
