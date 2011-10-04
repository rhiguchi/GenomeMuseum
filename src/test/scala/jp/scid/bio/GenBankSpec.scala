package jp.scid.bio

import java.text.ParseException
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
    
    "Version.Format" in {
      import GenBank.Version
      val format = new Version.Format
      val text1 = "VERSION     NC_001773.2  GI:10954552"
      val text2 = "VERSION     NC_009347  GI:145294040"
      
      "Head オブジェクトの行認知" in {
        format.Head.unapply(text1) must beTrue
        format.Head.unapply(text2) must beTrue
      }
      
      "単行1" in {
        val v = format parse text1
        v.accession must_== "NC_001773"
        v.number must_== 2
        v.identifier must_== "10954552"
      }
      
      "単行2" in {
        val v = format parse text2
        v.accession must_== "NC_009347"
        v.number must_== 0
        v.identifier must_== "145294040"
      }
    }
    
    
    "Keywords.Format" in {
      import GenBank.Keywords
      val format = new Keywords.Format
      val text = "KEYWORDS    ."
      
      "Head オブジェクトの行認知" in {
        format.Head.unapply(text) must beTrue
      }
      
      "単行" in {
        val k = format parse text
        k.values must_== List(".")
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
        format.Head.unapply(lines.head) must beTrue
        format.Head.unapply(lines.tail.head) must beFalse
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
    
    "Features.Format" in {
      import GenBank.Features
      
      val format = new Features.Format
      "Head オブジェクトの行認知" in {
        format.Head.unapply("FEATURES             Location/Qualifiers") must beTrue
      }
    }
    
    "Feature.Qualifier.Format" in {
      import GenBank.Feature.Qualifier
      
      val format = new Qualifier.Format
      val line1 = """                     /organism="Pyrococcus abyssi GE5""""
      val line2 = """                     /transl_table=11"""
      val lines1 = """                     /note="putative double-stranded origin; dso; similar to
                     |                     dso seequences from pC194 plasmids"""".stripMargin.split("\n").toList
      val lines2 = """                     /translation="MSEDKFLSDYSPRDAVWDTQRTLTDSVGGIYQTAAEFERYALRM
                     |                     ASCSGLLRFGWSTIMETGETRLRLRSAQFCRVRHCPVCQWRRTLMWQARFYQALPKIV
                     |                     DRETNEDLVIADDVGDGTDDGKRTAFVWDSGKRRYKRAPEKDKSD"""".stripMargin.split("\n").toList
      
      "Head オブジェクトの行認知" in {
        format.Head.unapply(line1) must beTrue
        format.Head.unapply(line2) must beTrue
        format.Head.unapply(lines1.head) must beTrue
        format.Head.unapply(lines1.tail.head) must beFalse
        format.Head.unapply(lines2.head) must beTrue
        format.Head.unapply(lines2.tail.head) must beFalse
      }
      
      "単行1" in {
        val q = format parse line1
        q.key must_== "organism"
        q.value must_== "Pyrococcus abyssi GE5"
        format unapply line1 must beSome.which(
          Qualifier("organism", "Pyrococcus abyssi GE5").equals)
      }
      
      "単行2" in {
        val q = format parse line2
        q.key must_== "transl_table"
        q.value must_== "11"
        format unapply line2 must beSome.which(
          Qualifier("transl_table", "11").equals)
      }
    }
    
    "Feature 構文解析" in {
      import GenBank.Feature
      import Feature.{Qualifier => Qf}
      
      val format = new Feature.Format
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
        format.Head.unapply(sourceFeature.head) must beTrue
        format.Head.unapply(geneFeature.head) must beTrue
        format.Head.unapply(cdsFeature.head) must beTrue
        format.Head.unapply(repeatRegionFeature.head) must beTrue
        format.Head.unapply(sourceFeature.tail.head) must beFalse
        format.Head.unapply("     source         ") must beFalse
      }
      
      "Key オブジェクト抽出" in {
        format.parseKey(sourceFeature.head) must_== "source"
        format.parseKey(geneFeature.head) must_== "gene"
        format.parseKey(cdsFeature.head) must_== "CDS"
        format.parseKey(repeatRegionFeature.head) must_== "repeat_region"
        format.parseKey(sourceFeature.tail.head) must throwA[ParseException]
        format.parseKey("     source         ") must throwA[ParseException]
      }
      
      "source 情報" in {
        val f = format parse sourceFeature
        f.key must_== "source"
        f.location must_== "1..3444"
        f.qualifiers must_== List(Qf("organism", "Pyrococcus abyssi GE5"),
          Qf("mol_type", "genomic DNA"), Qf("strain", "GE5"), Qf("db_xref", "taxon:272844"),
          Qf("plasmid", "pGT5"))
      }
      
      "gene 情報" in {
        val f = format parse geneFeature
        f.key must_== "gene"
        f.location must_== "703..1668"
        f.qualifiers must_== List(Qf("gene", "rep"),
          Qf("locus_tag", "SSON_PC01"), Qf("db_xref", "GeneID:4991518"))
      }
        
      "CDS 情報" in {
        val f = format parse cdsFeature
        f.key must_== "CDS"
        f.location must_== "703..1668"
        f.qualifiers must_== List(Qf("gene", "rep"),
          Qf("locus_tag", "SSON_PC01"), Qf("note", "replication protein"), Qf("codon_start", "1"),
          Qf("transl_table", "11"), Qf("product", "Rep"), Qf("protein_id", "YP_001139965.1"),
          Qf("db_xref", "GI:145294041"), Qf("db_xref", "GeneID:4991518"),
          Qf("translation", "MSEDKFLSDYSPRDAVWDTQRTLTDSVGGIYQTAAEFERYALRM" +
            "ASCSGLLRFGWSTIMETGETRLRLRSAQFCRVRHCPVCQWRRTLMWQARFYQALPKIV" +
            "VDYPSSRWLFLTLTVRNCEIGELGTVLTAMNAAFKRMEKRKELSPVQGWIRATEVTRG" +
            "KDGSAHPHFHCLLMVQPSWFKGKNYVKHERWVELWRDCLRVNYEPNIDIRAVKTKTGE" +
            "VVANVAEQLQSAVAETLKYSVKPEDMANDPEWFLELTRQLHKRRFISTGGALKNVLQL" +
            "DRETNEDLVIADDVGDGTDDGKRTAFVWDSGKRRYKRAPEKDKSD"))
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
