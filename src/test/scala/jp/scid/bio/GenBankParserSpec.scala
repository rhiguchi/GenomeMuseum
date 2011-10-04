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
      
      "Version" in {
        genbank.version must_== Version("NC_001773", 1, "10954552")
      }
      
      "Keywords" in {
        genbank.keywords must_== Keywords.Dot
      }
      
      "Source" in {
        genbank.source must_== Source("Pyrococcus abyssi GE5", "Pyrococcus abyssi GE5",
          IndexedSeq("Archaea", "Euryarchaeota", "Thermococci", "Thermococcales",
            "Thermococcaceae", "Pyrococcus"))
      }
      
      "References" in {
        genbank.references.size must_== 3
        genbank.references(0) must_== Reference(1, 3444,
          "Erauso,G., Marsin,S., Benbouzid-Rollet,N., Baucher,M.F., " +
          "Barbeyron,T., Zivanovic,Y., Prieur,D. and Forterre,P.",
          "Sequence of plasmid pGT5 from the archaeon Pyrococcus abyssi: " +
          "evidence for rolling-circle replication in a hyperthermophile",
          "J. Bacteriol. 178 (11), 3232-3237 (1996)", "8655503")
        genbank.references(1) must_== Reference(1, 3444, title = "Direct Submission",
          journal = "Submitted (24-OCT-2000) National Center for Biotechnology " +
          "Information, NIH, Bethesda, MD 20894, USA",
          others = Map('CONSRTM -> "NCBI Genome Project"))
        genbank.references(2) must_== Reference(1, 3444, "Forterre,P.",
          "Direct Submission", "Submitted (20-FEB-1996) Patrick Forterre, Institut de Genetique et " +
          "Microbiologie, Universite Paris-Sud, Orsay 91405, France")
      }
      
      "Comment" in {
        genbank.comment must_== Comment("PROVISIONAL REFSEQ: This record has not yet been subject to final " +
          "NCBI review. The reference sequence was derived from U49503. " +
          "COMPLETENESS: full length.")
      }
      
      "Feature" in {
        genbank.features.size must_== 11
        genbank.features(0).key must_== "source"
        genbank.features(0).location must_== "1..3444"
        genbank.features(0).qualifiers.size must_== 5
        genbank.features(0).qualifiers(0).key must_== "organism"
        genbank.features(0).qualifiers(0).value must_== "Pyrococcus abyssi GE5"
        
        genbank.features(1).key must_== "rep_origin"
        genbank.features(1).location must_== "1..114"
        genbank.features(1).qualifiers.size must_== 1
        genbank.features(1).qualifiers(0).key must_== "note"
        genbank.features(1).qualifiers(0).value must_== "putative single-stranded origin; sso"
        
        genbank.features(2).key must_== "repeat_region"
        genbank.features(2).location must_== "22..68"
        genbank.features(2).qualifiers.size must_== 2
        
        genbank.features(3).key must_== "gene"
        genbank.features(3).location must_== "103..2067"
        genbank.features(3).qualifiers.size must_== 2
        
        genbank.features(4).key must_== "CDS"
        genbank.features(4).location must_== "103..2067"
        genbank.features(4).qualifiers.size must_== 9
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
      
      "Version" in {
        genbank.version must_== Version("NC_009347", 1, "145294040")
      }
      
      "Keywords" in {
        genbank.keywords must_== Keywords(List("."))
      }
      
      "Source" in {
        genbank.source must_== Source("Shigella sonnei Ss046", "Shigella sonnei Ss046",
          IndexedSeq("Bacteria", "Proteobacteria", "Gammaproteobacteria",
            "Enterobacteriales", "Enterobacteriaceae", "Shigella"))
      }
      
      "Comment" in {
        genbank.comment must_== Comment("PROVISIONAL REFSEQ: This record has not yet been subject to final " +
          "NCBI review. The reference sequence was derived from CP000643. " +
          "COMPLETENESS: full length.")
      }
      
      "Feature" in {
        genbank.features.size must_== 3
        genbank.features(0).key must_== "source"
        genbank.features(0).location must_== "1..2101"
        genbank.features(0).qualifiers(0).key must_== "organism"
        genbank.features(0).qualifiers(0).value must_== "Shigella sonnei Ss046"
        genbank.features(0).qualifiers.size must_== 5
        
        genbank.features(1).key must_== "gene"
        genbank.features(1).location must_== "703..1668"
        genbank.features(1).qualifiers.size must_== 3
        
        genbank.features(2).key must_== "CDS"
        genbank.features(2).location must_== "703..1668"
        genbank.features(2).qualifiers.size must_== 10
      }
    }
  }
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}
