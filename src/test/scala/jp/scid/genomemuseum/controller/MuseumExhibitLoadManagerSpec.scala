package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import actors.{Futures}

import java.io.{File, IOException}
import java.net.URL

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitLoader, MuseumExhibitService}
import jp.scid.genomemuseum.gui.ExhibitTableModel
import MuseumExhibitLoadManager._

class MuseumExhibitLoadManagerSpec extends Specification with Mockito {
  private type Manager = MuseumExhibitLoadManager
  def is = "MuseumExhibitLoadManager" ^
    "loadExhibit" ^ 
      "ファイルの読み込みに成功する時" ^ successLoadingSpec(simpleManager) ^ bt ^
      "ファイルの読み込みに失敗する時" ^ failLoadingSpec(faliLoaderManager) ^ bt ^
    bt ^ "イベント" ^ 
      "Start" ^ canPublishStartEvent(simpleManager) ^ bt ^
      "ProgressChange" ^ canPublishProgressEvent(simpleManager) ^ bt ^
      "MessageChange" ^ canPublishMessageEvent(simpleManager) ^ bt ^
      "Done" ^ canPublishDoneEvent(simpleManager) ^ bt ^
    bt ^ "アラート表示" ^
      "形式不対応アラート" ^ canAlertInvalidFormat(faliLoaderManager) ^ bt ^
      "IOE 例外アラート" ^ canAlertFail(ioeLoaderManager) ^ bt ^
      "ParseExceptin 例外アラート" ^ canAlertFail(parseExpLoaderManager) ^ bt ^
    end
  
  def museumExhibitMock = {
    mock[MuseumExhibit]
  }
  
  /** 読み込みモック */
  def loaderOf(value: Boolean) = {
    val loader = mock[MuseumExhibitLoader]
    loader.makeMuseumExhibit(any, any) returns value
    loader
  }
  
  /** 例外を発生する読み込みモック */
  def loaderThrows(exepction: Exception) = {
    val loader = mock[MuseumExhibitLoader]
    loader.makeMuseumExhibit(any, any) throws exepction
    loader
  }
  
  /** サービスモック */
  def serviceMock = {
    val service = mock[MuseumExhibitService]
    service.create returns mock[MuseumExhibit].asInstanceOf[service.ElementClass]
    service
  }
  
  // マネージャーオブジェクト
  /** 読み込みが成功するマネージャ */
  def simpleManager = {
    new MuseumExhibitLoadManager(serviceMock, loaderOf(true))
  }
  /** 読み込みが失敗するマネージャ */
  def faliLoaderManager = {
    new MuseumExhibitLoadManager(serviceMock, loaderOf(false))
  }
  /** 読み込み中例外を送出するマネージャ */
  def ioeLoaderManager = {
    new MuseumExhibitLoadManager(serviceMock,
      loaderThrows(new IOException("exception")))
  }
  
  def parseExpLoaderManager = {
    new MuseumExhibitLoadManager(serviceMock,
      loaderThrows(new java.text.ParseException("exception", 0)))
  }
  
  def waitEDTProcessing() {
    import java.awt.EventQueue
    if (!EventQueue.isDispatchThread) {
      EventQueue.invokeAndWait(new Runnable {
        def run {}
      })
    }
  }
  
  lazy val dummyFile = File.createTempFile("MuseumExhibitLoadManagerSpec", "temp")
  lazy val dummyUrl = dummyFile.toURI.toURL
  
  val ioException = new java.io.IOException
  
  def successLoadingSpec(m: => Manager) =
    "ファイルから Some 値の Future 作成" ! loadExhibit(m).returnsFutureFromFile ^
    "URL から Some 値の Future 作成" ! loadExhibit(m).returnsFutureFromUrl ^
    "複数回の作成" ! loadExhibit(m).returnsTrueManyTimes
    
  def failLoadingSpec(m: => Manager) =
    "ファイルから None 値の Future 作成" ! loadExhibit(m).returnsNoneFutFromFile ^
    "URL から None 値の Future 作成" ! loadExhibit(m).returnsNoneFutFromUrl
  
  def canPublishStartEvent(m: => Manager) =
    "発行" ! eventStart(m).publish ^
    "ソースオブジェクト" ! eventStart(m).source
  
  def canPublishProgressEvent(m: => Manager) =
    "発行" ! eventProgress(m).publish ^
    "ソースオブジェクト" ! eventProgress(m).source ^
    "max 値" ! eventProgress(m).max ^
    "value 値" ! eventProgress(m).value
  
