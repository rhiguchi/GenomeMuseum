package jp.scid.bio.ws

import org.specs2._

class TogoWebServiceAgentSpec extends Specification {
  def is = "TogoWebServiceAgent" ^
    "findEntry" ^
      "取得" ! findEntry.s1 ^
      "不明要素で None" ! findEntry.s2
  
  trait TestBase {
    val agent = new TogoWebServiceAgent()
  }
  
  def findEntry = new TestBase {
    def result = agent.findEntry("M22112")
    
    def invalid = agent.findEntry("xxxx")
    
    val locus = "LOCUS       VFAUSPA                 1071 bp    mRNA    linear   PLN 27-APR-1993"
    
    def s1 = {
      val r = result  
      r must beSome and
        (result.get must startWith(locus))
    }
    
    def s2 = invalid must beNone
  }
}
