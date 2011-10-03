package jp.scid.bio

import java.io.InputStream
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import GenBank._

/**
 * {@code GenBank} オブジェクトを文字列から作成する構文解析クラス
 */
class GenBankParser {
  import collection.mutable.{ListBuffer, Buffer}
  
  val locusFormat = new Locus.Format
  val definitionFormat = new Definition.Format
  val accessionFormat = new Accession.Format
  val sourceFormat = new Source.Format
  
  /**
   * 文字列情報から {@code GenBank} オブジェクトを生成する
   * @param source 生成もとのテキスト。 LOCUS 行までは無視される。
   * @return 作成された {@code GenBank} オブジェクト
   * @throws ParseException {@code source} に解析できない文字列が含まれていた場合。
   */
  @throws(classOf[ParseException])
  def parseFrom(source: Iterator[String]): GenBank = {
    val bufferedSource = source.buffered
    
    def nonLocusHead(line: String) = ! isSectionHead(line)
    
    val unknownStart = readElementTail(ListBuffer.empty[String],
      bufferedSource, nonLocusHead _)
    
    createFrom(bufferedSource)
  }
  
  @throws(classOf[ParseException])
  private def createFrom(source: BufferedIterator[String]) = {
    val locus = locusFormat.parse(source.next)
    val defaultVal = GenBank()
    var definition = defaultVal.definition
    var accession = defaultVal.accession
    var version = defaultVal.version
    var keywords = defaultVal.keywords
    var sourceObj = defaultVal.source
    var references = defaultVal.references
    var comment = defaultVal.comment
    var features = defaultVal.features
    var origin = defaultVal.origin
    
    /** 要素の行を読み込む。 */
    def readElementLines(head: String, source: BufferedIterator[String]) =
      readElementTail(ListBuffer(head), source, isElementContinuing _)
    
    /** 要素の構文解析 */
    def parseElementFrom(head: String, tail: BufferedIterator[String]) = head match {
      case definitionFormat.Head() => 
        definition = definitionFormat parse readElementLines(head, source)
      case accessionFormat.Head() => 
        accession = accessionFormat parse readElementLines(head, source)
      case sourceFormat.Head() => 
        sourceObj = sourceFormat parse readElementLines(head, source)
      case _ =>
        // TODO ログ出力
    }
    
    /** 全ての要素の構文解析 */
    @annotation.tailrec
    def parseAllElementsFrom(source: BufferedIterator[String]) {
      source.hasNext match {
        case true =>
          val head = source.next
          isTermination(head) match {
            case false =>
              parseElementFrom(head, source)
              parseAllElementsFrom(source)
            case true =>
          }
        case false =>
      }
    }
    
    parseAllElementsFrom(source)
    
    GenBank(locus, definition, accession, version, keywords, sourceObj,
      references, comment, features, origin)
  }
  
  /**
   * 指定した要素形式から、次の要素形式が検出されるまでの文字列を読み出す
   * {@code format#detectNextFrom} が {@code Some} を返すまで読み出す。
   * @param accumu 読み出した文字列の追加先
   * @param source 文字列の読み出し元
   * @param elementContinuing 要素文字列が継続しているかの判定関数
   * @return {@code accumu} オブジェクト
   */
  @annotation.tailrec
  private def readElementTail(accumu: Buffer[String], source: BufferedIterator[String], 
      elementContinuing: String => Boolean): Buffer[String] =
    if (source.hasNext && elementContinuing(source.head)) {
      accumu += source.next
      readElementTail(accumu, source, elementContinuing)
    }
    else accumu
  
  /** 要素の行が継続しているか */
  private def isElementContinuing(line: String) =
    line.startsWith("  ")
  
  /** セクションの終末を表す文字列であるか */
  protected def isTermination(line: String) =
    line.startsWith("//")
  
  /** Locus 行を表す文字列であるか */
  protected def isSectionHead(line: String) =
    line.startsWith(locusFormat.headKey)
}
