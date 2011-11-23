package jp.scid.genomemuseum.gui

import java.util.Date

import ca.odell.glazedlists.gui.TableFormat

import jp.scid.gui.StringFilterable
import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitService}

class ExhibitTableModel(tableFormat: TableFormat[MuseumExhibit])
    extends DataTableModel[MuseumExhibit](tableFormat)
    with StringFilterable[MuseumExhibit] with TableColumnSortable[MuseumExhibit]
    with ListDataServiceSource[MuseumExhibit] {
  
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
  
  protected def getFilterString(base: java.util.List[String], e: MuseumExhibit) {
    base add e.name
    base add e.source
  }
}
