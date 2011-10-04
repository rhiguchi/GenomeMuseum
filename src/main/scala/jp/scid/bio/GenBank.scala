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
  type Features = IndexedSeq[Feature]
  
  sealed abstract private[GenBank] class ElementFormat(val headKey: String) {
    /** この要素のキー領域の大きさ */
    val keySize = 12
    
    /**
     * この要素の先頭行であるかの判定
     * @param line GenBank データ行
     * @return 先頭である場合 true
     */
    protected def isElementHead(line: String) = 
      line.length >= keySize && line.startsWith(headKey)
    
    /**
     * 要素の先頭行判定の抽出子
     */
    object Head {
      override def toString = headKey
      def unapply(line: String): Boolean = isElementHead(line)
    }
  }
  
  /**
   * parse メソッドでの例外を補足する unapply メソッドを提供するトレイト
   */
  private[GenBank] trait Extractable[T <: Element] {
    /**
     * GenBank 要素の行文字列からインスタンスを作成する
     * @param source 作成元文字列
     * @return 要素インスタンス
     * @throws ParseException 文字列から要素が作成できない場合
     */
    @throws(classOf[ParseException])
    def parse(source: Seq[String]): T
    
    /**
     * GenBank 要素行の文字列から要素インスタンスを作成する
     * @param source 作成元文字列
     * @return 作成に成功した時は {@code Some[Definition]} 。
     *         形式に誤りがあるなどで作成できない時は {@code None}
     */
    def unapply(source: Seq[String]): Option[T] =
      try {
        Some(parse(source))
      }
      catch {
        case e: ParseException => None
      }
    
    /**
     * 要素の行文字列からインスタンスを作成する
     * @param source 作成元文字列
     * @return 要素インスタンス。形式に誤りがあるなどで作成できない時は {@code None} 。
     */
    def unapply(source: String): Option[T] = unapply(Seq(source))
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
    class Format extends ElementFormat("LOCUS") with Extractable[Locus] {
      /**
       * LOCUS 行の文字列から Locus インスタンスを作成する
       * @param source 作成元文字列
       * @return Locus インスタンス
       * @throws ParseException {@code source} が {@code LOCUS}
       *         から始まっていなく、また {@code keySize } で定義される
       *         文字列長より短い場合。もしくは文字列形式に誤りがある場合。
       */
      @throws(classOf[ParseException])
      def parse(source: String): Locus = fragmentate(source) match {
        case Some((name, seqPair, molType, topology, division, date)) =>
          val (seqLength, seqUnit) = seqPair.getOrElse("0", "")
          Locus(name, parseInt(seqLength), seqUnit, molType.getOrElse(""),
            topology.getOrElse(""), division.getOrElse(""), date.map(parseDate))
        case _ => throw new ParseException("Invalid format for Locus", 0)
      }
      
      def parse(source: Seq[String]) = parse(source.head)
      
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
    class Format extends ElementFormat("DEFINITION") with Extractable[Definition] {
      /**
       * DEFINITION 行の文字列から Definition インスタンスを作成する
       * @param source 作成元文字列の配列
       * @return {@code Definition} インスタンス
       * @throws ParseException 形式に誤りがあるなどで作成できない場合
       */
      def parse(source: Seq[String]): Definition = source match {
        case Seq(Head(), _*) =>
          val defValue = toSingleValue(source, keySize)
          Definition(defValue)
        case _ => throw new ParseException("Invalid format for Definition", 0)
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
    class Format extends ElementFormat("ACCESSION") with Extractable[Accession] {
      /**
       * ACCESSION 行の文字列から Accession インスタンスを作成する
       * @param source 作成元文字列の配列
       * @return {@code Accession} インスタンス
       * @throws ParseException 形式に誤りがあるなどで作成できない場合
       */
      @throws(classOf[ParseException])
      def parse(source: Seq[String]): Accession = source match {
        case Seq(Head(), _*) =>
          val seqVal = source withFilter (_.length >= keySize) flatMap
            (_ substring keySize trim() split "\\s+")
          seqVal match {
            case Seq(head, tail @ _*) => Accession(head, tail.toIndexedSeq)
            case _ => Accession()
          }
        case _ => throw new ParseException("Invalid format for Accession", 0)
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
    class Format extends ElementFormat("VERSION") with Extractable[Version] {
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
    
    class Format extends ElementFormat("KEYWORDS") with Extractable[Keywords] {
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
    class Format extends ElementFormat("SOURCE") with Extractable[Source] {
      val organismFormat = new OrganismFormat
      
      /**
       * SOURCE 行の文字列から Source インスタンスを作成する
       * @param source 作成元文字列の配列
       * @return {@code Source} インスタンス
       * @throws ParseException 形式に誤りがあるなどで作成できない場合
       */
      @throws(classOf[ParseException])
      def parse(source: Seq[String]): Source = source match {
        case Seq(head @ Head(), tail @ _*) =>
          val (sourceLines, remaining) = tail span isValueContinuing
          val sourceValue = toSingleValue(head +: sourceLines, keySize)
          val (organism, taxonomy) =
            if (remaining.isEmpty) ("", IndexedSeq.empty)
            else organismFormat.parse(remaining)
          Source(sourceValue, organism, taxonomy)
        case _ => throw new ParseException("Invalid format for Source", 0)
      }
      
      protected def isValueContinuing(line: String) =
        line != null && line.startsWith("   ")
    }
    
    class OrganismFormat extends ElementFormat("  ORGANISM") {
      @throws(classOf[ParseException])
      def parse(source: Seq[String]): (String, IndexedSeq[String]) = source match {
        case Seq(Head(), _*) =>
          val (organismLines, taxonomyLines) = source span (isOrganismValue)
          val organism = toSingleValue(organismLines, keySize)
          val taxonomy = makeTaxonomy(taxonomyLines)
          (organism, taxonomy)
        case _ => throw new ParseException("Invalid format for Organism", 0)
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
  
  object Reference {
    class Format extends ElementFormat("REFERENCE") with Extractable[Reference] {
      import collection.mutable.Map
      /** REFERENCE 行マッチパターン（8 要素） */
      lazy private val ReferencePattern = "(?x)" +
        """(\d+)""" + // Reference 番号
        """(?: \s* \(bases \s+ (\d+) \s+ to \s+ (\d+) \) )?""" r // 位置
      
      @throws(classOf[ParseException])
      def parse(source: Seq[String]): Reference = source match {
        case Seq(head @ Head(), tail @ _*) =>
          
          val (basesStart, basesEnd) = baseFragmentate(head) match {
            case Some((start, end)) => (parseInt(start), parseInt(end))
            case None => (0, 0)
          }
          
          val elmMap = readElements(Map.empty, tail)
          val authors = elmMap.remove('AUTHORS).getOrElse("")
          val title = elmMap.remove('TITLE).getOrElse("")
          val journal = elmMap.remove('JOURNAL).getOrElse("")
          val pubmed = elmMap.remove('PUBMED).getOrElse("")
          val remark = elmMap.remove('REMARK).getOrElse("")
          
          Reference(basesStart, basesEnd,
            authors, title, journal, pubmed, remark, elmMap.toMap)
        case _ => throw new ParseException("Invalid format", 0)
      }
      
      @annotation.tailrec
      private def readElements(accume: Map[Symbol, String], source: Seq[String]): Map[Symbol, String] = {
        source match {
          case Seq(keyLine, tail @ _*) =>
            val key = Symbol(parseKey(keyLine))
            val (valueTail, remaining) = tail span isReferenceValueContinuing
            val value = toSingleValue(keyLine +: valueTail, keySize)
            accume(key) = value
            readElements(accume, remaining)
          case _ => accume
        }
      }
      
      protected def baseFragmentate(source: String): Option[(String, String)] = source.drop(keySize) match {
        case ReferencePattern(index, start, end) => Some(start, end)
        case _ => None
      }
      
      protected def isReferenceValueContinuing(line: String) =
        line.startsWith("    ")
      
      protected def parseKey(line: String) = line.substring(0, keySize).trim()
    }
  }
  
  /** Comment 要素 */
  case class Comment (
    value: String = ""
  ) extends Element
  
  object Comment {
    class Format extends ElementFormat("COMMENT") with Extractable[Comment] {
      /**
       * COMMENT 行の文字列から Comment インスタンスを作成する
       * @param source 作成元文字列の配列
       * @return Comment インスタンス
       */
      @throws(classOf[ParseException])
      def parse(source: Seq[String]): Comment = source match {
        case Seq(Head(), _*) =>
          val defValue = toSingleValue(source, keySize)
          Comment(defValue)
        case _ => throw new ParseException("Invalid format", 0)
      }
    }
  }
  
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
    ) extends Element
    
    object Qualifier {
      class Format extends Extractable[Qualifier] {
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
         * Qualifier のキーを抽出する
         * @param line {@code null} でない行文字列
         * @return {@code line} の長さが {@code keyIndent + 1} 以上でかつ、
         *         {@code keyIndent} 文字目が {@code keyStartChar} である場合 {@code true} 。
         */
        protected def isQualifierHead(line: String) =
          line.length > keyIndent && line.charAt(keyIndent) == keyStartChar &&
            line.charAt(keyIndent + 1) != ' '
        
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
        
        /** Qualifier 開始行の抽出子 */
        object Head {
          def unapply(line: String): Boolean = isQualifierHead(line)
        }
      }
    }
    
    class Format extends Extractable[Feature] {
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
      
      /**
       * Feature のキー行であるかの判定
       * @param line {@code null} でない行文字列
       * @return {@code line} の長さが {@code keySize} 以上でかつ、
       *         {@code keyIndent} 文字目に文字列が含まれている場合 {@code true} 。
       */
      protected def isFeatureHead(line: String) =
        line.charAt(keyIndent) != ' ' && line.length >= keySize
      
      /** Feature 開始行の抽出子 */
      object Head {
        def unapply(line: String): Boolean = isFeatureHead(line)
      }
    }
  }
  
  /** Origin 要素 */
  case class Origin (
    sequence: String = ""
  ) extends Element
  
  object Origin {
    class Format extends ElementFormat("ORIGIN") with Extractable[Origin] {
      val locationSize = 10
      
      @throws(classOf[ParseException])
      def parse(source: Seq[String]): Origin = source match {
        case Seq(Head(), tail @ _*) =>
          val sequence = readSequence(tail.iterator)
          Origin(sequence)
        case _ => throw new ParseException("Invalid format", 0)
      }
      
      def readSequence(source: Iterator[String]): String =
        readSequence(new StringBuilder, source.buffered).toString
      
      protected def isSequenceContinuing(line: String) =
        line.length >= locationSize && line.charAt(0) == ' '
      
      protected def extractSequence(line: String) =
        if (line.length >= 75)
          line.substring(10, 20) + line.substring(21, 31) + line.substring(32, 42) +
            line.substring(43, 53) + line.substring(54, 64) + line.substring(65, 75)
        else line.substring(10).replace(" ", "")
      
      private def readSequence(sb: StringBuilder, source: BufferedIterator[String]): StringBuilder = {
        if (source.hasNext && isSequenceContinuing(source.head)) {
          sb.append(extractSequence(source.next))
          readSequence(sb, source)
        }
        else sb
      }
    }
  }
  
  /** 終末要素 */
  object Termination {
    class Format extends ElementFormat("//") {
      override val keySize = 2
      override protected def isElementHead(line: String) = 
        line.startsWith(headKey)
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
