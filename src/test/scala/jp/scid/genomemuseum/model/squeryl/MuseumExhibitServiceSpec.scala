package jp.scid.genomemuseum.model.squeryl

import org.specs2._
import specification.Step

import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{MuseumExhibit => IMuseumExhibit}

class MuseumExhibitServiceSpec extends Specification with DatabaseConnectable {
  def is = "MuseumExhibitService" ^ Step(openDatabase) ^
    "create" ^ canCreate ^ bt ^
    "allElements" ^ canGetAllElements ^ bt ^
    "remove" ^ canRemove ^ bt ^
    "save" ^ canSave(saveTask) ^ bt ^
    "indexOf" ^ canFindIndex ^ bt ^
    Step(closeDatabase) ^
    end
  
  val table = schema.museumExhibit
  
  val service = new MuseumExhibitService(table)
  
  def insertElement = inTransaction {
    table.insert(MuseumExhibit(""))
  }
  
  def saveTask = {
    val e = insertElement
    e.name = "test"
    e.sequenceLength = 12345
    e.accession = "test"
    service.save(e)
    e
  }
  
  class TestBase {
    def lookup(id: Long) = inTransaction {
      table.lookup(id)
    }
    
    def allElements = service.allElements
  }
  
  def canCreate =
    "ID が付加" ! createSpec.hasId ^
    "テーブルに項目が追加" ! createSpec.tableInserted
  
  def canGetAllElements =
    "テーブル内の要素を取得" ! allElementsSpec.containsInserted
  
  def canRemove =
    "テーブルから削除される" ! removeSpec.deleteFromTable ^
    "削除される時は true が返る" ! removeSpec.returnsTrue ^
    "テーブルに存在しない要素は false が返る" ! removeSpec.nonEntityReturnsFalse
  
  def canSave(e: => MuseumExhibit) =
    "名前の変更" ! entitySpec(e).nameParsistence ^
    "塩基長の変更" ! entitySpec(e).sequenceLengthParsistence ^
    "アクセッション番号の変更" ! entitySpec(e).accessionParsistence
  
  def canFindIndex =
    "存在する要素はその番号を返す" ! indefOfSpec.returnsIndex ^
    "存在しない要素は -1 返す" ! indefOfSpec.returnsMinus
  
  def createSpec = new TestBase {
    private def createElement = service.create
    
    def hasId = createElement.id must be_>(0L)
    
    def tableInserted = {
      val e = createElement
      lookup(e.id) must beSome(e)
    }
  }
  
  def allElementsSpec = new TestBase {
    def containsInserted = {
      val e1, e2, e3 = insertElement
      service.allElements must contain(e1, e2, e3).inOrder
    }
  }
  
  def removeSpec = new TestBase {
    def deleteFromTable = {
      val e = insertElement
      remove(e)
      lookup(e.id) must beNone
    }
    
    def returnsTrue =
      remove(insertElement) must beTrue
    
    def nonEntityReturnsFalse = {
      remove(MuseumExhibit("")) must beFalse
    }
    
    private def remove(e: MuseumExhibit) = service.remove(e)
  }
  
  def entitySpec(entity: MuseumExhibit) = new TestBase {
    def nameParsistence = {
      val value = inTransaction {
        from(table)(e => where(e.id === entity.id) select(e.name)).headOption
      }
      value must beSome(entity.name)
    }
    
    def sequenceLengthParsistence = {
      val value = inTransaction {
        from(table)(e => where(e.id === entity.id) select(e.sequenceLength)).headOption
      }
      value must beSome(entity.sequenceLength)
    }
    
    def accessionParsistence = {
      val value = inTransaction {
        from(table)(e => where(e.id === entity.id) select(e.accession)).headOption
      }
      value must beSome(entity.accession)
    }
  }
  
  def indefOfSpec = new TestBase {
    def returnsIndex = {
      val e = insertElement
      val index = allElements.indexOf(e)
      service.indexOf(e) must_== index
    }
    
    def returnsMinus =
      service.indexOf(MuseumExhibit("")) must_== -1
  }
}
