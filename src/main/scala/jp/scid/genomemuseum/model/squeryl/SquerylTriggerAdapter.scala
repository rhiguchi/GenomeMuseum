package jp.scid.genomemuseum.model.squeryl

import collection.mutable.Publisher

import org.squeryl.{Schema, Session, Table, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._

/**
 * H2 Database にトリガーを作成し、 Squeryl テーブルの操作を通知する接続ユーティリティ。
 */
object SquerylTriggerAdapter {
  import H2DatabaseChangeTrigger.{Publisher, createTriggers}
  
  /** テーブル操作のイベント基底クラス */
  sealed abstract class TableOperation[A] {
    def table: Table[A]
    def id: Long
  }
  
  /** 挿入操作イベント */
  case class Inserted[A](table: Table[A], id: Long) extends TableOperation[A]
  /** 更新操作イベント */
  case class Updated[A](table: Table[A], id: Long) extends TableOperation[A]
  /** 削除操作イベント */
  case class Deleted[A](table: Table[A], id: Long) extends TableOperation[A]
  
  /**
   * テーブルの変更を接続する。
   * @param table テーブル変化を監視する Squeryl テーブルオブジェクト
   * @param idColumnIndex 行の id 列の番号
   */
  def connect[A <: KeyedEntity[Long]](table: Table[A], idColumnIndex: Int): Publisher[TableOperation[A]] = {
    // イベント変換接続
    val connector = new Connector(table, idColumnIndex)
    Publisher.subscribe(connector, connector)
    
    connector
  }
  
  /**
   * H2 database にトリガーを設定する。
   * @param table 変更トリガーを作成する Squeryl テーブルオブジェクト。
   */
  def installTriggerFor(table: Table[_]) {
    H2DatabaseChangeTrigger.createTriggers(Session.currentSession.connection,
      table.name, table.schema.name.get)
  }
  
  /**
   * Squerl テーブルの変更を接続するクラス。
   * @param table テーブル変化を監視する Squeryl テーブルオブジェクト
   * @param idColumnIndex 行の id 列の番号
   */
  private class Connector[A](table: Table[A], idColumnIndex: Int) extends Publisher[TableOperation[A]]
      with Publisher.Sub with Publisher.Filter {
    import H2DatabaseChangeTrigger.Operation
    
    /** フィルタリング用の大文字化したスキーマ名 */
    private val schemaName = table.schema.name.getOrElse("public").toUpperCase
    /** フィルタリング用の大文字化したテーブル名 */
    private val tableName = table.name.toUpperCase
    
    def notify(pub: Publisher.Pub, evt: Operation) {
      // イベントの変換
      val tableOperation = evt match {
        case H2DatabaseChangeTrigger.Inserted(_, _, data) =>
          Inserted(table, getIdValue(data))
        case H2DatabaseChangeTrigger.Updated(_, _, _, data) =>
          Updated(table, getIdValue(data))
        case H2DatabaseChangeTrigger.Deleted(_, _, data) =>
          Deleted(table, getIdValue(data))
      }
      
      // 発行
      publish(tableOperation)
    }
    
    // このテーブルのイベントのみでフィルタリング
    def apply(evt: Operation) =
      evt.schemaName == schemaName &&
      evt.tableName == tableName
    
    private def getIdValue(data: Array[AnyRef]) = data(idColumnIndex) match {
      case id: java.lang.Long => id
      case _ => throw new IllegalStateException(
        "column index '%d' may not be index of id. Data: %d"
          .format(idColumnIndex, data.toList))
    }
  }
}
