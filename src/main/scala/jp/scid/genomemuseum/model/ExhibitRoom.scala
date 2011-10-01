package jp.scid.genomemuseum.model

/**
 * リストモデル
 */
trait ExhibitRoom {
  def name: String
  def contents: List[MuseumExhibit]
}

object ExhibitRoom {
  def apply(name: String): ExhibitRoom = apply(name, Nil)
  
  def apply(name: String, contents: List[MuseumExhibit]) = new ExhibitRoom {
    def name = name
    def contents = contents
  }
}