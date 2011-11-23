package jp.scid.genomemuseum.gui

import java.util.Date
import jp.scid.gui.DataListModel
import jp.scid.genomemuseum.model.ListDataService

trait ListDataServiceSource[A] {
  this: DataListModel[A] =>
  
  /** 現在の {@code TableDataService} を取得する */
  def dataService: ListDataService[A]
  
  /** 再読み込み */
  def reloadSource() {
    source = dataService.allElements
  }
  
  /**
   * 選択中の要素を削除する
   */
  def removeSelections() {
    val service = dataService
    sourceListWithWriteLock { list => 
      selections foreach { selection =>
        service remove selection.asInstanceOf[service.ElementClass]
        list remove selection
      }
    }
  }
  
  /**
   * 要素の削除を行う
   */
  def removeElement(element: A) {
    // TODO
  }
  
  
  /**
   * 要素の更新を行う。
   */
  def updateElement(element: A) {
//    dataService.save(element)
    val service = dataService
    val index = service.indexOf(element.asInstanceOf[service.ElementClass])
    if (index >= 0)
      sourceListWithReadLock { _.set(index, element) }
  }
}

