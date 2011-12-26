package jp.scid.genomemuseum.model.squeryl

import java.net.URI
import java.io.File

import org.specs2._

import jp.scid.genomemuseum.model.UriFileStorage

class MuseumExhibitSpec extends Specification with mock.Mockito {
  def is = "MuseumExhibit" ^ sequential ^
    "exhibitFileStorage" ^ exhibitFileStorageSpec(exhibit) ^
    "sourceFile 取得" ^ canGetSourceFile(exhibit) ^
    "sourceFile 設定" ^ canSetSourceFile(exhibit) ^
    end
  
  def exhibit = new MuseumExhibit
  
  def exhibitFileStorageSpec(e: => MuseumExhibit) =
    "初期値は None" ! exhibitFileStorage(e).initVal ^
    bt
  
  def canGetSourceFile(e: => MuseumExhibit) =
    "dataSourceUri が空白だと None" ! sourceFile(e).empty ^
    "dataSourceUri から File を作成" ! sourceFile(e).fileUri ^
    "dataSourceUri がファイル URI ではない時 None" ! sourceFile(e).notFileUri ^
    "exhibitFileStorage から値を取得" ! sourceFile(e).fromStorage ^
    bt
  
  def canSetSourceFile(e: => MuseumExhibit) =
    "None だと dataSourceUri が空白" ! sourceFileGet(e).none ^
    "ファイルの URI が dataSourceUri に適用" ! sourceFileGet(e).fileUri ^
    "exhibitFileStorage から値を取得" ! sourceFileGet(e).fromStorage ^
    bt
  
  def exhibitFileStorage(e: MuseumExhibit) = new {
    def initVal = e.exhibitFileStorage must beNone
  }
  
  def sourceFile(e: MuseumExhibit) = new {
    val uriString1 = "file:/path/to/file1"
    val uriString2 = "file:/path/to/file2"
    e.exhibitFileStorage = None
    
    def empty = {
      e.dataSourceUri = ""
      e.sourceFile must beNone
    }
    
    def fileUri = {
      List(uriString1, uriString2).flatMap(u => setUriAndGetFile(u)) must_==
        List(new File("/path/to/file1"), new File("/path/to/file2"))
    }
    
    def notFileUri = {
      List("http://example.com/x", "ftp://example.net/file")
        .flatMap(u => setUriAndGetFile(u)) must beEmpty
    }
    
    private def setUriAndGetFile(uri: String): Option[File] = {
      e.dataSourceUri = uri
      e.sourceFile
    }
    
    def fromStorage = {
      val file1 = new File("lib/file1")
      val file2 = new File("lib/file2")
      val storage = mock[UriFileStorage]
      e.exhibitFileStorage = Some(storage)
      storage.getFile(URI.create(uriString1)) returns file1
      storage.getFile(URI.create(uriString2)) returns file2
      
      List(uriString1, uriString2).flatMap(u => setUriAndGetFile(u)) must_==
        List(file1, file2)
    }
  }
  
  def sourceFileGet(e: MuseumExhibit) = new {
    e.dataSourceUri = "xxx"
    val file1 = new File("/path/to/file1")
    val file2 = new File("/path/to/file2")
    
    private def setFileAndGeUri(file: Option[File]): String = {
      e.sourceFile = file
      e.dataSourceUri
    }
    
    def none = setFileAndGeUri(None) must beEmpty
    
    def fileUri = {
      e.exhibitFileStorage = None
      List(file1, file2).map(f => setFileAndGeUri(Some(f))) must_==
        List("file:/path/to/file1", "file:/path/to/file2")
    }
    
    def fromStorage = {
      val storage = mock[UriFileStorage]
      e.exhibitFileStorage = Some(storage)
      storage.getUri(file1) returns URI.create("http://example.com")
      storage.getUri(file2) returns URI.create("uri:/addr/x")
      
      List(file1, file2).map(f => setFileAndGeUri(Some(f))) must_==
        List("http://example.com", "uri:/addr/x")
    }
  }
}
