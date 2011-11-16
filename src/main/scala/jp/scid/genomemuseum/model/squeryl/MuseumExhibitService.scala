package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table

import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{MuseumExhibitService => IMuseumExhibitService,
  MuseumExhibit => IMuseumExhibit}

/**
 * ローカルライブラリ中の {@link jp.scid.genomemuseum.model.MuseumExhibit}
 * データサービス。
 */
private[squeryl] class MuseumExhibitService(table: Table[MuseumExhibit]) extends IMuseumExhibitService {
  
  def create() = inTransaction {
    table.insert(MuseumExhibit(""))
  }
  
  /** {@inheritDoc} */
  def remove(element: IMuseumExhibit): Boolean = {
    val count = inTransaction {
      val id = element.id
      table.deleteWhere{ e => e.id === id }
    }
    count > 0
  }
  
  /** {@inheritDoc} */
  def allElements = inTransaction {
    from(table)( e => select(e) orderBy(e.id asc)).toList
  }
  
  /** {@inheritDoc} */
  def save(element: IMuseumExhibit) = element match {
    case element: MuseumExhibit => inTransaction {
      table.update(element)
    }
    case _ =>
  }
  
  def indexOf(element: IMuseumExhibit): Int = inTransaction {
    from(table)(e => where(e.id lte element.id) compute(count)).toInt - 1
  }
}
