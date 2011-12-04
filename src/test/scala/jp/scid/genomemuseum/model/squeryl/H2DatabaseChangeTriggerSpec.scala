package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import java.sql.Connection

import org.squeryl.{Schema, Table}
import org.squeryl.PrimitiveTypeMode._

import H2DatabaseChangeTrigger._

object H2DatabaseChangeTriggerSpec {
  trait TestSchema {
    this: Schema =>
    
    def museumExhibit: Table[MuseumExhibit]
    def userExhibitRoom: Table[UserExhibitRoom]
    def roomExhibit: Table[RoomExhibit]
    
    def name: Option[String]
  }
  
  class TestSchemaImpl(schemaSuffix: String = "") extends Schema with TestSchema {
    override val name = Some("H2DatabaseChangeTriggerSpec_" + schemaSuffix)
    
    val museumExhibit = table[MuseumExhibit]
    val userExhibitRoom = table[UserExhibitRoom]
    
    val roomExhibit = table[RoomExhibit]
  
    val roomToRoomExhibitRelation = oneToManyRelation(userExhibitRoom, roomExhibit)
      .via((room, content) => room.id === content.roomId)
    roomToRoomExhibitRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
    
    val exhibitToRoomExhibitRelation = oneToManyRelation(museumExhibit, roomExhibit)
      .via((exhibit, content) => exhibit.id === content.exhibitId)
    exhibitToRoomExhibitRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
  }
}

class H2DatabaseChangeTriggerSpec extends Specification with SquerylConnection {
  import H2DatabaseChangeTriggerSpec._
  
  def is = "H2DatabaseChangeTrigger" ^
    "トリガー作成" ^ canCreateTrigger ^ bt ^
    "テーブル行挿入通知" ^ canPublishInserting(triggeredSchema) ^ bt ^
    "テーブル行更新通知" ^ canPublishUpdating(triggeredSchema) ^ bt ^
    "テーブル行削除通知" ^ canPublishDeleting(triggeredSchema) ^ bt ^
    end
  
  @deprecated("dont use", "2011-12-03")
  val schema = new TestSchemaImpl
  
  def canCreateTrigger =
    "SQL 発行" ! trigger.executeSql
  
  def canPublishInserting(s: => TestSchema) =
    "通知の受け取り" ! inserting(s).reaction ^
    "テーブル名取得" ! inserting(s).tableName ^
    "行データ取得" ! inserting(s).rowData
  
  def canPublishUpdating(s: => TestSchema) =
    "通知の受け取り" ! updating(s).reaction ^
    "テーブル名取得" ! updating(s).tableName ^
    "古いデータの取得" ! updating(s).oldData ^
    "新しいデータの取得" ! updating(s).newData
  
  def canPublishDeleting(s: => TestSchema) =
    "通知の受け取り" ! deleting(s).reaction ^
    "テーブル名取得" ! deleting(s).tableName ^
    "行データ取得" ! deleting(s).rowData
  
  def triggeredSchema(): TestSchema = {
    val schema = new TestSchemaImpl(util.Random.alphanumeric.take(5).mkString)
    
    val session = setUpSchema(schema)
    createTriggers(session.connection, schema.museumExhibit.name,
      schema.name.get)
    createTriggers(session.connection, schema.userExhibitRoom.name,
      schema.name.get)
    
    schema
  }
  
  def trigger = new Object {
    def executeSql = {
      triggeredSchema()
      success
    }
  }
  
  abstract class EventTestBase(val schema: TestSchema) {
    import scala.concurrent.ops.spawn
    import scala.collection.mutable.{Buffer, ArrayBuffer, SynchronizedBuffer}

    private val operations: Buffer[Operation] =
      new ArrayBuffer[Operation] with SynchronizedBuffer[Operation]
    
    protected def accept(op: Operation): Boolean
    
    Publisher.subscribe( new Publisher.Sub {
      def notify(pub: Publisher.Pub, e: Operation) {
        if (accept(e)) spawn {
          operations += e
        }
      }
    }, schema.name.get.toUpperCase)
    
    def events(): List[Operation] = events(1)
    
    def events(count: Int) = {
      val end = System.currentTimeMillis + 1000
      while(operations.size < count && System.currentTimeMillis < end)
        Thread.sleep(10)
      operations.toList
    }
    
    def eventsOf[E <: Operation](implicit c: ClassManifest[E]): List[E] =
      events.filter(_.getClass == c.erasure).map(_.asInstanceOf[E])
  }
  
  def inserting(schema: TestSchema) = new EventTestBase(schema) {
    def accept(op: Operation) = op.isInstanceOf[Inserted]
    
    schema.museumExhibit.insert(MuseumExhibit("exhibit"))
    schema.userExhibitRoom.insert(UserExhibitRoom("room"))
    
    def reaction = events must not beEmpty
    
    def tableName = events.map(_.tableName) must
      contain("MUSEUMEXHIBIT", "USEREXHIBITROOM").only
    
    def rowData = eventsOf[Inserted].flatMap(e => Option(e.rowData))
      .flatMap(e => e) must not beEmpty
  }
  
  def updating(s: TestSchema) = new EventTestBase(s) {
    def accept(op: Operation) = op.isInstanceOf[Updated]
    
    val e1 = schema.museumExhibit.insert(MuseumExhibit("exhibit"))
    val e2 = schema.userExhibitRoom.insert(UserExhibitRoom("room"))
    schema.museumExhibit.update(e1)
    schema.userExhibitRoom.update(e2)

    def reaction = events must not beEmpty
    
    def tableName = events.map(_.tableName) must
      contain("MUSEUMEXHIBIT", "USEREXHIBITROOM").only
    
    def oldData = eventsOf[Updated].flatMap(e => Option(e.oldData))
      .flatMap(e => e) must not beEmpty
    
    def newData = eventsOf[Updated].flatMap(e => Option(e.newData))
      .flatMap(e => e) must not beEmpty
  }
  
  def deleting(s: TestSchema) = new EventTestBase(s) {
    def accept(op: Operation) = op.isInstanceOf[Deleted]
    
    val e1 = schema.museumExhibit.insert(MuseumExhibit("exhibit"))
    val e2 = schema.userExhibitRoom.insert(UserExhibitRoom("room"))
    schema.museumExhibit.delete(e1.id)
    schema.userExhibitRoom.delete(e2.id)
    
    def reaction = events must not beEmpty
    
    def tableName = events.map(_.tableName) must
      contain("MUSEUMEXHIBIT", "USEREXHIBITROOM").only
    
    def rowData = eventsOf[Deleted].flatMap(e => Option(e.rowData))
      .flatMap(e => e) must not beEmpty
  }
}
