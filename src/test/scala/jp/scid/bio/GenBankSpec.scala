package jp.scid.bio

import scala.io.Source
import org.specs2.mutable._

class GenBankSpec extends Specification {
  "GenBank オブジェクト" should {
    "Locus.Format" in {
      import GenBank.Locus
      import java.util.{Date, Calendar}
      import java.text.SimpleDateFormat
      
      val format = new Locus.Format
      def dateFromat = new SimpleDateFormat("yyyy-MM-dd")
      val text1 = "LOCUS       NC_001773               3444 bp    DNA     circular BCT 30-MAR-2006"
      val text2 = "LOCUS       NC_009347               2101 aa    RNA     linear   BCT 19-APR-2007"
      
      "Head オブジェクトの行認知" in {
        format.Head.unapply(text1) must beTrue
        format.Head.unapply(text2) must beTrue
        format.Head.unapply("other") must beFalse
      }
      
      "構文解析 1" in {
        val locus = format parse text1
        locus.name must_== "NC_001773"
        locus.sequenceLength must_== 3444
        locus.sequenceUnit must_== "bp"
        locus.molculeType must_== "DNA"
        locus.topology must_== "circular"
        locus.division must_== "BCT"
        locus.date must_== Some(dateFromat.parse("2006-03-30"))
      }
      
      "構文解析 2" in {
        val locus = format unapply text2 get;
        locus.name must_== "NC_009347"
        locus.sequenceLength must_== 2101
        locus.sequenceUnit must_== "aa"
        locus.molculeType must_== "RNA"
        locus.topology must_== "linear"
        locus.division must_== "BCT"
        locus.date must_== Some(dateFromat.parse("2007-04-19"))
      }
    }
    
    "Definition.Format" in {
      import GenBank.Definition
      val format = new Definition.Format
      val text = "DEFINITION  Pyrococcus abyssi GE5 plasmid pGT5, complete sequence."
      val lines =
        """DEFINITION  Staphylococcus aureus subsp. aureus USA300 plasmid pUSA01, complete
          |            sequence.""".stripMargin.split("\n").toList
      
      "Head オブジェクトの行認知" in {
        format.Head.unapply(text) must beTrue
        format.Head.unapply(lines.head) must beTrue
        format.Head.unapply(lines.tail.head) must beFalse
      }
      
      "単行" in {
        val d = format unapply text get;
        d.value must_==("Pyrococcus abyssi GE5 plasmid pGT5, complete sequence.")
      }
      
      "複数行" in {
        val d = format parse lines
        d.value must_==("Staphylococcus aureus subsp. aureus USA300 plasmid pUSA01, complete sequence.")
      }
    }
    
    "Accession.Format" in {
      import GenBank.Accession
      val format = new Accession.Format
      val text1 = "ACCESSION   AE017263 AADU01000000 AADU01000001"
      val text2 = "ACCESSION   NC_009347"
      val lines =
        """ACCESSION   BA000002 AP000058 AP000059 AP000060 AP000061 AP000062 AP000063
          |            AP000064""".stripMargin.split("\n").toList
      
      "Head オブジェクトの行認知" in {
        format.Head.unapply(text1) must beTrue
        format.Head.unapply(text2) must beTrue
        format.Head.unapply(lines.head) must beTrue
        format.Head.unapply(lines.tail.head) must beFalse
      }
      
      "単行1" in {
        val a = format unapply text1 get;
        a.primary must_==("AE017263")
        a.secondary must_== IndexedSeq("AADU01000000", "AADU01000001")
      }
      
      "単行2" in {
        val a = format unapply text2 get;
        a.primary must_==("NC_009347")
        a.secondary must_== IndexedSeq()
      }
      
      "複数行" in {
        val a = format parse lines
        a.primary must_==("BA000002")
        a.secondary must_== IndexedSeq("AP000058", "AP000059", "AP000060",
          "AP000061", "AP000062", "AP000063", "AP000064")
      }
    }
    
    "Source.Format" in {
      import GenBank.Source
      val format = new Source.Format
      val text1 = "SOURCE      Pyrococcus abyssi GE5"
      val lines =
        """SOURCE      Shigella sonnei Ss046
          |  ORGANISM  Shigella sonnei Ss046
          |            Bacteria; Proteobacteria; Gammaproteobacteria; Enterobacteriales;
          |            Enterobacteriaceae; Shigella.""".stripMargin.split("\n").toList
      
      "Head オブジェクトの行認知" in {
        format.Head.unapply(text1) must beTrue
      }
      
      "単行" in {
        val s = format unapply text1 get;
        s.value must_== "Pyrococcus abyssi GE5"
      }
      
      "複数行" in {
        val s = format parse lines
        s.value must_== "Shigella sonnei Ss046"
        s.organism must_== "Shigella sonnei Ss046"
        s.taxonomy must_== IndexedSeq("Bacteria", "Proteobacteria", "Gammaproteobacteria",
          "Enterobacteriales", "Enterobacteriaceae", "Shigella")
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
    
    "Origin 構文解析" in {
      import GenBank.Origin
      
      "Head オブジェクトの行認知" in {
        Origin.Head.unapply("ORIGIN      ") must beTrue
      }
    }
    
  }
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}
