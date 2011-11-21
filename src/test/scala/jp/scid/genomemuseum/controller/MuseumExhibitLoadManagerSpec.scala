package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import actors.{Futures}

import java.io.File

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitLoader}
import jp.scid.genomemuseum.gui.ListDataServiceSource

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
      "ファイル形式が不明のアラート" ^ doShoIAEAlert(iaeManager) ^ bt
    end
  
  private type TableModel = ListDataServiceSource[MuseumExhibit]
  
  def rightManager = new RightMuseumExhibitLoadManager
  
  def iaeManager = new IAEMuseumExhibitLoadManager
  
  def queuedOneceManager = {
    val manager = new RightMuseumExhibitLoadManager
    manager.loadExhibits(tableModelMock(museumExhibitMock),
      singletonDummyFile).apply
    manager
  }
  
  def museumExhibitMock = {
    mock[MuseumExhibit]
  }
  
  def tableModelMock(returnEntity: MuseumExhibit) = {
    val model = mock[TableModel]
    model.createElement returns returnEntity
    model
  }
  
  def singletonDummyFile = List(new File("dummy"))
  
  val illegalArgumentException = new IllegalArgumentException
  
  class RightMuseumExhibitLoadManager extends MuseumExhibitLoadManager {
    override val loader = new MuseumExhibitLoader {
      override def makeMuseumExhibit(e: MuseumExhibit, f: File) = {
        Thread.sleep(200)
        true
      }
    }
  }
    
  class IAEMuseumExhibitLoadManager extends MuseumExhibitLoadManager {
    override val loader = new MuseumExhibitLoader {
      override def makeMuseumExhibit(e: MuseumExhibit, f: File) = {
        Thread.sleep(200)
        false
      }
    }
  }
  
  def canLoadExhibits(manager: => MuseumExhibitLoadManager) =
    "テーブルの createElement コール" ! loadExhibits(manager).callCreateElement ^
    "テーブルの updateElement コール" ! loadExhibits(manager).callUpdateElement ^
    "createElement はファイル数通りコール" ! loadExhibits(manager).callCreateElement2 ^
    "updateElement は順序通りコール" ! loadExhibits(manager).callUpdateElement2 ^
    "時間差呼び出し" ! loadExhibits(manager).twice
    
  def failLoadExhibits(manager: => MuseumExhibitLoadManager) =
    "テーブルの createElement コール" ! loadExhibits(manager).callCreateElement ^
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
  
  class TestBase {
    import scala.collection.mutable.ListBuffer
    
    val exhibit = museumExhibitMock
    
    lazy val model = tableModelMock(exhibit)
  }
  
  def loadExhibits(manager: MuseumExhibitLoadManager) = new TestBase {
    def callCreateElement = {
      manager.loadExhibits(model, singletonDummyFile)
      there was one(model).createElement
    }
    
    def callUpdateElement = {
      manager.loadExhibits(model, singletonDummyFile).apply
      there was one(model).updateElement(exhibit)
    }
    
    def callsRemoveElement = {
      manager.loadExhibits(model, singletonDummyFile).apply
      there was one(model).removeElement(exhibit)
    }
    
    def callCreateElement2 = {
      val files = Range(0, 10) map (num => new File("dummyfile" + num))
      manager.loadExhibits(model, files)
      there was 10.times(model).createElement
    }
    
    def callUpdateElement2 = {
      val files = Range(0, 5) map (num => new File("dummyfile" + num))
      val e1, e2, e3, e4, e5 = museumExhibitMock
      model.createElement returns (e1, e2, e3, e4, e5)
      manager.loadExhibits(model, files).apply
      there was one(model).updateElement(e1) then
      one(model).updateElement(e2) then
      one(model).updateElement(e3) then
      one(model).updateElement(e4) then
      one(model).updateElement(e5)
    }
    
    def twice = {
      val e1, e2 = museumExhibitMock
      model.createElement returns (e1, e2)
      manager.loadExhibits(model, singletonDummyFile)
      Thread.sleep(200)
      manager.loadExhibits(model, singletonDummyFile).apply
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
      manager.loadExhibits(model, singletonDummyFile)
      
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
      manager.loadExhibits(model, singletonDummyFile)
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
      manager.loadExhibits(model, List(e1, e2, e3, e4, e5)).apply
      
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
      manager.loadExhibits(model, List(e1, e2, e3, e4, e5)).apply
      
      published must beEmpty
    }
  }
  
  def alertSpec(manager: MuseumExhibitLoadManager) = new TestBase {
    def display = {
      val spyManager = spy(manager)
      val files = singletonDummyFile
      spyManager.loadExhibits(model, files).apply
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
      spyManager.loadExhibits(model, files).apply
      Thread.sleep(100)
      
      displayed must haveTheSameElementsAs(files)
    }
  }
}