  def canPublishMessageEvent(m: => Manager) =
    "発行" ! eventMessage(m).publish ^
    "ソースオブジェクト" ! eventMessage(m).source ^
    "message 値" ! eventMessage(m).message
  
  def canPublishDoneEvent(m: => Manager) =
    "発行" ! eventDone(m).publish ^
    "ソースオブジェクト" ! eventDone(m).source
  
  def canAlertInvalidFormat(m: => Manager) = todo
//    "表示" ! invalidFormatAlert(m).display ^
//    "複数ファイルの時は複数表示" ! invalidFormatAlert(m).diaplayMany
  
  def canAlertFail(m: => Manager) = todo
//    "表示" ! alertFail(m).display ^
//    "複数ファイルの時は複数表示" ! alertFail(m).diaplayMany
  
    
  class TestBase(val manager: Manager)
  
  def loadExhibit(m: Manager) = new TestBase(m) {
    def returnsFutureFromFile = {
      manager.loadExhibit(dummyFile).get must beSome
    }
    
    def returnsFutureFromUrl = {
      manager.loadExhibit(dummyUrl).get must beSome
    }
    
    def returnsNoneFutFromFile = {
      manager.loadExhibit(dummyFile).get must beNone
    }
    
    def returnsNoneFutFromUrl = {
      manager.loadExhibit(dummyUrl).get must beNone
    }
    
    def returnsTrueManyTimes = {
      manager.loadExhibit(dummyUrl)
      manager.currentLoadTask.get
      manager.loadExhibit(dummyUrl).get must beSome
    }
  }
  
  abstract class EventTestBase[E <: TaskEvent: ClassManifest](m: Manager) extends TestBase(m) {
    import scala.collection.mutable.ListBuffer
    val eventBuffer = ListBuffer.empty[E]
    
    manager.reactions += {
      case e if implicitly[ClassManifest[E]].erasure.isInstance(e) =>
        eventBuffer += e.asInstanceOf[E]
    }
    
    def publish = {
      manager.loadExhibit(dummyFile)
      manager.currentLoadTask.get
      Thread.sleep(100)
      eventBuffer must not beEmpty
    }
    
    def source = {
      manager.loadExhibit(dummyFile)
      manager.currentLoadTask.get
      Thread.sleep(100)
      eventBuffer.map(_.task).distinct must haveSize(1)
    }
  }
  
  def eventStart(m: Manager) = new EventTestBase[Started](m) {
  }
  
  def eventProgress(m: Manager) = new EventTestBase[ProgressChange](m) {
    def max = {
      manager.loadExhibit(dummyFile)
      manager.currentLoadTask.get
      eventBuffer.map(_.max).headOption must beSome(1)
    }
    
    def value = {
      manager.loadExhibit(dummyFile)
      manager.currentLoadTask.get
      eventBuffer.map(_.value).headOption must beSome(1)
    }
  }
  
  def eventMessage(m: Manager) = new EventTestBase[MessageChange](m) {
    def message = {
      manager.loadExhibit(dummyFile)
      manager.currentLoadTask.get
      eventBuffer.map(_.message).headOption.getOrElse("") must not beEmpty
    }
  }
  
  def eventDone(m: Manager) = new EventTestBase[Done](m) {
  }
  
  def invalidFormatAlert(m: Manager) = new TestBase(spy(m)) {
    var tasks: Seq[manager.LoadTask] = Nil
    manager.alertInvalidFormat(any) answers { arg =>
      println("alertInvalidFormat")
      tasks = arg.asInstanceOf[Seq[manager.LoadTask]]
    }
    
    def display = {
      manager.loadExhibit(dummyUrl)
      manager.currentLoadTask.get
      
      tasks.nonEmpty must beTrue
    }
      
    def diaplayMany = {
      1 until 10 map (i => manager.loadExhibit(dummyUrl))
      manager.currentLoadTask.get
      
      tasks.size must_== 10
    }
  }
  
  def alertFail(m: Manager) = new TestBase(spy(m)) {
    var tasks: Seq[(manager.LoadTask, Exception)] = Nil
    manager.alertFailToLoad(any) answers { arg =>
      println("alertInvalidFormat")
      tasks = arg.asInstanceOf[Seq[(manager.LoadTask, Exception)]]
    }
    
    def display = {
      manager.loadExhibit(dummyUrl)
      manager.currentLoadTask.get
      
      tasks.nonEmpty must beTrue
    }
      
    def diaplayMany = {
      1 until 10 map (i => manager.loadExhibit(dummyUrl))
      manager.currentLoadTask.get
      
      tasks.size must_== 10
    }
  }
}
