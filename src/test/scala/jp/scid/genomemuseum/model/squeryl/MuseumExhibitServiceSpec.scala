package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import org.squeryl.PrimitiveTypeMode._

class MuseumExhibitServiceSpec extends Specification with SquerylConnection {
  import MuseumExhibitServiceSpec.Schema
  
  def is = "MuseumExhibitService" ^
    "初期状態" ^ isEmpty(emptyService) ^ bt ^
    "テーブルに要素が存在" ^ isNotEmpty(serviceSomeElements) ^ bt ^
    "要素の作成" ^ canCreate(emptyService) ^ bt ^
    "要素の削除" ^ canRemove(emptyService) ^ bt ^
    "要素の永続化" ^ canSave(emptyService) ^ bt ^
    "要素の順序取得" ^ canGetIndex(serviceSomeElements) ^ bt ^
    end
  
  val schema = new Schema
  
  def emptyService = {
    setUpSchema()
    new MuseumExhibitService(schema.museumExhibit)
  }
  
  def serviceSomeElements = {
    setUpSchema()
    0 to 2 map (i => MuseumExhibit("item" + i)) foreach schema.museumExhibit.insert
    new MuseumExhibitService(schema.museumExhibit)
  }
  
  def isEmpty(service: => MuseumExhibitService) =
    "要素は取得できない" ! allElementsOf(service).returnsNoElement
  
  def isNotEmpty(service: => MuseumExhibitService) =
    "要素が取得できる" ! allElementsOf(service).returnsSomeElement
  
  def canCreate(service: => MuseumExhibitService) =
    "allElements で取得できる" ! createOn(service).returnsByAllElements ^
    "永続化はされない" ! createOn(service).notPersist
  
  def canRemove(service: => MuseumExhibitService) =
    "テーブルから削除される" ! removeOn(service).deleteFromTable ^
    "削除されると ture が返る" ! removeOn(service).returnsTrue ^
    "非永続化項目を削除" ! removeOn(service).deletesNonPersistedItem ^
    "非永続化項目の削除で true" ! removeOn(service).returnsTrueByNonPersistedItem ^
    "サービスの要素ではない時は false が返る" ! removeOn(service).returnsFalseByNonEntity
  
  def canSave(service: => MuseumExhibitService) =
    "id が付加" ! savingOn(service).appliedId ^
    "非永続化項目が永続化" ! savingOn(service).persists ^
    "非永続化項目ではなくなる" ! savingOn(service).removeFromNotPersists
  
  def canGetIndex(service: => MuseumExhibitService) =
    "要素の順序取得" ! indexOf(service).returnsIndex ^
    "非永続化要素の順序取得" ! indexOf(service).returnsIndexByNotPersistedItem ^
    "存在しない項目は -1" ! indexOf(service).notReturnIndex
  
  class TestBase(service: MuseumExhibitService) {
    def allElements = service.allElements
    
    def table = schema.museumExhibit
    
    def allElementsOfTable = from(table)(e => select(e)).toList
  }
  
  def allElementsOf(service: MuseumExhibitService) = new TestBase(service) {
    def returnsNoElement = allElements must beEmpty
    
    def returnsSomeElement = allElements must not beEmpty
  }
  
  def createOn(service: MuseumExhibitService) = new TestBase(service) {
    def returnsByAllElements = {
      val e1, e2, e3 = service.create
      service.allElements must contain(e1, e2, e3).inOrder
    }
    
    def notPersist = {
      val e1, e2, e3 = service.create
      from(table)(e => select(e)).toList must not contain(e1, e2, e3)
    }
  }
  
  def removeOn(service: MuseumExhibitService) = new TestBase(service) {
    def deleteFromTable = {
      val e1, e2, e3, e4 = MuseumExhibit("item")
      table.insert(List(e1, e2, e3, e4))
      List(e2, e4) foreach service.remove
      allElementsOfTable must not contain(e2, e4) and
        contain(e1, e3).inOrder
    }
    
    def returnsTrue = {
      val e = MuseumExhibit("item")
      table.insert(e)
      service.remove(e) must beTrue
    }
    
    def deletesNonPersistedItem = {
      val e1, e2, e3 = service.create
      service.remove(e2)
      allElements must contain(e1, e3).inOrder and
        not contain(e2)
    }
    
    def returnsTrueByNonPersistedItem =
      service.remove(service.create) must beTrue
    
    def returnsFalseByNonEntity =
      service.remove(MuseumExhibit("item")) must beFalse
  }
  
  def savingOn(service: MuseumExhibitService) = new TestBase(service) {
    def saveOneElement = {
      val e = service.create
      service.save(e)
      e
    }
    
    def appliedId = {
      saveOneElement.id must be_>(0L)
    }
    
    def persists = {
      val e = saveOneElement
      allElementsOfTable must contain(e)
    }
    
    def removeFromNotPersists = {
      val e = saveOneElement
      allElements.filter(e.==).size must_== 1
    }
  }
  
  def indexOf(service: MuseumExhibitService) = new TestBase(service) {
    def returnsIndex = {
      val indices = allElements.take(3) map service.indexOf
      indices must contain(0, 1, 2).inOrder
    }
    
    def returnsIndexByNotPersistedItem = {
      val e = service.create
      val index = allElements.indexOf(e)
      service.indexOf(e) must_== index
    }
    
    def notReturnIndex = {
      val e = allElements.head
      service.remove(e)
      service.indexOf(e) must_== -1
    }
  }
}

private[squeryl] object MuseumExhibitServiceSpec {
  /**
   * スキーマ
   */
  private[squeryl] class Schema extends org.squeryl.Schema {
    val museumExhibit = table[MuseumExhibit]("ListDataServiceSpec")
  }
}
