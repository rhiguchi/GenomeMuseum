package jp.scid.genomemuseum.gui

import java.util.Date
import jp.scid.gui.DataListModel
import jp.scid.genomemuseum.model.ListDataService

trait ListDataServiceSource[A] {
  this: DataListModel[A] =>
  
  /** 現在設定されているサービス */
  private var currentService: Option[ListDataService[A]] = None
  
  /** 現在の {@code TableDataService} を取得する */
  def dataService = currentService.get
  
  /** このモデルの {@code TableDataService} ソースを設定する */
  def dataService_=(newDataService: ListDataService[A]) {
    currentService = Option(newDataService)
    reloadSource()
  }
  
  /** 再読み込み */
  def reloadSource() {
    source = currentService match {
      case Some(service) => service.allElements
      case None => Nil
    }
  }
  
  /**
   * 選択中の要素を削除する
   */
  def removeSelections() {
    sourceListWithWriteLock { list => 
      selections foreach { selection =>
        dataService remove selection
        list remove selection
      }
    }
  }
  
  /**
   * 新しい要素を追加する。
   */
  def createElement() = {
    val newElement = dataService.create()
    sourceListWithWriteLock { list => list add newElement }
    newElement
  }
  
  /**
   * 要素の更新を行う。
   */
  def updateElement(element: A) {
    dataService.save(element)
    val index = dataService.indexOf(element)
    if (index >= 0)
      sourceListWithReadLock { _.set(index, element) }
  }
}

