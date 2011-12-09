package jp.scid.genomemuseum.model

import java.awt.datatransfer.DataFlavor
import java.io.File
import java.net.URL

import DataFlavor.{javaFileListFlavor, stringFlavor}

import org.specs2._
import mock._

class MuseumExhibitTransferDataImplSpec extends Specification with Mockito {
  private type TransferData = MuseumExhibitTransferDataImpl
  private type Factory = (MuseumExhibit*) => TransferData
  import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}
  
  def is = "MuseumExhibitTransferDataImpl" ^
    "対応データフレーバー" ^ isDataFlavorSupportedSpec(transferDataOf) ^
    "対応フレーバー取得" ^ canGetTransferDataFlavors(transferDataOf) ^
    "データ取得" ^ canGetTransferData(transferDataOf) ^
    end
  
  private def transferDataOf(exhibits: MuseumExhibit*) = {
    new MuseumExhibitTransferDataImpl(exhibits)
  }
  
  def isDataFlavorSupportedSpec(f: Factory) =
    "MuseumExhibit" ! isDataFlavorSupported(f).exhibit ^
    "文字列" ! isDataFlavorSupported(f).string ^
    "ファイル" ! isDataFlavorSupported(f).file ^
    "ファイル格納管理オブジェクトが無い時はファイル転送できない" ! isDataFlavorSupported(f).fileWithOutStorage ^
    bt
  
  def canGetTransferDataFlavors(f: Factory) =
    "取得" ! getTransferDataFlavors(f).returnsFlavors ^
    "ファイル格納管理オブジェクトがある時は file も含まれる" ! getTransferDataFlavors(f).returnsFileFlavor ^
    bt
  
  def canGetTransferData(f: Factory) =
    "展示物取得" ! getTransferData(f).getExhibits ^
    "ファイル取得" ! getTransferData(f).getFiles ^
    "文字列取得" ! getTransferData(f).getString ^
    bt
  
  def exhibitOf = MuseumExhibitSpec.mockOf _
  
  class TestBase(f: Factory) {
    val exhibits = 0 to 9 map (i => exhibitOf("e" + i))
    val storage = mock[MuseumExhibitStorage]
    private[model] val t = f(exhibits: _*)
  }
  
  def isDataFlavorSupported(f: Factory) = new TestBase(f) {
    def exhibit = t.isDataFlavorSupported(exhibitDataFlavor) must beTrue
    
    def string = t.isDataFlavorSupported(stringFlavor) must beTrue
    
    def fileWithOutStorage = t.isDataFlavorSupported(javaFileListFlavor) must beFalse
    
    def file = {
      t.fileStorage = Some(storage)
      t.isDataFlavorSupported(javaFileListFlavor) must beTrue
    }
  }
  
  def getTransferDataFlavors(f: Factory) = new TestBase(f) {
    def returnsFlavors =
      t.getTransferDataFlavors.toList must contain(exhibitDataFlavor, stringFlavor).only
    
    def returnsFileFlavor = {
      t.fileStorage = Some(storage)
      t.getTransferDataFlavors.toList must contain(javaFileListFlavor)
    }
  }
  
  def getTransferData(f: Factory) = new TestBase(f) {
    def getExhibits =
      t.getTransferData(exhibitDataFlavor) must beAnInstanceOf[TransferData]
    
    def getFiles = {
      import scala.collection.JavaConverters._
      
      val fileUrls = 0 to 5 map (i => new File("file" + i)) map (_.toURI.toURL)
      val otherUrls = new URL("http://example.com")
      storage.getSource(any) returns None
      (0 to 5).foreach(i => storage.getSource(exhibits(i)) returns Some(fileUrls(i)))
      (6 to 8).foreach(i => storage.getSource(exhibits(i)) returns Some(otherUrls))
      
      t.fileStorage = Some(storage)
      t.getTransferData(javaFileListFlavor) must_== fileUrls.map(u => new File(u.toURI)).asJava
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
