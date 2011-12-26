package jp.scid.genomemuseum.gui

import java.util.concurrent.{SynchronousQueue, TimeUnit}

import collection.mutable.Publisher
import collection.script.{Message, Include, Update, Remove}

import org.specs2._

class PublisherScheduleTaskAdapterSpec extends Specification {
  def is = "PublisherScheduleTaskAdapter" ^
    "処理の実行" ^ canReloadSource ^
    end
  
  private class TestPublisher extends Publisher[Message[Symbol]] {
    def publishInclude(value: Symbol) = publish(new Include(value))
  }
  
  def canReloadSource =
    "通知を受けてから指定秒後に実行" ! executing.callExecute ^
    "最後の通知を受けてから指定秒後に実行" ! executing.delaying ^
    bt
  
  def executing = new {
    private val publisher = new TestPublisher
    
    val start = System.currentTimeMillis
    val publishedTime = new SynchronousQueue[Long]
    
    val adapter = PublisherScheduleTaskAdapter[Symbol] { msg =>
      publishedTime.offer(System.currentTimeMillis, 3000, TimeUnit.MILLISECONDS)
    }
    adapter.connect(publisher)
    
    def callExecute = {
      
      publisher.publishInclude('test)
      val time = publishedTime.poll(3000, TimeUnit.MILLISECONDS)
      time - start must be_>(200L)
    }
    
    def delaying = {
      adapter.delay = 500
      
      1 to 5 foreach { _ =>
        publisher.publishInclude('test)
        Thread.sleep(500)
      }
      
      val time = publishedTime.poll(3000, TimeUnit.MILLISECONDS)
      time - start must be_>(2000L)
    }
  }
}
