package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import org.squeryl.{Table, Schema}

import jp.scid.genomemuseum.model.{MuseumExhibit => IMuseumExhibit}

private[squeryl] object MuseumExhibitServiceSpec {
  private[squeryl] class TestSchema extends Schema {
    val exhibitTable = table[MuseumExhibit]
    val roomTable = table[UserExhibitRoom]
  }
}

class MuseumExhibitServiceSpec extends Specification with mock.Mockito {
  private type Factory = Table[MuseumExhibit] => MuseumExhibitService
  
  def is = "ローカル展示物データモデル" ^
    "全展示物の取得" ^ exhibitListSpec(museumExhibitService) ^
    "Exhibit を作成する" ^ canCreate(museumExhibitService) ^
    "Exhibit を追加する" ^ canAdd(museumExhibitService) ^
    "Exhibit を除去する" ^ canRemove(museumExhibitService) ^
    "Exhibit を更新する" ^ canSave(museumExhibitService) ^
//    "Room が追加可能か調べる" ^ canAddChildSpec(museumExhibitService) ^
//    "Room を追加する" ^ addChildSpec(museumExhibitService) ^
    end
  
  def museumExhibitService(exhibitTable: Table[MuseumExhibit]) =
    new MuseumExhibitService(exhibitTable)
  
  def exhibitListSpec(f: Factory) =
    "最初は空" ! exhibitList(f).initial ^
    "データベースのテーブルから取得する" ! exhibitList(f).fromTable ^
    bt
  
  def canCreate(f: Factory) =
    "MuseumExhibit インスタンスを作成" ! create(f).exhibit ^
    bt
  
  def canAdd(f: Factory) =
    "データベーステーブルに挿入される" ! add(f).toTable ^
    "追加された要素が取得できる" ! add(f).andGet ^
    "複数の要素を追加できる" ! add(f).multiple ^
    "エンティティクラスでないときは追加されない" ! add(f).nonEntity ^
    "すでに追加されている時はなにも起きない" ! add(f).alreadyEntered ^
    bt
  
  def canRemove(f: Factory) =
    "データベーステーブルから除去される" ! remove(f).fromTable ^
    "除去された要素は取得できなくなる" ! remove(f).andCannotGet ^
    "除去時は true を返す" ! remove(f).returnsTrueToRemove ^
    "除去されない時は false を返す" ! remove(f).returnsFalseNotToRemove ^
    bt
  
  def canSave(f: Factory) =
    "データベーステーブルに更新が伝えられる" ! save(f).toTable ^
    bt
  
//  def canAddChildSpec(f: Factory) =
//    "部屋がなにかの子になっている時は true" ! canAddChild(f).withParentReturnsTrue ^
//    "部屋が子になっていない時は false" ! canAddChild(f).rootReturnsFalse ^
//    bt
//  
//  def addChildSpec(f: Factory) =
//    "データベーステーブルへ更新される" ! addChild(f).toTable ^
//    bt
  
  // テストメソッド
  // ベースクラス
  class TestBase(f: Factory) {
    val schema = new MuseumExhibitServiceSpec.TestSchema
    SquerylConnection.setUpSchema(schema)
    
    private def mockTable[A] = mock[Table[A]]
  
    val exhibitTable = spy(schema.exhibitTable)
    val service = f(exhibitTable)
    
    def insertToTable() = exhibitTable insert MuseumExhibit("")
  }
  
  def exhibitList(f: Factory) = new TestBase(f) {
    def initial = service.exhibitList must beEmpty
    
    def fromTable = {
      val elements = (0 to 3) map (_ => exhibitTable insert MuseumExhibit("")) toList
      
      service.exhibitList must haveTheSameElementsAs(elements)
    }
  }
  
  def create(f: Factory) = new TestBase(f) {
    def exhibit = service.create must beAnInstanceOf[service.ElementClass]
  }
  
  def add(f: Factory) = new TestBase(f) {
    val element = MuseumExhibit("")
    service.add(element: IMuseumExhibit)
    
    def toTable = {
      there was one(exhibitTable).insert(element)
    }
    
    def andGet = service.exhibitList must contain(element).only.inOrder
    
    def multiple = {
      val element2 = MuseumExhibit("")
      service.add(element2: IMuseumExhibit)
      service.exhibitList must contain(element, element2).only.inOrder
    }
    
    def nonEntity = {
      service.add(mock[IMuseumExhibit])
      service.exhibitList must haveSize(1)
    }
    
    def alreadyEntered = {
      service.add(element: IMuseumExhibit)
      (there was one(exhibitTable).insert(element)) and
      andGet
    }
  }
  
  def remove(f: Factory) = new TestBase(f) {
    import org.squeryl.PrimitiveTypeMode._
    (0 to 1) foreach (_ => insertToTable())
    val element = insertToTable()
    (0 to 1) foreach (_ => insertToTable())
    
    val result = service remove element
    
    def fromTable = there was one(exhibitTable).delete(element.id)
    
    def andCannotGet = service.exhibitList.indexOf(element) must_== -1
    
    def returnsTrueToRemove = result must beTrue
    
    def returnsFalseNotToRemove = service remove element must beFalse
  }
  
  def save(f: Factory) = new TestBase(f) {
    val element = insertToTable()
    
    service save element
    
    def toTable = there was one(exhibitTable).update(element) 
  }
// TODO MuseumExhibitContentService へ移動  
//  def canAddChild(f: Factory) = new TestBase(f) {
//    val parent = roomTable insert UserExhibitRoom()
//    val room = roomTable insert UserExhibitRoom(parentId = Some(parent.id))
//    
//    def withParentReturnsTrue = service canAddRoom room must beTrue
//    
//    def rootReturnsFalse = service canAddRoom parent must beFalse
//  }
//  
//  def addChild(f: Factory) = new TestBase(f) {
//    import org.squeryl.PrimitiveTypeMode._
//    val parent = roomTable insert UserExhibitRoom()
//    val room = roomTable insert UserExhibitRoom(parentId = Some(parent.id))
//    
//    service addRoom room
//    
//    def toTable = roomTable.lookup(room.id).get.parentId must beNone
//  }
}
