package jp.scid.bio

import java.text.ParseException
import Fasta._

/**
 * GenBank や FASTA ファイル等のオブジェクトインターフェイス
 * @param header ヘッダ要素
 * @param sequence 配列要素
 */
case class Fasta(
  val header: Header = Header(),
  val sequence: Sequence = Sequence()
)

object Fasta {
  /**
   * FASTA 形式のヘッダー要素
   * @param identifier: 識別子値。例：{@code 532319}
   * @param namespace: 名前空間。例：{@code pir}
   * @param accession: アクセッション番号。例：{@code TVFV2E}
   * @param version: バージョン番号。例：{@code 1}
   * @param name: 名前。例：{@code TVFV2E}
   * @param description: 記述領域値。例：{@code envelope protein}
   */
  case class Header(
    identifier: String = "",
    namespace: String = "",
    accession: String = "",
    version: Int = 0,
    name: String = "",
    description: String = ""
  )
  
  /**
   * {@code Header} 生成など
   */
  object Header {
    /** ヘッダーの名前領域と記述領域を分ける正規表現 */
    lazy private val HeaderPattern = """(?x)^> \s* (\S+) (?:\s+(.*))? """ r // e.g. >gi|532319|pir|TVFV2E|TVFV2E envelope protein
    /** ヘッダーの名前領域内の要素を分ける正規表現 */
    lazy private val DescriptionPattern = """(?x)^(?:gi \| (\d+) \| )?""" + // e.g. "gi|532319|"
        """(\w+) \| (\w+?) (?:\. (\d+) )? \|""" + // e.g. "pir|TVFV2E|" or "pir|TVFV2E.1|"
        """(\S+)? $""" r // e.g. "TVFV2E"
        
    /**
     * FASTA ヘッダ文字列から {@code Header} オブジェクトを作成。
     * @param source: FASTA 形式のヘッダ文字列
     * @return Header オブジェクト
     */
    @throws(classOf[ParseException])
    def parseFrom(source: String): Header = source match {
      case HeaderPattern(name, description) => name match {
        case DescriptionPattern(identifier, namespace, accession, version, name) =>
          Header(nonNull(identifier), namespace, accession,
            Integer.parseInt(nonNull(version, "0")),
            nonNull(name), nonNull(description))
        case _ => Header(name = name, description = nonNull(description))
      }
      case _ => throw new ParseException("invalid format", 0)
    }
    
    private def nonNull(value: String, nullValue: String = "") = value match {
      case null => nullValue
      case _ => value
    }
    
    /**
     * 行の開始文字抽出子
     */
    object Head {
      def unapply(line: String): Boolean = line.startsWith(">")
    }
  }
  
  /**
   * FASTA 形式の配列要素
   * @param source: シーケンス文字列
   */
  case class Sequence(
    val value: String = ""
  )
  
  /**
   * {@code Sequence} 生成など
   */
  object Sequence {
    /**
     * FASTA 配列文字列から {@code Sequence} オブジェクトを作成。
     * @param source: FASTA 形式の配列文字列
     * @return Sequence オブジェクト
     */
    @throws(classOf[ParseException])
    def parseFrom(source: Seq[String]) =
      Sequence(source.view.map(_.trim).mkString)
  }
}

