package jp.scid.genomemuseum.model

import java.net.URL
import java.io.File

trait MuseumExhibitStorage {
  def saveSource(exhibit: MuseumExhibit, data: File): URL
  def getSource(exhibit: MuseumExhibit): Option[URL]
}