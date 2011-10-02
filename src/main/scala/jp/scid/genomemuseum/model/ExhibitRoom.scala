package jp.scid.genomemuseum.model

/**
 * リストモデル
 */
trait ExhibitRoom {
  def name: String
  def children: List[ExhibitRoom]
}

object ExhibitRoom {
  def apply(name: String, children: ExhibitRoom*): ExhibitRoom =
    ExhibitRoom(name, children.toList)
  
  def apply(name0: String, children0: List[ExhibitRoom]) = new ExhibitRoom {
    val name: String = name0
    val children: List[ExhibitRoom] = children0
    
    override def toString() = name
  }
}