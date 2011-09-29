package jp.scid.bio

import scala.io.Source
import org.specs2.mutable._

class GenBankSpec extends Specification {
  val testResource1 = classOf[GenBankSpec].getResource("NC_001773.gbk")
  val testResource2 = classOf[GenBankSpec].getResource("NC_009347.gbk")
  
  "GenBank オブジェクト" should {
    "テキストファイルからの読み込み" in {
      val genbank = using(testResource1.openStream) { source =>
        GenBank.fromInputStream(source)
      }
      todo // genbank.locus must beEqualTo("NC_001773")
    }
    
    "Locus 構文解析" in {
      import GenBank.Locus
      import java.util.{Date, Calendar}
      import java.text.SimpleDateFormat
      
      def dateFromat = new SimpleDateFormat("yyyy-MM-dd")
      val text1 = "LOCUS       NC_001773               3444 bp    DNA     circular BCT 30-MAR-2006"
      val text2 = "LOCUS       NC_009347               2101 aa    RNA     linear   BCT 19-APR-2007"
      
      "Head オブジェクトの行認知" in {
        Locus.Head.unapply(text1) must beTrue
        Locus.Head.unapply(text2) must beTrue
        Locus.Head.unapply("other") must beFalse
      }
      
      "構文解析 1" in {
        val locus = Locus parseFrom text1
        locus.name must_== "NC_001773"
        locus.sequenceLength must_== 3444
        locus.sequenceUnit must_== "bp"
        locus.molculeType must_== "DNA"
        locus.topology must_== "circular"
        locus.division must_== "BCT"
        locus.date must_== dateFromat.parse("2006-03-30")
      }
      
      "構文解析 2" in {
        val locus = Locus parseFrom text2
        locus.name must_== "NC_009347"
        locus.sequenceLength must_== 2101
        locus.sequenceUnit must_== "aa"
        locus.molculeType must_== "RNA"
        locus.topology must_== "linear"
        locus.division must_== "BCT"
        locus.date must_== dateFromat.parse("2007-04-19")
      }
    }
    
    "Definition 構文解析" in {
      import GenBank.Definition
      val text = "DEFINITION  Pyrococcus abyssi GE5 plasmid pGT5, complete sequence."
      val lines =
        """DEFINITION  Staphylococcus aureus subsp. aureus USA300 plasmid pUSA01, complete
          |            sequence.""".stripMargin.split("\n").toList
      
      "Head オブジェクトの行認知" in {
        Definition.Head.unapply(text) must beTrue
        Definition.Head.unapply(lines.head) must beTrue
        Definition.Head.unapply(lines.tail.head) must beFalse
      }
      
      "単行" in {
        val d = Definition.parseFrom(text)
        d.value must_==("Pyrococcus abyssi GE5 plasmid pGT5, complete sequence.")
      }
      
      "複数行" in {
        val d = Definition.parseFrom(lines)
        d.value must_==("Staphylococcus aureus subsp. aureus USA300 plasmid pUSA01, complete sequence.")
      }
    }
    
    "Features 構文解析" in {
      import GenBank.Features
      
      "Head オブジェクトの行認知" in {
        Features.Head.unapply("FEATURES             Location/Qualifiers") must beTrue
      }
    }
    
    "Feature 構文解析" in {
      import GenBank.Feature
      val sourceFeature =
        """     source          1..3444
          |                     /organism="Pyrococcus abyssi GE5"
          |                     /mol_type="genomic DNA"
          |                     /strain="GE5"
          |                     /db_xref="taxon:272844"
          |                     /plasmid="pGT5"""".stripMargin.split("\n").toList
      val geneFeature =
        """     gene            703..1668
          |                     /gene="rep"
          |                     /locus_tag="SSON_PC01"
          |                     /db_xref="GeneID:4991518"""".stripMargin.split("\n").toList
      val cdsFeature =
        """     CDS             703..1668
          |                     /gene="rep"
          |                     /locus_tag="SSON_PC01"
          |                     /note="replication protein"
          |                     /codon_start=1
          |                     /transl_table=11
          |                     /product="Rep"
          |                     /protein_id="YP_001139965.1"
          |                     /db_xref="GI:145294041"
          |                     /db_xref="GeneID:4991518"
          |                     /translation="MSEDKFLSDYSPRDAVWDTQRTLTDSVGGIYQTAAEFERYALRM
          |                     ASCSGLLRFGWSTIMETGETRLRLRSAQFCRVRHCPVCQWRRTLMWQARFYQALPKIV
          |                     VDYPSSRWLFLTLTVRNCEIGELGTVLTAMNAAFKRMEKRKELSPVQGWIRATEVTRG
          |                     KDGSAHPHFHCLLMVQPSWFKGKNYVKHERWVELWRDCLRVNYEPNIDIRAVKTKTGE
          |                     VVANVAEQLQSAVAETLKYSVKPEDMANDPEWFLELTRQLHKRRFISTGGALKNVLQL
          |                     DRETNEDLVIADDVGDGTDDGKRTAFVWDSGKRRYKRAPEKDKSD"""".stripMargin.split("\n").toList
      val repeatRegionFeature =
        """     repeat_region   2051..2123
          |                     /note="repeat region RIII"
          |                     /rpt_type=inverted""".stripMargin.split("\n").toList
      
      "Head オブジェクトの行認知" in {
        Feature.Head.unapply(sourceFeature.head) must beSome.which("source".equals)
        Feature.Head.unapply(geneFeature.head) must beSome.which("gene".equals)
        Feature.Head.unapply(cdsFeature.head) must beSome.which("CDS".equals)
        Feature.Head.unapply(repeatRegionFeature.head) must beSome.which("repeat_region".equals)
        Feature.Head.unapply(sourceFeature.tail.head) must beNone
        Feature.Head.unapply("     source         ") must beNone
      }
      
      "source 情報" in {
        val f = Feature.parseFrom(sourceFeature)
        f.key must_== "source"
      }
    }
  }
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}
