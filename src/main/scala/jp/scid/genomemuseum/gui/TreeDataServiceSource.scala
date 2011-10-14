package jp.scid.genomemuseum.gui

import java.util.Date

import jp.scid.gui.tree.DataTreeModel
import jp.scid.genomemuseum.model.TreeDataService

/**
 * TreeDataService から DataTreeModel へソースを接続するトレイト
 */
trait TreeDataServiceSource[A <: AnyRef, B <: A] {
  this: DataTreeModel[A] =>
  
  /** サービス */
  private var _dataService = TreeDataService[B]()
  /** 更新チェック */
  private val reloadedTime = new Date(0)
  
  /** 現在の {@code TreeDataService} を取得する */
  def dataService = _dataService
  
  /** このモデルの {@code TableDataService} ソースを設定する */
  protected def dataService_=(newDataService: TreeDataService[B]) {
    _dataService = newDataService
    reloadSource()
  }
  
  /**
   * 要素を追加する
   */
  def addElement(newElement: B, parent: Option[B] = None) {
    dataService.add(newElement, parent)
    sourceTreeModel.someChildrenWereInserted(parent.getOrElse(serviceRootElement))
  }
  
  /**
   * ユーザーボックスを削除
   */
  def removeElementFromParent(element: B) {
    val parent = dataService.getParent(element)
    dataService.remove(element)
    sourceTreeModel.someChildrenWereRemoved(parent.getOrElse(serviceRootElement))
  }
  
  /**
   * データサービス更新時にイベント送出に使用するルート要素
   */
  protected def serviceRootElement: A = treeSource.root
  
  /** サービスからデータを読み込み直す */
  protected def reloadSource() {
    reloadedTime.setTime(dataService.lastModified.getTime)
    sourceTreeModel.reset(serviceRootElement)
    // TODO 最読み込み
  }
  
  /** データサービス内の情報が更新されたかを調べる */
  private def serviceIsUpdated: Boolean = {
    reloadedTime.compareTo(dataService.lastModified) > 0
  }
}
