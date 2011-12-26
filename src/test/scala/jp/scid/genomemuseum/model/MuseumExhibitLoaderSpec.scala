package jp.scid.genomemuseum.model

import org.specs2._
import mock._

import java.net.URL
import java.io.{File, FileInputStream, IOException, InputStream, InputStreamReader,
  FileReader, BufferedInputStream, ByteArrayInputStream, FileOutputStream, BufferedOutputStream}
import java.text.ParseException

import jp.scid.bio.BioData
import MuseumExhibit.FileType._

object MuseumExhibitLoaderSpec {
  def download(source: URL) = {
    val file = File.createTempFile("bioFile", ".txt")
    
    using(new FileOutputStream(file)) { out =>
      val dest = new BufferedOutputStream(out)
      
      using(source.openStream) { inst =>
        val buf = new Array[Byte](8196)
        val source = new BufferedInputStream(inst, buf.length)
        
        Iterator.continually(source.read(buf)).takeWhile(_ != -1)
          .foreach(dest.write(buf, 0, _))
      }
      
      dest.flush
    }
    
    file
  }
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}

class MuseumExhibitLoaderSpec extends Specification with Mockito {
  import MuseumExhibitLoaderSpec._
  
  def is = "MuseumExhibitLoader" ^
    "MuseumExhibitの作成" ^ canMakeMuseumExhibit(defaultParsers) ^
    "genbank ファイル" ^ canParseGenBankFile(defaultParsers) ^ bt ^
    "FASTA ファイル" ^ canParseFastaFile(defaultParsers) ^ bt ^
    end
  
  def defaultParsers = new MuseumExhibitLoader()
  
  val gbkResource = getClass.getResource("sample-file1.gbk")
  val fastaResource = getClass.getResource("sample-file2.fna")
  val invalidResourceResource = getClass.getResource("invalid.data")
  
  lazy val genBankFile = download(gbkResource)
  lazy val fastaFile = download(fastaResource)
  lazy val emptyFile = File.createTempFile("empty", ".txt")
  lazy val invalidFile = download(invalidResourceResource)
  
  def canMakeMuseumExhibit(l: => MuseumExhibitLoader) =
    "GenBank 形式ファイルから作成" ! makeMuseumExhibit(l).fromGenBank ^
    "FASTA 形式ファイルから作成" ! makeMuseumExhibit(l).fromFasta ^
    "空ファイルから取得できない" ! makeMuseumExhibit(l).fromEmpty ^
    "不正形式ファイルから取得できない" ! makeMuseumExhibit(l).fromINvalid
    bt
  
  def canParseGenBankFile(parsers: => MuseumExhibitLoader) =
    "name" ! fromGenBank(parsers).name ^
    "sequenceLength" ! fromGenBank(parsers).sequenceLength ^
    "accession" ! fromGenBank(parsers).accession ^
    "identifier" ! fromGenBank(parsers).identifier ^
    "namespace" ! fromGenBank(parsers).namespace ^
    "version" ! fromGenBank(parsers).version ^
    "definition" ! fromGenBank(parsers).definition ^
    "source" ! fromGenBank(parsers).source
  
  def canParseFastaFile(parsers: => MuseumExhibitLoader) =
    "name" ! fromFasta(parsers).name ^
    "sequenceLength" ! fromFasta(parsers).sequenceLength ^
    "accession" ! fromFasta(parsers).accession ^
    "identifier" ! fromFasta(parsers).identifier ^
    "namespace" ! fromFasta(parsers).namespace ^
    "version" ! fromFasta(parsers).version ^
    "definition" ! fromFasta(parsers).definition
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
  
  class TestBase(loader: MuseumExhibitLoader) {
    val exhibit = mock[MuseumExhibit]
    
    def makeFromGenBankFile = using(new InputStreamReader(gbkResource.openStream)) { reader => 
      loader.makeMuseumExhibit(exhibit, reader)
    }
    
    def makeFromFastaFile = using(new InputStreamReader(fastaResource.openStream)) { reader => 
      loader.makeMuseumExhibit(exhibit, reader)
    }
  }
  
  def makeMuseumExhibit(loader: MuseumExhibitLoader) = new TestBase(loader) {
    def fromGenBank = makeFromGenBankFile must beSome(exhibit) and
        (there was one(exhibit).fileType_=(GenBank))
    
    def fromFasta = makeFromFastaFile must beSome(exhibit) and
        (there was one(exhibit).fileType_=(FASTA))
    
    def fromEmpty = {
      val result = using(new FileReader(emptyFile)) { reader => 
        loader.makeMuseumExhibit(exhibit, reader)
      }
      result must beNone
    }
    
    def fromINvalid = {
      val result = using(new InputStreamReader(invalidResourceResource.openStream)) { reader => 
        loader.makeMuseumExhibit(exhibit, reader)
      }
      result must beNone
    }
  }
  
  def fromGenBank(parsers: MuseumExhibitLoader) = new TestBase(parsers) {
    makeFromGenBankFile
    
    def name = there was one(exhibit).name_=("NC_009347")
      
    def sequenceLength = there was one(exhibit).sequenceLength_=(2101)
      
    def accession = there was one(exhibit).accession_=("NC_009347")
    
    def identifier = there was one(exhibit).identifier_=("145294040")
    
    def namespace = there was one(exhibit).namespace_=("BCT")
    
    def version = there was one(exhibit).version_=(Some(1))
    
    def definition = there was one(exhibit).definition_=(
      "Shigella sonnei Ss046 plasmid pSS046_spC, complete sequence.")
    
    def source = there was one(exhibit).source_=(
      "Shigella sonnei Ss046")
  }
  
  def fromFasta(l: MuseumExhibitLoader) = new TestBase(l) {
    makeFromFastaFile
    
    def name = there was one(exhibit).name_=("")
    
    def sequenceLength = there was one(exhibit).sequenceLength_=(5629)
    
    def accession = there was one(exhibit).accession_=("NC_009473")
    
    def identifier = there was one(exhibit).identifier_=("148244127")
    
    def namespace = there was one(exhibit).namespace_=("ref")
    
    def version = there was one(exhibit).version_=(Some(1))
    
    def definition = there was one(exhibit).definition_=(
      "Acidiphilium cryptum JF-5 plasmid pACRY07, complete sequence")
  }
}
