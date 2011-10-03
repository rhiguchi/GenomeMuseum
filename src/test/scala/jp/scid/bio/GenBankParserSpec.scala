package jp.scid.bio

import org.specs2.mutable._

class GenBankParserSpec extends Specification {
  val testResource1 = classOf[GenBankParserSpec].getResource("NC_001773.gbk")
  val testResource2 = classOf[GenBankParserSpec].getResource("NC_009347.gbk")
  
  "GenBankParser" should {
    import GenBank._
    def dateFromat = new java.text.SimpleDateFormat("yyyy-MM-dd")
    val parser = new GenBankParser
    
    "テキストファイルからの読み込み 1" in {
      val genbank = using(testResource1.openStream) { inst =>
        val source = io.Source.fromInputStream(inst)
        parser.parseFrom(source.getLines)
      }
      
      "Locus" in {
        genbank.locus must_== Locus("NC_001773", 3444, "bp", "DNA", "circular",
          "BCT", Some(dateFromat.parse("2006-03-30")))
      }
      
      "Definition" in {
        genbank.definition must_== Definition("Pyrococcus abyssi GE5 plasmid pGT5, complete sequence.")
      }
      
      "Accession" in {
        genbank.accession must_== Accession("NC_001773")
      }
      
      "Source" in {
        genbank.source must_== Source("Pyrococcus abyssi GE5", "Pyrococcus abyssi GE5",
          IndexedSeq("Archaea", "Euryarchaeota", "Thermococci", "Thermococcales",
            "Thermococcaceae", "Pyrococcus"))
      }
    }
    
    "テキストファイルからの読み込み 2" in {
      val genbank = using(testResource2.openStream) { inst =>
        val source = io.Source.fromInputStream(inst)
        parser.parseFrom(source.getLines)
      }
      
      "Locus" in {
        genbank.locus must_== Locus("NC_009347", 2101, "bp", "DNA", "circular",
          "BCT", Some(dateFromat.parse("2007-04-19")))
      }
      
      "Definition" in {
        genbank.definition must_== Definition("Shigella sonnei Ss046 plasmid pSS046_spC, complete sequence.")
      }
      
      "Accession" in {
        genbank.accession must_== Accession("NC_009347")
      }
      
      "Source" in {
        genbank.source must_== Source("Shigella sonnei Ss046", "Shigella sonnei Ss046",
          IndexedSeq("Bacteria", "Proteobacteria", "Gammaproteobacteria",
            "Enterobacteriales", "Enterobacteriaceae", "Shigella"))
      }
    }
  }
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}
