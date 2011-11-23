package jp.scid.genomemuseum.model.squeryl

import org.specs2._

class MuseumSchemaSpec extends Specification with SquerylConnection {
  def is = "MuseumSchema" ^
    "museumExhibitService" ^ emptyMuseumExhibitService(emptySchema) ^ bt ^
    end
  
  def schema = new MuseumSchema
  
  def emptySchema = {
    setUpSchema
    schema
  }
  
  def emptyMuseumExhibitService(schema: => MuseumSchema) =
    "項目の取得" ! museumExhibitService(schema).isEmpty
  
  def museumExhibitService(schema: MuseumSchema) = new Object {
    def isEmpty =
      schema.museumExhibitService.allElements must beEmpty
  }
}
