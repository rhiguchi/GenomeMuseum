package jp.scid.genomemuseum.model

import scala.collection.{mutable, script}
import mutable.{ArrayBuffer, ObservableBuffer}
import script.{Script, Remove}

class MuseumDataSource(scheme: MuseumScheme) {
  private lazy val myAllExibits = ExhibitBuffer(scheme.allMuseumExhibits)
  
  def allExibits: ObservableBuffer[MuseumExhibit] = myAllExibits
  
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