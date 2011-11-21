package jp.scid.genomemuseum.model

import java.net.URL
import java.io.{File, IOException, FileReader}
import java.text.ParseException

import collection.mutable.{Buffer, ListBuffer}

import jp.scid.bio.{BioFileParser, GenBankParser, FastaParser, BioData,
  GenBank, Fasta}

class MuseumExhibitLoader {
  // 対応するファイル形式の構文解析オブジェクト
  private val parsers = List(GenBnakSource, FastaSource)
  
  /**
   * ファイルからデータを読み込み、そのデータをサービスへ格納する。
   */
  def makeMuseumExhibit(exhibit: MuseumExhibit, file: File): Boolean = {
    def source = io.Source.fromFile(file).getLines
    
    val headString = readHeadFrom(file)
    
    def headSource = io.Source.fromString(headString).getLines
    
    parsers.find(_.canParse(headSource)).map(
      _.makeExhibitFromFile(exhibit, source).isRight).getOrElse(false)
  }
  
  /**
   * BioData のファイルから MuseumExhibit を構成する抽象オブジェクト。
   * 継承クラスは {@code makeExhibit} を実装する。
   */
  private[model] abstract class ExhibitFileLoader[A <: BioData] {
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
    
    def makeExhibitFromFile(target: MuseumExhibit, source: Iterator[String])
        : Either[Throwable, MuseumExhibit] = {
      import util.control.Exception.catching
      
      catching(classOf[ParseException]) either {
        val data = loadSections(ListBuffer.empty[A], source, parser).toList
        makeExhibit(target, data)
        target
      }
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
    }
  }
  
  /** バージョン値 */
  private def getVersionNumber(value: Int) =
    if (value == 0) None else Some(value)
    
  /** ソースから全てのセクションを読み込む */
  private def loadSections[A <: BioData](sections: Buffer[A],
      source: Iterator[String], parser: BioFileParser[A]): Buffer[A] = {
    source.hasNext match {
      case true =>
        sections += parser.parseFrom(source)
        loadSections(sections, source, parser)
      case false =>
        sections
    }
  }
  
  /** 指定したバイト数分、ストリームの先頭を読み込む */
  @throws(classOf[IOException])
  private def readHeadFrom(file: File, length: Int = 2048) = {
    val cbuf = new Array[Char](length)
    val read = using(new FileReader(file)) { reader =>
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
