package jp.scid.genomemuseum.controller

import org.mockito.Matchers.{eq => mockEq}
import org.jdesktop.application.ResourceMap

import org.specs2._

import actors.{Futures}

import java.io.{File, IOException, Reader}
import java.net.URL
import javax.swing.SwingWorker
import java.util.concurrent.ExecutorService

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitLoader, MuseumExhibitService,
  MuseumExhibitFileLibrary}
import MuseumExhibit.FileType._
import MuseumExhibitLoadManager._

class MuseumExhibitLoadManagerSpec extends Specification with mock.Mockito {
  private type Manager = MuseumExhibitLoadManager
  private type Factory = (MuseumExhibitService, MuseumExhibitLoader) => MuseumExhibitLoadManager
  
  val emptyResource = File.createTempFile("empty", ".txt").toURI.toURL
  
  def is = "MuseumExhibitLoadManager" ^
    "展示物の読み込みができる" ^ canLoadMuseumExhibit(createManager) ^
    "読み込みタスクの実行" ^ executeSpec(createManager) ^
    end
  
  def createManager() = new MuseumExhibitLoadManager
  
  def canLoadMuseumExhibit(m: => MuseumExhibitLoadManager) =
    "GenBank ファイルの読み込み" ! loadMuseumExhibit(m).genBank ^
    "Fasta ファイルの読み込み" ! loadMuseumExhibit(m).fasta ^
    "不明形式ファイルは読み込みを実行しない" ! loadMuseumExhibit(m).unknown ^
    bt
  
  def executeSpec(m: => MuseumExhibitLoadManager) =
    "ハンドラの追加" ! execute(m).addHandler ^
    "実行される" ! execute(m).execute ^
    bt
  
  def loadMuseumExhibit(manager: MuseumExhibitLoadManager) = new {
    val museumExhibitLoader = mock[MuseumExhibitLoader]
    
    manager.museumExhibitLoader = museumExhibitLoader
    
//    val service = mock[MuseumExhibitService]
//    manager.museumExhibitService = Some(service)
    
    val exhibit = mock[MuseumExhibit] //.asInstanceOf[service.ElementClass]
    
    def genBank = {
      museumExhibitLoader.findFormat(emptyResource) returns GenBank
      val result = manager.loadMuseumExhibit(exhibit, emptyResource)
      (result must beTrue) and
      (there was one(museumExhibitLoader).loadMuseumExhibit(exhibit, emptyResource, GenBank))
    }
    
    def fasta = {
      museumExhibitLoader.findFormat(emptyResource) returns FASTA
      val result = manager.loadMuseumExhibit(exhibit, emptyResource)
      (result must beTrue) and
      (there was one(museumExhibitLoader).loadMuseumExhibit(exhibit, emptyResource, FASTA))
    }
    
    def unknown = {
      museumExhibitLoader.findFormat(emptyResource) returns Unknown
      val result = manager.loadMuseumExhibit(exhibit, emptyResource)
      (result must beFalse) and
      (there was no(museumExhibitLoader).loadMuseumExhibit(any, any, any))
    }
  }
  
  def execute(manager: MuseumExhibitLoadManager) = new {
    val task = new SwingWorker[Unit, Unit] {
      def doInBackground() {}
    }
    manager execute task
    
    def addHandler = task.getPropertyChangeSupport.getPropertyChangeListeners
      .toList.contains(manager.TaskPropertyChangeHandler)
    def execute = task.getState must_!= SwingWorker.StateValue.PENDING
  }
}
