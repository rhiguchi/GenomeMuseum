package jp.scid.genomemuseum.gui

import java.util.concurrent.{Executors, TimeUnit, ScheduledFuture}

import collection.mutable.Publisher
import collection.script.Message

import jp.scid.gui.DataListModel

object PublisherScheduleTaskAdapter {
  def apply[A](task: Message[A] => Unit) =
    new PublisherScheduleTaskAdapter[A](task)
}

/**
 * Publisher が発行するメッセージが最後に受信されてから
 * 一定時間後に処理を実行するアダプタクラス
 */
class PublisherScheduleTaskAdapter[A](task: Message[A] => Unit) extends PublisherAdapter[Message[A]] {
  val executor = Executors.newSingleThreadScheduledExecutor
  
  var latestFeature: Option[ScheduledFuture[_]] = None
  
  var delay = 200L
  
  type ObservableService = Publisher[Message[A]]
  
  protected def notifyModel(service: ObservableService, message: Message[A]) {
    schedule(new UpdateTask(message))
  }
  
  private def schedule(task: Runnable) = synchronized {
    latestFeature.foreach(_.cancel(true))
    val f = executor.schedule(task, delay, TimeUnit.MILLISECONDS)
    
    latestFeature = Some(f)
  }
  
  private class UpdateTask(message: Message[A]) extends Runnable {
    def run() {
      task(message)
      latestFeature = None
    }
  }
}