package jp.scid.bio

import java.io.InputStream
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import collection.mutable.Buffer
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
  features: GenBank.Features = IndexedSeq.empty,
  origin: Origin = Origin()
)

object GenBank {
  sealed abstract class Element
  sealed abstract private[GenBank] class ElementObject(head: String)
      extends HeadExtractor {
    override protected val headKey = head
    val keySize = 12
  }
  type Features = IndexedSeq[Feature]
  
  sealed abstract private[GenBank] class ElementFormat(val headKey: String) {
    val keySize = 12
    
    object Head {
      override def toString = headKey
      def unapply(line: String): Boolean =
        line != null && line.length >= keySize && line.startsWith(headKey)
    }
  }
  
  private[GenBank] trait ElementParser[T <: Element] {
    /**
     * GenBank 要素の行文字列からインスタンスを作成する
     * @param source 作成元文字列
     * @return 要素インスタンス
     * @throws ParseException {@code source} が {@code DEFINITION}
     *         から始まっていない場合。
     */
    @throws(classOf[ParseException])
    def parse(source: Seq[String]): T = unapply(source) match {
      case Some(definition) => definition
      case None => throw new ParseException("Invalid format", 0)
    }
    
    /**
     * GenBank 要素行の文字列から要素インスタンスを作成する
     * @param source 作成元文字列
     * @return 作成に成功した時は {@code Some[Definition]} 。
     *         形式に誤りがあるなどで作成できない時は {@code None}
     */
    def unapply(source: String): Option[T] = unapply(Seq(source))
    
    /**
     * 要素の行文字列からインスタンスを作成する
     * @param source 作成元文字列
     * @return 要素インスタンス。形式に誤りがあるなどで作成できない時は {@code None} 。
     */
    def unapply(source: Seq[String]): Option[T]
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
    class Format extends ElementFormat("LOCUS") with ElementParser[Locus] {
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
      
      override def parse(source: Seq[String]) = parse(source.head)
      
      /**
       * LOCUS 行の文字列から Locus インスタンスを作成する
       * @param source 作成元文字列
       * @return 作成に成功した時は {@code Some[Locus]} 。
       *         形式に誤りがあるなどで作成できない時は {@code None}
       */
      override def unapply(source: String): Option[Locus] = fragmentate(source) match {
        case Some((name, seqPair, molType, topology, division, date)) =>
          val (seqLength, seqUnit) = seqPair.getOrElse("0", "")
          Some(Locus(name, parseInt(seqLength), seqUnit, molType.getOrElse(""),
            topology.getOrElse(""), division.getOrElse(""), date.map(parseDate)))
        case _ => None
      }
      
      def unapply(source: Seq[String]) = unapply(source.head)
      
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
    class Format extends ElementFormat("DEFINITION") with ElementParser[Definition] {
      /**
       * DEFINITION 行の文字列から Definition インスタンスを作成する
       * @param source 作成元文字列の配列
       * @return 作成に成功した時は {@code Some[Definition]} 。
       *         形式に誤りがあるなどで作成できない時は {@code None}
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
  
  /**
   * Accession 生成など
   */
  object Accession {
    class Format extends ElementFormat("ACCESSION") with ElementParser[Accession] {
      /**
       * ACCESSION 行の文字列から Accession インスタンスを作成する
       * @param source 作成元文字列の配列
       * @return 作成に成功した時は {@code Some[Accession]} 。
       *         形式に誤りがあるなどで作成できない時は {@code None}
       */
      def unapply(source: Seq[String]): Option[Accession] = source match {
        case Seq(Head(), _*) =>
          val seqVal = source withFilter (_.length >= keySize) flatMap
            (_ substring keySize trim() split "\\s+")
          val acc = seqVal match {
            case Seq(head, tail @ _*) => Accession(head, tail.toIndexedSeq)
            case _ => Accession()
          }
          Some(acc)
        case _ => None
      }
    }
  }
  
  /** Version 要素 */
  case class Version (
    accession: String = "",
    number: Int = 0,
    identifier: String = ""
  ) extends Element
  
  object Version {
    class Format extends ElementFormat("VERSION") {
      /** VERSION 行マッチパターン（8 要素） */
      lazy private val VersionPattern = "VERSION     (?x)" + // Header
        """([^.\s]+)  (?: \. (\d+) )? \s*""" + // Accession.VersionNum
        """(?: GI: (\S+) )?""" r // Identifier
      
      @throws(classOf[ParseException])
      def parse(source: Seq[String]): Version = parse(source.head)
      
      @throws(classOf[ParseException])
      def parse(source: String): Version = fragmentate(source) match {
        case Some((accession, versionOp, identifierOp)) => 
          Version(accession, versionOp.map(parseInt).getOrElse(0), identifierOp.getOrElse(""))
        case None => throw new ParseException("Invalid format", 0)
      }
      
      private type Elements = (String, Option[String], Option[String])
      
      /**
       * Version 文字列行を、要素に分解する。
       * @return キーと値。値は、先頭 {@code keyStart} 文字が落とされている。
       */
      protected def fragmentate(source: String): Option[Elements] = source match {
        case VersionPattern(accession, version, identifier) =>
          Some(accession, Option(version), Option(identifier))
        case _ => None
      }
      
    }
  }
  
  /** Keywords 要素 */
  case class Keywords (
    values: List[String] = Nil
  ) extends GenBank.Element
  
  object Keywords {
    val Dot = Keywords(List("."))
    
    class Format extends ElementFormat("KEYWORDS") {
      @throws(classOf[ParseException])
      def parse(source: Seq[String]): Keywords = {
        val seqVal = source withFilter (_.length >= keySize) flatMap
          (_ substring keySize trim() split "\\s+")
        val acc = seqVal match {
          case Seq(".") => Dot
          case seqVal => Keywords(seqVal.toList)
        }
        acc
      }
      
      @throws(classOf[ParseException])
      def parse(source: String): Keywords = parse(Seq(source))
    }
  }
  
  /** Source 要素 */
  case class Source (
    value: String = "",
    organism: String = "",
    taxonomy: IndexedSeq[String] = IndexedSeq.empty
  ) extends Element
  
  object Source {
    class Format extends ElementFormat("SOURCE") with ElementParser[Source] {
      val organismFormat = new OrganismFormat
      
      /**
       * ACCESSION 行の文字列から Accession インスタンスを作成する
       * @param source 作成元文字列の配列
       * @return 作成に成功した時は {@code Some[Accession]} 。
       *         形式に誤りがあるなどで作成できない時は {@code None}
       */
      def unapply(source: Seq[String]): Option[Source] = source match {
        case Seq(head @ Head(), tail @ _*) =>
          val (sourceLines, remaining) = tail span isValueContinuing
          val sourceValue = toSingleValue(head +: sourceLines, keySize)
          val (organism, taxonomy) = organismFormat.unapply(remaining)
            .getOrElse("", IndexedSeq.empty)
          
          Some(Source(sourceValue, organism, taxonomy))
        case _ => None
      }
      
      protected def isValueContinuing(line: String) =
        line != null && line.startsWith("   ")
    }
    
    class OrganismFormat extends ElementFormat("  ORGANISM") {
      def unapply(source: Seq[String]): Option[(String, IndexedSeq[String])] = source match {
        case Seq(Head(), _*) =>
          val (organismLines, taxonomyLines) = source span (isOrganismValue)
          val organism = toSingleValue(organismLines, keySize)
          val taxonomy = makeTaxonomy(taxonomyLines)
          
          Some(organism, taxonomy)
        case _ => None
      }
      
      private def isOrganismValue(line: String) =
        line != null && line.indexOf(";") < 0
      
      protected def makeTaxonomy(lines: Seq[String]): IndexedSeq[String] = {
        var text = toSingleValue(lines, keySize)
        if (text.endsWith(".")) text = text.substring(0, text.length - 1)
        text.split(";\\s+")
      }
    }
  }
  
  // GenBankParser と重複
  @annotation.tailrec
  private def readElementLines(accumu: Buffer[String], source: Seq[String], 
      elementContinuing: String => Boolean): Seq[String] = source match {
    case Seq(head, tail @ _*) => elementContinuing(head) match {
      case true =>
        accumu += head
        readElementLines(accumu, tail, elementContinuing)
      case false =>
        source
    }
    case _ => source
  }
  
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
  object Features {
    class Format extends ElementFormat("FEATURES")
  }
  
  /** Feature 要素 */
  case class Feature (
    key: String = "",
    location: String = "",
    qualifiers: IndexedSeq[Feature.Qualifier] = IndexedSeq.empty
  ) extends Element
  
  object Feature {
    case class Qualifier(
      key: String = "",
      value: String = ""
    )
    
    object Qualifier {
      class Format {
        val keyIndent = 21
        val keyStartChar = '/'
        val valueSplitChar = '='
        
        @throws(classOf[ParseException])
        def parse(source: String): Qualifier = parse(Seq(source))
        
        @throws(classOf[ParseException])
        def parse(source: Seq[String]): Qualifier = fragmentate(source.toList) match {
          case Some((key, valueLines)) =>
            var value = makeValueFor(key, valueLines)
            Qualifier(key, value)
          case None => throw new ParseException("Invalid format", 0)
        }
        
        /**
         * Qualifier キーと値を抽出する
         * @param source Feature 項目の最初の行
         * @return {@code Head} が {@code true} を返すとき、
         *         {@code keyIndent + 1} から {@code valueSplitChar} が表れるまでの文字列の
         *         {@code Option} 値。当てはまらない場合は {@code None}
         */
        def unapply(source: Seq[String]): Option[Qualifier] =
          try { Some(parse(source)) }
          catch {
            case e: ParseException => None
            case e => throw e
          }
        
        def unapply(source: String): Option[Qualifier] = unapply(Seq(source))
        
        /** Qualifier 開始行の抽出子 */
        object Head {
          /**
           * Qualifier のキーを抽出する
           * @param line {@code null} でない行文字列
           * @return {@code line} の長さが {@code keyIndent + 1} 以上でかつ、
           *         {@code keyIndent} 文字目が {@code keyStartChar} である場合 {@code true} 。
           */
          def unapply(line: String): Boolean =
            line.length > keyIndent && line.charAt(keyIndent) == keyStartChar &&
              line.charAt(keyIndent + 1) != ' '
        }
        
        /**
         * Qualifier 文字列行を、キーと値に分解する。
         * @return キーと値。値は、先頭 {@code keyStart} 文字が落とされている。
         */
        protected def fragmentate(source: List[String]): Option[(String, List[String])] = source match {
          case (head @ Head()) :: tail =>
            val keyStart = keyIndent + 1
            val keyEnd = head.indexOf(valueSplitChar, keyStart)
            val (key, valueLines) = 
              if (keyEnd >= 0) 
                (head.substring(keyStart, keyEnd),
                  head.substring(keyEnd + 1) :: tail.map(_.substring(keyIndent)))
              else (head.substring(keyStart), Nil)
            Some(key, valueLines)
          case _ => None
        }
        
        /**
         * Qualifier のキーに対応した 1 行値を作成する
         */
        protected def makeValueFor(key: String, valueLines: List[String]) = {
          // 文字列両端のダブルクオートを除去する
          def removeDoubleQuates(value: String) = {
            if (value.charAt(0) == '"' && value.charAt(value.length - 1) == '"')
              value.substring(1, value.length - 1)
            else value
          }
          
          // 複数行の結合は、キーごとに手法を変える
          val value = key match {
            case "translation" => valueLines.mkString
            case _ => toSingleValue(valueLines, 0)
          }
          
          // ダブルクオート無しの文字列に
          removeDoubleQuates(value)
        }
      }
    }
    
    class Format {
      val qualifierFormat = new Qualifier.Format
      val keySize = qualifierFormat.keyIndent
      val keyIndent = 5
      
      @throws(classOf[ParseException])
      def parse(source: Seq[String]): Feature = source match {
        case Seq(head, tail @ _*) =>
          val key = try { parseKey(head) }
            catch {
              case _: ParseException => throw new ParseException(
                "Invalid format for feature key: '" + head + "'", 0)
            }
          val (locationLines, qualifiersLines) = tail span isQualifierContinuing
          val location = toSingleValue(head +: locationLines, keySize, "")
          val qualifiers = readQualifiers(Buffer.empty, qualifiersLines).toIndexedSeq
          
          Feature(key, location, qualifiers)
        case _ => throw new ParseException("Invalid format", 0)
      }
      
      def unapply(source: Seq[String]): Option[Feature] =
        try { Some(parse(source)) }
        catch {
          case e: ParseException => None
          case e => throw e
        }
      
      protected def isQualifierContinuing(line: String) =
        line.length >= keySize && line.charAt(keySize) != '/'
      
      /**
       * Feature のキーを抽出する
       * @param line Feature 項目の最初の行
       * @return {@code Head} が {@code true} を返すとき、
       *         {@code keyIndent} から空白文字が洗われるまでの文字列。
       */
      @throws(classOf[ParseException])
      protected[bio] def parseKey(line: String) = {
        val key = line match {
          case Head() =>
            val keyEnd = line.indexOf(' ', keyIndent)
            if (keyEnd < 0) line.substring(keyIndent)
            else line.substring(keyIndent, keyEnd)
          case _ => ""
        }
        if (key.isEmpty) throw new ParseException("Invalid format", 0)
        key
      }
      
      @throws(classOf[ParseException])
      private def readQualifiers(qualis: Buffer[Qualifier], source: Seq[String]): Buffer[Qualifier] = source match {
        case Seq(keyHead, tail @ _*) =>
          val (keyTail, otherQfLines) = tail span isQualifierContinuing
          val qf = try { qualifierFormat.parse(keyHead +: keyTail) }
          catch { case e: ParseException => throw new ParseException(
                "Invalid format for qualifier: '" + (keyHead +: keyTail).mkString("\\n") + "'", 0) }
          qualis += qf
          readQualifiers(qualis, otherQfLines)
        case _ =>
          qualis
      }
      
      /** Feature 開始行の抽出子 */
      object Head {
        /**
         * Feature のキーを抽出する
         * @param line {@code null} でない行文字列
         * @return {@code line} の長さが {@code keySize} 以上でかつ、
         *         {@code keyIndent} 文字目に文字列が含まれている場合 {@code true} 。
         */
        def unapply(line: String): Boolean =
          line.charAt(keyIndent) != ' ' && line.length >= keySize
      }
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
   * @param join 結合文字列。デフォルトで半角空白文字。
   */
  private def toSingleValue(source: Seq[String], dropSize: Int, join: String = " ") =
    source withFilter (_.length >= dropSize) map
          (_ substring dropSize) mkString join
  
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
