package jp.scid.genomemuseum.model.squeryl

import org.specs2._
import specification.Step

import java.io.File

import org.squeryl.SessionFactory
import org.squeryl.PrimitiveTypeMode._


class MuseumSchemaSpec extends Specification with SquerylConnection {
  def is = "MuseumSchema" ^ sequential ^ Step(closeDatabase) ^
    "スキーマ未作成" ^ emptySchemaSpec(emptySchema) ^ bt ^
    "スキーマ作成" ^ existenceSchemaSpec(anonMemSchema) ^ bt ^
    "onMemory 作成" ^ existenceSchemaSpec(onMemorySchema) ^ bt ^
    "onFile 作成" ^ existenceSchemaSpec(onFileSchema) ^ bt ^
    "DB 上にスキーマが存在する" ^ craeteionSpec ^ bt ^
    Step(closeDatabase) ^
    end
  
  def schema = new MuseumSchema
  
  def emptySchema = schema
  
  def anonMemSchema = {
    createSession.bindToCurrentThread
    val schema = new MuseumSchema
    schema.create
    schema
  }
  
  def closeDatabase {
    MuseumSchema.closeConnection
  }
  
  def onMemorySchema = {
    MuseumSchema.closeConnection
    MuseumSchema.onMemory("MuseumSchemaSpec")
  }
  
  def onFileSchema = {
    val dbFile = File.createTempFile("MuseumSchemaSpec", "")
    MuseumSchema.closeConnection
    MuseumSchema.onFile(dbFile)
  }
  
  def emptySchemaSpec(schema: => MuseumSchema) =
    "DB 上にスキーマが存在しない" ! empty(schema).schemaNotExists
  
  def existenceSchemaSpec(schema: => MuseumSchema) =
    "DB 上にスキーマが作成される" ! existence(schema).schemaExists
    
  def craeteionSpec =
    "onMemory 作成" ! creation.onMemory ^
    "onFile 作成" ! creation.onFile
  
  def empty(schema: MuseumSchema) = new Object {
    def schemaNotExists = {
      createSession.bindToCurrentThread
      schema.exists must beFalse
    }
  }
  
  def existence(schema: MuseumSchema) = new Object {
    def schemaExists = inTransaction {
      schema.exists must beTrue
    }
  }
  
  def creation = new Object {
    def onMemory = {
      MuseumSchema.closeConnection
      MuseumSchema.onMemory("MuseumSchemaSpec")
      MuseumSchema.closeConnection
      MuseumSchema.onMemory("MuseumSchemaSpec")
      success
    }
    
    def onFile = {
      val dbFile = File.createTempFile("MuseumSchemaSpec", "")
      MuseumSchema.closeConnection
      MuseumSchema.onFile(dbFile)
      MuseumSchema.closeConnection
      MuseumSchema.onFile(dbFile)
      success
    }
  }
}
