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
      genbank.locus must beEqualTo("NC_001773")
    }
  }
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}
