package jp.scid.genomemuseum.controller

import javax.swing.JTable

import ca.odell.glazedlists.{swing => glswing, BasicEventList}
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