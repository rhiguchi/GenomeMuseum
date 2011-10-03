package jp.scid.genomemuseum.model

import scala.collection.{mutable, script}
import mutable.{ArrayBuffer, ObservableBuffer}
import script.{Script, Remove, Include}

class MuseumDataSource(scheme: MuseumScheme) {
  private lazy val myAllExibits = ExhibitBuffer(scheme.allMuseumExhibits)
  
  def allExibits: ObservableBuffer[MuseumExhibit] = myAllExibits
  
  def store(entity: MuseumExhibit) {
    val isInsert = entity.id.is <= 0
    if (scheme store entity) {
      reloadExibits()
    }
  }
  
  protected def reloadExibits() {
    val newElements = scheme.allMuseumExhibits
    val removeItems = myAllExibits.diff(newElements)
    val addItems = newElements.diff(myAllExibits)
    
    val updator = new Script[MuseumExhibit]
    updator ++= removeItems.map(new Remove(_))
    updator ++= addItems.map(new Include(_))
    
    myAllExibits << updator
  }
  
  protected class ExhibitBuffer extends ArrayBuffer[MuseumExhibit]
      with ObservableBuffer[MuseumExhibit]
  
  protected object ExhibitBuffer {
    def apply(initial: Seq[MuseumExhibit]) = {
      val buf = new ExhibitBuffer()
      buf ++= initial
      buf
    }
  }
}