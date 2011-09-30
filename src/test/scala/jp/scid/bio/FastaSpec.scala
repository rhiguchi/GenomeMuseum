package jp.scid.bio

import scala.io.Source
import org.specs2.mutable._

class FastaSpec extends Specification {
  "Fasta オブジェクト" should {
    "Header オブジェクト" in {
      import Fasta.Header
      val text1 = ">gi|16127995|ref|NP_414542.1| thr operon leader peptide [Escherichia coli K12]"
      val text2 = ">gi|129295|sp|P01013|OVAX_CHICK GENE X PROTEIN (OVALBUMIN-RELATED)"
      val text3 = ">ref|NC_009473.1|:c1094-897 hypothetical protein Acry_3628 [Acidiphilium cryptum JF-5]"
      
      "Head オブジェクトの行認知" in {
        Header.Head.unapply(text1) must beTrue
        Header.Head.unapply(text2) must beTrue
        Header.Head.unapply(text3) must beTrue
        Header.Head.unapply("other") must beFalse
      }
      
      "構文解析 1" in {
        val h = Header parseFrom text1
        
        h.identifier must_== "16127995"
        h.namespace must_== "ref"
        h.accession must_== "NP_414542"
        h.version must_== 1
        h.name must_== ""
        h.description must_== "thr operon leader peptide [Escherichia coli K12]"
      }
      
      "構文解析 2" in {
        val h = Header parseFrom text2
        
        h.identifier must_== "129295"
        h.namespace must_== "sp"
        h.accession must_== "P01013"
        h.version must_== 0
        h.name must_== "OVAX_CHICK"
        h.description must_== "GENE X PROTEIN (OVALBUMIN-RELATED)"
      }
      
      "構文解析 3" in {
        val h = Header parseFrom text3
        
        h.identifier must_== ""
        h.namespace must_== "ref"
        h.accession must_== "NC_009473"
        h.version must_== 1
        h.name must_== ":c1094-897"
        h.description must_== "hypothetical protein Acry_3628 [Acidiphilium cryptum JF-5]"
      }
    }
    
    "Sequence オブジェクト" in {
      import Fasta.Sequence
      val lines =
        """MKKMQSIVLALSLVLVAPMAAQAAEITLVPSVKLQIGDRDNRGYYWDGGHWRDHGWWKQHYEWRGNRWHL
          |HGPPPPPRHHKKAPHDHHGGHGPGKHHR""".stripMargin.split("\n").toList
      
      "構文解析" in {
        val s = Sequence.parseFrom(lines)
        
        s.value must_== "MKKMQSIVLALSLVLVAPMAAQAAEITLVPSVKLQIGDRDNRGYYWDGGHWRDHGWWKQHYEWRGNRWHLHGPPPPPRHHKKAPHDHHGGHGPGKHHR"
      }
    }
  }
}
