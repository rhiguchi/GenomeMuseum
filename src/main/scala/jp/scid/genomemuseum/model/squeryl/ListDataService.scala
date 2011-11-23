package jp.scid.genomemuseum.model.squeryl

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.PrimitiveTypeMode.{count => queryCount}

import jp.scid.genomemuseum.model.{ListDataService => IListDataService}

/**
 * {@link jp.scid.genomemuseum.model.ListDataService} の Squeryl 実装。
 */
private[squeryl] class ListDataService[A <: KeyedEntity[Long]](
    tableService: Table[A]) extends IListDataService[A] {
  type ElementClass = A
  
  /** {@inheritDoc} */
  override def remove(element: ElementClass): Boolean = {
    val count = inTransaction {
      tableService.deleteWhere{ e => e.id === element.id }
    }
    count > 0
  }
  
  /** {@inheritDoc} */
  override def allElements = inTransaction {
    from(tableService)( e => select(e) orderBy(e.id asc)).toList
  }
  
  /** {@inheritDoc} */
  override def save(element: ElementClass) = inTransaction {
    tableService.insertOrUpdate(element)
  }
  
  def indexOf(element: ElementClass): Int = inTransaction {
    from(tableService)(e => where(e.id lte element.id) compute(count)).toInt - 1
  }
}
