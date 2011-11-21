package jp.scid.genomemuseum.controller

import java.io.File
import java.beans.{PropertyChangeListener, PropertyChangeEvent}
import java.util.concurrent.ExecutionException
import javax.swing.SwingWorker
import SwingWorker.StateValue

import actors.{Future, Futures}
import swing.Publisher

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitLoader}
import jp.scid.genomemuseum.gui.ListDataServiceSource

/**
 * ファイルから展示物を作成するマネージャ
 */
class MuseumExhibitLoadManager extends Publisher {
  import MuseumExhibitLoadManager._
  
  /** ファイルから MuseumExhibit を作成するオブジェクト */
  private[controller] val loader = new MuseumExhibitLoader
  
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
  private def createLoadTask() = new LoadTask(loader) {
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
abstract class LoadTask(loader: MuseumExhibitLoader) extends SwingWorker[Unit, LoadTask.Chunk] {
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
      loader.makeMuseumExhibit(entity, file) match {
        case false =>
          tableModel.removeElement(entity)
          publish(FailToLoad(file))
        case true =>
          tableModel.updateElement(entity)
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
