package jp.scid.bio

import java.io.InputStream
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import GenBank._

case class GenBank (
  locus: Locus = Locus(),
  definition: Definition = Definition(),
  accession: Accession = Accession(),
  version: Version = Version(),
  keywords: Keywords = Keywords.Dot,
  source: Source = Source(),
  references: IndexedSeq[Reference] = IndexedSeq.empty,
  comment: Comment = Comment(),
  features: GenBank.Features = Nil,
  origin: Origin = Origin()
)

object GenBank {
  sealed abstract class Element
  sealed abstract private[GenBank] class ElementObject(head: String)
      extends HeadExtractor {
    override protected val headKey = head
    val keySize = 12
  }
  type Features = List[Feature]
  
  sealed abstract private[GenBank] class ElementFormat(val headKey: String) {
    val keySize = 12
    
    object Head {
      override def toString = headKey
      def unapply(line: String): Boolean =
        line != null && line.length >= keySize && line.startsWith(headKey)
    }
  }
    
  /**
   * Locus 要素
   * @param name Locus 名
   * @param sequenceLength 配列長
   * @param sequenceUnit {@code bp} もしくは {@code aa} である単位
   * @param molculeType {@code DNA} や {@code RNA} などの分子型
   * @param topology {@code circular} もしくは {@code linear} の構造形態
   * @param division {@code BCT} のような区分
   * @param date この Locus を持つ情報が作成された日付
   */
  case class Locus (
    name: String = "",
    sequenceLength: Int = 0,
    sequenceUnit: String = "",
    molculeType: String = "",
    topology: String = "",
    division: String = "",
    date: Option[Date] = None
  ) extends Element
  
  /**
   * Locus 生成など
   */
  object Locus {
    /** LOCUS 行マッチパターン（8 要素） */
    lazy private val LocusPattern = "LOCUS       (?x)" + // Header
      """(\S+)\s+""" + // Locus Name
      """(?:(\d+)\s+(bp|aa)\s{1,4})?""" + // Sequence Length & Unit
      """(\S+)?\s+""" + // Molecule Type
      """(circular|linear)?\s*""" + // Topology
      """(\S+)?\s*""" + // Division 
      """(\S+)?""" r // Date
    
    private type Elements = (String, Option[(String, String)], Option[String],
      Option[String], Option[String], Option[String])
    
    /**
     * LOCUS 行の文字列形式のクラス
     */
    class Format extends ElementFormat("LOCUS") {
      /**
       * LOCUS 行の文字列から Locus インスタンスを作成する
       * @param source 作成元文字列
       * @return Locus インスタンス
       * @throws ParseException {@code source} が {@code LOCUS}
       *         から始まっていなく、また {@code keySize } で定義される
       *         文字列長より短い場合。もしくは文字列形式に誤りがある場合。
       */
      @throws(classOf[ParseException])
      def parse(source: String): Locus = unapply(source) match {
        case Some(locus) => locus
        case None => throw new ParseException("Invalid format", 0)
      }
      
      /**
       * LOCUS 行の文字列から Locus インスタンスを作成する
       * @param source 作成元文字列
       * @return 作成に成功した時は {@code Some[Locus]} 。
       *         形式に誤りがあるなどで作成できない時は {@code None}
       */
      def unapply(source: String): Option[Locus] = fragmentate(source) match {
        case Some((name, seqPair, molType, topology, division, date)) =>
          val (seqLength, seqUnit) = seqPair.getOrElse("0", "")
          Some(Locus(name, parseInt(seqLength), seqUnit, molType.getOrElse(""),
            topology.getOrElse(""), division.getOrElse(""), date.map(parseDate)))
        case _ => None
      }
      
      /**
       * 文字列を Locus 作成用に要素に分解する。
       * @param source 作成元文字列
       * @return 作成に成功した時は {@code Some()} 。
       *         形式に誤りがあるなどで作成できない時は {@code None}
       */
      protected def fragmentate(source: String): Option[Elements] = source match {
        case source @ Head() if source.length >= keySize => source match {
          case LocusPattern(name, seqLength, seqUnit, molType, topology,
              division, date) =>
            val lengthPairOp = if (seqLength == null || seqUnit == null) None
              else Some(seqLength, seqUnit)
            Some(name, lengthPairOp, Option(molType), Option(topology),
              Option(division), Option(date))
          case _ => None
        }
        case _ => None
      }
    
