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
  }
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}
