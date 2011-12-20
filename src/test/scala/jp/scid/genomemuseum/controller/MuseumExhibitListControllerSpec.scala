package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import javax.swing.{JTable, JTextField, JLabel, TransferHandler}

import jp.scid.gui.event.DataListSelectionChanged
import jp.scid.genomemuseum.{model, gui, view}
import model.{MuseumExhibitService, MuseumExhibit, UserExhibitRoom}
import gui.ExhibitTableModel
import view.FileContentView
import DataListController.View

import GenomeMuseumControllerSpec.spyApplicationActionHandler

@org.junit.runner.RunWith(classOf[runner.JUnitRunner])
class MuseumExhibitListControllerSpec extends Specification with Mockito {
  
  private type Factory = (ApplicationActionHandler, View) =>
    MuseumExhibitListController
  
  def is = "MuseumExhibitListController" ^
    "現在の部屋" ^ userExhibitRoomSpec(createController) ^
    "選択項目" ^ tableSelectionSpec(createController) ^
    "クイックサーチ" ^ searchFieldFilteringSpec(createController) ^
    end
  
  def createController(app: ApplicationActionHandler, view: View) =
    new MuseumExhibitListController(app, view)
  
  def userExhibitRoomSpec(f: Factory) =
    "テーブルモデルに適用" ! userExhibitRoom(f).toTableModel ^
    bt
  
  def tableSelectionSpec(f: Factory) =
    "テーブルモデルの選択が変わるとモデルに適用" ! tableSelection(f).fromTableModel ^
    bt
  
  def searchFieldFilteringSpec(f: Factory) =
    "フィールドの文字列が抽出文字列として適用" ! searchFieldFiltering(f).toTableModel ^
    bt
  
  class TestBase(f: Factory) {
    val searchField = new JTextField
    val view = View(new JTable, searchField)
    val ctrl = f(spyApplicationActionHandler, view)
    ctrl.bind()
  }
  
  def userExhibitRoom(f: Factory) = new TestBase(f) {
    def toTableModel = {
      val room = mock[UserExhibitRoom]
      ctrl.userExhibitRoom := Some(room)
      
      ctrl.tableModel.userExhibitRoom must beSome(room)
    }
  }
  
  def tableSelection(f: Factory) = new TestBase(f) {
    def fromTableModel = {
      val exhibit1, exhibit2, exhibit3 = mock[MuseumExhibit]
      val evt = DataListSelectionChanged(ctrl.tableModel, false,
        List(exhibit1, exhibit2, exhibit3))
      ctrl.tableModel.publish(evt)
      ctrl.tableSelection() must contain(exhibit1, exhibit2, exhibit3).only.inOrder
    }
  }
  
  def searchFieldFiltering(f: Factory) = new TestBase(f) {
    def toTableModel = {
      searchField.setText("12345")
      ctrl.tableModel.filterText must_== "12345"
    }
  }
}
