package jp.scid.genomemuseum.gui

import collection.mutable.Publisher

import org.specs2._

class EventQueuePublisherAdapterSpec extends Specification with mock.Mockito {
  def is = "EventQueuePublisherAdapter" ^
    "追加" ^ canAdd ^
    "Publisher と接続" ^ canProcess ^
    "コンパニオンオブジェクト" ^ companionObjSpec ^
    end
  
  private class TestPublisher extends Publisher[Symbol] {
    override def publish(value: Symbol) = super.publish(value)
  }
  
  def canAdd() =
    "submit が 1 度呼び出される" ! add.callsSubmit ^
    "process が EDT 上で呼び出される" ! add.callsProcess ^
    bt
  
  def canProcess() =
    "イベントディスパッチスレッドで呼び出される" ! process.onEDT ^
    bt
  
  def companionObjSpec() =
    "apply 生成" ! companionObj.apply ^
    bt
  
  protected class TestAdapter extends EventQueuePublisherAdapter[Symbol] {
    var processed = List.empty[Symbol]
    override def process(chunk: List[Symbol]) {
      processed = processed ::: chunk
    }
  }
  
  def add() = new {
    private val publisher = new TestPublisher
    val adapter = spy(new TestAdapter)
    
    List('a, 'b, 'c) foreach (e => adapter.add(e))
    
    def callsSubmit = {
      there was one(adapter).submit()
    }
    
    def callsProcess = {
      waitEDTProcessing()
      adapter.processed must_== List('a, 'b, 'c)
    }
  }
  
  def process() = new {
    private val publisher = new TestPublisher
    val adapter = spy(new TestAdapter)
    adapter.connect(publisher)
    
    def onEDT = {
      List('a, 'b, 'c) foreach publisher.publish
      waitEDTProcessing()
      there was one(adapter).process(List('a, 'b, 'c))
    }
  }
  
  def companionObj = new {
    private val publisher = new TestPublisher
    val task = mock[List[Symbol] Function1 Unit]
    
    def apply = {
      EventQueuePublisherAdapter(publisher)(task)
      
      List('c, 'b, 'a) foreach publisher.publish
      waitEDTProcessing()
      there was atLeastOne(task).apply(any)
    }
  }
  
  private def waitEDTProcessing() {
    import java.awt.EventQueue
    if (!EventQueue.isDispatchThread) {
      EventQueue.invokeAndWait(new Runnable {
        def run {}
      })
    }
    Thread.sleep(500)
  }
}
