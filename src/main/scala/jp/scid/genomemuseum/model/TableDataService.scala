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
