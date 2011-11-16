package jp.scid.gui

import org.specs2._

class StringFilterableSpec extends Specification {
  def is = "StringFilterable" ^
    "抽出1" ! s1 ^
    "抽出2" ! s2 ^
    "抽出3" ! s3
  
  class TestBase {
    val model = new DataListModel[String] with StringFilterable[String] {
      def getFilterString(baseList: java.util.List[String], element: String) {
        baseList add element
      }
    }
    
    model.source = List("apple", "SAMPLE", "bread")
  }
  
  def base = new TestBase {
    def pleFiltered = {
      model filterWith "ple"
      model.viewSource
    }
    
    def noMatch = {
      model filterWith "123456"
      model.viewSource
    }
    
    def emptyFilter = {
      model filterWith ""
      model.viewSource
    }
  }
  
  
  def s1 = base.pleFiltered must contain("apple", "SAMPLE").only.inOrder
  
  def s2 = base.noMatch must beEmpty
  
  def s3 = base.emptyFilter must contain("apple", "SAMPLE", "bread").only.inOrder
}
