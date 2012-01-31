package jp.scid.genomemuseum.model

import org.specs2._

import java.io.File
import MuseumExhibit.FileType._

class DefaultMuseumExhibitFileLibrarySpec extends Specification with mock.Mockito {
  private type Factory = File => DefaultMuseumExhibitFileLibrary
  
  def is = "DefaultMuseumExhibitFileLibrary" ^
    "exhibit から格納先名の作成" ^ canGetDefaultStorePath(createLibrary) ^
    "保管" ^ canStore(createLibrary) ^
    "ファイルライブラリの保存場所管理オブジェクト" ^ uriFileStorageSpec(createLibrary) ^
    end
  
  val fileResource = getClass.getResource("sample-file1.gbk")
  lazy val testFile = MuseumExhibitLoaderSpec.download(fileResource)
  
  def createTempFile = File.createTempFile("LibraryFileManagerSpec", "")
  
  def createLibrary(dir: File) = {
    new DefaultMuseumExhibitFileLibrary(dir)
  }
  
  def canStore(f: Factory) =
    "baseDir と getDefaultStorePath の格納先が返される" ! store(f).returnsFile ^
    "格納先にファイルが複製される" ! store(f).copies ^
    "標準格納先に既にファイルがあるときは、連番が付与されて複製される" ! store(f).seqNumedFile ^
    bt
  
  def canGetDefaultStorePath(f: Factory) =
    "name 属性から作成" ! getDefaultStorePath(f).fromName ^
    "name が空の時は identifier 属性から作成" ! getDefaultStorePath(f).fromIdentifier ^
    "name と identifier が空の時は unidentifiable" ! getDefaultStorePath(f).unidentifiable ^
    "fileType が GenBank の時は拡張子が gbk" ! getDefaultStorePath(f).gbkType ^
    "fileType が FASTA の時は拡張子が fasta" ! getDefaultStorePath(f).fastaType ^
    bt
  
  def uriFileStorageSpec(f: Factory) =
    "識別名が gmlib" ! uriFileStorage(f).name ^
    "baseDir が library と同じ" ! uriFileStorage(f).baseDir ^
    bt
  
  def exhibitMockOf(fileType: FileType, name: String = "", identifier: String = "",
      filePath: String = "") = {
    val exhibit = mock[MuseumExhibit]
    exhibit.name returns name
    exhibit.identifier returns identifier
    exhibit.fileType returns fileType
    exhibit
  }
  
  class TestBase(f: Factory) {
    val tempDir = new File(createTempFile.getPath + ".d")
    tempDir.deleteOnExit
    val library = spy(f(tempDir))
  }
  
  def store(f: Factory) = new TestBase(f) {
    val exhibit = mock[MuseumExhibit]
    
    doAnswer(_ => new File("path/to/file.txt")).when(library).getDefaultStorePath(exhibit)
    
    private def defaultDest = new File(tempDir, "path/to/file.txt")
    
    def returnsFile = library.store(testFile, exhibit) must_== defaultDest
    
    def copies = {
      val file = library.store(testFile, exhibit)
      file.length must_== testFile.length
    }
    
    def seqNumedFile = {
      defaultDest.getParentFile.mkdirs
      defaultDest.createNewFile
      new File(tempDir, "path/to/file 1.txt").createNewFile
      new File(tempDir, "path/to/file 2.txt").createNewFile
      val file = library.store(testFile, exhibit)
      new File(tempDir, "path/to/file 3.txt").length must_== testFile.length
    }
  }
  
  def getDefaultStorePath(f: Factory) = new TestBase(f) {
    def fromName = {
      val exhibit = exhibitMockOf(Unknown, "NAME")
      library.getDefaultStorePath(exhibit).getPath must_== "NAME.txt"
    }
    
    def fromIdentifier = {
      val exhibit = exhibitMockOf(Unknown, "", "IDENTIFIER")
      library.getDefaultStorePath(exhibit).getPath must_== "IDENTIFIER.txt"
    }
    
    def unidentifiable = {
      val exhibit = exhibitMockOf(Unknown, "", "")
      library.getDefaultStorePath(exhibit).getPath must_== "unidentifiable.txt"
    }
    
    def gbkType = {
      val exhibit = exhibitMockOf(GenBank, "GenBank")
      library.getDefaultStorePath(exhibit).getPath must_== "GenBank.gbk"
    }
    
    def fastaType = {
      val exhibit = exhibitMockOf(FASTA, "FASTA")
      library.getDefaultStorePath(exhibit).getPath must_== "FASTA.fasta"
    }
  }
  
  def uriFileStorage(f: Factory) = new TestBase(f) {
    def name = {
      val uri = library.uriFileStorage.getUri(new File(tempDir, "lib/file"))
      uri.getAuthority must_== "gmlib"
    }
    def baseDir = {
      val uri = library.uriFileStorage.getUri(new File(tempDir, "lib/file"))
      uri.getPath must_== "/lib/file"
    }
  }
}