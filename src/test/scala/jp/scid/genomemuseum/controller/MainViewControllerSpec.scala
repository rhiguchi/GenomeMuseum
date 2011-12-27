package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.{view, gui, model, GenomeMuseumGUI}
import model.{ExhibitRoom, UserExhibitRoom, MuseumSchema, MuseumExhibitService}
import view.MainView
import gui.ExhibitTableModel

class MainViewControllerSpec extends Specification with Mockito {
  private type Factory = (GenomeMuseumGUI, MainView) => MainViewController
  
  def is = "MainViewController" ^
//    "部屋の選択" ^ selectedRoomSpec(createController) ^
    "ビュー結合" ^ canBindMainView(createController) ^
//    "ウェブソース選択" ^ webSourceModeSpec(webSourceSelected) ^ bt ^
//    "UserRoom 選択" ^ localSourceModeSpec(userRoomSelected) ^ bt ^
//    "ExhibitRoom 選択" ^ localSourceModeSpec(userRoomSelected) ^ bt ^
//    "プロパティ" ^ propertiesSpec(defaultController) ^ bt ^
    end
  
  def createController(app: GenomeMuseumGUI, mainView: MainView) =
    new MainViewController(app, mainView)
  
  private implicit def construct(f: Factory): MainViewController = {
    val app = new GenomeMuseumGUI
    val mainView = new MainView
    createController(app, mainView)
  }
  
  def selectedRoomSpec(f: Factory) =
    "ローカルソース選択で部屋選択は None となる" ! selectedRoom(f).localSourceToNone ^
    "ローカルソース選択で展示物一覧表示となる" ! selectedRoom(f).localSourceLocalView ^
    "ウェブソース選択でウェブ検索表示となる" ! selectedRoom(f).webSource ^
    bt
  
  def canBindMainView(f: Factory) =
    "ツリーが sourceListCtrl と結合される" ! bindMainView(f).toSourceListCtrl ^
    "museumExhibitListCtrl と結合される" ! bindMainView(f).toExhibitCtrl ^
    "ウェブソース選択で museumExhibitListCtrl と結合される" ! bindMainView(f).toWebSource ^
    "addBasicRoom ボタンアクション" ! bindMainView(f).addBasicRoom ^
    "addGroupRoom ボタンアクション" ! bindMainView(f).addGroupRoom ^
    "addSmartRoom ボタンアクション" ! bindMainView(f).addSmartRoom ^
    "removeRoom ボタンアクション" ! bindMainView(f).removeRoom ^
    "コンテントビューワーと結合" ! todo ^
    bt
  
  def selectedRoom(f: Factory) = new {
    val view = new MainView
    val ctrl = f(new GenomeMuseumGUI, view)
    
    private def museumStructure = ctrl.sourceListCtrl.sourceStructure
    
    // ローカルソースを選択
    private def selectLocalSource() = ctrl.selectedRoom := museumStructure.localSource
    
    // ウェブソースを選択
    private def selectWebSource() = ctrl.selectedRoom := museumStructure.webSource
    
    def localSourceToNone = {
      selectLocalSource()
      
      ctrl.museumExhibitListCtrl.userExhibitRoom must beNone
    }
    
    def localSourceLocalView = {
      selectWebSource()
      selectLocalSource()
      
      view.dataTable.getModel must_== ctrl.museumExhibitListCtrl.tableModel.tableModel
    }
    
    def webSource = {
      selectLocalSource()
      selectWebSource()
      
      view.dataTable.getModel must_== ctrl.webServiceResultCtrl.tableModel.tableModel
    }
  }
  
  def bindMainView(c: MainViewController) = new {
    val ctrl = spy(c)
    ctrl.sourceListCtrl returns spy(c.sourceListCtrl)
    ctrl.museumExhibitListCtrl returns spy(c.museumExhibitListCtrl)
    ctrl.webServiceResultCtrl returns spy(c.webServiceResultCtrl)
    
    val view = new MainView
    ctrl.bindMainView(view)
    
    private def sourceListCtrl = ctrl.sourceListCtrl
    
    def toSourceListCtrl = there was
      one(sourceListCtrl).bindTree(view.sourceList)
    
    def toExhibitCtrl = there was
      one(ctrl.museumExhibitListCtrl).bindTable(view.dataTable) then
      one(ctrl.museumExhibitListCtrl).bindSearchField(view.quickSearchField)
    
    def toWebSource = {
      ctrl.selectedRoom := sourceListCtrl.sourceStructure.webSource
      there was
        one(ctrl.webServiceResultCtrl).bindTable(view.dataTable) then
        one(ctrl.webServiceResultCtrl).bindSearchField(view.quickSearchField)
    }
    
    def addBasicRoom = view.addListBox.getAction must_==
      sourceListCtrl.addBasicRoomAction.peer
    
    def addGroupRoom = view.addSmartBox.getAction must_==
      sourceListCtrl.addSamrtRoomAction.peer
    
    def addSmartRoom = view.addBoxFolder.getAction must_==
      sourceListCtrl.addGroupRoomAction.peer
    
    def removeRoom = view.removeBoxButton.getAction must_==
      sourceListCtrl.removeSelectedUserRoomAction.peer
  }
  
//  
//  def webSourceModeSpec(ctrl: => MainViewController) =
//    "テーブルモデルが webServiceResultCtrl#tableModel" ! views(ctrl).appliedWebSourceTableModel
//  
//  def localSourceModeSpec(ctrl: => MainViewController) =
//    "テーブルモデルが museumExhibitListCtrl#tableModel" ! views(ctrl).appliedExhibitTableModel
//  
//  def propertiesSpec(ctrl: => MainViewController) =
//    "loadManager 設定" ! properteis(ctrl).loadManager ^
//    "dataSchema 設定" ! properteis(ctrl).dataSchema
//  
//  class TestBase(ctrl: MainViewController) {
//    def mainView = ctrl.mainView
//    
//    def sourceListCtrl = ctrl.sourceListCtrl
//  }
//  
//  def properteis(ctrl: MainViewController) = new TestBase(ctrl) {
//    def loadManager = {
//      val service = mock[MuseumExhibitService]
//      val manager = new MuseumExhibitLoadManager(service, None)
//      ctrl.loadManager = manager
//      ctrl.museumExhibitListCtrl.loadManager must_== manager
//    }
//    
//    def dataSchema = {
//      val schema = MuseumSchemaSpec.makeMock(mock[MuseumSchema])
//      ctrl.dataSchema = schema
//      ctrl.sourceListCtrl.userExhibitRoomService must_== schema.userExhibitRoomService
//    }
//  }
}

object MainViewControllerSpec extends Mockito {
  import jp.scid.genomemuseum.model.TreeDataService
  
  def makeTreeDataServiceMock[A](service: TreeDataService[A]) {
    service.getChildren(any) returns Nil
    service.getParent(any) returns None
  }
}