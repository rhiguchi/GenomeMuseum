package jp.scid.genomemuseum.controller

import java.util.concurrent.{locks, Callable, Future, ExecutorService,
  Executors, CancellationException, ExecutionException}
import locks.ReentrantReadWriteLock
import javax.swing.SwingWorker

import collection.mutable.SynchronizedQueue
import util.control.Exception.allCatch

private[controller] object SwingTaskService {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[SwingTaskService[_, _]])
  
  /** 処理経過通知オブジェクトの基底クラス */
  sealed abstract class Chunk[A, Q <: Callable[A]] {
    /** 対象の処理 */
    def query: Q
  }
  /** 実行中に例外が生じたことを表すオブジェクト */
  case class ExceptionThrown[A, Q <: Callable[A]](query: Q, cause: Exception) extends Chunk[A, Q]
  /** 実行が正常に完了したことを表すオブジェクト */
  case class Success[A, Q <: Callable[A]](query: Q, result: A) extends Chunk[A, Q]
}

/**
 * Callable をクエリとして実行を行う Swing タスク
 */
abstract class SwingTaskService[A, Q <: Callable[A]](executor: ExecutorService)
    extends SwingWorker[Unit, SwingTaskService.Chunk[A, Q]] {
  import SwingTaskService._
  
  /** 結果オブジェクトの型 */
  type ResultChunk = Chunk[A, Q]
  
  /** 登録されたタスク数 */
  private var taskCount = 0
  /** 完了したタスク数 */
  private var finished = 0
  /** 停止状態確認用ロック */
  private val shutdownLock = new ReentrantReadWriteLock
  /** 実行中のタスクを納めたキュー */
  private val taskQueue = new SynchronizedQueue[TaskEntry]
  
  /**
   * 処理を登録する。
   * このサービスが有効でタスクの追加が行われた突起、 Future が返る。
   */
  def trySubmit(task: Q): Option[Future[A]] = {
    shutdownLock.readLock.lock()
    try {
      val futureOp = executor.isShutdown match {
        case false =>
          taskCount += 1
          Some(executor.submit(task))
        case true => None
      }
      futureOp.map(f => TaskEntry(task, f)).foreach(t => taskQueue.enqueue(t))
      futureOp
    }
    finally { shutdownLock.readLock.unlock() }
  }
  
  def doInBackground() {
    dequeue()
  }
  
  @annotation.tailrec
  private def dequeue() {
    /** クエリキューの先頭を取得 */
    if (!isCancelled) try {
      taskQueue.dequeueFirst(_ => true) match {
        case Some(query) =>
          process(query)
          finished += 1
        case None =>
          // 1 秒待機してキューが追加されなかったら終了。
          Thread.sleep(1000)
          shutdownLock.writeLock.lock()
          try {
            if (taskQueue.isEmpty)
              executor.shutdown()
          }
          finally { shutdownLock.writeLock.unlock() }
      }
    }
    catch {
      case e: CancellationException => // 個別キャンセルは無視
      case e => // その他の例外は再スロー
        executor.shutdownNow()
        throw e
    }
    else {
      executor.shutdownNow()
    }
    
    if (!executor.isShutdown)
      dequeue()
  }
  
  /**
   * 読み込み処理を行う。
   * @return ファイルが形式に対応し、読み込みが行われたときは {@code true} 。
   */
  private[controller] def process(entry: TaskEntry) {
    val TaskEntry(query, future) = entry
    
    // イベント発行
    firePropertyChange("target", null, query)
    
    val chunk = allCatch.either(future.get) match {
      case Right(result) => Success(query, result)
      case Left(e: ExecutionException) => e.getCause match {
        case e: Exception => ExceptionThrown[A, Q](query, e)
        case throwable => throw throwable
      }
      case Left(throwable) => throw throwable
    }
    
    publish(chunk)
  }
  
  /** 処理が完了した数 */
  def getFinishedCount = finished
  
  /** 処理を登録した数 */
  def getTaskCount = taskCount
  
  /** クエリとフィーチャのオブジェクト */
  private case class TaskEntry(query: Q, future: Future[A])
}
