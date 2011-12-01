package jp.scid.genomemuseum.gui

import java.util.Date

import ca.odell.glazedlists.gui.TableFormat

import jp.scid.gui.StringFilterable
import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitService}

class ExhibitTableModel(tableFormat: TableFormat[MuseumExhibit])
    extends DataTableModel[MuseumExhibit](tableFormat)
    with StringFilterable[MuseumExhibit] with TableColumnSortable[MuseumExhibit] {
  
  def this() = this(new ExhibitTableFormat)
  
  /** 現在設定されているサービス */
  private var currentService: Option[MuseumExhibitService] = None
  
  /** 現在の {@code TableDataService} を取得する */
  def dataService = currentService.get
  
  /** このモデルの {@code TableDataService} ソースを設定する */
  def dataService_=(newDataService: MuseumExhibitService) {
    currentService = Option(newDataService)
    reloadSource()
  }
  
  /**
   * 新しい要素を追加する。
   */
  def createElement() = {
    val newElement = dataService.create()
    sourceListWithWriteLock { list => list add newElement }
    newElement
  }
  
  /** 再読み込み */
  def reloadSource() {
    source = dataService.allElements
  }
  
  /**
   * 選択中の要素を削除する
   */
  def removeSelections() {
    sourceListWithWriteLock { list => 
      selections foreach { selection =>
        currentService foreach { service =>
          service remove selection.asInstanceOf[service.ElementClass]
        }
        list remove selection
      }
    }
  }
  
  /**
   * 要素の削除を行う
   */
  def removeElement(element: MuseumExhibit) {
    sourceListWithWriteLock { list => 
      currentService foreach { service =>
        service remove element.asInstanceOf[service.ElementClass]
      }
      list remove element
    }
  }
  
  /**
   * 要素の更新を行う。
   */
  def updateElement(element: MuseumExhibit) {
    val service = dataService
    val serviceElement = element.asInstanceOf[service.ElementClass]
    service.save(serviceElement)
    updated(element)
  }
  
  protected def getFilterString(base: java.util.List[String], e: MuseumExhibit) {
    base add e.name
    base add e.source
  }
}
