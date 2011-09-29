package jp.scid.bio

import java.io.InputStream
import java.text.ParseException
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
    
  /** Locus 要素 */
  case class Locus (
    name: String = "",
    sequenceLength: Int = 0,
    sequenceUnit: String = "",
    molculeType: String = "",
    topology: String = "",
    division: String = "",
    date: String = ""
  ) extends Element
  
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
  object Definition extends ElementObject("DEFINITION") {
    /**
     * DEFINITION 行の文字列から Definition インスタンスを作成する
     * @param source 作成元文字列
     * @return Definition インスタンス
     * @throws ParseException {@code source} が {@code DEFINITION}
     *         から始まっていない場合。
     */
    @throws(classOf[ParseException])
    def parseFrom(source: String): Definition = parseFrom(List(source))
    
    /**
     * DEFINITION 行の文字列から Definition インスタンスを作成する
     * @param source 作成元文字列の配列
     * @return Definition インスタンス
     * @throws ParseException {@code source} の一つ目の要素が
     *         {@code DEFINITION} から始まっていない場合。
     */
    @throws(classOf[ParseException])
    def parseFrom(source: Seq[String]): Definition = source match {
      case Seq(Head(), _*) =>
        val defValue = toSingleValue(source, keySize)
        Definition(defValue)
      case _ => throw new ParseException(
        "souse '%s' does not start with '%s'".format(source, Head), 0)
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
  
  /** Feature 要素 */
  case class Feature (
    key: String = "",
    location: String = "",
    qualifiers: List[(String, String)] = Nil
  ) extends Element
  
  /** Origin 要素 */
  case class Origin (
    value: String = ""
  ) extends Element
  
  def fromInputStream(stream: InputStream) = {
    GenBank(Locus("NC_001773"))
  }
  
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
}