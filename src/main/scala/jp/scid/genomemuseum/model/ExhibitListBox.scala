package jp.scid.genomemuseum.model

import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

/**
 * 永続化用リストボックスモデル
 */
case class ExhibitListBox(
  var name: String,
  @Column("type")
  val boxType: ExhibitListBox.BoxType.Value = ExhibitListBox.BoxType.ListBox,
  @Column("parent_id")
  var parentId: Option[Long] = None
) extends KeyedEntity[Long] with ExhibitRoom {
  var id: Long = 0
  
  def this() = this("", parentId = Some(0))
  
  def children = Nil
}

object ExhibitListBox {
  object BoxType extends Enumeration {
    type BoxType = Value
    val BoxFolder = Value(1)
    val ListBox = Value(2)
    val SmartBox = Value(3)
  }
}
