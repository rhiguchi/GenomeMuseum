package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{MuseumExhibit => IMuseumExhibit}

class MuseumExhibitServiceSpec extends Specification with SquerylConnection {
  import MuseumExhibitServiceSpec.Schema
  
  def is = "MuseumExhibitService" ^
    "初期状態" ^ isEmpty(emptyService) ^ bt ^
    "テーブルに要素が存在" ^ isNotEmpty(serviceSomeElements) ^ bt ^
    "要素の作成" ^ canCreate(emptyService) ^ bt ^
    "要素の削除" ^ canRemove(emptyService) ^ bt ^
    "要素の永続化" ^ canSave(emptyService) ^ bt ^
    "挿入イベント" ^ canPublishInsertEvent(emptyService) ^ bt ^
    "更新イベント" ^ canPublishUpdateEvent(emptyService) ^ bt ^
    "削除イベント" ^ canPublishDeleteEvent(emptyService) ^ bt ^
    end
  
  def schema = new Schema
  
  def emptyService = {
    val s = schema
    val session = setUpSchema(s)
    SquerylTriggerAdapter.installTriggerFor(s.museumExhibit)
    new MuseumExhibitService(s.exhibitRelation, null, s.museumExhibitObserver)
  }
  
  def serviceSomeElements = {
    val service = emptyService
    0 to 2 map (i => MuseumExhibit("item" + i)) foreach
      service.exhibitTable.insert
    service
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
  
  def canPublishInsertEvent(service: => MuseumExhibitService) = sequential ^
    "イベント発行" ! insertEvent(service).published ^
    "イベントの要素が挿入された要素と同一" ! insertEvent(service).element ^
    "DB テーブルの挿入からイベント発行" ! insertEvent(service).fromDatabase ^
    "DB テーブルの要素" ! insertEvent(service).elementFromDB ^
    "DB テーブルの挿入位置" ! insertEvent(service).indexFromDB
  
  def canPublishUpdateEvent(service: => MuseumExhibitService) = sequential ^
    "イベント発行" ! updateEvent(service).published ^
    "イベントの要素が更新された要素と同一" ! updateEvent(service).element ^
    "DB テーブルの更新からイベント発行" ! updateEvent(service).fromDatabase ^
    "DB テーブルの要素" ! updateEvent(service).elementFromDB ^
    "DB テーブルの更新位置" ! updateEvent(service).indexFromDB
  
  def canPublishDeleteEvent(service: => MuseumExhibitService) = sequential ^
    "イベント発行" ! deleteEvent(service).published ^
    "イベントの要素が挿入された要素と同一" ! deleteEvent(service).element ^
    "DB テーブルの削除からイベント発行" ! deleteEvent(service).fromDatabase ^
    "DB テーブルの要素" ! deleteEvent(service).elementFromDB ^
    "DB テーブルの削除位置" ! deleteEvent(service).indexFromDB
  
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
  
  import scala.collection.script.{Message, End, Include, Update, Remove, Index}
  
  abstract class EventTestBase[E <: Message[IMuseumExhibit]](service: MuseumExhibitService)
      extends TestBase(service) {
    import scala.concurrent.ops.future
    
    @volatile
    var message: Option[E] = None
    
    def eventToId: PartialFunction[Message[IMuseumExhibit], E]
    
    service.subscribe(new service.Sub {
      def notify(pub: service.Pub, event: Message[_ <: IMuseumExhibit]) {
        if (eventToId.isDefinedAt(event)) {
          message = Some(eventToId(event))
        }
      }
    })
    
    def getMessage = message
  }
  
  def insertEvent(service: MuseumExhibitService) = new EventTestBase[Include[IMuseumExhibit]](service) {
    override def eventToId = {
      case event @ Include(_, e) => event
    }
    
    def published = {
      service.create
      message must beSome
    }
    
    def element = {
      val e = service.create
      message.map(_.elem) must beSome(e)
    }
    
    def fromDatabase = {
      table.insert(MuseumExhibit("new exhibit"))
      getMessage must beSome
    }
    
    def elementFromDB = {
      val e = MuseumExhibit("new exhibit")
      table.insert(e)
      getMessage.map(_.elem) must beSome(e)
    }
    
    def indexFromDB = {
      val e = MuseumExhibit("new exhibit")
      table.insert(e)
      getMessage.map(_.location) must beSome(Index(indexOf(e.id)))
    }
  }
  
  def updateEvent(service: MuseumExhibitService) = new EventTestBase[Update[IMuseumExhibit]](service) {
    override def eventToId = {
      case event @ Update(_, e) => event
    }
    
    def published = {
      service.save(service.create)
      message must beSome
    }
    
    def element = {
      val e = service.create
      service.save(e)
      message.map(_.elem) must beSome(e)
    }
    
    def dbTask() = {
      val e = MuseumExhibit("new exhibit")
      table.insert(e)
      table.update(e)
      e
    }
    
    def fromDatabase = {
      dbTask()
      getMessage must beSome
    }
    
    def elementFromDB = {
      val e = dbTask()
      getMessage.map(_.elem) must beSome(e)
    }
    
    def indexFromDB = {
      table.insert(MuseumExhibit("new exhibit"))
      val e = dbTask()
      table.insert(MuseumExhibit("new exhibit"))
      getMessage.map(_.location) must beSome(Index(indexOf(e.id)))
    }
  }
  
  def deleteEvent(service: MuseumExhibitService) = new EventTestBase[Remove[IMuseumExhibit]](service) {
    override def eventToId = {
      case event @ Remove(_, e) => event
    }
    
    def published = {
      service.remove(service.create)
      message must beSome
    }
    
    def element = {
      val e = service.create
      service.remove(e)
      message.map(_.elem) must beSome(e)
    }
    
    def fromDatabase = {
      val e = MuseumExhibit("new exhibit")
      table.insert(e)
      table.delete(e.id)
      getMessage must beSome
    }
    
    def elementFromDB = {
      val e = MuseumExhibit("new exhibit")
      table.insert(e)
      table.delete(e.id)
      getMessage.map(_.elem) must beSome(e)
    }
    
    def indexFromDB = {
      table.insert(MuseumExhibit("new exhibit"))
      val e = MuseumExhibit("new exhibit")
      table.insert(e)
      table.insert(MuseumExhibit("new exhibit"))
      val index = indexOf(e.id)
      table.delete(e.id)
      getMessage.map(_.location) must beSome(Index(index))
    }
  }
}

private[squeryl] object MuseumExhibitServiceSpec {
  /**
   * スキーマ
   */
  private[squeryl] class Schema extends RoomElementServiceSpec.TestMuseumSchema {
    val museumExhibitObserver = SquerylTriggerAdapter.connect(museumExhibit, 7)
  }
}
