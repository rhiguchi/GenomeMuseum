package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import javax.swing.{JTable, JTextField, JLabel, TransferHandler}

import jp.scid.gui.event.DataListSelectionChanged
import jp.scid.genomemuseum.{model, gui, view}
import model.{MuseumExhibitService, MuseumExhibit, UserExhibitRoom,
  MuseumExhibitServiceMock}
import gui.ExhibitTableModel
import view.FileContentView
import DataListController.View

@org.junit.runner.RunWith(classOf[runner.JUnitRunner])
class MuseumExhibitListControllerSpec extends Specification with Mockito with DataListControllerSpec {
  private type Factory = (MuseumExhibitService, View) =>
    MuseumExhibitListController
  
  def is = "MuseumExhibitListController" ^
    "テーブルモデル" ^ tableModelSpec(createController) ^
    "ビュー - テーブル" ^ viewTableSpec2(createController) ^
    "部屋の選択" ^ userExhibitRoomSpec(createController) ^
    "選択項目" ^ tableSelectionSpec(createController) ^
    "検索文字列モデル" ^ searchTextModelSpec(convertFactory(createController)) ^
    "検索フィールドの挙動" ^ searchFieldFilteringSpec(createController) ^
    "削除アクション" ^ remoevSelectionActionSpec(createController) ^
    "転送ハンドラ" ^ tableTransferHandlerSpec(createController) ^
    end
  
  def createController(s: MuseumExhibitService, view: View) =
    new MuseumExhibitListController(s, view)
  
  implicit def convertFactory(f: this.Factory): View => _ <: DataListController =
    (view: View) => f(MuseumExhibitServiceMock.of(), view)
    
  
  def tableModelSpec(f: Factory) =
    "exhibitService の内容が適用" ! tableModel(f).showsContentfsOfService ^
    bt
  
  def viewTableSpec2(f: Factory) =
    "ドラッグ可能" ! viewTable2(f).isDragEnabled ^
    super.viewTableSpec(f)
  
  def userExhibitRoomSpec(f: Factory) =
    "初期内容は空" ! userExhibitRoom(f).isEmptyOnInitial ^
    "テーブルモデルに適用" ! userExhibitRoom(f).toTableModel ^
    bt
  
  def tableSelectionSpec(f: Factory) =
    "初期内容は空" ! tableSelection(f).isEmptyOnInitial ^
    "テーブルモデルの選択が変わるとモデルに適用" ! tableSelection(f).fromTableModel ^
    bt
  
  def searchFieldFilteringSpec(f: Factory) =
    "初期内容は空" ! searchFieldFiltering(f).isEmptyOnInitial ^
    "フィールドの文字列が抽出文字列として適用" ! searchFieldFiltering(f).toTableModel ^
    bt
  
  def remoevSelectionActionSpec(f: Factory) =
    "最初は利用不可" ! remoevSelectionAction(f).notEnabled ^
    "行選択されると利用可能" ! remoevSelectionAction(f).enableWithRowSelected ^
    "選択が解除されると利用不可" ! remoevSelectionAction(f).enableWithRowUnselected ^
    bt
  
  def tableTransferHandlerSpec(f: Factory) =
    "ファイルの読み込み" ! todo ^
    "展示物の転入" ! todo ^
    "転送物の作成" ! todo ^
    bt
  
  class TestBase(f: Factory) {
    val exhibit1, exhibit2, exhibit3 = mock[MuseumExhibit]
    val exhibitService = MuseumExhibitServiceMock.of(exhibit1, exhibit2, exhibit3)
    val searchField = new JTextField
    val table = new JTable
    val view = View(table, searchField)
    val ctrl = f(exhibitService, view)
    jp.scid.gui.DataListModel.waitEventListProcessing
  }
  
  // テーブルモデル
  def tableModel(f: Factory) = new TestBase(f) {
    def showsContentfsOfService = ctrl.tableModel.source must
      contain(exhibit1, exhibit2, exhibit3).only
  }
  
  // ビューのテーブル
  def viewTable2(f: Factory) = new TestBase(f) {
    def isDragEnabled = table.getDragEnabled must beTrue
  }
  
  // 部屋の選択モデル
  def userExhibitRoom(f: Factory) = new TestBase(f) {
    def isEmptyOnInitial = ctrl.userExhibitRoom() must beNone
    
    def toTableModel = {
      val room = mock[UserExhibitRoom]
      ctrl.userExhibitRoom := Some(room)
      
      ctrl.tableModel.userExhibitRoom must beSome(room)
    }
  }
  
  // 行選択
  def tableSelection(f: Factory) = new TestBase(f) {
    def isEmptyOnInitial = ctrl.tableSelection() must beEmpty
    
    def fromTableModel = {
      ctrl.tableModel.selections = Seq(exhibit1)
      val sel1 = ctrl.tableSelection()
      ctrl.tableModel.selections = Seq(exhibit2, exhibit3)
      val sel2 = ctrl.tableSelection()
      
      (sel1, sel2) must_== (Seq(exhibit1), Seq(exhibit2, exhibit3))
    }
  }
  
  // 検索フィールド
  def searchFieldFiltering(f: Factory) = new TestBase(f) {
    def isEmptyOnInitial = searchField.getText must beEmpty
    
    def toTableModel = {
      searchField.setText("12345")
      ctrl.tableModel.filterText must_== "12345"
    }
  }
  
  def remoevSelectionAction(f: Factory) = new TestBase(f) {
    def notEnabled = ctrl.removeSelectionAction.enabled must beFalse
    
    def enableWithRowSelected = {
      ctrl.tableModel.select(exhibit1)
      ctrl.removeSelectionAction.enabled must beTrue
    }
    
    def enableWithRowUnselected = {
      ctrl.tableModel.select(exhibit1)
      ctrl.tableModel.selections = Nil
      ctrl.removeSelectionAction.enabled must beFalse
    }
  }
}
