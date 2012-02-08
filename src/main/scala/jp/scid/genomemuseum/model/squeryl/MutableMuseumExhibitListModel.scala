package jp.scid.genomemuseum.model.squeryl

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.OneToManyRelation
import org.squeryl.Table

import jp.scid.genomemuseum.model.{MutableMuseumExhibitListModel => IMutableMuseumExhibitListModel,
  MuseumExhibit => IMuseumExhibit, UserExhibitRoom => IUserExhibitRoom}
import IUserExhibitRoom.RoomType
import RoomType._

class MutableMuseumExhibitListModel(
    exhibitRelation: OneToManyRelation[MuseumExhibit, RoomExhibit],
    roomTable: Table[UserExhibitRoom],
    room: Option[IUserExhibitRoom]
) extends MuseumExhibitListModel(exhibitRelation, roomTable, room)
    with IMutableMuseumExhibitListModel {

  /**
   * このデータサービスが持つ要素を除去する。
   * 要素がこのサービスに存在しない時は無視される。
   * @return 削除に成功した場合は {@code true} 。
   *         項目が存在しなかったなどでサービス内に変更が発生しなかった時は {@code false} 。
   */
  override def remove(element: IMuseumExhibit): Boolean = inTransaction {
      exhibitTable.delete(element.id)
  }
  
  /**
   * 要素の更新をサービスに通知する。
   * 要素がまだサービスに永続化されていない時は、永続化される。
   * 要素がこのサービスに存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  override def add(element: IMuseumExhibit) = room match {
    case Some(room @ RoomType(BasicRoom)) => inTransaction {
      relationTable.insert(RoomExhibit(room, element.asInstanceOf[MuseumExhibit]))
    }
    case None => exhibitTable.insertOrUpdate(element.asInstanceOf[MuseumExhibit])
    case _ =>
      throw new IllegalArgumentException("Cannot add element to %s.".format(room))
  }
}