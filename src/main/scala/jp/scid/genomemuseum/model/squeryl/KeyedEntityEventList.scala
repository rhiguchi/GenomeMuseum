package jp.scid.genomemuseum.model.squeryl

import org.squeryl.{KeyedEntity, Table}
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{FunctionList, GlazedLists}

import jp.scid.gui.model.AbstractPersistentEventList

private object KeyedEntityEventList {
  import java.util.Comparator
  
  /**
   * ID で比較する比較器。
   * 
   * 両方の ID が 0 の時は hashCode で比較する。
   */
  private class KeyedEntityIdComparator[E <: KeyedEntity[Long]] extends Comparator[E] {
    /** ID で比較する */
    def compareId(o1: E, o2: E): Int = o1.id == o2.id match {
      case false => if (o1.id < o2.id) -1 else 1
      case true => o1.id match {
        case 0 => if (o1.hashCode < o2.hashCode) -1 else 1
        case _ => 0
      }
    }
    
    /** null チェックを行いながら比較する。 */
    def compare(o1: E, o2: E): Int = o1 eq o2 match {
      case true => 0
      case false => o1 match {
        case null => 1
        case _ => o2 match {
          case null => -1
          case _ => compareId(o1, o2)
        }
      }
    }
  }
  
  /** エンティティから ID を作成する関数クラス */
  private class IdentifierMaker[E <: KeyedEntity[Long]] extends FunctionList.Function[E, Long] {
    def evaluate(element: E) = element.id
  }
}

/**
 * Squeryl テーブルと EventList のアダプター。
 * ID 順でテーブル項目と結合される。
 */
class KeyedEntityEventList[E <: KeyedEntity[Long]](table: Table[E])
      extends AbstractPersistentEventList[E](new KeyedEntityEventList.KeyedEntityIdComparator[E]) {
  // Read
  /** 常に ID でソートされる。 */
  override def fetchAll() = inTransaction {
    import collection.JavaConverters._
    from(table)( e => getFetchQuery(e) select(e) orderBy(e.id asc)).toIndexedSeq.asJava
  }
  
  /** 読み出し用クエリを返す */
  private[squeryl] protected def getFetchQuery(e: E) = where(e.id gt 0)
  
  // Insert
  override def insertToTable(element: E) = inTransaction {
    table.insert(element)
    true
  }
  
  // Update
  override def updateToTable(element: E) = inTransaction {
    table.update(element)
  }
  
  // Delete
  override def deleteFromTable(entity: E) = inTransaction {
    table.delete(entity.id)
  }
  
  override def dispose() {
    identifierMap.dispose()
    super.dispose()
  }
  
  /** id から取得 */
  def findOrNull(id: Long): E = identifierMap.get(id)
  
  /** ID から取得するためのマップ */
  lazy val identifierMap = GlazedLists.syncEventListToMap(this, new KeyedEntityEventList.IdentifierMaker[E])
}
