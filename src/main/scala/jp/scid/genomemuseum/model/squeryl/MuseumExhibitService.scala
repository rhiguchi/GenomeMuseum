package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{MuseumExhibitService => IMuseumExhibitService}

/**
 * ローカルライブラリ中の {@link jp.scid.genomemuseum.model.MuseumExhibit}
 * データサービス。
 */
private[squeryl] class MuseumExhibitService(table: Table[MuseumExhibit],
    nonPersistedExhibits: SortedSetMuseumExhibitService) extends IMuseumExhibitService {
  type ElementClass = MuseumExhibit
  
  /** テーブルアクセスの処理委譲 */
  private val tableDelegate = new ListDataService(table)
  
  /**
   * Squeryl MuseumExhibit エンティティを作成する。
   * 永続化はされないが、 {@link allElements} では要素が返される。
   */
  def create() = {
    val e = MuseumExhibit("")
    nonPersistedExhibits.save(e)
    e
  }
  
  /** {@inheritDoc} */
  def remove(element: MuseumExhibit): Boolean = {
    if (tableDelegate.remove(element)) true
    else nonPersistedExhibits.remove(element)
  }
  
  /** {@inheritDoc} */
  def allElements =
    nonPersistedExhibits.allElements ::: tableDelegate.allElements
  
  /** {@inheritDoc} */
  def save(element: ElementClass) {
    tableDelegate.save(element)
    nonPersistedExhibits.remove(element)
  }
  
  def indexOf(element: MuseumExhibit) = {
    tableDelegate.indexOf(element) match {
      case -1 => nonPersistedExhibits.indexOf(element)
      case index => index + nonPersistedExhibits.count
    }
  }
}
