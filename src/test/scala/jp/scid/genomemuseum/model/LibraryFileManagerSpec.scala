package jp.scid.genomemuseum.model

import org.specs2._
import java.io.{File, IOException}
import java.net.URI
import MuseumExhibit.FileType._

class LibraryFileManagerSpec extends Specification {
  def is = if (tryCreateTempDir.isEmpty) "LibraryFileManager" ^
    "To create temporary directory was not allowed." ^ skipped
  else "LibraryFileManager" ^
    "getDefaultStorePathFor" ^
      "MuseumExhibit.name が含まれる" ! pathFor.s1 ^
      "name が空白の時は identifier を使用" ! pathFor.s2 ^
      "name と identifier が空白の時は untitled" ! pathFor.s3 ^
      "fileType が GenBank の拡張子は .gbk" ! pathFor.s4 ^
      "fileType が FASTA の拡張子は .fasta" ! pathFor.s5 ^
      "fileType が Unknown の拡張子は .txt" ! pathFor.s6 ^
    bt ^ "getFile" ^
      "URI のパス部分と File の絶対パスの終端が一致" ! getFile.s1 ^
      "ライブラリのパスから開始されている" ! getFile.s2 ^
      "ライブラリスキーマでない URI は例外" ! getFile.s3 ^
      "file スキーマは、File に変換される" ! getFile.s4 ^
    bt ^ "store" ^
      "MuseumExhibit にパスが適用される" ! store.s1 ^
      "ライブラリ内にファイルがコピーされる" ! store.s2 ^
      "標準の保存先にすでにファイルがある時は、拡張子前に連番でリネームされる" ! store.s3 ^
    bt ^ "delete" ^
      "URI を渡すと削除される" ! delete.s1 ^
      "ライブラリスキーマでない URI は例外" ! delete.s2
  
  trait TestBase {
    val baseDir = tryCreateTempDir.get
    val manager = new LibraryFileManager(baseDir)
    
    protected def pathFor(e: MuseumExhibit) = manager.getDefaultStorePathFor(e)
  }
  
  def pathFor = new TestBase {
    
    def s1 = pathFor(MuseumExhibit("exhibit")).getPath must contain("exhibit")
    
    def s2 = pathFor(MuseumExhibit("", identifier = "1234")).getPath must contain("1234")
    
    def s3 = pathFor(MuseumExhibit("")).getPath must contain("untitled")
    
    def s4 = pathFor(MuseumExhibit("a", fileType = GenBank))
      .getPath must endWith("/a.gbk")
    
    def s5 = pathFor(MuseumExhibit("b", fileType = FASTA))
      .getPath must endWith("/b.fasta")
    
    def s6 = pathFor(MuseumExhibit("c", fileType = Unknown))
      .getPath must endWith("/c.txt")
  }
  
  def getFile = new TestBase {
    val uri = new URI(manager.uriScheme, "/path/to/file", null)
    val invalidSchemeURI = new URI(manager.uriScheme + "x", "/path/to/file", null)
    val fileSchema = new URI("file", "/path/to/file", null)
    
    def s1 = manager.getFile(uri).getAbsolutePath must endWith("/path/to/file")
    
    def s2 = manager.getFile(uri).getAbsolutePath must
      startWith(baseDir.getAbsolutePath)
    
    def s3 = manager.getFile(invalidSchemeURI) must throwA[IllegalArgumentException]
    
    def s4 = manager.getFile(fileSchema).getAbsolutePath must_== "/path/to/file"
  }
  
  def store = new TestBase {
    val e = MuseumExhibit("exhibit", fileType = GenBank)
    val file = File.createTempFile("testTemp", ".gbk")
    
    val e2 = MuseumExhibit("exhibit", fileType = GenBank)
    val e3 = MuseumExhibit("exhibit", fileType = GenBank)
    
    assert(e.filePath == "")
    manager.store(e, file)
    manager.store(e2, file)
    manager.store(e3, file)
    
    val libraryFile = manager.getFile(e.filePathAsURI)
    
    val e2File = manager.getFile(e2.filePathAsURI)
    val e3File = manager.getFile(e3.filePathAsURI)
    
    def s1 = e.filePath must_!= ""
    
    def s2 = libraryFile must beAFile
    
    def s3_2 = e3File.getPath must endWith("/exhibit 2.gbk")
    def s3 = e2File.getPath must be_!=(libraryFile.getPath) and
      endWith("/exhibit 1.gbk") and s3_2
  }
  
  def delete = new TestBase {
    val e = MuseumExhibit("exhibit", fileType = GenBank)
    val testFile = File.createTempFile("testTemp", ".gbk")
    manager.store(e, testFile)
    
    val invalidSchemeURI = new URI(manager.uriScheme + "x", e.filePathAsURI.getPath, null)
    
    val file = manager.getFile(e.filePathAsURI)
    
    assert(file.exists)
    
    val result = manager.delete(e.filePathAsURI)
    
    def s1 = result must beTrue and (file must not be exist)
    
    def s2 = manager.delete(invalidSchemeURI) must throwA[IllegalArgumentException]
  }
  
  @throws(classOf[IOException])
  def tryCreateTempDir: Option[File] = {
    try {
      Some(createTempDir)
    }
    catch {
      case e: SecurityException => None
    }
  }
  
  @throws(classOf[IOException])
  def createTempDir = {
    val tempFile = File.createTempFile("LibraryFileManagerSpec", "")
    tempFile.delete
    tempFile.mkdir
    tempFile
  }
}