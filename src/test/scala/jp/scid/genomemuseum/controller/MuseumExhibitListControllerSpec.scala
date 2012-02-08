package jp.scid.genomemuseum.controller

import org.specs2._

import javax.swing.{JTable, JTextField, TransferHandler}

import jp.scid.genomemuseum.{model, gui}
import model.{MuseumExhibitService, MuseumExhibit, UserExhibitRoom,
  MuseumExhibitServiceMock}

@org.junit.runner.RunWith(classOf[runner.JUnitRunner])
class MuseumExhibitListControllerSpec extends Specification with mock.Mockito {
  private val dataListCtrlSpec = new DataListControllerSpec
  
  def is = "MuseumExhibitListController" ^ sequential ^
    "テーブルモデル" ^ tableModelSpec(new MuseumExhibitListController) ^
    "プロパティ" ^ propertiesSpec(new MuseumExhibitListController) ^
//    "dataTable 結合" ^ canBindToTable(new MuseumExhibitListController) ^
//    "quickSearchField 結合" ^ canBindToQuickSearchField(new MuseumExhibitListController) ^
    "部屋の選択" ^ userExhibitRoomSpec(new MuseumExhibitListController) ^
    "選択項目" ^ tableSelectionSpec(new MuseumExhibitListController) ^
    "選択項目の削除" ^ canRemoevSelection(new MuseumExhibitListController) ^
    "削除アクション" ^ remoevSelectionActionSpec(new MuseumExhibitListController) ^
//    "転送ハンドラ" ^ tableTransferHandlerSpec(new MuseumExhibitListController) ^
    end
  
  
  def tableModelSpec(f: => MuseumExhibitListController) =
//    "exhibitService で作成されている" ! tableModel(f).showsContentfsOfService ^
    bt
  
  def propertiesSpec(c: => MuseumExhibitListController) =
//    "ドラッグ可能" ! properties(c).isTableDraggable ^
//    "削除アクションがある" ! properties(c).tableDeleteAction ^
    bt

//  def canBindToTable(ctrl: => MuseumExhibitListController) =
//    dataListCtrlSpec.canBindToTable(ctrl)  
  
//  def canBindToQuickSearchField(ctrl: => MuseumExhibitListController) =
//    dataListCtrlSpec.canBindSearchField(ctrl)
  
  def userExhibitRoomSpec(f: => MuseumExhibitListController) =
//    "初期内容は空" ! userExhibitRoom(f).isEmptyOnInitial ^
//    "テーブルモデルに適用" ! userExhibitRoom(f).toTableModel ^
    bt
  
  def tableSelectionSpec(ctrl: => MuseumExhibitListController) =
//    "初期内容は空" ! tableSelection(ctrl).isEmptyOnInitial ^
//    "テーブルモデルの選択が変わるとモデルに適用" ! tableSelection(ctrl).fromTableModel ^
    bt
  
  def canRemoevSelection(ctrl: => MuseumExhibitListController) =
//    "選択モデル内の項目をサービスから削除" ! remoevSelection(ctrl).fromService ^
    bt
  
  def remoevSelectionActionSpec(ctrl: => MuseumExhibitListController) =
//    "フレームワークから生成" ! remoevSelectionAction(ctrl).create ^
//    "最初は利用不可" ! remoevSelectionAction(ctrl).notEnabled ^
//    "行選択されると利用可能" ! remoevSelectionAction(ctrl).enableWithRowSelected ^
//    "選択が解除されると利用不可" ! remoevSelectionAction(ctrl).enableWithRowUnselected ^
    bt
  
  def tableTransferHandlerSpec(f: => MuseumExhibitListController) =
    "ファイルの読み込み" ! todo ^
    "展示物の転入" ! todo ^
    "転送物の作成" ! todo ^
    bt
  
  class TestBase(f: MuseumExhibitListController) {
//    val exhibit1, exhibit2, exhibit3 = mock[MuseumExhibit]
//    val exhibitService = MuseumExhibitServiceMock.of(exhibit1, exhibit2, exhibit3)
//    val ctrl = f(exhibitService)
//    jp.scid.gui.DataListModel.waitEventListProcessing
  }
  
  // テーブルモデル
  def tableModel(ctrl: MuseumExhibitListController) = new {
//    def showsContentfsOfService = ctrl.tableModel.dataService must_==
//      ctrl.exhibitService
  }
  
  // プロパティ
  def properties(ctrl: MuseumExhibitListController) = new {
//    def isTableDraggable = ctrl.isTableDraggable must beTrue
//    def tableDeleteAction = ctrl.tableDeleteAction must beSome(ctrl.removeSelectionAction.peer)
  }
  
  // dataTable 結合
  def bindTableModel(ctrl: MuseumExhibitListController) = new {
  }
  
  // SearchField 結合
  def bindSearchField(ctrl: MuseumExhibitListController) = new {
  }
  
  // 部屋の選択プロパティ
  def userExhibitRoom(f: MuseumExhibitListController) = new TestBase(f) {
//    def isEmptyOnInitial = ctrl.userExhibitRoom must beNone
//    
//    def toTableModel = {
//      val room = mock[UserExhibitRoom]
//      ctrl.userExhibitRoom = Some(room)
//      
//      ctrl.tableModel.userExhibitRoom must beSome(room)
//    }
  }
  
  // 行選択
  def tableSelection(ctrl: MuseumExhibitListController) = new {
//    val exhibit1, exhibit2, exhibit3 = mock[MuseumExhibit]
//    ctrl.tableModel.source = List(exhibit1, exhibit2, exhibit3)
//    
//    def isEmptyOnInitial = ctrl.tableSelection() must beEmpty
//    
//    def fromTableModel = {
//      ctrl.tableModel.selections = Seq(exhibit1)
//      val sel1 = ctrl.tableSelection()
//      ctrl.tableModel.selections = Seq(exhibit2, exhibit3)
//      val sel2 = ctrl.tableSelection()
//      
//      (sel1, sel2) must_== (Seq(exhibit1), Seq(exhibit2, exhibit3))
//    }
  }
  
  // 選択項目削除
  def remoevSelection(f: MuseumExhibitListController) = new TestBase(f) {
//    ctrl.tableModel.selections = Seq(exhibit1, exhibit3)
//    ctrl.removeSelections()
//    
//    def fromService = there was one(exhibitService).remove(exhibit1) then
//      one(exhibitService).remove(exhibit3) then
//      two(exhibitService).remove(any)
  }
  
  // 削除アクション
  def remoevSelectionAction(ctrl: MuseumExhibitListController) = new {
//    val exhibit1, exhibit2, exhibit3 = mock[MuseumExhibit]
//    ctrl.tableModel.source = List(exhibit1, exhibit2, exhibit3)
//    
//    def create = ctrl.removeSelectionAction.name must_== "removeSelections"
//    
//    def notEnabled = ctrl.removeSelectionAction.enabled must beFalse
//    
//    def enableWithRowSelected = {
//      ctrl.tableModel.select(exhibit1)
//      ctrl.removeSelectionAction.enabled must beTrue
//    }
//    
//    def enableWithRowUnselected = {
//      ctrl.tableModel.select(exhibit1)
//      ctrl.tableModel.selections = Nil
//      ctrl.removeSelectionAction.enabled must beFalse
//    }
  }
}
