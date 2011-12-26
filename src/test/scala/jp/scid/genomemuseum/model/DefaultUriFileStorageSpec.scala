package jp.scid.genomemuseum.model

import java.io.File
import java.net.URI

import org.specs2._

class DefaultUriFileStorageSpec extends Specification {
  private type Factory = File => DefaultUriFileStorage
  
  def is = "DefaultUriFileStorage" ^
    "プロパティ" ^ propertiesSpec(createStorage) ^
    "ライブラリ URI の取得" ^ canGetUri(createStorage) ^
    "ライブラリファイルの取得" ^ canGetFile(createStorage) ^
    end
  
  def createStorage(baseDir: File) =
    new DefaultUriFileStorage("testlib", baseDir)
  
  def propertiesSpec(f: Factory) =
    "baseDir 初期値" ! properties(f).baseDirInit ^
    "baseDir 設定" ! properties(f).baseDirSet ^
    bt

  def canGetUri(f: Factory) =
    "ライブラリ内ファイルはライブラリ URI を返す" ! getUri(f).libFile ^
    "ライブラリ外ファイルはファイルの URI を返す" ! getUri(f).nonlibFile ^
    bt

  def canGetFile(f: Factory) =
    "ライブラリ URI は basedir を基にした URI を返す" ! getFile(f).libUri ^
    "ファイル URI はそのまま File として返す" ! getFile(f).fileUri ^
    "ライブラリのものではない権限の URI は例外" ! getFile(f).nonlibUri ^
    "ファイルではない URI は例外" ! getFile(f).nonFileUri ^
    bt
  
  def properties(f: Factory) = new {
    val file1 = new File("/path/to/file")
    val file2 = new File("/xx/dir/file")
    
    def baseDirInit = {
      List(f(file1).baseDir, f(file2).baseDir) must_== List(file1, file2)
    }
    
    def baseDirSet = {
      val storage = f(file1)
      storage.baseDir = file2
      storage.baseDir must_== file2
    }
  }
  
  def getUri(f: Factory) = new {
    val storage = f(new File("/path/to/baseDir"))
    
    def libFile = {
      val uri = storage.getUri(new File("/path/to/baseDir/some/file"))
      (uri.getScheme, uri.getAuthority, uri.getPath) must_==
        ("file", storage.name, "/some/file")
    }
    
    def nonlibFile = {
      val uri = storage.getUri(new File("/not/path/to/baseDir/some/file"))
      uri.toString must_== "file:/not/path/to/baseDir/some/file"
    }
  }
  
  def getFile(f: Factory) = new {
    val storage = f(new File("/path/to/baseDir"))
    
    def libUri = {
      val uri = new URI("file", storage.name, "/lib/file", null, null)
      storage.getFile(uri) must_== new File("/path/to/baseDir/lib/file")
    }
    
    def fileUri = {
      val uri = new URI("file", null, "/path/to/baseDir/file", null, null)
      storage.getFile(uri) must_== new File("/path/to/baseDir/file")
    }
    
    def nonlibUri = {
      val uri = new URI("file", "not" + storage.name, "/file", null, null)
      storage.getFile(uri) must throwA[IllegalArgumentException]
    }
    
    def nonFileUri = {
      storage.getFile(new URI("http://example.com/")) must throwA[IllegalArgumentException]
    }
  }
}
