package jp.scid.genomemuseum.model

import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

/**
 * 永続化用リストボックスモデル
 */
case class ExhibitListBox(
  var name: String,
  @Column("type")
  var listType: ExhibitListBox.BoxType.Value = ExhibitListBox.BoxType.ListBox,
  @Column("parent_id")
  var parentId: Option[Long] = Some(0)
) extends KeyedEntity[Long] with ExhibitRoom {
  var id: Long = 0
  
  def this() = this("untitled")
  
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