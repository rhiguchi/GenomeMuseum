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
  
  protected def getFilterString(base: java.util.List[String], e: MuseumExhibit) {
    base add e.name
    base add e.source
  }
}
