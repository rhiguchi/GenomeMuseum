package jp.scid.genomemuseum.model

import java.util.Date

/**
 * ツリー構造のデータを扱うためのトレイト
 */
trait TreeDataService[A] {
  /**
   * 全ての項目数を返す
   */
  def count: Int
  
  /**
   * 要素を追加する
   * @param newRoom 新しい要素。
   * @param parent 親要素。ルートに追加するときは {@code None} 。無指定時は {@code None} 。
   */
  def add(newRoom: A, parent: Option[A] = None)
  
  /**
   * ルートに位置する（親が存在しない）項目を返す。
   */
  def rootItems: Iterable[A]
  
  /**
   * 子要素を返す。
   */
  def getChildren(parent: A): Iterable[A]
  
  /**
   * 親要素を取得する
   * @param element 子要素
   * @return 親要素。{@rootItems} の時は {@code None} 。
   */
  def getParent(element: A): Option[A]
  
  /**
   * 要素の更新を通知する。
   * @param element 削除する要素。
   * @return 削除された要素数。
   */
  def save(element: A)
  
  /**
   * 要素と、その子孫全てを削除する
   * @param element 削除する要素。
   * @return 削除された要素数。
   */
  def remove(element: A): Int
  
  /**
   * このサービスの最終変更時間を取得する
   */
  def lastModified: Date
}

object TreeDataService {
  def apply[A](): TreeDataService[A] = newHashMapImpl[A]
  
  private def newHashMapImpl[A]() = new HashMapImpl[A]
}

/**
 * HashMap で親子関係を管理する実装
 */
private class HashMapImpl[A] extends TreeDataService[A] {
  import collection.mutable.{Map, Set}
  
  private val store = Map.empty[A, Set[A]]
  private val parentMap = Map.empty[A, Option[A]]
  private val rootItem = Set.empty[A]
  var lastModified = new Date()
  
  def count = rootItem.size +
    store.foldLeft(0){ (count, pair) => count + pair._2.size }
  
  def add(newRoom: A, parent: Option[A] = None) {
    parent match {
      case Some(parent) =>
        store.getOrElseUpdate(parent, Set.empty) += newRoom
      case None => 
        rootItem += newRoom
    }
    parentMap(newRoom) = parent
    updateLastModified()
  }
  
  def rootItems = {
    rootItem.toIterable
  }
  
  def getChildren(parent: A) = {
    store.getOrElse(parent, Iterable.empty).toIterable
  }
  
  def save(element: A) {
    updateLastModified()
  }
  
  def remove(element: A): Int = {
    val c = removeDescendent(element)
    updateLastModified()
    c
  }
  
  def getParent(element: A): Option[A] = {
    parentMap.getOrElse(element, None)
  }
  
  private def removeDescendent(element: A): Int = {
    def removeElement(e: A) {
      val parent: Option[A] = parentMap.remove(e).getOrElse(None)
      parent match {
        case Some(parent) => store.get(parent).map(_.remove(e))
        case None => rootItem.remove(e)
      }
      store.remove(e)
    }
    
    val c = getChildren(element).toList.map(removeDescendent(_)).sum
    removeElement(element)
    c + 1
  }
    
  private def updateLastModified() {
    lastModified = new Date()
  }
}
