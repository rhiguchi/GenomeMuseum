package jp.scid.genomemuseum.model.squeryl

import collection.mutable.Publisher
import collection.script.{Message, Include, Update, Remove}

import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom}
import SquerylTriggerAdapter._

private[squeryl] object UserExhibitRoomPublisher {
  import org.squeryl.Table
  import java.sql.Connection
  
  private[squeryl] def apply(conn: Connection, table: Table[UserExhibitRoom]) = new UserExhibitRoomPublisher {
    def userExhibitRoomTablePublisher = SquerylTriggerAdapter.connect(conn, table, 2)
  }
}

/**
 * 部屋テーブルの変換通知を接続するトレイト
 */
private[squeryl] trait UserExhibitRoomPublisher extends Publisher[Message[IUserExhibitRoom]] {
  protected def userExhibitRoomTablePublisher: Publisher[TableOperation[UserExhibitRoom]]
  
  private val tablePublisher = userExhibitRoomTablePublisher
  
  private val subscriber = new tablePublisher.Sub {
    def notify(pub: tablePublisher.Pub, message: TableOperation[UserExhibitRoom]) {
      val newMessage = message match {
        case Inserted(table, id) => new Include(table.lookup(id).get)
        case Updated(table, id) => new Update(table.lookup(id).get)
        case Deleted(table, id) =>
          val e = UserExhibitRoom()
          e.id = id
          new Remove(e)
      }
      publish(newMessage)
    }
  }
  
  tablePublisher subscribe subscriber
}
