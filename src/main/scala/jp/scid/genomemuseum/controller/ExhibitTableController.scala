package jp.scid.genomemuseum.controller

import javax.swing.JTable

import ca.odell.glazedlists.{swing => glswing, BasicEventList,
  TextFilterator}
import glswing.EventTableModel

import jp.scid.genomemuseum.{gui, model}
import gui.ExhibitTableFormat
import model.{MuseumExhibit}

class ExhibitTableController(
  table: JTable
) {
  protected val tableFormat = new ExhibitTableFormat
  val tableSource = new BasicEventList[MuseumExhibit]
  protected val tableModel = new EventTableModel(tableSource, tableFormat)
  
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