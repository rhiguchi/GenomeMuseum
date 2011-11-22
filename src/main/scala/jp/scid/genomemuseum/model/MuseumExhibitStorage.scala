package jp.scid.genomemuseum.model

import java.net.URL
import java.io.File

trait MuseumExhibitStorage {
  def save(data: File, exhibit: MuseumExhibit): Option[URL]
}