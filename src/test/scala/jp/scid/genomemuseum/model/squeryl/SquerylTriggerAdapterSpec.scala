package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import scala.collection.mutable.{Buffer, ArrayBuffer, SynchronizedBuffer,
  Publisher}

import org.squeryl.{Table, KeyedEntity, Session}

import SquerylTriggerAdapter._
import SquerylConnection._

private[squeryl] object SquerylTriggerAdapterSpec {
  private[squeryl] class Schema(schemaName: String = "") extends org.squeryl.Schema {
    override val name = Some("SquerylTriggerAdapterSpec" +  schemaName)
    
    val entityTable = table[TestEntity]
  }
  
  private[squeryl] case class TestEntity(
    var name: String,
    var age: Int
  ) extends KeyedEntity[Long] {
    var id: Long = 0
  }
}

class SquerylTriggerAdapterSpec extends Specification {
  import SquerylTriggerAdapterSpec._
  
  private type OpePub = Publisher[TableOperation[TestEntity]]
  
  def is = "SquerylTriggerAdapter" ^
    "挿入操作" ^ canPublishInsertedEvent(basicTable) ^ bt ^
    "更新操作" ^ canPublishUpdatedEvent(basicTable) ^ bt ^
    "削除操作" ^ canPublishDeletedEvent(basicTable) ^ bt ^
    end
  
  def basicTable = {
    val s = new Schema(util.Random.alphanumeric.take(10).mkString)
    setUpSchema(s)
    def table = s.entityTable
    
    s.entityTable
  }
  
  def canPublishInsertedEvent(table: => Table[TestEntity]) =
    "イベントを発行" ! insertedEvent(table).publish
  
  def canPublishUpdatedEvent(table: => Table[TestEntity]) =
    "イベントを発行" ! updatedEvent(table).publish
  
  def canPublishDeletedEvent(table: => Table[TestEntity]) =
    "イベントを発行" ! deletedEvent(table).publish
  
  abstract class EventTestBase(table: Table[TestEntity]) {
    val eventIds = new ArrayBuffer[Long] with SynchronizedBuffer[Long]
    
    val pub = SquerylTriggerAdapter.connect(Session.currentSession.connection,
      table, 2)
    
    def eventToId: PartialFunction[TableOperation[TestEntity], Long]
    
    pub.subscribe(new pub.Sub {
      def notify(p: pub.Pub, event: TableOperation[TestEntity]) {
        if (eventToId.isDefinedAt(event))
          eventIds += eventToId(event)
      }
    })
    
    def notPublish = eventIds must beEmpty
    
    def getEventIds(count: Int) = {
      val end = System.currentTimeMillis + 1000
      while(eventIds.size < count && System.currentTimeMillis < end)
        Thread.sleep(10)
      eventIds
    }
  }
  
  def insertedEvent(table: Table[TestEntity]) = new EventTestBase(table) {
    override def eventToId = {
      case Inserted(`table`, id) => id
    }
    
    table.insert(TestEntity("name", 100))
    table.insert(TestEntity("name2", 50))
    table.insert(TestEntity("name3", 10))
    
    def publish = getEventIds(3) must contain(1L, 2L, 3L).only.inOrder
  }
  
  def updatedEvent(table: Table[TestEntity]) = new EventTestBase(table) {
    override def eventToId = {
      case Updated(`table`, id) => id
    }
    
    val entities = List(TestEntity("name", 100),
      TestEntity("name2", 50), TestEntity("name3", 10))
    entities foreach table.insert
    entities foreach table.update
    
    def publish = getEventIds(3) must contain(1L, 2L, 3L).only.inOrder
  }
  
  def deletedEvent(table: Table[TestEntity]) = new EventTestBase(table) {
    import org.squeryl.PrimitiveTypeMode._
    
    override def eventToId = {
      case Deleted(`table`, id) => id
    }
    
    val entities = List(TestEntity("name", 100),
      TestEntity("name2", 50), TestEntity("name3", 10))
    entities foreach table.insert
    entities map (_.id) foreach (id => table.delete(id))
    
    def publish = getEventIds(3) must contain(1L, 2L, 3L).only.inOrder
  }
}
