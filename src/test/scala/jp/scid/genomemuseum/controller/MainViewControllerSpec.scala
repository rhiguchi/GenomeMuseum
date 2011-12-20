package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.{view, gui, model}
import model.{ExhibitRoom, UserExhibitRoom, MuseumSchema,
  MuseumSchemaSpec, MuseumExhibitService}
import view.MainView
import gui.ExhibitTableModel

import GenomeMuseumControllerSpec.spyApplicationActionHandler

class MainViewControllerSpec extends Specification with Mockito {
  private type Factory = (ApplicationActionHandler, MainView) => MainViewController
  
  def is = "MainViewController" ^
    "部屋の選択" ^ selectedRoomSpec(createController) ^
//    "mainView" ^ mainViewSpec(defaultController) ^ bt ^
//    "ウェブソース選択" ^ webSourceModeSpec(webSourceSelected) ^ bt ^
//    "UserRoom 選択" ^ localSourceModeSpec(userRoomSelected) ^ bt ^
//    "ExhibitRoom 選択" ^ localSourceModeSpec(userRoomSelected) ^ bt ^
//    "プロパティ" ^ propertiesSpec(defaultController) ^ bt ^
//    "ボタンアクション" ^ appliedActions(defaultController) ^ bt ^
    end
  
  def createController(app: ApplicationActionHandler, mainView: MainView) =
    new MainViewController(app, mainView)
  
  def selectedRoomSpec(f: Factory) =
    "ローカルソース選択で部屋選択は None となる" ! selectedRoom(f).localSourceToNone ^
    "ローカルソース選択で展示物一覧表示となる" ! selectedRoom(f).localSourceLocalView ^
    "ウェブソース選択でウェブ検索表示となる" ! selectedRoom(f).webSource ^
    bt
  
  def selectedRoom(f: Factory) = new {
    val view = new MainView
    val ctrl = f(spyApplicationActionHandler, view)
    
    private def museumStructure = ctrl.sourceListCtrl.sourceStructure
    
    // ローカルソースを選択
    private def selectLocalSource() = ctrl.selectedRoom := museumStructure.localSource
    
    // ウェブソースを選択
    private def selectWebSource() = ctrl.selectedRoom := museumStructure.webSource
    
    def localSourceToNone = {
      selectLocalSource()
      
      ctrl.museumExhibitListCtrl.userExhibitRoom() must beNone
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
  
//  def defaultController = {
//    val view = new MainView()
//    new MainViewController(view)
//  }
//  
//  def webSourceSelected = {
//    val ctrl = new MainViewController(new MainView())
//    ctrl.dataSchema = MuseumSchemaSpec.makeMock(mock[MuseumSchema])
//    ctrl.sourceListCtrl.selectedRoom :=
//      ctrl.sourceListCtrl.sourceStructure.webSource
//    ctrl
//  }
//  
//  def userRoomSelected = {
//    val ctrl = new MainViewController(new MainView())
//    ctrl.dataSchema = MuseumSchemaSpec.makeMock(mock[MuseumSchema])
//    ctrl.sourceListCtrl.selectedRoom := mock[UserExhibitRoom]
//    ctrl
//  }
//  
//  def exhibitRoomSelected = {
//    val ctrl = new MainViewController(new MainView())
//    ctrl.dataSchema = MuseumSchemaSpec.makeMock(mock[MuseumSchema])
//    ctrl.sourceListCtrl.selectedRoom := mock[ExhibitRoom]
//    ctrl
//  }
//  
//  def mainViewSpec(ctrl: => MainViewController) =
//    "ツリーにモデルの設定" ! views(ctrl).appliedTreeModel ^
//    "テーブルに展示物リストモデルの設定" ! views(ctrl).appliedExhibitTableModel ^
//    "LocalSource が選択されている" ! views(ctrl).loadlSourceSelected
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
//  def appliedActions(ctrl: => MainViewController) =
//    "addBasicRoom" ! actionBound(ctrl).addBasicRoom ^
//    "addGroupRoom" ! actionBound(ctrl).addGroupRoom ^
//    "addSmartRoom" ! actionBound(ctrl).addSmartRoom ^
//    "removeRoom" ! actionBound(ctrl).removeRoom
//  
//  class TestBase(ctrl: MainViewController) {
//    def mainView = ctrl.mainView
//    
//    def sourceListCtrl = ctrl.sourceListCtrl
//  }
//  
//  def views(ctrl: MainViewController) = new TestBase(ctrl) {
//    def appliedTreeModel =
//      mainView.sourceList.getModel must_== ctrl.sourceListCtrl.sourceListModel.treeModel
//    
//    def appliedExhibitTableModel =
//      mainView.dataTable.getModel must_== ctrl.museumExhibitListCtrl.tableModel.tableModel
//    
//    def appliedWebSourceTableModel =
//      mainView.dataTable.getModel must_== ctrl.webServiceResultCtrl.tableModel.tableModel
//    
//    def loadlSourceSelected =
//      ctrl.selectedRoom() must_== ctrl.sourceListCtrl.sourceStructure.localSource
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
//  
//  def actionBound(ctrl: MainViewController) = new TestBase(ctrl) {
//    def addBasicRoom =
//      mainView.addListBox.getAction must_== sourceListCtrl.addBasicRoomAction.peer
//    
//    def addGroupRoom =
//      mainView.addSmartBox.getAction must_== sourceListCtrl.addSamrtRoomAction.peer
//    
//    def addSmartRoom =
//      mainView.addBoxFolder.getAction must_== sourceListCtrl.addGroupRoomAction.peer
//    
//    def removeRoom =
//      mainView.removeBoxButton.getAction must_== sourceListCtrl.removeSelectedUserRoomAction.peer
//  }
}

object MainViewControllerSpec extends Mockito {
  import jp.scid.genomemuseum.model.TreeDataService
  
  def makeTreeDataServiceMock[A](service: TreeDataService[A]) {
    service.getChildren(any) returns Nil
    service.getParent(any) returns None
  }
}