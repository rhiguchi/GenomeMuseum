package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import actors.{Futures}

import java.io.File
import java.net.URL

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitLoader, MuseumExhibitService}
import jp.scid.genomemuseum.gui.ExhibitTableModel

class MuseumExhibitLoadManagerSpec extends Specification with Mockito {
  def is = "MuseumExhibitLoadManager" ^
    "loadExhibits" ^ 
      "ファイルの読み込みに成功する時" ^ canLoadExhibits(rightManager) ^ bt ^
      "ファイルの読み込みに失敗する時" ^ failLoadExhibits(iaeManager) ^ bt ^
      "ファイル読み込み後" ^ canLoadExhibits(queuedOneceManager) ^ bt ^
    bt ^ "イベント" ^ 
      "ファイルの読み込みに成功する時" ^ canPublishEvent(rightManager) ^ bt ^
      "ファイルの読み込みに失敗する時" ^ canPublishEvent(iaeManager) ^ bt ^
      "ファイル読み込み後" ^ canPublishEvent(queuedOneceManager) ^ bt ^
    bt ^ "アラート表示" ^
      "ファイル形式が不明のアラート" ^ doShoIAEAlert(failManager) ^ bt
    end
  
  private type TableModel = ExhibitTableModel
  
  def rightManager = new MuseumExhibitLoadManager(serviceMock(), loadableLoader)
  
  def iaeManager = new MuseumExhibitLoadManager(serviceMock(), unloadableLoader)
  
  def failManager = new MuseumExhibitLoadManager(serviceMock(), throwableLoader)
  
  def queuedOneceManager = {
    val manager = rightManager
    manager.loadExhibits(Some(tableModelMock(museumExhibitMock)),
      singletonDummyFile).apply
    manager
  }
  
  def museumExhibitMock = {
    mock[MuseumExhibit]
  }
  
  def serviceMock(returnEntity: MuseumExhibit = exhibit) = {
      val service = mock[MuseumExhibitService]
      service.create returns returnEntity.asInstanceOf[service.ElementClass]
      service
  }
  
  def tableModelMock(returnEntity: MuseumExhibit) = {
    val model = mock[TableModel]
    val service = serviceMock(returnEntity)
    model
  }
  
  def singletonDummyFile = List(new File("dummy"))
  
  val ioException = new java.io.IOException
  
  def loadableLoader = new MuseumExhibitLoader() {
    override def makeMuseumExhibit(e: MuseumExhibit, f: URL) = {
      Thread.sleep(200)
      true
    }
  }
  
  def unloadableLoader = new MuseumExhibitLoader() {
    override def makeMuseumExhibit(e: MuseumExhibit, f: URL) = {
      Thread.sleep(200)
      false
    }
  }
  
  def throwableLoader = new MuseumExhibitLoader() {
    override def makeMuseumExhibit(e: MuseumExhibit, f: URL) = {
      throw ioException
    }
  }
  
  def canLoadExhibits(manager: => MuseumExhibitLoadManager) =
    "テーブルの createElement コール" ! todo ^
    "テーブルの updateElement コール" ! loadExhibits(manager).callUpdateElement ^
    "createElement はファイル数通りコール" ! loadExhibits(manager).callCreateElement2.pendingUntilFixed ^
    "updateElement は順序通りコール" ! loadExhibits(manager).callUpdateElement2.pendingUntilFixed ^
    "時間差呼び出し" ! loadExhibits(manager).twice.pendingUntilFixed
    
  def failLoadExhibits(manager: => MuseumExhibitLoadManager) =
    "テーブルの createElement コール" ! todo ^
    "テーブルの removeElement コール" ! loadExhibits(manager).callsRemoveElement
  
  def canPublishEvent(manager: => MuseumExhibitLoadManager) =
    "Started イベント" ! event(manager).publishStarted ^
    "ProgressChange イベント" ! event(manager).publishProgressChange ^
    "Done イベント" ! event(manager).publishDone
  
  def canPublishEventWithoutProgress(manager: => MuseumExhibitLoadManager) =
    "Started イベント" ! event(manager).publishStarted ^
    "ProgressChange イベントは呼ばない" ! event(manager).notPublishProgressChange ^
    "Done イベント" ! event(manager).publishDone
  
  def doShoIAEAlert(manager: => MuseumExhibitLoadManager) =
    "表示" ! alertSpec(manager).display ^
    "複数ファイルの時は複数表示" ! alertSpec(manager).displayMany
  
    val exhibit = museumExhibitMock
    
  class TestBase {
    import scala.collection.mutable.ListBuffer
    
    
    
