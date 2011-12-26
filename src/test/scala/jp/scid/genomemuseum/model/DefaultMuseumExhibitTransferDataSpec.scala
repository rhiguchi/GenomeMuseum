package jp.scid.genomemuseum.model

import java.awt.datatransfer.DataFlavor
import java.io.File
import java.net.URL

import DataFlavor.{javaFileListFlavor, stringFlavor}

import org.specs2._

class DefaultMuseumExhibitTransferDataSpec extends Specification with mock.Mockito {
  private type TransferData = DefaultMuseumExhibitTransferData
  private type Factory = (MuseumExhibit*) => TransferData
  import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}
  
  def is = "DefaultMuseumExhibitTransferData" ^
    "対応データフレーバー" ^ isDataFlavorSupportedSpec(transferDataOf) ^
    "対応フレーバー取得" ^ canGetTransferDataFlavors(transferDataOf) ^
    "データ取得" ^ canGetTransferData(transferDataOf) ^
    end
  
  private def transferDataOf(exhibits: MuseumExhibit*) = {
    new DefaultMuseumExhibitTransferData(exhibits)
  }
  
  def isDataFlavorSupportedSpec(f: Factory) =
    "MuseumExhibit" ! isDataFlavorSupported(f).exhibit ^
    "文字列" ! isDataFlavorSupported(f).string ^
    "ファイル" ! isDataFlavorSupported(f).file ^
    bt
  
  def canGetTransferDataFlavors(f: Factory) =
    "取得" ! getTransferDataFlavors(f).returnsFlavors ^
    bt
  
  def canGetTransferData(f: Factory) =
    "展示物取得" ! getTransferData(f).getExhibits ^
    "ファイル取得" ! getTransferData(f).getFiles ^
    "文字列取得" ! getTransferData(f).getString ^
    bt
  
  def exhibitOf = MuseumExhibitMock.of _
  
  class TestBase(f: Factory) {
    val exhibits = 0 to 9 map (i => exhibitOf("e" + i))
    private[model] val t = f(exhibits: _*)
  }
  
  def isDataFlavorSupported(f: Factory) = new TestBase(f) {
    def exhibit = t.isDataFlavorSupported(exhibitDataFlavor) must beTrue
    
    def string = t.isDataFlavorSupported(stringFlavor) must beTrue
    
    def file = t.isDataFlavorSupported(javaFileListFlavor) must beTrue
  }
  
  def getTransferDataFlavors(f: Factory) = new TestBase(f) {
    def returnsFlavors =
      t.getTransferDataFlavors.toList must contain(exhibitDataFlavor, stringFlavor, javaFileListFlavor)
  }
  
  def getTransferData(f: Factory) = new TestBase(f) {
    def getExhibits =
      t.getTransferData(exhibitDataFlavor) must beAnInstanceOf[TransferData]
    
    def getFiles = {
      import scala.collection.JavaConverters._
      
      val files = 0 to 5 map (i => new File("file" + i))
      (0 to 5).foreach(i => exhibits(i).sourceFile returns Some(files(i)))
      (6 to 9).foreach(i => exhibits(i).sourceFile returns None)
      
      t.getTransferData(javaFileListFlavor) must_== files.asJava
    }
    
    def getString = {
      val stringConverter = (exhibit: MuseumExhibit) => exhibit.name
      t.stringConverter = stringConverter
      val lineSep = System.getProperty("line.separator")
      val text = exhibits map stringConverter mkString lineSep + lineSep
      t.getTransferData(stringFlavor) must_== text
    }
  }
}
