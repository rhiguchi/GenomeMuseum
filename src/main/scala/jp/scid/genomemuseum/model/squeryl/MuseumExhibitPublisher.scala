package jp.scid.genomemuseum.model.squeryl

import collection.mutable.Publisher
import collection.script.{Message, Include, Update, Remove}

import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{MuseumExhibit => IMuseumExhibit}

import SquerylTriggerAdapter._

trait MuseumExhibitPublisher extends Publisher[Message[IMuseumExhibit]] {
  protected def museumExhibitTablePublisher: Publisher[TableOperation[MuseumExhibit]]
  
  private val tablePublisher = museumExhibitTablePublisher
  
  private val subscriber = new tablePublisher.Sub {
    def notify(pub: tablePublisher.Pub, message: TableOperation[MuseumExhibit]) {
      val newMessage = message match {
        case Inserted(table, id) => new Include(table.lookup(id).get)
        case Updated(table, id) => new Update(table.lookup(id).get)
        case Deleted(table, id) =>
          val e = MuseumExhibit()
          e.id = id
          new Remove(e)
      }
      publish(newMessage)
    }
  }
  
  tablePublisher subscribe subscriber
}