    lazy val model = tableModelMock(exhibit)
  }
  
  def loadExhibits(manager: MuseumExhibitLoadManager) = new TestBase {
    def callUpdateElement = {
      manager.loadExhibits(Some(model), singletonDummyFile).apply
      there was one(model).updateElement(exhibit)
    }
    
    def callsRemoveElement = {
      manager.loadExhibits(Some(model), singletonDummyFile).apply
      there was one(model).removeElement(exhibit)
    }
    
    def callCreateElement2 = {
      val files = Range(0, 10) map (num => new File("dummyfile" + num))
      manager.loadExhibits(Some(model), files)
      there was 10.times(model).createElement
    }
    
    def callUpdateElement2 = {
      val files = Range(0, 5) map (num => new File("dummyfile" + num))
      
      val service = model.dataService
      val e1, e2, e3, e4, e5 = museumExhibitMock.asInstanceOf[service.ElementClass]
      model.createElement returns (e1, e2, e3, e4, e5)
      
      manager.loadExhibits(Some(model), files).apply
      there was one(model).updateElement(e1) then
      one(model).updateElement(e2) then
      one(model).updateElement(e3) then
      one(model).updateElement(e4) then
      one(model).updateElement(e5)
    }
    
    def twice = {
      val service = model.dataService
      val e1, e2 = museumExhibitMock.asInstanceOf[service.ElementClass]
      model.createElement returns (e1, e2)
      manager.loadExhibits(Some(model), singletonDummyFile)
      Thread.sleep(200)
      manager.loadExhibits(Some(model), singletonDummyFile).apply
      there was one(model).updateElement(e1) then
      one(model).updateElement(e2)
    }
  }
  
  def event(manager: MuseumExhibitLoadManager) = new TestBase {
    import MuseumExhibitLoadManager._
    
    def publishStarted = {
      var published = false
      manager.reactions += {
        case Started() => published = true
      }
      manager.loadExhibits(None, singletonDummyFile)
      
      // 待機
      val resultFut = Futures.future {
        while(!published) Thread.sleep(50)
      }
      Futures.awaitAll(3000, resultFut)
      
      published must beTrue
    }
    
    def publishDone = {
      var published = false
      manager.reactions += {
        case Done() => published = true
      }
      manager.loadExhibits(None, singletonDummyFile)
      // 待機
      val resultFut = Futures.future {
        while(!published) Thread.sleep(50)
      }
      Futures.awaitAll(3000, resultFut)
      
      published must beTrue
    }
    
    def publishProgressChange = {
      import scala.collection.mutable.ListBuffer
      
      var published = ListBuffer.empty[ProgressChange]
      manager.reactions += {
        case e @ ProgressChange(_, _, _) =>
          published += e
      }
      
      val Seq(e1, e2, e3, e4, e5) = Range(0, 5) map (n => new File("f" + n))
      manager.loadExhibits(None, List(e1, e2, e3, e4, e5)).apply
      
      published.toList.map(e => (e.next, e.finishedCount, e.queuedCount)) must
        contain((e1, 0, 5), (e2, 1, 5), (e3, 2, 5), (e4, 3, 5), (e5, 4, 5)).only.inOrder 
    }
    
    def notPublishProgressChange = {
      import scala.collection.mutable.ListBuffer
      
      var published = ListBuffer.empty[ProgressChange]
      manager.reactions += {
        case e @ ProgressChange(_, _, _) =>
          published += e
      }
      
      val Seq(e1, e2, e3, e4, e5) = Range(0, 5) map (n => new File("f" + n))
      manager.loadExhibits(None, List(e1, e2, e3, e4, e5)).apply
      
      published must beEmpty
    }
  }
  
  def alertSpec(manager: MuseumExhibitLoadManager) = new TestBase {
    def display = {
      val spyManager = spy(manager)
      val files = singletonDummyFile
      spyManager.loadExhibits(None, files).apply
      Thread.sleep(100)
      
      there was one(spyManager).alertFailToLoad(files)
    }
      
    def displayMany = {
      import scala.collection.mutable.ListBuffer
      
      var displayed = ListBuffer.empty[File]
        
      val spyManager = spy(manager)
      spyManager.alertFailToLoad(any) answers { arg =>
        displayed ++= arg.asInstanceOf[List[File]]
      }
      
      val files = Range(0, 5).toList map (n => new File("f" + n))
      spyManager.loadExhibits(None, files).apply
      Thread.sleep(100)
      
      displayed must haveTheSameElementsAs(files)
    }
  }
}