      /** 日付値を Date オブジェクトへ変換 */
      @throws(classOf[ParseException])
      protected def parseDate(text: String): Date =
        new SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.US).parse(text)
    }
  }
  
  /**
   * Definition 要素
   * @param value 要素の値。未定義の時は空の文字列。
   */
  case class Definition (
    value: String = ""
  ) extends Element
  
  /**
   * Definition 生成など
   */
  object Definition {
    class Format extends ElementFormat("DEFINITION") {
      /**
       * DEFINITION 行の文字列から Definition インスタンスを作成する
       * @param source 作成元文字列
       * @return Definition インスタンス
       * @throws ParseException {@code source} が {@code DEFINITION}
       *         から始まっていない場合。
       */
      @throws(classOf[ParseException])
      def parse(source: Seq[String]): Definition = unapply(source) match {
        case Some(definition) => definition
        case None => throw new ParseException("Invalid format", 0)
      }
      
      /**
       * DEFINITION 行の文字列から Definition インスタンスを作成する
       * @param source 作成元文字列
       * @return 作成に成功した時は {@code Some[Definition]} 。
       *         形式に誤りがあるなどで作成できない時は {@code None}
       */
      def unapply(source: String): Option[Definition] = unapply(Seq(source))
      
      /**
       * DEFINITION 行の文字列から Definition インスタンスを作成する
       * @param source 作成元文字列の配列
       * @throws ParseException {@code source} の一つ目の要素が
       *         {@code DEFINITION} から始まっていない場合。
       */
      def unapply(source: Seq[String]): Option[Definition] = source match {
        case Seq(Head(), _*) =>
          val defValue = toSingleValue(source, keySize)
          Some(Definition(defValue))
        case _ => None
      }
    }
  }
  
  /** Accession 要素 */
  case class Accession (
    primary: String = "",
    secondary: IndexedSeq[String] = IndexedSeq.empty
  ) extends Element
  
  
  
  /** Version 要素 */
  case class Version (
    accession: String = "",
    number: Int = 0,
    identifier: String = ""
  ) extends Element
  
  /** Keywords 要素 */
  case class Keywords (
    values: List[String] = Nil
  ) extends GenBank.Element
  
  object Keywords {
    val Dot = Keywords(List("."))
  }
  
  /** Source 要素 */
  case class Source (
    value: String = "",
    organism: String = "",
    taxonomy: IndexedSeq[String] = IndexedSeq.empty
  ) extends Element
  
  /** Reference 要素 */
  case class Reference (
    basesStart: Int = 0,
    basesEnd: Int = 0,
    authors: String = "",
    title: String = "",
    journal: String = "",
    pubmed: String = "",
    remark: String = "",
    others: Map[Symbol, String] = Map.empty
  ) extends Element
  
  /** Comment 要素 */
  case class Comment (
    value: String = ""
  ) extends Element
  
  /** Features 要素 */
  object Features extends ElementObject("FEATURES") {
  }
  
  /** Feature 要素 */
  case class Feature (
    key: String = "",
    location: String = "",
    qualifiers: List[(String, String)] = Nil
  ) extends Element
  
  object Feature {
    val keySize: Int = 21
    
    /** Feature キーの抽出子 */
    object Head {
      /**
       * Feature のキーを抽出する
       * @param source Feature 項目の最初の行
       * @return {@code source} の長さが {@code keySize} 以上でかつ、
       *         {@code keySize} 文字以内に文字列が含まれていたらその文字列の
       *         {@code Option} 値。当てはまらない場合は {@code None}
       */
      def unapply(source: String) = source match {
        case source if source.length >= keySize =>
          val key = source.substring(0, keySize).trim
          if (key.isEmpty) None
          else Some(key)
        case _ => None
      }
    }
    
    @throws(classOf[ParseException])
    def parseFrom(source: Seq[String]): Feature = source match {
      case Seq(head @ Head(key), tail @ _*) =>
        Feature(key)
      case _ => throw new ParseException("invalid format", 0)
    }
  }
  
  /** Origin 要素 */
  case class Origin (
    value: String = ""
  ) extends Element
  
  object Origin extends ElementObject("ORIGIN") {
  }
  
  /** 終末要素 */
  object Termination extends ElementObject("//")
  
  /**
   * 先頭行の開始文字の抽出子オブジェクト Head をもたせるトレイト
   * {@code headKey} をオーバーライドして、実装する
   */
  private[GenBank] trait HeadExtractor {
    protected val headKey: String
    object Head {
      override def toString = headKey
      def unapply(line: String): Boolean = line.startsWith(headKey)
    }
  }
  
  /**
   * 文字列リストを一行テキストの値に変換
   * @param source 変換元
   * @param dropSize 先頭から切り落とす文字数
   */
  private def toSingleValue(source: Seq[String], dropSize: Int) =
    source withFilter (_.length >= dropSize) map
          (_ substring dropSize) mkString " "
  
  /**
   * 数字文字列を {@code Int} に変換する。
   * @param text 変換元
   * @return 変換後の値
   * @throws ParseException {@code Int} に変換できなかった時。
   */
  @throws(classOf[ParseException])
  private def parseInt(text: String): Int = try {
    Integer.parseInt(text)
  }
  catch {
    case e: NumberFormatException =>
      throw new ParseException(e.getLocalizedMessage, 0)
  }
}
