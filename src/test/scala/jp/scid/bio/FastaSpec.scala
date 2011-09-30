package jp.scid.bio

import scala.io.Source
import org.specs2.mutable._

class FastaSpec extends Specification {
  "Fasta オブジェクト" should {
    "Header オブジェクト" in {
      import Fasta.Header
      
      "構文解析 1" in {
        val line = ">gi|16127995|ref|NP_414542.1| thr operon leader peptide [Escherichia coli K12]"
        val h = Header parseFrom line
        
        h.identifier must_== "16127995"
        h.namespace must_== "ref"
        h.accession must_== "NP_414542"
        h.version must_== 1
        h.name must_== ""
        h.description must_== "thr operon leader peptide [Escherichia coli K12]"
      }
      
      "構文解析 2" in {
        val line = ">gi|129295|sp|P01013|OVAX_CHICK GENE X PROTEIN (OVALBUMIN-RELATED)"
        val h = Header parseFrom line
        
        h.identifier must_== "129295"
        h.namespace must_== "sp"
        h.accession must_== "P01013"
        h.version must_== 0
        h.name must_== "OVAX_CHICK"
        h.description must_== "GENE X PROTEIN (OVALBUMIN-RELATED)"
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
