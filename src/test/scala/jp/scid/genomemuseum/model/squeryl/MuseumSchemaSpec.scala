package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import java.io.File

import org.squeryl.SessionFactory
import org.squeryl.PrimitiveTypeMode._

import SquerylConnection._

class MuseumSchemaSpec extends Specification  {
  def is = "MuseumSchema" ^
    "スキーマ" ^ schemaSpec(simpleSchema) ^ bt ^ 
    "スキーマオブジェクト" ^ schemaObjectSpec ^ bt ^ 
    end
  
  def simpleSchema = new MuseumSchema
  
  def schemaObjectSpec = sequential ^
    "onMemory 作成" ! schemaObject.onMemory ^
    "onFile 作成" ! schemaObject.onFile
  
  def schemaSpec(s: => MuseumSchema) =
    "スキーマ名が指定されている" ! param(s).hasName ^
    "データベース構築ができる" ! param(s).buildDatabase
  
  def param(schema: MuseumSchema) = new Object {
    def hasName = schema.name must beSome
    
    def buildDatabase = {
      createSession.bindToCurrentThread
      schema.create
      success
    }
  }
  
  def schemaObject = new Object {
    def onMemory = {
      val s = MuseumSchema.onMemory("")
      from(s.userExhibitRoom)(e => compute(count)).toInt must_== 0
    }
    
    def onFile = {
      val dbFile = File.createTempFile("MuseumSchemaSpec", "")
      val s = MuseumSchema.onFile(dbFile)
      from(s.userExhibitRoom)(e => compute(count)).toInt must_== 0
    }
  }
}
