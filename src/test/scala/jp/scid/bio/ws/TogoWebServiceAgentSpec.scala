package jp.scid.bio.ws

import org.specs2._
import WebServiceAgent.{Query, Identifier, EntryValues}

class TogoWebServiceAgentSpec extends Specification {
  def is = "TogoWebServiceAgent" ^ sequential ^
    "該当数の取得" ^ canGetCount(agent) ^ bt ^
    "識別子の取得" ^ cangetIdentifier(agent) ^ bt ^
    "属性値の取得" ^ canGetFieldValues(agent) ^ bt ^
    end
  
  
  def agent = new TogoWebServiceAgent()
  
  def canGetCount(a: => TogoWebServiceAgent) =
    "識別子" ! getCount(a).byIdentifier ^
    "前後にスペースがあっても検索できる" ! getCount(a).spaced ^
    "空白区切り" ! getCount(a).bySpaceSeparated ^
    "空文字で 0" ! getCount(a).byEmptyString ^
    "空白文字のみで 0" ! getCount(a).byBlankStrings
  
  def cangetIdentifier(a: => TogoWebServiceAgent) =
    "単独項目" ! searchIdentifier(a).singleQuery ^
    "副数項目" ! searchIdentifier(a).multipleQuery ^
    "該当しないときは空配列" ! searchIdentifier(a).notMatch
  
  def canGetFieldValues(a: => TogoWebServiceAgent) =
    "取得" ! getFieldValues(a).singleQuery
  
  def getCount(agent: TogoWebServiceAgent) = new Object {
    def byIdentifier =
      agent.getCount("HM367685") must_== Query("HM367685", 1)
    
    def spaced =
      agent.getCount("  HM367685  ") must_== Query("  HM367685  ", 1)
    
    def bySpaceSeparated =
      agent.getCount("lung cancer").count must be_>(1000)
    
    def byEmptyString =
      agent.getCount("") must_== Query("", 0)
    
    def byBlankStrings =
      agent.getCount(" 　") must_== Query(" 　", 0)
  }
  
  def searchIdentifier(agent: TogoWebServiceAgent) = new Object {
    def singleQuery =
      agent.searchIdentifiers(Query("HM367685", 1)) must
        contain(Identifier("308206734")).only
    
    def multipleQuery =
      agent.searchIdentifiers(Query("lung cancer", 10)) must
        haveSize(10)
    
    def notMatch =
      agent.searchIdentifiers(Query("_", 10)) must beEmpty
  }
  
  def getFieldValues(agent: TogoWebServiceAgent) = new Object {
    def singleQuery = {
      val identifier = Identifier("308206734")
      agent.getFieldValues(Seq(identifier)) must
        contain(EntryValues(identifier, "HM367685", 401262,
          "Vigna radiata mitochondrion, complete genome.")).only
    }
  }
}
