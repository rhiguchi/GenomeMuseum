package jp.scid.genomemuseum.model

import java.net.URL
import java.io.{File, IOException, InputStreamReader, Reader, PushbackReader, BufferedReader}
import java.text.ParseException

import collection.mutable.{Buffer, ListBuffer}

import jp.scid.bio.{BioFileParser, GenBankParser, FastaParser, BioData,
  GenBank, Fasta}
import MuseumExhibit.FileType

private[model] object MuseumExhibitLoader {
  /**
   * BioData のファイルから MuseumExhibit を構成する抽象オブジェクト。
   * 継承クラスは {@code makeExhibit} を実装する。
   */
  abstract private[model] class ExhibitFileLoader[A <: BioData] {
    /** パーサー */
    def parser: BioFileParser[A]
    
    /**
     * ファイルから生成された BioData オブジェクトを {@code MuseumExhibit} に適用する。
     */
    protected def makeExhibit(e: MuseumExhibit, data: List[A])
    
    /**
     * ファイルが、このオブジェクトが対応する形式として読み込み可能か。
     */
    def canParse(source: Iterator[String]): Boolean
    
    /**
     * 文字列ソースから展示物データを構成する。
     * @throws ParseException ソースに解析不能な文字列が含まれていた時
     */
    @throws(classOf[ParseException])
    def makeExhibitFromFile(target: MuseumExhibit, source: Iterator[String]) {
      
      /** ソースから全てのセクションを読み込む */
      def loadSections(sections: Buffer[A]): Buffer[A] = {
        source.hasNext match {
          case true =>
            sections += parser.parseFrom(source)
            loadSections(sections)
          case false =>
            sections
        }
      }
      
      makeExhibit(target, loadSections(ListBuffer.empty[A]).toList)
    }
  }
  
  /** 指定したバイト数分、ストリームの先頭を読み込む */
  @throws(classOf[IOException])
  private def readHeadFrom(source: URL, length: Int = 2048) = {
    val cbuf = new Array[Char](length)
    val read = using(source.openStream) { inst =>
      val reader = new InputStreamReader(inst)
      reader.read(cbuf)
    }
    read match {
      case -1 => ""
      case read => new String(cbuf, 0, read)
    }
  }
 
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}

/**
 * BioData のファイルから MuseumExhibit を構成するクラス。
 */
class MuseumExhibitLoader {
  import MuseumExhibitLoader._
  
  def this(exhibitServcie: MuseumExhibitService) {
    this()
    this.exhibitServcie = Option(exhibitServcie)
  }
  
  // 対応するファイル形式の構文解析オブジェクト
  private val parsers = List(GenBnakSource, FastaSource)
  
  private var exhibitServcie: Option[MuseumExhibitService] = None
  
  def loadFromUri(source: URL): Option[MuseumExhibit] = {
    val reader = new InputStreamReader(source.openStream)
    // ファイルの先頭部分の一部の文字列
    val pushBackReader = new PushbackReader(reader, 2048)
    val cbuf = new Array[Char](2048)
    val read = pushBackReader.read(cbuf)
    val headString = if (read <= 0) "" else new String(cbuf, 0, read)
    
    /** ファイルの先頭部分の文字列から Source オブジェクトを作成 */
    def headSource = io.Source.fromString(headString).getLines
    
    parsers.find(_.canParse(headSource)).map { parser =>
      pushBackReader.unread(cbuf, 0, read)
      val servcie = exhibitServcie.get
      
      val bufSource = io.Source.fromURL(source)
      val exhibit = servcie.create
      parser.makeExhibitFromFile(exhibit, bufSource.getLines)
      servcie.save(exhibit)
      exhibit
    }
  }
  
  /**
   * GenBank 形式ファイルから MuseumExhibit を構成する。
   */
  private[model] object GenBnakSource extends ExhibitFileLoader[GenBank] {
    val parser = new GenBankParser
    
    /**
     * Locus 行が存在するかで判断
     */
    def canParse(source: Iterator[String]) =
      source.find(parser.locusFormat.Head.unapply).nonEmpty
    
    protected def makeExhibit(e: MuseumExhibit, sections: List[GenBank]) {
      val data = sections.head
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
  private[model] object FastaSource extends ExhibitFileLoader[Fasta] {
    val parser = new FastaParser
    
    /**
     * Locus 行が存在するかで判断
     */
    def canParse(source: Iterator[String]) =
      source.find(parser.headerParser.Head.unapply).nonEmpty
    
    protected def makeExhibit(e: MuseumExhibit, sections: List[Fasta]) {
      val data = sections.head
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
  
  /** バージョン値 */
  private def getVersionNumber(value: Int) =
    if (value == 0) None else Some(value)
}
