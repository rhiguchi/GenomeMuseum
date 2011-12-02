package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import java.sql.Connection

import org.squeryl.Schema
import org.squeryl.PrimitiveTypeMode._

import H2DatabaseChangeTrigger._

object H2DatabaseChangeTriggerSpec {
  class TestSchema extends Schema {
    val museumExhibit = table[MuseumExhibit]
    val userExhibitRoom = table[UserExhibitRoom]
  }
}

class H2DatabaseChangeTriggerSpec extends Specification with SquerylConnection {
  import H2DatabaseChangeTriggerSpec._
  
  def is = "H2DatabaseChangeTrigger" ^
    "トリガー作成" ^ canCreateTrigger ^ bt ^
    "テーブル行挿入通知" ^ canPublishInserting ^ bt ^
    "テーブル行更新通知" ^ canPublishUpdating ^ bt ^
    "テーブル行削除通知" ^ canPublishDeleting ^ bt ^
    end
  
  val schema = new TestSchema
  
  def canCreateTrigger =
    "SQL 発行" ! trigger.executeSql
  
  def canPublishInserting =
    "通知の受け取り" ! inserting.reaction ^
    "テーブル名取得" ! inserting.tableName ^
    "行データ取得" ! inserting.rowData
  
  def canPublishUpdating =
    "通知の受け取り" ! updating.reaction ^
    "テーブル名取得" ! updating.tableName ^
    "古いデータの取得" ! updating.oldData ^
    "新しいデータの取得" ! updating.newData
  
  def canPublishDeleting =
    "通知の受け取り" ! deleting.reaction ^
    "テーブル名取得" ! deleting.tableName ^
    "行データ取得" ! deleting.rowData
  
  def setUpTrigger() {
    val session = setUpSchema()
    H2DatabaseChangeTrigger.createTriggers(session.connection, "MuseumExhibit")
    H2DatabaseChangeTrigger.createTriggers(session.connection, "UserExhibitRoom")
  }
  
  def trigger = new Object {
    def executeSql = {
      setUpTrigger()
      success
    }
  }
  
  def inserting = new Object {
    import H2DatabaseChangeTrigger.Publisher
    
    def insertAndGetEvent() = {
      setUpTrigger()
      
      var events: List[Inserted] = Nil
      H2DatabaseChangeTrigger.Publisher.watch {
        case e: Inserted => events = e :: events
      }
      schema.museumExhibit.insert(MuseumExhibit("exhibit"))
      schema.userExhibitRoom.insert(UserExhibitRoom("room"))
      
      events
    }
    
    def reaction = insertAndGetEvent must not beEmpty
    
    def tableName = insertAndGetEvent.map(_.tableName) must
      contain("MUSEUMEXHIBIT", "USEREXHIBITROOM")
    
    def rowData = insertAndGetEvent.flatMap(e => Option(e.rowData))
      .flatMap(e => e) must not beEmpty
  }
  
  def updating = new Object {
    
    def updateAndGetEvent() = {
      setUpTrigger()
      
      var events: List[Updated] = Nil
      H2DatabaseChangeTrigger.Publisher.watch {
        case e: Updated => events = e :: events
      }
      val e1 = schema.museumExhibit.insert(MuseumExhibit("exhibit"))
      val e2 = schema.userExhibitRoom.insert(UserExhibitRoom("room"))
      schema.museumExhibit.update(e1)
      schema.userExhibitRoom.update(e2)
      
      events
    }
    
    def reaction = updateAndGetEvent must not beEmpty
    
    def tableName = updateAndGetEvent.map(_.tableName) must
      contain("MUSEUMEXHIBIT", "USEREXHIBITROOM")
    
    def oldData = updateAndGetEvent.flatMap(e => Option(e.oldData))
      .flatMap(e => e) must not beEmpty
    
    def newData = updateAndGetEvent.flatMap(e => Option(e.newData))
      .flatMap(e => e) must not beEmpty
  }
  
  def deleting = new Object {
    def deleteAndGetEvent() = {
      setUpTrigger()
      
      var events: List[Deleted] = Nil
      H2DatabaseChangeTrigger.Publisher.watch {
        case e: Deleted => events = e :: events
      }
      val e1 = schema.museumExhibit.insert(MuseumExhibit("exhibit"))
      val e2 = schema.userExhibitRoom.insert(UserExhibitRoom("room"))
      schema.museumExhibit.deleteWhere(t => t.id === e1.id)
      schema.userExhibitRoom.deleteWhere(t => t.id === e2.id)
      
      events
    }
    
    def reaction = deleteAndGetEvent must not beEmpty
    
    def tableName = deleteAndGetEvent.map(_.tableName) must
      contain("MUSEUMEXHIBIT", "USEREXHIBITROOM")
    
    def rowData = deleteAndGetEvent.flatMap(e => Option(e.rowData))
      .flatMap(e => e) must not beEmpty
  }
}
