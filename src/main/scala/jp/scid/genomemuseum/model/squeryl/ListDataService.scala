package jp.scid.genomemuseum.model.squeryl

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.PrimitiveTypeMode.{count => queryCount}

import jp.scid.genomemuseum.model.{ListDataService => IListDataService}

/**
 * {@link jp.scid.genomemuseum.model.ListDataService} の Squeryl 実装。
 */
trait ListDataService[A <: KeyedEntity[Long]]
    extends IListDataService[A] {
  
  protected def tableService: Table[A]
  
  /** {@inheritDoc} */
  override def remove(element: A): Boolean = {
    val count = inTransaction {
      val id = element.id
      tableService.deleteWhere{ e => id === e.id }
    }
    count > 0
  }
  
  /** {@inheritDoc} */
  override def allElements = inTransaction {
    from(tableService)( e => select(e) orderBy(e.id asc)).toList
  }
  
  /** {@inheritDoc} */
  override def save(element: A) = inTransaction {
    tableService.update(element)
  }
  
  def indexOf(element: A): Int = -1
}

private trait LastModifiedProvider[A <: KeyedEntity[Long]] {
  private var lastModifiedVar: Long = 0
  
  def table: Table[A]
  
  def add(element: A) = inTransaction {
    table.insert(element)
    updateLastModified(element)
  }
  
  def save(element: A) = inTransaction {
    table.update(element)
    updateLastModified(element)
  }
  
  def remove(element: A): Boolean = {
    val count = inTransaction {
      val id = element.id
      table.deleteWhere{ e => id === e.id }
    }
    lastModifiedVar = System.currentTimeMillis
    count > 0
  }
  
  /**
   * 要素の最終更新時間の値を取得するメソッド
   * @param 要素
   * @return この要素の最終更新ミリ時間
   */
  protected def lastModifiedFor(element: A): Long
  
  def lastModified = {
    val lm = inTransaction {
      from(table){ e => compute(max(lastModifiedFor(e))) }.single.measures.getOrElse(0L)
    }
    if (lastModifiedVar < lm) {
      lastModifiedVar = lm
    }
    lastModifiedVar
  }
  
  /* lastModified の更新 SQL を実行 */
  private def updateLastModified(element: A) {
    update(table)(e =>
      where(e.id === element.id)
      set(lastModifiedFor(e) := System.currentTimeMillis))
  }
  
}