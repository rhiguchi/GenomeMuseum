package jp.scid.genomemuseum.model.squeryl

import org.h2.api.Trigger

import java.sql.Connection

private[squeryl] object H2DatabaseChangeTrigger {
  object Publisher extends scala.collection.mutable.Publisher[Operation] {
    /** アクセス権の上書きのため */
    override protected[H2DatabaseChangeTrigger] def publish(evt: Operation) = super.publish(evt)
    
    /** スキーマ名を限定して変化監視 */
    def subscribe(sub: Sub, schema: String) {
      val schemaName = schema.toUpperCase
      val filter = (event: Operation) => event.schemaName == schemaName
      
      subscribe(sub, filter)
    }
  }
  
  /**
   * テーブル操作イベントの基底クラス。
   */
  sealed abstract class Operation {
    /** スキーマ名。全て大文字で構成される。 */
    def schemaName: String
    /** テーブル名。全て大文字で構成される。 */
    def tableName: String
  }
  
  /** 挿入操作イベント */
  case class Inserted(schemaName: String, tableName: String,
    rowData: Array[AnyRef]) extends Operation
  /** 更新操作イベント */
  case class Updated(schemaName: String, tableName: String,
    oldData: Array[AnyRef], newData: Array[AnyRef]) extends Operation
  /** 削除操作イベント */
  case class Deleted(schemaName: String, tableName: String,
    rowData: Array[AnyRef]) extends Operation
  
  /** トリガーを作成する SQL を構築。 */
  private def createTriggerSql(operation: String, tableName: String, schema: String) = {
    val triggerName = schema + "_" + tableName + "_" + operation + "_reaction" 
    
    """create trigger if not exists %s after %s on %s.%s for each row call "%s""""
      .format(triggerName, operation, schema, tableName,
        classOf[H2DatabaseChangeTrigger].getName)
  }
  
  /**
   * トリガーを作成する。
   * メモリの匿名領域にデータベースが存在する時、ことなるコネクション上のデータベース（スキーマ）でも
   * トリガーは匿名領域上から共通して発行されるため、注意する。
   */
  def createTriggers(conn: Connection, tableName: String, schema: String = "public") {
    List("insert", "update", "delete") map (t => createTriggerSql(t, tableName, schema)) foreach
      conn.createStatement.execute
  }
}

private[squeryl] class H2DatabaseChangeTrigger extends Trigger {
  import H2DatabaseChangeTrigger._
  
  private var schemaName = ""
  private var tableName = ""
  private var before: Boolean = _
  private var factory: (Array[AnyRef], Array[AnyRef]) => Operation = _
  
  def init(conn: Connection, schemaName: String, triggerName: String,
      tableName: String, before: Boolean, operation: Int) {
    this.schemaName = schemaName.toUpperCase
    this.tableName = tableName.toUpperCase
    this.before = before
    
    factory = operation match {
      case Trigger.INSERT => (oldRow: Array[AnyRef], newRow: Array[AnyRef]) => {
        Inserted(schemaName, tableName, newRow)
      }
      case Trigger.UPDATE => (oldRow: Array[AnyRef], newRow: Array[AnyRef]) => {
        Updated(schemaName, tableName, oldRow, newRow)
      }
      case Trigger.DELETE => (oldRow: Array[AnyRef], newRow: Array[AnyRef]) => {
        Deleted(schemaName, tableName, oldRow)
      }
    }
  }
  
  def fire(conn: Connection, oldRow: Array[AnyRef], newRow: Array[AnyRef]) {
    val evt = factory(oldRow, newRow)
    Publisher.publish(evt)
  }
  
  def close {}
  def remove {}
}
