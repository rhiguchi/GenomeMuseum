package jp.scid.genomemuseum.model

import java.util.concurrent.Callable
import org.specs2._
import scala.actors.Actor

class HttpDownloaderSpec extends SpecificationWithJUnit {
  import HttpDownloader._

  def is = "HttpDownloader" ^ sequential ^
    "ファイル読み出し" ^ canCall(defaultDownloader) ^ bt ^
    "メッセージ" ^ canSendMessage(defaultDownloader) ^ bt ^
    end
  
  val httpSource = "http://togows.dbcls.jp/entry/pubmed/16381885"
  
  def defaultDownloader = new HttpDownloader(httpSource)
  
  def canCall(d: => HttpDownloader) =
    "ダウンロード" ! call(d).download

  def canSendMessage(d: => HttpDownloader) =
    "Start メッセージ" ! message(d).start ^
    "InProgress メッセージ" ! message(d).inProgress ^
    "Done メッセージ" ! message(d).done
    
  def call(d: HttpDownloader) = new Object {
    def download = d.call.length must_== 3187 
  }
  
  def message(d: HttpDownloader) = new Object {
    def start = {
      var msg: Option[Start] = None
      
      val channel = Actor.actor {
        Actor.self.reactWithin(1000) {
          case e @ Start(_, _) => msg = Some(e)
        }
      }
      d.outputChannel = Some(channel)
      d.call
      while (channel.getState == Actor.State.Runnable)
        Thread.sleep(20)
      
      msg must beSome(Start(d, 3187))
    }
    
    def inProgress = {
      var msg: Option[InProgress] = None
      
      val channel = Actor.actor {
        Actor.self.reactWithin(1000) {
          case e @ InProgress(`d`, _) => msg = Some(e)
        }
      }
      d.outputChannel = Some(channel)
      d.call
      while (channel.getState == Actor.State.Runnable)
        Thread.sleep(20)
                
      msg must beSome[InProgress]
    }
   
    def done = {
      var msg: Option[Done] = None
      
      val channel = Actor.actor {
        Actor.self.reactWithin(1000) {
          case e @ Done(_) => msg = Some(e)
        }
      }
      d.outputChannel = Some(channel)
      d.call
      while (channel.getState == Actor.State.Runnable)
        Thread.sleep(20)
                
      msg must beSome(Done(d))
    }
  }
}