package jp.scid.genomemuseum.gui

import collection.mutable.{Publisher, ListBuffer}

object EventQueuePublisherAdapter {
  /**
   * アダプタを生成
   */
  def apply[Msg](publisher: Publisher[Msg])(processTask: List[Msg] => Unit) = new EventQueuePublisherAdapter[Msg] {
    protected[gui] def process(chunk: List[Msg]) = processTask(chunk)
  }.connect(publisher)
}

/**
 * Publisher のメッセージを、イベントディスパッチスレッド上で処理するためのアダプタ
 */
abstract class EventQueuePublisherAdapter[Msg] extends PublisherAdapter[Msg] {
  type ObservableService = Publisher[Msg]
  
  private var chunkArgs: Option[ListBuffer[Msg]] = None
  
  private class Runner extends Runnable {
    def run() {
      process(flush())
    }
  }
  
  /**
   * メッセージの処理を行う。
   * 
   * このメソッドはイベントディスパッチスレッド上で呼び出される。
   */
  protected[gui] def process(chunk: List[Msg])
  
  override protected def notifyModel(service: ObservableService, message: Msg) {
    add(message)
  }
  
  final def add(msg: Msg*) = synchronized {
    val isSubmitted = chunkArgs match {
      case Some(args) => true
      case None =>
        chunkArgs = Some(ListBuffer.empty[Msg])
        false
    }
    chunkArgs.get ++= msg
    if (!isSubmitted) submit()
  }
  
  protected[gui] def submit() {
    import java.awt.EventQueue
    EventQueue.invokeLater(new Runner)
  }
  
  final private def flush(msg: Msg*) = synchronized {
    val arg = chunkArgs match {
      case Some(arg) => arg.toList
      case None => Nil
    }
    chunkArgs = None
    arg
  }
}
