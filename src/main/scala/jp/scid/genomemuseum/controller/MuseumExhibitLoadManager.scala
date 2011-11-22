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

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitLoader,
  MuseumExhibitStorage}
import jp.scid.genomemuseum.gui.ListDataServiceSource

/**
 * ファイルから展示物を作成するマネージャ
 */
class MuseumExhibitLoadManager extends Publisher {
  import MuseumExhibitLoadManager._
  
  /** ファイルから MuseumExhibit を作成するオブジェクト */
  private[controller] val loader = new MuseumExhibitLoader
  /** MuseumExhibit ファイルの格納先 */
  var storage: Option[MuseumExhibitStorage] = None
  
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
  
  /**
   * ファイルから展示物の読み込みを非同期で行う
   * @param tableModel 追加先のテーブルモデル。
   * @param file 読み込みもとのファイル
   * @return ファイルの読み込みに失敗したときは原因を含んだ Left 値。
   *         読み込みに成功し、サービスに正常に追加されたときはエンティティの Right 値。
   */
  def loadExhibits(tableModel: ListDataServiceSource[MuseumExhibit],
      files: Seq[File]): Future[Option[_]] = {
    import util.control.Exception.catching
    
    // 読み込み処理を行うタスクを取得
    // 実行中タスクが指定されているが終了フラグとなっている時は新しいタスクを作成
    val task = currentLoadTask.filter(_.getState != StateValue.DONE)
      .getOrElse(createLoadTask())
    
    // クエリを追加する
    task.tryQueue(tableModel, files) match {
      case true =>
        // タスクを実行状態にする。
        if (task.getState == StateValue.PENDING)
          execute(task)
        // このクエリが完了することを通知する Future を返す。
        Futures.future {
          catching(classOf[InterruptedException],
            classOf[ExecutionException]) opt task.get
        }
      case false => {
        // クエリの追加失敗時には、現在の処理が終了するのを待ち、再度クエリ追加を行う。
        currentLoadTask = None
        catching(classOf[InterruptedException],
          classOf[ExecutionException]) opt task.get
        
        loadExhibits(tableModel, files)
      }
    }
  }
  
  @throws(classOf[IOException])
  protected def loadMuseumExhibit(entity: MuseumExhibit, source: URL) = {
    val file = copyToTempFile(source)
    
    val succeed = loader.makeMuseumExhibit(entity, file) match {
      // ファイル保管が有効の時は、保管を試みる。
      case true => storage match {
        case None => true
        case Some(storage) => storage.save(file, entity) match {
          case Some(url) =>
            // 保管先を entity に設定する
            entity.filePath = url.toString
            true
          case None => false
        }
      }
      case false => false
    }
    true
  }
  
  /** 一時ファイルにコピーする */
  private[controller] def copyToTempFile(source: URL): File = {
    val file = File.createTempFile("LoadingTmp", "txt")
    
    using(new FileOutputStream(file)) { out =>
      val dest = new BufferedOutputStream(out)
      
      using(source.openStream) { inst =>
        val buf = new Array[Byte](8196)
        val source = new BufferedInputStream(inst, buf.length)
        
        Iterator.continually(source.read(buf)).takeWhile(_ != -1)
          .foreach(dest.write(buf, 0, _))
      }
      
      dest.flush
    }
    file
  }
  
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
  
  /**
   * タスクの実行を行う。現在実行中のタスクはキャンセルされる。
   */
  private def execute(task: LoadTask) {
    import util.control.Exception.catching
    
    currentLoadTask match {
      case Some(task) => if (!task.isDone) {
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
  private def createLoadTask() = new LoadTask(loader, storage) {
    import LoadTask.FailToLoad
    
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
  protected[controller] def alertFailToLoad(
      causes: List[File]) {
    // TODO
  }
  
  /**
   * 取り込み中に起きた例外を通知する。
   */
  protected[controller] def alertFailToTransfer(cause: Exception) {
    // TODO
  }
}

object MuseumExhibitLoadManager {
  sealed abstract class LoadingEvent extends swing.event.Event
  case class Started() extends LoadingEvent
  case class ProgressChange(next: File, finishedCount: Int, queuedCount: Int) extends LoadingEvent
  case class Done() extends LoadingEvent
}

/**
 * データ読み込み Swing タスク
 */
abstract class LoadTask(loader: MuseumExhibitLoader,
    storage: Option[MuseumExhibitStorage]) extends SwingWorker[Unit, LoadTask.Chunk] {
  import LoadTask._
  import collection.mutable.Queue
  
  private val fileQueue = Queue.empty[Query]
  private var shutdown = false
  private var queuedCount = 0
  private var finishedCount = 0
  
  def doInBackground() {
    load()
  }
  
  @annotation.tailrec
  private def load() {
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
      val succeed = loader.makeMuseumExhibit(entity, file) match {
        case true => storage match {
          case None => true
          case Some(storage) => storage.save(file, entity) match {
            case Some(url) =>
              entity.filePath = url.toString
              true
            case None => false
          }
        }
        case false => false
      }
      if (succeed) {
        tableModel.updateElement(entity)
      }
      else {
        tableModel.removeElement(entity)
        publish(FailToLoad(file))
      }
      // 処理完了数の増加
      finishedCount += 1
    }
    
    if (!shutdown)
      load()
  }
  
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
  def tryQueue(tableModel: ListDataServiceSource[MuseumExhibit], files: Seq[File]) = {
    fileQueue.synchronized {
      // タスクが終了していなければ追加
      if (!shutdown) {
        val queries = files.view.map(file =>
          Query(tableModel.createElement(), tableModel, file)).toIndexedSeq
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
  private case class Query(entity: MuseumExhibit,
    tableModel: ListDataServiceSource[MuseumExhibit], file: File)
}

private[controller] object LoadTask {
  sealed abstract class Chunk
  case class FailToLoad(file: File) extends Chunk
}
