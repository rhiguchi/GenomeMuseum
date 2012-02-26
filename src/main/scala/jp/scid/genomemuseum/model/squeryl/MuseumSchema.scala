package jp.scid.genomemuseum.model.squeryl

import org.squeryl.{Schema, Session, Table}
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{EventList, FunctionList}

import jp.scid.genomemuseum.model.{MuseumSchema => IMuseumSchema, UserExhibitRoom => IUserExhibitRoom,
  MuseumExhibit => IMuseumExhibit}
import IUserExhibitRoom.RoomType
import RoomType._

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
  
  // user_exhibit_room
  /** UserExhibitRoom のテーブルオブジェクト */
  private[squeryl] val userExhibitRoom = table[UserExhibitRoom]("user_exhibit_room")
  
  /** 親子関係 */
  private val roomTree = oneToManyRelation(userExhibitRoom, userExhibitRoom)
      .via((c, p) => c.parentId === p.id)
  roomTree.foreignKeyDeclaration.constrainReference(onDelete cascade)
  
  // museum_exhibit
  /** MuseumExhibit のテーブルオブジェクト */
  private[squeryl] val museumExhibit = table[MuseumExhibit]("museum_exhibit")
  
  // room_exhibit
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
  
  /** Squeryl で実装した『部屋』データのサービス */
  val userExhibitRoomService = new UserExhibitRoomService(
    userExhibitRoom, museumExhibit, roomExhibit)
  
  /** Squeryl で実装した『展示物』データのサービス */
  val museumExhibitService = new MuseumExhibitService(museumExhibit)
  
  /** 展示室サービス */
  val freeExhibitPavilion = new FreeExhibitPavilion(
    roomExhibit, museumExhibitService.exhibitEventList, userExhibitRoomService)

  /** 部屋のコンテンツを返す */
  @deprecated("2012/02/26", "use via museumExhibitService")
  def getExhibitRoomModel(room: IUserExhibitRoom) =
    freeExhibitPavilion.createExhibitRoomModel(room)
}

object MuseumSchema {
  import org.squeryl.{SessionFactory, Session, Schema, adapters}
  import org.h2.jdbcx.JdbcConnectionPool
  
  /** コネクション管理用ロック */
  private object connectionLock
  /** コネクションプール */
  private var connectionPool: Option[JdbcConnectionPool] = None
  /** 接続アダプタ */
  val adapter = new adapters.H2Adapter
  
  def on(string: String) = {
    makeH2SquerylConnection("jdbc:h2:" + string)
    craeteSchema()
  }
  
  /** スキーマを構築する */
  private def craeteSchema() = {
    SessionFactory.newSession.bindToCurrentThread
    val schema = new MuseumSchema
    if (!schema.exists)
      schema.create
    
    // トリガー作成
    import H2DatabaseChangeTrigger.{createTriggers, Publisher}
    val conn = Session.currentSession.connection
    createTriggers(conn, schema.userExhibitRoom.name, schema.name.get)
    createTriggers(conn, schema.museumExhibit.name, schema.name.get)
    createTriggers(conn, schema.roomExhibit.name, schema.name.get)
    
    schema
  }
  
  private def makeH2SquerylConnection(connectionString: String) {
    Class.forName("org.h2.Driver")
    
    val h2cp = JdbcConnectionPool.create(connectionString, "", "")
    SessionFactory.concreteFactory = Some( () =>
      Session.create(h2cp.getConnection, adapter)
    )
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
