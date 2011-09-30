package jp.scid.bio

import scala.io.Source
import org.specs2.mutable._

class FastaParserSpec extends Specification {
  val testResource1 = classOf[FastaParserSpec].getResource("NC_009473.faa")
  val testResource2 = classOf[FastaParserSpec].getResource("NC_009473.ffn")
  val testResource3 = classOf[FastaParserSpec].getResource("NC_009473.fna")
  
  "FastaParser" should {
    val parser = new FastaParser
    
    "テキストファイルからの読み込み 1" in {
      val fasta = using(testResource1.openStream) { inst =>
        val source = io.Source.fromInputStream(inst)
        parser.parseFrom(source.getLines)
      }
      fasta.header must_== Fasta.Header(
        "148244128", "ref", "YP_001220362", 1, "",
        "hypothetical protein Acry_3628 [Acidiphilium cryptum JF-5]")
      fasta.sequence must_== Fasta.Sequence(
        "MITTSYDPEADALYVRFAPKGATIAETRELEPGILLDMDEIGHLVGIEVLGVRSRATLPAIPYAA")
    }
    
    "テキストファイルからの読み込み 2" in {
      val fasta = using(testResource2.openStream) { inst =>
        val source = io.Source.fromInputStream(inst)
        parser.parseFrom(source.getLines)
      }
      fasta.header must_== Fasta.Header(
        "", "ref", "NC_009473", 1, ":c1094-897",
        "hypothetical protein Acry_3628 [Acidiphilium cryptum JF-5]")
      fasta.sequence must_== Fasta.Sequence(
        "GTGATCACGACCAGCTATGATCCCGAGGCGGATGCGCTCTATGTCCGGTTCGCCCCTAAGGGAGCGACGA" + 
        "TTGCCGAGACCAGGGAACTGGAGCCTGGCATCCTGCTGGATATGGACGAGATCGGACACCTAGTCGGGAT" + 
        "CGAAGTGCTGGGTGTGCGATCACGGGCAACACTACCGGCAATTCCATATGCGGCATGA")
    }
    
    "テキストファイルからの読み込み 3" in {
      val fasta = using(testResource3.openStream) { inst =>
        val source = io.Source.fromInputStream(inst)
        parser.parseFrom(source.getLines)
      }
      fasta.header must_== Fasta.Header(
        "148244127", "ref", "NC_009473", 1, "",
        "Acidiphilium cryptum JF-5 plasmid pACRY07, complete sequence")
      fasta.sequence.value.length must_== 5629
    }
  }
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}
