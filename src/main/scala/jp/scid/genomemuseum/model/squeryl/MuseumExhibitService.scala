package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import math.Ordering
import collection.SortedSet

import jp.scid.genomemuseum.model.{MuseumExhibitService => IMuseumExhibitService}

/**
 * ローカルライブラリ中の {@link jp.scid.genomemuseum.model.MuseumExhibit}
 * データサービス。
 */
private[squeryl] class MuseumExhibitService(table: Table[MuseumExhibit]) extends IMuseumExhibitService {
  type ElementClass = MuseumExhibit
  
  /** テーブルアクセスの処理委譲 */
  private val tableDelegate = new ListDataService(table)
  
  /** 非永続化要素の比較器（兼アクセスロック） */
  private val idReverseOrdering = Ordering.fromLessThan[MuseumExhibit]((e1, e2) => e1.id > e2.id)
  /** 永続化されていない、{@code create} によって作成された要素 */
  private var nonPersistedEntities = SortedSet.empty[MuseumExhibit](idReverseOrdering)
  
  /**
   * Squeryl MuseumExhibit エンティティを作成する。
   * 永続化はされないが、 {@link allElements} では要素が返される。
   */
  def create() = {
    val e = MuseumExhibit("")
    idReverseOrdering.synchronized {
      nonPersistedEntities = nonPersistedEntities + e
    }
    e
  }
  
  /** {@inheritDoc} */
  def remove(element: MuseumExhibit): Boolean = {
    if (tableDelegate.remove(element)) {
      true
    }
    else idReverseOrdering.synchronized {
      val oldSize = nonPersistedEntities.size
      removeNotPersistedEntity(element)
      nonPersistedEntities.size < oldSize
    }
  }
  
  /** {@inheritDoc} */
  def allElements =
    nonPersistedEntities.toList ::: tableDelegate.allElements
  
  /** {@inheritDoc} */
  def save(element: ElementClass) {
    tableDelegate.save(element)
    idReverseOrdering.synchronized {
      removeNotPersistedEntity(element)
    }
  }
  
  def indexOf(element: MuseumExhibit) = {
    tableDelegate.indexOf(element) match {
      case -1 => nonPersistedEntities.to(element).size - 1
      case index => index + nonPersistedEntities.size
    }
  }
  
  /** 非永続化要素を除去する */
  private def removeNotPersistedEntity(element: MuseumExhibit) =
    nonPersistedEntities = nonPersistedEntities - element
}
