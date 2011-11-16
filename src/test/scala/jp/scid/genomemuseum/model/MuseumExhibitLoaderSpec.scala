package jp.scid.genomemuseum.model

import org.specs2._
import mock._

import java.net.URL
import java.io.{File, FileInputStream, IOException, InputStream,
  BufferedInputStream, ByteArrayInputStream, FileOutputStream, BufferedOutputStream}
import java.text.ParseException

import actors.{Actor, Futures}
import Actor.State
import jp.scid.bio.BioData

class MuseumExhibitLoaderSpec extends Specification with Mockito {
  import MuseumExhibitLoader._
  
  def is = "MuseumExhibitLoader" ^
    "サービスの create コール" ! loadTo.s1 ^
    "ファイルの読み込み" ! loadTo.s2 ^
    "GenBnakSource" ^
      "読み込み判定" ^
        "genbank ファイル" ! gbk.s1 ^
        "FASTA ファイル" ! gbk.s2 ^
      bt ^ "makeExhibit" ! gbk.s3 ^
    bt ^ "FastaSource" ^
      "読み込み判定" ^
        "FASTA ファイル" ! fasta.s1 ^
        "genbank ファイル" ! fasta.s2 ^
      bt ^ "makeExhibit" ! fasta.s3 ^
    bt ^ "BioData 自動判定" ^
      "genbank ファイル" ! bioData.s1 ^
      "FASTA ファイル" ! bioData.s2 ^
    bt ^ "Actor" ^
      "サービスの craete コール" ! actorLoad.s1 ^
      "サービスの save コール" ! actorLoad.s2
  
  val sampleFile1 = getClass.getResource("sample-file1.gbk")
  val sampleFile2 = getClass.getResource("sample-file2.fna")
  
  lazy val getGenbankFile = download(sampleFile1)
  lazy val getFastaFile = download(sampleFile2)
  
  class TestBase {
    val loader = new MuseumExhibitLoader
    
    val service = mock[MuseumExhibitService]
    service.create returns MuseumExhibit("")
  }
    
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
  
  def loadTo = new TestBase {
    val element1 = MuseumExhibit("loading")
    val element2 = MuseumExhibit("loading")
    
    def loadedService = {
      service.create returns MuseumExhibit("loading")
      loader.loadTo(service, getGenbankFile)
      service
    }
    
    def loadFile1 = {
      service.create returns (element1, element2)
      loader.loadTo(service, getGenbankFile)
      loader.loadTo(service, getFastaFile)
      element1
    }
    
    
    def s1 = there was one(loadedService).create
    
    def s2_1 = loadFile1.name must_== "NC_009347"
//    def s2_2 = exhibit2 must beSome
//    def s2_3 = exhibit1.get.asInstanceOf[MuseumExhibit].name must_== "NC_009347"
//    def s2_4 = exhibit2.get.asInstanceOf[MuseumExhibit].name must_== "Acidiphilium cryptum JF-5 plasmid pACRY07, complete sequence"
    def s2 = s2_1 // and s2_2 // and s2_3 and s2_4
  }
  
  abstract class ParseTest extends TestBase {
    def sourceLoader: loader.ExhibitFileLoader[_ <: BioData]
    
    def loaderCanParse(source: File) = {
      sourceLoader.canParse(source)
    }
  }
  
  def gbk = new ParseTest {
    val sourceLoader = loader.GenBnakSource
    
    def genbankExhibit = {
      val e = MuseumExhibit("")
      sourceLoader.makeExhibit(e, getGenbankFile)
      e
    }
    
    def s1 = loaderCanParse(getGenbankFile) must beTrue
    
    def s2 = loaderCanParse(getFastaFile) must beFalse
    
    def s3 = genbankExhibit.name must_== "NC_009347"
  }
  
  def fasta = new ParseTest {
    val sourceLoader = loader.FastaSource
    
    def fastaExhibit = {
      val e = MuseumExhibit("")
      sourceLoader.makeExhibit(e, getFastaFile)
      e
    }
    
    def s1 = loaderCanParse(getFastaFile) must beTrue
    
    def s2 = loaderCanParse(getGenbankFile) must beFalse
    
    def s3 = fastaExhibit.definition must_== "Acidiphilium cryptum JF-5 plasmid pACRY07, complete sequence"
  }
  
  def bioData = new TestBase {
    import jp.scid.bio.{BioFileParser, BioData, GenBankParser, FastaParser}
    
    def emptyExhibit = MuseumExhibit("")
    
    def genbankLoad = loader.loadTo(service, getGenbankFile).getOrElse(emptyExhibit)
    
    def fastaLoad = loader.loadTo(service, getFastaFile).getOrElse(emptyExhibit)
    
    def s1 = genbankLoad.definition must_== "Shigella sonnei Ss046 plasmid pSS046_spC, complete sequence."
    
    def s2 = fastaLoad.definition must_== "Acidiphilium cryptum JF-5 plasmid pACRY07, complete sequence"
  }
  
  val actorLoad = new TestBase {
    val e1 = MuseumExhibit("")
    val e2 = MuseumExhibit("")
    
    service.create returns (e1, e2)
    
    loader.query(service, getGenbankFile)
    loader.query(service, getFastaFile)
    
    // 読み込みが終わるか 1 秒間待機
    val timer = Futures.alarm(1000)
    while (loader.getState != State.Terminated && !timer.isSet) {
      Thread.sleep(50)
    }
    
    def s1 = there was two(service).create
    
    def s2_1 = there was one(service).save(e1)
    def s2_2 = there was one(service).save(e2)
    def s2 = s2_1 and s2_2
  }
  
}
