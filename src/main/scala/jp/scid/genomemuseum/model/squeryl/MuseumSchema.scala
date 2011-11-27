package jp.scid.genomemuseum.model.squeryl

import jp.scid.genomemuseum.model.{MuseumSchema => IMuseumSchema,
  UserExhibitRoom => IUserExhibitRoom}

import org.squeryl.{Schema, Session}
import org.squeryl.PrimitiveTypeMode._

/**
 * GenomeMuseum データソースの Squeryl 実装
 */
class MuseumSchema extends Schema with IMuseumSchema {
  // 文字列格納長
  override def defaultLengthOfString = Integer.MAX_VALUE
  
  /** スキーマ内のテーブル名を取得する SQL */
  private def tableNameSelectSql(schemaName: String) =
    """select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA = '%s'"""
      .format(schemaName.toUpperCase)
  
  /** スキーマを作成する SQL */
  private def schemaCreationSql(schemaName: String) =
    """create schema if not exists %s""".format(schemaName)
  
  /** スキーマ名 */
  override val name = Some("genomemuseum_09a2")
  
  private[squeryl] def exists = {
    val conn = Session.currentSession.connection
    val rs = conn.createStatement.executeQuery(tableNameSelectSql(name.get))
    rs.next
  }
  
  override def create = {
    val conn = Session.currentSession.connection
    conn.createStatement.execute(schemaCreationSql(name.get))
    super.create
  }
  
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
  
  /** 永続化されていない展示物 */
  val nonPersistedExhibits = new SortedSetMuseumExhibitService
  
  /** Squeryl で実装した『展示物』データのサービス */
  val museumExhibitService = new MuseumExhibitService(museumExhibit, nonPersistedExhibits)
  
  /** 部屋の展示物の関連づけを保持するテーブル */
  private[squeryl] val roomExhibit = table[RoomExhibit]("room_exhibit")
  
  /** 部屋の中身と部屋の関連 */
  private[squeryl] val roomToRoomExhibitRelation = oneToManyRelation(userExhibitRoom, roomExhibit)
    .via((room, content) => room.id === content.roomId)
  roomToRoomExhibitRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
    
  /** 部屋の中身と展示物の関連 */
  private val exhibitToRoomExhibitRelation = oneToManyRelation(museumExhibit, roomExhibit)
    .via((exhibit, content) => exhibit.id === content.exhibitId)
  exhibitToRoomExhibitRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
    
  /** Squeryl で実装した部屋の展示物データのサービス */
  def roomExhibitService(room: IUserExhibitRoom) =
    new RoomExhibitService(room.asInstanceOf[UserExhibitRoom], exhibitToRoomExhibitRelation, nonPersistedExhibits)
}

object MuseumSchema {
  import org.squeryl.{SessionFactory, Session, Schema, adapters}
  import org.h2.jdbcx.JdbcConnectionPool
  
  /** コネクション管理用ロック */
  private object connectionLock
  /** コネクションプール */
  private var connectionPool: Option[JdbcConnectionPool] = None
  
  def onMemory(name: String) = {
    makeH2SquerylConnection("jdbc:h2:mem:" + name)
    craeteSchema()
  }
  
  def onFile(file: java.io.File) = {
    makeH2SquerylConnection("jdbc:h2:" + file.getPath)
    craeteSchema()
  }
  
  /** スキーマを構築する */
  private def craeteSchema() = {
    val schema = new MuseumSchema
    if (!schema.exists) transaction {
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
      SessionFactory.newSession.bindToCurrentThread
      Some(h2cp)
    }
  }
  
  def closeConnection {
    connectionLock.synchronized {
      Session.currentSessionOption.foreach(_.close)
      SessionFactory.concreteFactory = None
      connectionPool.foreach(_.dispose())
      connectionPool = None
    }
  }
}
