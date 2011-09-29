package jp.scid.bio

import scala.io.Source
import org.specs2.mutable._

class GenBankParserSpec extends Specification {
  val testResource1 = classOf[GenBankParserSpec].getResource("NC_001773.gbk")
  val testResource2 = classOf[GenBankParserSpec].getResource("NC_009347.gbk")
  
  "GenBankParser" should {
    val parser = new GenBankParser
    
    "テキストファイルからの読み込み 1" in {
      val genbank = using(testResource1.openStream) { inst =>
        val source = io.Source.fromInputStream(inst)
        parser.parseFrom(source.getLines)
      }
      genbank.locus.name must beEqualTo("NC_001773")
    }
    
    "テキストファイルからの読み込み 2" in {
      val genbank = using(testResource2.openStream) { inst =>
        val source = io.Source.fromInputStream(inst)
        parser.parseFrom(source.getLines)
      }
      genbank.locus.name must beEqualTo("NC_009347")
    }
  }
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}
