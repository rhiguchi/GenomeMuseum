package jp.scid.genomemuseum.model

import java.util.Date

/**
 * データ提供インターフェイス
 */
trait TableDataService[A] {
  /** 全ての要素を取得する */
  def getAll(): List[A]
  
  /** このサービスの最終変更時間を取得する */
  def lastModified(): Date
  
  /** このサービスへ要素を追加する */
  def add(element: A)
  
  /** このサービスから要素を除去する */
  def remove(element: A)
  
  /** このサービスにある要素の更新を行う */
  def save(element: A)
  
  /** このサービスにある要素の数を取得する */
  def count: Int
}

object TableDataService {
  def apply[A](): TableDataService[A] = new ListImpl[A]()
  
  /**
   * scala collection での実装
   */
  private[model] class ListImpl[A] extends TableDataService[A] {
    import collection.mutable.ListBuffer
    
    private val elementsBuffer = ListBuffer.empty[A]
    
    var lastModified = new Date()
    
    def getAll() = elementsBuffer.toList
    
    def add(element: A) {
      elementsBuffer += element
      updateLastModified()
    }
    
    def remove(element: A) {
      elementsBuffer -= element
      updateLastModified()
    }
    
    def save(element: A) {
      updateLastModified()
    }
    
    def count = elementsBuffer.size
    
    private def updateLastModified() {
      lastModified = new Date()
    }
  }
}
