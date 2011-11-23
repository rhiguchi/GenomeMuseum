package jp.scid.genomemuseum.model.squeryl

import math.Ordering
import collection.SortedSet

/**
 * メモリー上に保持されるサービス
 */
class SortedSetMuseumExhibitService
    extends jp.scid.genomemuseum.model.ListDataService[MuseumExhibit] {
  type ElementClass = MuseumExhibit
  
  /** 非永続化要素の比較器（兼アクセスロック） */
  private val idReverseOrdering = Ordering.fromLessThan[MuseumExhibit]((e1, e2) => e1.id > e2.id)
  /** 保持するオブジェクト */
  private var entities = SortedSet.empty[MuseumExhibit](idReverseOrdering)
  /** 部屋の項目 */
  private var roomContents = Map.empty[Long, Seq[MuseumExhibit]]
  
  def allElements = entities.toList
  
  def save(element: MuseumExhibit) {
    idReverseOrdering.synchronized {
      entities = entities + element
    }
  }
  
  def indexOf(element: MuseumExhibit) =
    entities.to(element).size - 1
  
  def remove(element: MuseumExhibit) = {
    idReverseOrdering.synchronized {
      // 部屋のコンテンツ除去
      roomContents = roomContents.map { e => 
        Pair(e._1, e._2.filter(element.!=))
      }
      
      val oldSize = entities.size
      entities = entities - element
      entities.size < oldSize
    }
  }
  
  def roomContent(room: UserExhibitRoom): IndexedSeq[MuseumExhibit] =
    roomContents.getOrElse(room.id, Nil).toIndexedSeq
  
  def addRoomContent(room: UserExhibitRoom, element: MuseumExhibit) {
    idReverseOrdering.synchronized {
      val newSet = roomContents.getOrElse(room.id, createExhibitSet) :+ element
      roomContents = roomContents.updated(room.id, newSet)
    }
  }
  
  def removeRoomContent(room: UserExhibitRoom, element: MuseumExhibit): Boolean = {
    idReverseOrdering.synchronized {
      roomContents.get(room.id).map(_.toIndexedSeq).map { contents =>
        val oldSize = contents.size
        val newContents = contents.filter(element.!=).toList
        roomContents = roomContents.updated(room.id, newContents)
        newContents.size < oldSize
      }
      .getOrElse(false)
    }
  }
  
  private def createExhibitSet = IndexedSeq.empty[MuseumExhibit]
  
  def count = entities.size
}
