package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import scala.collection.mutable.{Publisher, ListBuffer}
import scala.collection.script.{Message, NoLo, Include, Update, Remove}

import org.squeryl.{Session, Table}
import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom}
import IUserExhibitRoom.RoomType._
import UserExhibitRoomServiceSpec.{TestSchema, UserExhibitRoomTesting}
import SquerylConnection._
import SquerylTriggerAdapter._

class UserExhibitRoomPublisherSpec extends Specification {
  private type Factory = Table[UserExhibitRoom] => UserExhibitRoomPublisher
  
  def is = "UserExhibitRoomPublisher" ^
    "挿入通知" ^ canPublishIncdlueMessage(simpleServiceOf) ^
    "更新通知" ^ canPublishUpdateMessage(simpleServiceOf) ^
    "削除通知" ^ canPublishRemoveMessage(simpleServiceOf) ^
    end
  
  def emptySchema = {
    val schema = new TestSchema
    setUpSchema(schema)
    schema
  }
  
  def simpleServiceOf(table: Table[UserExhibitRoom]): UserExhibitRoomPublisher = {
    val conn = Session.currentSession.connection
    UserExhibitRoomPublisher(conn, table)
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
  
  abstract class TestBase[M <: Message[_]: ClassManifest](f: Factory) extends UserExhibitRoomTesting {
    val table = emptySchema.userExhibitRoom
    
    def userExhibitRoomTable = table
    
    val messages = ListBuffer.empty[M] 
    
    val service = f(table)
    service.subscribe(new service.Sub {
      def notify(pub: service.Pub, message: Message[IUserExhibitRoom]) {
        if (implicitly[ClassManifest[M]].erasure.isInstance(message))
          messages += message.asInstanceOf[M]
      }
    })
  }
  
  def incdlueMessage(f: Factory) = new TestBase[Include[_]](f) {
    val room1, room2, room3 = insertRoom(BasicRoom)
      
    def published = messages must haveSize(3)
    
    def element = messages.map(_.elem) must_== List(room1, room2, room3)
  }
  
  def updateMessage(f: Factory) = new TestBase[Update[_]](f) {
    val room1, room2, room3 = insertRoom(BasicRoom)
    List(room2, room1, room2, room3) foreach table.update
      
    def published = messages must haveSize(4)
    
    def element = messages.map(_.elem) must_== List(room2, room1, room2, room3)
  }
  
  def removeMessage(f: Factory) = new TestBase[Remove[_]](f) {
    val room = insertRoom(BasicRoom)
    table.delete(room.id)
      
    def published = messages must haveSize(1)
    
    def element = messages.headOption.map(_.elem) must beSome(room)
  }
}
