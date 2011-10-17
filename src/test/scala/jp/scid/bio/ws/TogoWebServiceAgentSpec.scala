package jp.scid.bio.ws

import org.specs2._
import WebServiceAgent.{Identifier, EntryValues}

class TogoWebServiceAgentSpec extends Specification {
  def is = "TogoWebServiceAgent" ^
    "countHeavy" ^
      "クエリ検索" ! countHeavy.s1 ^
      "空白区切り" ! countHeavy.s2 ^
      "空文字で 0" ! countHeavy.s3 ^
      "空白文字のみで 0" ! countHeavy.s4 ^
    bt ^ "count" ^
      "クエリ検索" ! count.s1 ^
      "空白区切り" ! count.s2 ^
      "空文字で 0" ! count.s3 ^
      "空白文字のみで 0" ! count.s4 ^
    bt ^ "searchIdentifiersHeavy" ^
      "クエリ検索" ! sih.s1 ^
      "空白区切り" ! sih.s2 ^
      "offset 指定" ! sih.s3 ^
      "limit 指定" ! sih.s4 ^
      "空文字で空配列" ! sih.s5 ^
      "空白文字のみで空配列" ! sih.s6 ^
    bt ^ "searchIdentifiers" ^
      "クエリ検索" ! si.s1 ^
      "空白区切り" ! si.s2 ^
      "空文字で空配列" ! sih.s3 ^
      "空白文字のみで空配列" ! sih.s4 ^
    bt ^ "getIdentifiersFromWeb" ^
      "取得" ! ifw.s1 ^
      "不明要素が含まれている時は空オブジェクトが戻る" ! ifw.s2 ^
      "空配列で空配列が返る" ! ifw.s3 ^
    bt ^ "getFieldValuesFor" ^
      "取得" ! fv.s1 ^
      "不明要素が含まれている時は空オブジェクトが戻る" ! fv.s2 ^
      "空配列で空配列が返る" ! fv.s3
  
  abstract class TestBase {
    val agent = new TogoWebServiceAgent()
    val sampleAccession = "HM367685"
    val identifierOfSampleAccession = Identifier("308206734")
    val valuesOfSampleAccession = EntryValues(identifierOfSampleAccession,
      "HM367685", 401262, "Vigna radiata mitochondrion, complete genome.")
    val spacedQuery = "lung cancer"
  }
  
  val countHeavy = new TestBase {
    def s1 = agent.countHeavy(sampleAccession) must_== 1
    
    def s2 = agent.countHeavy(spacedQuery) must be_>=(1000)
    
    def s3 = agent.countHeavy("") must_== 0
    
    def s4 = agent.countHeavy(" ") must_== 0
  }
  
  val count = new TestBase {
    val s1 = agent.count(sampleAccession).apply must_== 1
    
    val s2 = agent.count(spacedQuery).apply must be_>=(1000)
    
    val s3 = agent.count("").apply must_== 0
    
    val s4 = agent.count(" ").apply must_== 0
  }
  
  val sih = new TestBase {
    // 単項目
    val result1 = agent.searchIdentifiersHeavy(sampleAccession, 0, 10)
    // 副数項目
    val result2 = agent.searchIdentifiersHeavy(spacedQuery, 0, 3)
    // 副数項目2
    val result3 = agent.searchIdentifiersHeavy(spacedQuery, 3, 3)
    // 副数項目3
    val result4 = agent.searchIdentifiersHeavy(spacedQuery, 0, 6)
    // 空白
    val emptyResult = agent.searchIdentifiersHeavy("", 0, 10)
    // 空白文字
    val blankCharResult = agent.searchIdentifiersHeavy(" ", 0, 10)
    // 項目数超えの取得
//    val overLimit = agent.searchIdentifiersHeavy(sampleAccession, 0, 2)
    
    def s1 = result1 must contain(identifierOfSampleAccession).only.inOrder
    
    def s2 = result2 must haveSize(3)
    
    def s3 = result3 must not contain(result2(0)) and not contain(result2(1)) and
      not contain(result2(2)) and have size(3)
    
    def s4 = result4 must haveTheSameElementsAs(result2 ++ result3)
    
    def s5 = emptyResult must be empty
    
    def s6 = blankCharResult must be empty
    
//    def s7 = overLimit must contain(identifierOfSampleAccession,
//      Identifier.empty).only.inOrder
  }
  
  val si = new TestBase {
    // 単項目
    val result1 = agent.searchIdentifiers(sampleAccession, 0, 10)
    // 副数項目
    val result2 = agent.searchIdentifiers(spacedQuery, 0, 3)
    // 空白
    val emptyResult = agent.searchIdentifiers("", 0, 10)
    // 空白文字
    val blankCharResult = agent.searchIdentifiers(" ", 0, 10)
    
    def s1 = result1.apply must contain(identifierOfSampleAccession).only
    
    def s2 = result2.apply must haveSize(3)
    
    def s3 = emptyResult.apply must be empty
    
    def s4 = blankCharResult.apply must be empty
  }
  
  val ifw = new TestBase {
    val result1 = agent.getFieldValues(Seq(identifierOfSampleAccession))
    
    val result2 = agent.getFieldValues(Seq(identifierOfSampleAccession,
      Identifier.empty))
    
    val result3 = agent.getFieldValues(Seq.empty)
    
    def s1 = result1 must contain(valuesOfSampleAccession).only
    def s2 = result2 must contain(valuesOfSampleAccession, EntryValues.empty).only.inOrder
    def s3 = result3 must be empty
  }
  
  val fv = new TestBase {
    val result1 = agent.getFieldValuesFor(Seq(identifierOfSampleAccession))
    
    val result2 = agent.getFieldValuesFor(Seq(identifierOfSampleAccession,
      Identifier.empty))
    
    val result3 = agent.getFieldValuesFor(Seq.empty)
    
    def s1 = result1.apply must contain(valuesOfSampleAccession).only
    def s2 = result2.apply must contain(valuesOfSampleAccession, EntryValues.empty).only.inOrder
    def s3 = result3.apply must be empty
  }
}
