package jp.scid.genomemuseum.model.squeryl

import org.specs2._
import specification.Step

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{Table, KeyedEntity}

class ListDataServiceSpec extends Specification with SquerylConnection {
  import ListDataServiceSpec.{Schema, SampleEntity}
  
  def is = "ListDataService" ^
    "空のテーブル" ^ isEmpty(emptyService) ^ bt ^
    "項目が追加されたテーブル" ^ isNotEmpty(serviceWithSomeItems) ^ bt ^
    "項目の削除" ^ canRemove(serviceWithSomeItems) ^ bt ^
    "項目の保存" ^ canSave(emptyService) ^ bt ^
    "順序の取得" ^ canGetIndex(emptyService) ^ bt ^
    end
  
  def isEmpty(service: => SampleDataService) =
    "取得できる要素は無い" ! allElementsOf(service).returnsNoElement
  
  def isNotEmpty(service: => SampleDataService) =
    "項目が取得できる" ! allElementsOf(service).returnsSomeElement
  
  def canRemove(service: => SampleDataService) =
    "存在する項目の削除" ! removingOn(service).removesItem ^
    "削除されると true" ! removingOn(service).returnsTrue ^
    "テーブルに無い項目を指定で false" ! removingOn(service).returnsFalseByNonEntity
  
  def canSave(service: => SampleDataService) =
    "テーブルに追加される" ! savingOn(service).insertsToTable ^
    "既に永続化された項目は、保存が適用される" ! savingOn(service).updatesTable
  
  def canGetIndex(service: => SampleDataService) =
    "要素の順序を取得できる" ! indexOf(service).returnsIndex ^
    "存在しない要素は -1" ! indexOf(service).returnsNonIndexByNonEntity
  
  class TestBase {
    val other = SampleEntity("other")
  }
  
  def allElementsOf(service: SampleDataService) = new TestBase {
    def returnsNoElement = service.allElements must beEmpty
    
    def returnsSomeElement = service.allElements must not beEmpty
  }
  
  def removingOn(service: SampleDataService) = new TestBase {
    def removesItem = {
      val (head :: tail) = service.allElements
      service.remove(head)
      service.allElements must haveTheSameElementsAs(tail)
    }
    
    def returnsTrue = {
      val (head :: tail) = service.allElements
      service.remove(head) must beTrue
    }
    
    def returnsFalseByNonEntity =
      service.remove(other) must beFalse
  }
  
  def savingOn(service: SampleDataService) = new TestBase {
    def insertsToTable = {
      service.save(other)
      service.allElements must contain(other).only
    }
    
    def updatesTable = {
      service.save(other)
      other.age = 10
      service.save(other)
      from(service.service)(e => where(e.id === other.id) select(e.age))
        .headOption must beSome(10)
    }
  }
  
  def indexOf(service: SampleDataService) = new TestBase {
    def returnsIndex = {
      val items = List(SampleEntity("item1"), SampleEntity("item2"),
        SampleEntity("item3"))
      items foreach service.save
      
      items map service.indexOf must contain(0, 1, 2).only.inOrder
    }
    
    def returnsNonIndexByNonEntity = {
      service.indexOf(SampleEntity("item1")) must_== -1
    }
  }
  
  protected val schema = new Schema
  
  def emptyService = {
    setUpSchema()
    new SampleDataService(schema.sampleTable)
  }
  
  def serviceWithSomeItems = {
    new ServiceWithSomeItems().servcie
  }
  
  /**
   * データサービスセット
   */
  class ServiceWithSomeItems {
    val e1 = SampleEntity("1")
    val e2 = SampleEntity("2")
    val e3 = SampleEntity("3")
    
    setUpSchema()
    List(e1, e2, e3) foreach schema.sampleTable.insert
    val servcie = new SampleDataService(schema.sampleTable)
  }
  
  private[squeryl] class SampleDataService(val service: Table[SampleEntity])
      extends ListDataService[SampleEntity](service)
}

private[squeryl] object ListDataServiceSpec {
  /**
   * スキーマ
   */
  private[squeryl] class Schema extends org.squeryl.Schema {
    val sampleTable = table[SampleEntity]("ListDataServiceSpec")
  }
  
  /**
   * エンティティモデル
   */
  private[squeryl] case class SampleEntity(
    var name: String,
    var age: Int = 0
  ) extends KeyedEntity[Long] {
    def this() = this("")
    var id: Long = 0
  }
}
