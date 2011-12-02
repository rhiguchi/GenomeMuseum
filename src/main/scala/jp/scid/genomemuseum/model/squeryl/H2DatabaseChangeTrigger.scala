package jp.scid.genomemuseum.model.squeryl

import org.h2.api.Trigger

import java.sql.Connection

private[squeryl] object H2DatabaseChangeTrigger {
  object Publisher extends scala.collection.mutable.Publisher[Operation] {
    override protected[squeryl] def publish(evt: Operation) = super.publish(evt)
    
    def watch(sub: PartialFunction[Operation, Unit]): Sub = {
      val subscriber = new Sub {
        def notify(pub: Pub, event: Operation) = sub(event)
      }
      val filter = { event => sub.isDefinedAt(event)}
      
      subscribe(subscriber, filter)
      subscriber
    }
  }
  
  sealed abstract class Operation
  
  case class Inserted(tableName: String, rowData: Array[AnyRef]) extends Operation
  case class Updated(tableName: String, oldData: Array[AnyRef],
    newData: Array[AnyRef]) extends Operation
  case class Deleted(tableName: String, rowData: Array[AnyRef]) extends Operation
  
    
  private def createTriggerSql(tableName: String, operation: String) = {
    val triggerName = tableName + "_" + operation + "_reaction" 
    
    """create trigger %s after %s on %s for each row nowait call "%s""""
      .format(triggerName, operation, tableName, classOf[H2DatabaseChangeTrigger].getName)
  }
  
  def createTriggers(conn: Connection, tableName: String) {
    List("insert", "update", "delete") map (t => createTriggerSql(tableName, t)) foreach
      conn.createStatement.execute
  }
}

private[squeryl] class H2DatabaseChangeTrigger extends Trigger {
  import H2DatabaseChangeTrigger._
  
  private var tableName = ""
  private var before: Boolean = _
  private var factory: (Array[AnyRef], Array[AnyRef]) => Operation = _
  
  def init(conn: Connection, schemaName: String, triggerName: String,
      tableName: String, before: Boolean, operation: Int) {
    this.tableName = tableName
    this.before = before
    factory = operation match {
      case Trigger.INSERT => (oldRow: Array[AnyRef], newRow: Array[AnyRef]) => {
        Inserted(tableName, newRow)
      }
      case Trigger.UPDATE => (oldRow: Array[AnyRef], newRow: Array[AnyRef]) => {
        Updated(tableName, oldRow, newRow)
      }
      case Trigger.DELETE => (oldRow: Array[AnyRef], newRow: Array[AnyRef]) => {
        Deleted(tableName, oldRow)
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