package jp.scid.genomemuseum.model

import org.specs2._
import mock._

import java.io.File
import MuseumExhibit.FileType._

class LibraryFileManagerSpec extends Specification with Mockito {
  def is = "LibraryFileManager" ^
    "ソースの適用" ^ canSaveSource(manager) ^ bt ^
    "exhibit から格納先名の作成" ^ canGetDefaultStorePath(manager) ^ bt ^
    "格納先 URL の取得" ^ canGetSource(manager) ^ bt ^
    end
  
  def createTempFile = File.createTempFile("LibraryFileManagerSpec", "")
  
  def manager = {
    val tempfile = createTempFile
    val tempDir = new File(tempfile.getPath + ".d", "BioFiles")
    tempDir.mkdirs
    new LibraryFileManager(tempDir)
  }
  
  def canSaveSource(m: => LibraryFileManager) =
    "exhibit にパス適用" ! saveSource(m).appliesPathToExhibit ^
    "ファイルの URI が返される" ! saveSource(m).returnsUriOfFile ^
    "ファイルが移動する" ! saveSource(m).movesFile ^
    "exhibit.name に空白文字" ! saveSource(m).spaceChar
  
  def canGetDefaultStorePath(m: => LibraryFileManager) =
    "name 属性から作成" ! getDefaultStorePath(m).fromName ^
    "name が空の時は identifier 属性から作成" ! getDefaultStorePath(m).fromIdentifier ^
    "name と identifier が空の時は unidentifiable" ! getDefaultStorePath(m).unidentifiable ^
    "fileType が GenBank の時は拡張子が gbk" ! getDefaultStorePath(m).gbkType ^
    "fileType が FASTA の時は拡張子が fasta" ! getDefaultStorePath(m).fastaType
  
  def canGetSource(m: => LibraryFileManager) =
    "filePath から取得" ! getSource(m).returnsUrl ^
    "filePath が直接ファイルを参照するときはそのまま返す" ! getSource(m).returnsFileUrl
  
  def exhibitMockOf(fileType: FileType, name: String = "", identifier: String = "",
      filePath: String = "") = {
    val exhibit = mock[MuseumExhibit]
    exhibit.name returns name
    exhibit.identifier returns identifier
    exhibit.fileType returns fileType
    exhibit
  }
  
  def saveSource(m: LibraryFileManager) = new Object {
    def appliesPathToExhibit = {
      val exhibit = exhibitMockOf(GenBank, "XX_NNNNNNN")
      m.saveSource(exhibit, createTempFile)
      
      there was one(exhibit).filePath_=("gmlib:XX_NNNNNNN.gbk")
    }
    
    def returnsUriOfFile = {
      val exhibit = exhibitMockOf(FASTA, "ABCDEFG")
      
      m.saveSource(exhibit, createTempFile).toString must
        startWith("file:" + m.baseDir.toString) and
        endWith("ABCDEFG.fasta")
    }
    
    def movesFile = {
      val exhibit = exhibitMockOf(FASTA, "A")
      val file = createTempFile
      
      val dest = m.saveSource(exhibit, file)
      val destFile = new File(dest.toURI)
      
      (file.exists, destFile.exists) must_== (false, true)
    }
    
    def spaceChar = {
      val exhibit = exhibitMockOf(GenBank, "XX_NNNNNNN 1")
      m.saveSource(exhibit, createTempFile)
      
      there was one(exhibit).filePath_=("gmlib:XX_NNNNNNN%201.gbk")
    }
  }
  
  def getDefaultStorePath(m: LibraryFileManager) = new Object {
    def fromName = {
      val exhibit = exhibitMockOf(Unknown, "NAME")
      m.getDefaultStorePath(exhibit).getName must_== "NAME.txt"
    }
    
    def fromIdentifier = {
      val exhibit = exhibitMockOf(Unknown, "", "IDENTIFIER")
      m.getDefaultStorePath(exhibit).getName must_== "IDENTIFIER.txt"
    }
    
    def unidentifiable = {
      val exhibit = exhibitMockOf(Unknown, "", "")
      m.getDefaultStorePath(exhibit).getName must_== "unidentifiable.txt"
    }
    
    def gbkType = {
      val exhibit = exhibitMockOf(GenBank, "GenBank")
      m.getDefaultStorePath(exhibit).getName must_== "GenBank.gbk"
    }
    
    def fastaType = {
      val exhibit = exhibitMockOf(FASTA, "FASTA")
      m.getDefaultStorePath(exhibit).getName must_== "FASTA.fasta"
    }
  }
  
  def getSource(m: LibraryFileManager) = new Object {
    def returnsUrl = {
      val exhibit = mock[MuseumExhibit]
      exhibit.filePath returns "gmlib:XX_NNNNNNN%201.gbk"
      
      m.getSource(exhibit).get.toString must_== 
        ("file:" + m.baseDir.toString + "/XX_NNNNNNN%201.gbk")
    }
    
    def returnsFileUrl = {
      val exhibit = mock[MuseumExhibit]
      exhibit.filePath returns "file:/some.file"
      m.getSource(exhibit).get.toString must_== "file:/some.file"
    }
  }
}
