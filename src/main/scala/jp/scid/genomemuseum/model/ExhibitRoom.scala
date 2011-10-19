package jp.scid.genomemuseum.model

import com.explodingpixels.widgets.TextProvider

/**
 * リストモデル
 */
trait ExhibitRoom extends TextProvider {
  def name: String
  def children: List[ExhibitRoom]
  def getText: String
}

object ExhibitRoom {
  def apply(name: String, children: ExhibitRoom*): ExhibitRoom =
    ExhibitRoom(name, children.toList)
  
  def apply(name0: String, children0: List[ExhibitRoom]) = new ExhibitRoom {
    val name: String = name0
    val children: List[ExhibitRoom] = children0
    def getText = name
    override def toString() = name
  }
}