package jp.scid.genomemuseum.model

import java.net.URL
import java.io.{File, IOException, InputStreamReader}
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
  
  // 対応するファイル形式の構文解析オブジェクト
  private val parsers = List(GenBnakSource, FastaSource)
  
  /**
   * ファイルからデータを読み込み、そのデータをオブジェクトへ格納する。
   * @return 展示物が構成された時は {@code true} 。対応するファイルパーサーが無かったときは {@code false} 。
   * @throws IOException ファイルのアクセスに不正状態が発生した時。
   * @throws ParseException ファイル内を解析中に不正な文字列が含まれていた時。
   */
  def makeMuseumExhibit(exhibit: MuseumExhibit, source: URL): Boolean = {
    // ファイルの先頭部分の一部の文字列
    val headString = readHeadFrom(source)
    
    /** ファイルの先頭部分の文字列から Source オブジェクトを作成 */
    def headSource = io.Source.fromString(headString).getLines
    
    parsers.find(_.canParse(headSource)) match {
      case Some(parser) =>
        parser.makeExhibitFromFile(exhibit, io.Source.fromURL(source).getLines)
        true
      case None =>
        false
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
