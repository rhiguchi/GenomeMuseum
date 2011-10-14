package jp.scid.genomemuseum.gui

import java.util.Date

import jp.scid.gui.table.DataTableModel
import jp.scid.genomemuseum.model.TableDataService

/**
 * TableDataService から DataTableModel へソースを接続するトレイト
 */
trait TableDataServiceSource[A] {
  this: DataTableModel[A] =>
  
  /** サービス */
  private var _dataService = null: TableDataService[A]
  /** 更新チェック */
  private val reloadedTime = new Date(0)
  
  /** 現在の {@code TableDataService} を取得する */
  def dataService = _dataService
  
  /** このモデルの {@code TableDataService} ソースを設定する */
  def dataService_=(newDataService: TableDataService[A]) {
    _dataService = newDataService
    reloadSource()
  }
  
  /**
   * 要素を追加する。
   * サービスに要素が追加される。更新イベントが送出される
   */
  def addElement(e: A) {
    dataService add e
    reloadSource()
  }
  
  /**
   * 要素を削除する。更新イベントが送出される
   */
  def removeElement(e: A) {
    dataService remove e
    reloadSource()
  }
  
  /**
   * 要素を削除する。更新イベントが送出される
   */
  def removeElements(e: Seq[A]) {
    e map dataService.remove
    reloadSource()
  }
  
  
  /** サービスからデータを読み込み直す */
  protected def reloadSource() {
    reloadedTime.setTime(dataService.lastModified.getTime)
    source = loadSourceElements
  }
  
  /** データソースを取得する */
  protected def loadSourceElements() = dataService.getAll
  
  /** データサービス内の情報が更新されたかを調べる */
  private def serviceIsUpdated: Boolean = {
    reloadedTime.compareTo(dataService.lastModified) > 0
  }
}
