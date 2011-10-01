package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField}

import ca.odell.glazedlists.{swing => glswing, BasicEventList,
  TextFilterator, FilterList}
import glswing.{EventTableModel, SearchEngineTextFieldMatcherEditor}

import jp.scid.genomemuseum.{gui, model}
import gui.ExhibitTableFormat
import model.{MuseumExhibit}

class ExhibitTableController(
  table: JTable, quickSearchField: JTextField
) {
  val tableSource = new BasicEventList[MuseumExhibit]
  protected val filterator = new ExhibitTableTextFilterator
  protected val matcherEditor = new SearchEngineTextFieldMatcherEditor(
    quickSearchField, filterator)
  protected val filteredSource = new FilterList(tableSource, matcherEditor)
  protected val tableFormat = new ExhibitTableFormat
  protected val tableModel = new EventTableModel(filteredSource, tableFormat)
  
  // データバインディング
  table.setModel(tableModel)
}

protected class ExhibitTableTextFilterator
    extends TextFilterator[MuseumExhibit] {
  import java.{util => ju}
  override def getFilterStrings(baseList: ju.List[String],
      element: MuseumExhibit) {
    baseList add element.name
    baseList add element.source
  }
}