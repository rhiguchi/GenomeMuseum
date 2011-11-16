package jp.scid.genomemuseum.model.squeryl

import jp.scid.genomemuseum.model.{MuseumSchema => IMuseumSchema,
  UserExhibitRoom => IUserExhibitRoom}

import org.squeryl.Schema
import org.squeryl.PrimitiveTypeMode._

/**
 * GenomeMuseum データソースの Squeryl 実装
 */
class MuseumSchema extends Schema with IMuseumSchema {
  /** UserExhibitRoom のテーブルオブジェクト */
  private[squeryl] val userExhibitRoom = table[UserExhibitRoom]("user_exhibit_room")
  
  /** 親子関係 */
  private val roomTree = oneToManyRelation(userExhibitRoom, userExhibitRoom)
      .via((c, p) => c.parentId === p.id)
  roomTree.foreignKeyDeclaration.constrainReference(onDelete cascade)
  
  /** Squeryl で実装した『部屋』データのサービス */
  val userExhibitRoomService = new UserExhibitRoomService(userExhibitRoom)
  
  /** MuseumExhibit のテーブルオブジェクト */
  private[squeryl] val museumExhibit = table[MuseumExhibit]("museum_exhibit")
  
  /** Squeryl で実装した『展示物』データのサービス */
  val museumExhibitService = new MuseumExhibitService(museumExhibit)
  
  /** 部屋の展示物の関連づけを保持するテーブル */
  private[squeryl] val roomExhibit = table[RoomExhibit]
  
  /** 部屋の中身と部屋の関連 */
  private val roomToRoomExhibitRelation = oneToManyRelation(userExhibitRoom, roomExhibit)
    .via((room, content) => room.id === content.roomId)
  roomToRoomExhibitRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
    
  /** 部屋の中身と展示物の関連 */
  private val exhibitToRoomExhibitRelation = oneToManyRelation(museumExhibit, roomExhibit)
    .via((exhibit, content) => exhibit.id === content.exhibitId)
  exhibitToRoomExhibitRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
    
  /** Squeryl で実装した部屋の展示物データのサービス */
  def roomExhibitService(room: IUserExhibitRoom) =
    new RoomExhibitService(roomExhibit, userExhibitRoom, museumExhibit, room)
}

object MuseumSchema {
  import org.squeryl.{SessionFactory, Session, Schema, adapters}
  import org.h2.jdbcx.JdbcConnectionPool
  
  /** コネクション管理用ロック */
  private object connectionLock
  /** コネクションプール */
  private var connectionPool: Option[JdbcConnectionPool] = None
  
  def makeMemoryConnection(name: String) = {
    makeH2SquerylConnection("jdbc:h2:mem:" + name)
    
    // スキーマ構築
    val schema = new MuseumSchema
    transaction {
      schema.create
    }
    schema
  }
  
  def makeFileConnection(file: java.io.File) = {
    makeH2SquerylConnection("jdbc:h2:" + file.getPath)
    // スキーマ構築
    // TODO ファイルが既に存在している時はスキーマを構築しない
    val schema = new MuseumSchema
    transaction {
      schema.create
    }
    schema
  }
  
  private def makeH2SquerylConnection(connectionString: String) {
    connectionPool = connectionLock.synchronized {
      Class.forName("org.h2.Driver")
      
      // Squeryl セッションが他によって作られている時はデータベースを作成しない
      if (SessionFactory.concreteFactory.nonEmpty)
        throw new IllegalStateException("SessionFactory is already used by others")
      
      val h2cp = JdbcConnectionPool.create(connectionString, "", "")
      SessionFactory.concreteFactory = Some( () =>
        Session.create(h2cp.getConnection, new adapters.H2Adapter)
      )
      Some(h2cp)
    }
  }
  
  def closeConnection {
    connectionPool.foreach(_.dispose())
    connectionPool = None
  }
}
