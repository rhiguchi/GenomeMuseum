package jp.scid.genomemuseum.model.squeryl

import org.squeryl.{SessionFactory, Session, Schema, adapters}
import org.h2.jdbcx.JdbcConnectionPool
import org.squeryl.PrimitiveTypeMode.transaction

/**
 * データベース接続テスト用トレイト
 */
private[squeryl] trait DatabaseConnectable {
  import DatabaseConnectable._
  Class.forName("org.h2.Driver")
  
  def schema: MuseumSchema = testSchema
  
  /** データベース構築 */
  def openDatabase = {
    DatabaseConnectable.connectionLock.synchronized {
      cpOp = SessionFactory.concreteFactory.isEmpty match {
        case true =>
          val h2cp = JdbcConnectionPool.create("jdbc:h2:mem:squerylTest", "", "")
          SessionFactory.concreteFactory = Some( () =>
            Session.create(h2cp.getConnection, new adapters.H2Adapter)
          )
          transaction {
            schema.create
          }
          Some(h2cp)
        case false => None
      }
      openCount += 1
    }
  }
  
  /** データベース解放 */
  def closeDatabase = {
    DatabaseConnectable.connectionLock.synchronized {
      openCount -= 1
    }
    if (openCount == 0) {
      transaction {
        schema.drop
      }
      cpOp.foreach(_.dispose())
    }
  }
}

private object DatabaseConnectable {
  /** コネクション管理用ロック */
  object connectionLock
  
  /** コネクション数 */
  private var openCount = 0
  
  /** データベーススキーマ */
  private val testSchema = new MuseumSchema
  
  /** コネクションプール */
  private var cpOp: Option[JdbcConnectionPool] = None
  
}


trait SquerylConnection {
  private val adapter = new adapters.H2Adapter
  
  /**
   * スキーマ
   */
  protected def schema: Schema
  
  /**
   * {@code createSession} で作成されるセッションでスキーマを構築する。
   * @return 使用したスキーマ
   */
  protected def setUpSchema() = {
    val session = createSession
    session.bindToCurrentThread
    schema.create
    session
  }
  
  /**
   * H2 データベースの匿名データベースコネクションを作成し、セッションを構築する。
   */
  protected def createSession = {
    val h2cp = JdbcConnectionPool.create("jdbc:h2:mem:", "", "")
    h2cp setMaxConnections 1
    h2cp setLoginTimeout 10
    Session.create(h2cp.getConnection, adapter)
  }
}

