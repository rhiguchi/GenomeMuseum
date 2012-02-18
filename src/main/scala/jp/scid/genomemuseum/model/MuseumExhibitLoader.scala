package jp.scid.genomemuseum.model

import java.net.URL
import java.io.{File, IOException, InputStreamReader, Reader, PushbackReader, BufferedReader}
import java.text.ParseException

import collection.mutable.{Buffer, ListBuffer}

import jp.scid.bio.{BioFileParser, GenBankParser, FastaParser, BioData,
  GenBank, Fasta}
import MuseumExhibit.FileType

object MuseumExhibitLoader {
  /**
   * バイオデータから展示物を構築する処理の構造定義。
   */
  trait BioDataExhibitLoader {
    /**
     * このクラスが指定した文字列から始まるファイルを読み込めるかどうか
     */
    @throws(classOf[IOException])
    def canParse(dataText: String): Boolean
    
    /**
     * バイオデータを読み込み、展示物に適用する
     */
    @throws(classOf[IOException])
    @throws(classOf[ParseException])
    def loadTo(exhibit: MuseumExhibit, source: URL)
  }
  
  /**
   * GenBank 形式ファイルから MuseumExhibit を構成する。
   */
  class GenBnakExhibitLoader(parser: GenBankParser) extends BioDataExhibitLoader {
    def this() = this(new GenBankParser)
    
    /**
     * Locus 行が存在するかで判断
     */
    @throws(classOf[IOException])
    def canParse(dataText: String) = {
      val source = io.Source.fromString(dataText).getLines
      source.find(parser.locusFormat.Head.unapply).nonEmpty
    }
    
    /**
     * GenBnak 形式ファイルから読み込み
     */
    @throws(classOf[IOException])
    @throws(classOf[ParseException])
    def loadTo(exhibit: MuseumExhibit, source: URL) {
      val lines = io.Source.fromURL(source).getLines
      if (lines.hasNext) {
        val section = parser.parseFrom(lines)
        makeExhibit(exhibit, section)
      }
    }
    
    /**
     * GenBank データから展示物を構築
     */
    protected def makeExhibit(e: MuseumExhibit, data: GenBank) {
      e.name = data.locus.name
      e.sequenceLength = data.locus.sequenceLength
      e.accession = data.accession.primary
      e.identifier = data.version.identifier
      e.namespace = data.locus.division
      e.version = getVersionNumber(data.version.number)
      e.definition = data.definition.value
      e.source = data.source.value
      e.organism = data.source.taxonomy :+ data.source.organism mkString "\n"
      e.date = data.locus.date
      e.fileType = FileType.GenBank
    }
  }
  
  /**
   * FASTA 形式ファイルから MuseumExhibit を構成する。
   */
  class FastaExhibitLoader(parser: FastaParser) extends BioDataExhibitLoader {
    def this() = this(new FastaParser)
    
    /**
     * 「>」開始行が存在するかで判断
     */
    def canParse(dataText: String) = {
      val source = io.Source.fromString(dataText).getLines
      source.find(parser.headerParser.Head.unapply).nonEmpty
    }
    
    /**
     * FASTA 形式ファイルから読み込み
     */
    @throws(classOf[IOException])
    @throws(classOf[ParseException])
    def loadTo(exhibit: MuseumExhibit, source: URL) {
      val lines = io.Source.fromURL(source).getLines
      if (lines.hasNext) {
        val section = parser.parseFrom(lines)
        makeExhibit(exhibit, section)
      }
    }
    
    protected def makeExhibit(e: MuseumExhibit, data: Fasta) {
      e.name = data.header.name
      e.sequenceLength = data.sequence.value.length
      e.accession = data.header.accession
      e.identifier = data.header.identifier
      e.namespace = data.header.namespace
      e.version = getVersionNumber(data.header.version)
      e.definition = data.header.description
      e.fileType = FileType.FASTA
    }
  }
  
  /** 指定したバイト数分、ストリームの先頭を読み込む */
  @throws(classOf[IOException])
  private[model] def readHeadFrom(source: URL, length: Int = 2048) = {
    val cbuf = new Array[Char](length)
    val read = using(source.openStream) { inst =>
      val reader = new BufferedReader(new InputStreamReader(inst), length)
      reader.read(cbuf)
    }
    read match {
      case -1 => ""
      case read => new String(cbuf, 0, read)
    }
  }
  
  /** バージョン値 */
  private def getVersionNumber(value: Int) = if (value == 0) None else Some(value)
 
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}

/**
 * BioData のファイルから MuseumExhibit を構成するクラス。
 */
class MuseumExhibitLoader {
  import MuseumExhibitLoader._
  
  /** 展示物を作成するパーサー */
  private val parserMap = Map(
    FileType.GenBank -> new GenBnakExhibitLoader,
    FileType.FASTA -> new FastaExhibitLoader)
  
  /**
   * 形式を検索する。
   * 
   * @return ソースのファイル形式
   */
  @throws(classOf[IOException])
  def findFormat(source: URL): FileType.Value = {
    val headString = readHeadFrom(source)
    parserMap find (e => e._2.canParse(headString)) map (_._1) getOrElse FileType.Unknown
  }
  
  /**
   * 展示物を、バイオデータから指定した形式で読み込み、構築する。
   */
  @throws(classOf[IOException])
  @throws(classOf[ParseException])
  def loadMuseumExhibit(exhibit: MuseumExhibit, source: URL, format: FileType.Value) {
    parserMap(format).loadTo(exhibit, source)
    exhibit.filePathAsURI = source.toURI
    exhibit.fileType = format
  }
}
