package jp.scid.genomemuseum.model.squeryl

import org.squeryl.{SessionFactory, Session, Schema, adapters}
import org.h2.jdbcx.JdbcConnectionPool
import org.squeryl.PrimitiveTypeMode.transaction

/**
 * データベース接続テスト用トレイト
 */


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

object SquerylConnection {
  private val adapter = new adapters.H2Adapter
  
  def setUpSchema(schema: Schema): Session = {
    val session = createSession
    session.bindToCurrentThread
    schema.name.foreach { schemaName =>
      val conn = Session.currentSession.connection
      val sql = """create schema %s""".format(schemaName)
      conn.createStatement.execute(sql)
    }
    schema.create
    session
  }
  
  /**
   * H2 データベースの匿名データベースコネクションを作成し、セッションを構築する。
   */
  def createSession = {
    val h2cp = JdbcConnectionPool.create("jdbc:h2:mem:", "", "")
    h2cp setMaxConnections 1
    h2cp setLoginTimeout 10
    Session.create(h2cp.getConnection, adapter)
  }
}

trait SquerylConnection {
  /**
   * スキーマ
   */
  @deprecated("dont use", "2011/12/04")
  protected def schema: Schema
  
  /**
   * {@code createSession} で作成されるセッションでスキーマを構築する。
   * @return 使用したスキーマ
   */
  @deprecated("use setUpSchema(Schema)", "2011/12/04")
  protected def setUpSchema(): Session =
    setUpSchema(schema)
  
  protected def setUpSchema(schema: Schema) =
    SquerylConnection.setUpSchema(schema)
  
  /**
   * H2 データベースの匿名データベースコネクションを作成し、セッションを構築する。
   */
  protected def createSession = SquerylConnection.createSession
}

