package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import java.io.File

import org.squeryl.SessionFactory
import org.squeryl.PrimitiveTypeMode._

import SquerylConnection._
import jp.scid.genomemuseum.model.UriFileStorage

class MuseumSchemaSpec extends Specification with mock.Mockito {
  def is = "MuseumSchema" ^
    "スキーマ" ^ schemaSpec(simpleSchema) ^
    "スキーマオブジェクト" ^ schemaObjectSpec ^
    end
  
  def simpleSchema = new MuseumSchema
  
  def schemaObjectSpec = sequential ^
    "メモリー上作成" ! schemaObject.onMemory ^
    "ローカルファイル上作成" ! schemaObject.onFile ^
    bt
  
  def schemaSpec(s: => MuseumSchema) =
    "スキーマ名が指定されている" ! param(s).hasName ^
    "データベース構築ができる" ! param(s).buildDatabase ^
    bt
  
  def param(schema: MuseumSchema) = new {
    def hasName = schema.name must beSome
    
    def buildDatabase = {
      createSession.bindToCurrentThread
      schema.create
      success
    }
  }
  
  def schemaObject = new {
    def onMemory = {
      val s = MuseumSchema.on("mem:")
      from(s.userExhibitRoom)(e => compute(count)).toInt must_== 0
    }
    
    def onFile = {
      val dbFile = File.createTempFile("MuseumSchemaSpec", "")
      val s = MuseumSchema.on("file:" + dbFile.getAbsolutePath)
      from(s.userExhibitRoom)(e => compute(count)).toInt must_== 0
    }
  }
}
