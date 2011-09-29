package jp.scid.bio

import java.io.InputStream
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
  
  /** Definition 要素 */
  case class Definition (
    value: String = ""
  ) extends Element
  
  
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
}