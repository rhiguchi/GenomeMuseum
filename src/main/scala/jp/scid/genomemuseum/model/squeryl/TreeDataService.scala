package jp.scid.genomemuseum.model.squeryl

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.PrimitiveTypeMode.{count => queryCount}

/**
 * ツリー構造のデータを扱うためのトレイト
 */
abstract class TreeDataService[A <: KeyedEntity[Long]](val table: Table[A])
    extends jp.scid.genomemuseum.model.TreeDataService[A] {
  def count: Int = inTransaction {
    from(table){s => compute(queryCount)}.toInt
  }
  
  def add(entity: A, parent: Option[A] = None): Unit = inTransaction {
    parent.foreach(setParentTo(entity, _))
    table.insert(entity)
  }
  
  /** 親キーを設定する */
  protected def setParentTo(entity: A, parent: A)
  
  /** 親キーを取得する */
  protected def getParentValue(entity: A): Option[Long]
  
  def rootItems: List[A] = inTransaction {
    from(table){ e => where(getParentValue(e) isNull) select(e) }.toList
  }
  
  def getChildren(parent: A): List[A] = inTransaction {
    from(table){ e => where(getParentValue(e) === parent.id) select(e) }.toList
  }
  
  def getParent(element: A): Option[A] = inTransaction {
    from(table){ e => where(e.id === getParentValue(element)) select(e) }.headOption
  }
  
  def save(element: A): Unit = inTransaction {
    table.update(element)
  }
  
  def remove(element: A): Int = inTransaction {
    val id = element.id
    table.deleteWhere{ e => id === e.id }
  }
}
