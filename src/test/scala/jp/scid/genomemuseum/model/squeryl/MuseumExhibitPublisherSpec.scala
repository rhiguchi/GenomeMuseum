package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import scala.collection.mutable.{Publisher, ListBuffer}
import scala.collection.script.{Message, NoLo, Include, Update, Remove}

import org.squeryl.{Session, Table}
import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{MuseumExhibit => IMuseumExhibit}

import SquerylConnection.setUpSchema
import SquerylTriggerAdapter.TableOperation
import MuseumExhibitServiceSpec.{MuseumExhibitSchema, MuseumExhibitTesting}

private[squeryl] object MuseumExhibitPublisherSpec {
  private[squeryl] class TestSchema extends org.squeryl.Schema with MuseumExhibitSchema {
    private val schemaName = "MuseumExhibitPublisherSpec_" + util.Random.alphanumeric.take(5).mkString
    override def name = Some(schemaName)
  }
}

class MuseumExhibitPublisherSpec extends Specification {
  import MuseumExhibitPublisherSpec._
  
  private type Factory = Publisher[TableOperation[MuseumExhibit]] => MuseumExhibitPublisher
  
  def is = "MuseumExhibitPublisher" ^
    "挿入通知" ^ canPublishIncdlueMessage(simplePublisherOf) ^
    "更新通知" ^ canPublishUpdateMessage(simplePublisherOf) ^
    "削除通知" ^ canPublishRemoveMessage(simplePublisherOf) ^
    end
  
  
  def emptySchema = {
    val schema = new TestSchema
    setUpSchema(schema)
    schema
  }
  
  def simplePublisherOf(publisher: Publisher[TableOperation[MuseumExhibit]]): MuseumExhibitPublisher = {
    new MuseumExhibitPublisher {
      def museumExhibitTablePublisher = publisher
    }
  }
  
  def canPublishIncdlueMessage(f: Factory) = sequential ^
    "メッセージ発行" ! incdlueMessage(f).published ^
    "要素" ! incdlueMessage(f).element ^
    bt
  
  def canPublishUpdateMessage(f: Factory) = sequential ^
    "メッセージ発行" ! updateMessage(f).published ^
    "要素" ! updateMessage(f).element ^
    bt
  
  def canPublishRemoveMessage(f: Factory) = sequential ^
    "メッセージ発行" ! removeMessage(f).published ^
    "要素" ! removeMessage(f).element ^
    bt
  
  abstract class TestBase[M <: Message[_]: ClassManifest](f: Factory) extends MuseumExhibitTesting {
    val table = emptySchema.museumExhibit
    val conn = Session.currentSession.connection
    
    val tablePublisher = SquerylTriggerAdapter.connect(conn, table, 7)
    
    def museumExhibitTable = table
    
    val messages = ListBuffer.empty[M] 
    
    val service = f(tablePublisher)
    
    service.subscribe(new service.Sub {
      def notify(pub: service.Pub, message: Message[IMuseumExhibit]) {
        if (implicitly[ClassManifest[M]].erasure.isInstance(message))
          messages += message.asInstanceOf[M]
      }
    })
  }
  
  def incdlueMessage(f: Factory) = new TestBase[Include[_]](f) {
    val exhibit1, exhibit2, exhibit3 = insertExhibit()
      
    def published = messages must haveSize(3)
    
    def element = messages.map(_.elem) must_== List(exhibit1, exhibit2, exhibit3)
  }
  
  def updateMessage(f: Factory) = new TestBase[Update[_]](f) {
    val exhibit1, exhibit2, exhibit3 = insertExhibit()
    List(exhibit2, exhibit1, exhibit2, exhibit3) foreach table.update
      
    def published = messages must haveSize(4)
    
    def element = messages.map(_.elem) must_== List(exhibit2, exhibit1, exhibit2, exhibit3)
  }
  
  def removeMessage(f: Factory) = new TestBase[Remove[_]](f) {
    val exhibit = insertExhibit()
    table.delete(exhibit.id)
      
    def published = messages must haveSize(1)
    
    def element = messages.headOption.map(_.elem) must beSome(exhibit)
  }
}
