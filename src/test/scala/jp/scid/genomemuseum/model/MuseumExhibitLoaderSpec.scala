package jp.scid.genomemuseum.model

import org.specs2._
import mock._

import java.net.URL
import java.io.{File, FileInputStream, IOException, InputStream, InputStreamReader,
  FileReader, BufferedInputStream, ByteArrayInputStream, FileOutputStream, BufferedOutputStream}
import java.text.ParseException

import jp.scid.bio.BioData
import MuseumExhibit.FileType._
import MuseumExhibitLoader.{GenBnakExhibitLoader, FastaExhibitLoader}

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
    "ファイル形式を探索" ^ canFindFormat(loader) ^
    "GenBankFileLoader" ^
      "読み込み可能判断" ^ gbCanParseSpec(new GenBnakExhibitLoader) ^
      "読み込み" ^ gbLoadToSpec(new GenBnakExhibitLoader) ^
    bt ^
    "FastaExhibitLoader" ^
      "読み込み可能判断" ^ faCanParseSpec(new FastaExhibitLoader) ^
      "読み込み" ^ faLoadToSpec(new FastaExhibitLoader) ^
    bt ^
    "MuseumExhibitの作成" ^ canLoadMuseumExhibit(loader) ^
    end
  
  def loader = new MuseumExhibitLoader()
  
  val gbkResource = getClass.getResource("sample-file1.gbk")
  val fastaResource = getClass.getResource("sample-file2.fna")
  val emptyResource = File.createTempFile("empty", ".txt").toURI.toURL
  val invalidResource = getClass.getResource("invalid.data")
  
  implicit def urlToString(url: URL): String = MuseumExhibitLoader.readHeadFrom(url)
  
  def canFindFormat(l: => MuseumExhibitLoader) =
    "GenBank 形式ファイルを探索" ! findFormat(l).genbank ^
    "FASTA 形式ファイルを探索" ! findFormat(l).fasta ^
    "空ファイルは Unknown" ! findFormat(l).empty ^
    "不明形式は Unknown" ! findFormat(l).invalid ^
    bt
  
  def gbCanParseSpec(l: => GenBnakExhibitLoader) =
    "GenBank 形式ファイルは true" ! gbCanParse(l).genbank ^
    "FASTA 形式ファイルは false" ! gbCanParse(l).fasta ^
    "空ファイルは false" ! gbCanParse(l).empty ^
    "不正形式ファイルは false" ! gbCanParse(l).invalid ^
    bt
  
  def gbLoadToSpec(l: => GenBnakExhibitLoader) =
    "name" ! gbLoadTo(l).name ^
    "sequenceLength" ! gbLoadTo(l).sequenceLength ^
    "accession" ! gbLoadTo(l).accession ^
    "identifier" ! gbLoadTo(l).identifier ^
    "namespace" ! gbLoadTo(l).namespace ^
    "version" ! gbLoadTo(l).version ^
    "definition" ! gbLoadTo(l).definition ^
    "source" ! gbLoadTo(l).source ^
    bt
  
  def faCanParseSpec(l: => FastaExhibitLoader) =
    "FASTA 形式ファイルは true" ! faCanParse(l).fasta ^
    "GenBank 形式ファイルは false" ! faCanParse(l).genbank ^
    "空ファイルは false" ! faCanParse(l).empty ^
    "不正形式ファイルは false" ! faCanParse(l).invalid ^
    bt
  
  def faLoadToSpec(l: => FastaExhibitLoader) =
    "name" ! faLoadTo(l).name ^
    "sequenceLength" ! faLoadTo(l).sequenceLength ^
    "accession" ! faLoadTo(l).accession ^
    "identifier" ! faLoadTo(l).identifier ^
    "namespace" ! faLoadTo(l).namespace ^
    "version" ! faLoadTo(l).version ^
    "definition" ! faLoadTo(l).definition ^
    bt
  
  def canLoadMuseumExhibit(l: => MuseumExhibitLoader) =
    "GenBank 形式ファイルから作成" ! loadMuseumExhibit(l).genbank ^
    "FASTA 形式ファイルから作成" ! loadMuseumExhibit(l).fasta ^
    bt
  
  def findFormat(loader: MuseumExhibitLoader) = new {
    def genbank = loader.findFormat(gbkResource) must_== GenBank

    def fasta = loader.findFormat(fastaResource) must_== FASTA
    
    def empty = loader.findFormat(emptyResource) must_== Unknown
    
    def invalid = loader.findFormat(invalidResource) must_== Unknown
  }
  
  def loadMuseumExhibit(loader: MuseumExhibitLoader) = new {
    val exhibit = mock[MuseumExhibit]
    
    def genbank = {
      val result = loader.loadMuseumExhibit(exhibit, gbkResource, GenBank)
      there was one(exhibit).name_=("NC_009347")
    }
    
    def fasta = {
      val result = loader.loadMuseumExhibit(exhibit, fastaResource, FASTA)
      there was one(exhibit).accession_=("NC_009473")
    }
  }
  
  // GenBnakExhibitLoader
  /** GenBnakExhibitLoader 読み込み判断 */
  def gbCanParse(loader: GenBnakExhibitLoader) = new {
    def genbank = loader.canParse(gbkResource) must beTrue
    def fasta = loader.canParse(fastaResource) must beFalse
    def empty = loader.canParse(emptyResource) must beFalse
    def invalid = loader.canParse(invalidResource) must beFalse
  }
  
  /** GenBnakExhibitLoader 読み込み */
  def gbLoadTo(loader: GenBnakExhibitLoader) = new {
    val exhibit = mock[MuseumExhibit]
    loader.loadTo(exhibit, gbkResource)
    
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
  
  // FastaExhibitLoader
  /** FastaExhibitLoader 読み込み判断 */
  def faCanParse(loader: FastaExhibitLoader) = new {
    def fasta = loader.canParse(fastaResource) must beTrue
    def genbank = loader.canParse(gbkResource) must beFalse
    def empty = loader.canParse(emptyResource) must beFalse
    def invalid = loader.canParse(invalidResource) must beFalse
  }
  
  /** FastaExhibitLoader 読み込み */
  def faLoadTo(loader: FastaExhibitLoader) = new {
    val exhibit = mock[MuseumExhibit]
    loader.loadTo(exhibit, fastaResource)
    
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
