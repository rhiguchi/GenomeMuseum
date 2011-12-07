package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{MuseumExhibit => IMuseumExhibit}

import SquerylConnection._

class MuseumExhibitServiceSpec extends Specification {
  import MuseumExhibitServiceSpec.Schema
  
  def is = "MuseumExhibitService" ^
    "初期状態" ^ isEmpty(emptyService) ^
    "テーブルに要素が存在" ^ isNotEmpty(serviceSomeElements) ^
    "要素の作成" ^ canCreate(emptyService) ^
    "要素の削除" ^ canRemove(emptyService) ^
    "要素の永続化" ^ canSave(emptyService) ^
    end
  
  def emptyService = {
    val s = new Schema
    val session = setUpSchema(s)
    H2DatabaseChangeTrigger.createTriggers(session.connection,
      s.museumExhibit.name, s.name.get)
    new MuseumExhibitService(s.exhibitRelation, null)
  }
  
  def serviceSomeElements = {
    val service = emptyService
    0 to 2 map (i => MuseumExhibit("item" + i)) foreach
      service.exhibitTable.insert
    service
  }
  
  def isEmpty(service: => MuseumExhibitService) =
    "要素は取得できない" ! allElementsOf(service).returnsNoElement ^
    bt
  
  def isNotEmpty(service: => MuseumExhibitService) =
    "要素が取得できる" ! allElementsOf(service).returnsSomeElement ^
    bt
  
  def canCreate(service: => MuseumExhibitService) =
    "永続化はされない" ! createOn(service).notPersist ^
    bt
  
  def canRemove(service: => MuseumExhibitService) =
    "テーブルから削除される" ! removeOn(service).deleteFromTable ^
    "削除されると ture が返る" ! removeOn(service).returnsTrue ^
    "サービスの要素ではない時は false が返る" ! removeOn(service).returnsFalseByNonEntity ^
    bt
  
  def canSave(service: => MuseumExhibitService) =
    "id が付加" ! savingOn(service).appliedId ^
    bt
  
  class TestBase(service: MuseumExhibitService) {
    def allElements = service.allElements
    
    def table = service.exhibitTable
    
    def allElementsOfTable = from(table)(e => select(e)).toList
    
    def indexOf(id: Long) = from(table)(e => where(e.id lte id) compute(count)).toInt - 1
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
  }
}

private[squeryl] object MuseumExhibitServiceSpec {
  import org.squeryl.Table
  
  /**
   * スキーマ
   */
  private[squeryl] class Schema extends RoomElementServiceSpec.TestMuseumSchema {
  }
  
  private[squeryl] trait MuseumExhibitSchema {
    this: org.squeryl.Schema =>
    
    val museumExhibit = table[MuseumExhibit]
  }
  
  private[squeryl] trait MuseumExhibitTesting {
    protected def museumExhibitTable: Table[MuseumExhibit]
    
    def insertExhibit(name: String = "exhibit") =
      museumExhibitTable.insert(MuseumExhibit(name))
  }
}
