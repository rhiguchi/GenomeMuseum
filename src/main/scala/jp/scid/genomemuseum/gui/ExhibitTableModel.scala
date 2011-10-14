package jp.scid.genomemuseum.gui

import java.util.Date

import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.swing.SearchEngineTextFieldMatcherEditor

import jp.scid.gui.table.DataTableModel
import jp.scid.genomemuseum.model.{MuseumExhibit, ExhibitDataService, ExhibitRoom,
  ExhibitListBox}

class ExhibitTableModel(tableFormat: TableFormat[MuseumExhibit]) extends DataTableModel[MuseumExhibit](tableFormat)
    with TableDataServiceSource[MuseumExhibit] {
  import javax.swing.JTextField
  import collection.mutable.Buffer
  
  def this() = this(new ExhibitTableFormat)
  
  def filterUsing(field: JTextField) {
    filterUsing(field, getFilterStrings _)
  }
  
  /** フィルタリング対象文字列を取得 */
  protected def getFilterStrings(buffer: Buffer[String], e: MuseumExhibit) {
    buffer += e.name += e.source
  }
}

