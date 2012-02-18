package jp.scid.genomemuseum.controller

import javax.swing.{JComponent, JProgressBar, JLabel}

import org.specs2._

import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.{view, gui, model, GenomeMuseumGUI}
import model.{ExhibitRoom, UserExhibitRoom, MuseumSchema, UserExhibitRoomService, MuseumExhibitService}
import model.{UserExhibitRoomServiceMock, MuseumExhibitServiceMock}
import view.MainView

class MainViewControllerSpec extends Specification with mock.Mockito {
  def is = "MainViewController" ^
    "展示物リスト操作機" ^ museumExhibitListControllerSpec(new MainViewController) ^
    "ソース選択モデル" ^ dataListControllerSpec(new MainViewController) ^
    "進捗ビュー結合" ^ canBindProgressView(new MainViewController) ^
    "ビュー結合" ^ canBindMainView(new MainViewController) ^
    end
  
  def museumExhibitListControllerSpec(f: => MainViewController) =
//    "ソースの UserExhibitRoom が userExhibitRoom に適用" ! selectedRoom(f).userExhibitRoom ^
//    "ローカルソース選択で userExhibitRoom は None となる" ! selectedRoom(f).localSourceToNone ^
//    "ウェブソースを選択しても userExhibitRoom は変化しない" ! selectedRoom(f).webSource ^
    bt
  
  def dataListControllerSpec(f: => MainViewController) =
//    "初期値は exhibitList" ! dataListController(f).init ^
//    "exhibitRoomListController で webSource を選択すると webResult" ! dataListController(f).webSource ^
//    "exhibitRoomListController で UserExhibitRoom を選択すると exhibitList" ! dataListController(f).userRoom ^
    bt

  def canBindProgressView(f: => MainViewController) =
//    "表示・非表示結合" ! bindProgressView(f).progressViewVisibled ^
//    "表示・非表示切り替え" ! bindProgressView(f).progressViewVisibledBind ^
//    "メッセージ表示" ! bindProgressView(f).progressMessage ^
//    "メッセージ変化" ! bindProgressView(f).progressMessageBind ^
//    "最大値設定" ! bindProgressView(f).progressMaximum ^
//    "最大値変化" ! bindProgressView(f).progressMaximumBind ^
//    "現在値設定" ! bindProgressView(f).progressValue ^
//    "現在値変化" ! bindProgressView(f).progressValueBind ^
//    "不定状態設定" ! bindProgressView(f).progressIndeterminate ^
//    "不定状態変化" ! bindProgressView(f).progressIndeterminateBind ^
    bt
  
  def canBindMainView(f: => MainViewController) =
//    "ソースリストと結合" ! bindMainView(f).exhibitRoomListController ^
//    "コンテンツビューと結合" ! bindMainView(f).museumExhibitController ^
//    "進捗画面と結合" ! bindMainView(f).bindProgressView ^
//    "コンテンツモードを変更することができる。" ! bindMainView(f).contentsModeHandler ^
//    "addBasicRoom ボタンアクション" ! bindMainView(f).addBasicRoom ^
//    "addGroupRoom ボタンアクション" ! bindMainView(f).addGroupRoom ^
//    "addSmartRoom ボタンアクション" ! bindMainView(f).addSmartRoom ^
//    "removeRoom ボタンアクション" ! bindMainView(f).removeRoom ^
    bt
  
  def selectedRoom(ctrl: MainViewController) = new {
//    private def roomCtrl = ctrl.exhibitRoomListController
//    private def exhibitCtrl = ctrl.museumExhibitListController
//    private def museumStructure = roomCtrl.sourceStructure
//    val mockRoom = mock[UserExhibitRoom]
//    
//    def userExhibitRoom = {
//      roomCtrl.selectedRoom := mockRoom
//      exhibitCtrl.userExhibitRoom must beSome(mockRoom)
//    }
//    
//    def localSourceToNone = {
//      roomCtrl.selectedRoom := museumStructure.localSource
//      exhibitCtrl.userExhibitRoom must beNone
//    }
//    
//    def webSource = {
//      roomCtrl.selectedRoom := mockRoom
//      roomCtrl.selectedRoom := museumStructure.webSource
//      exhibitCtrl.userExhibitRoom must beSome(mockRoom)
//    }
  }
  
  def dataListController(ctrl: MainViewController) = new {
//    def init = ctrl.dataListController() must_== ctrl.museumExhibitListController
//    
//    def webSource = {
//      ctrl.exhibitRoomListController.selectedRoom := ctrl.exhibitRoomListController.sourceStructure.webSource
//      ctrl.dataListController() must_== ctrl.webServiceResultController
//    }
//    
//    def userRoom = {
//      ctrl.exhibitRoomListController.selectedRoom := mock[UserExhibitRoom]
//      init
//    }
  }
  
  def bindProgressView(ctrl: MainViewController) = new {
//    val contentPane = mock[JComponent]
//    val progressBar = mock[JProgressBar]
//    val statusLabel = mock[JLabel]
//    ctrl.bindProgressView(contentPane, progressBar, statusLabel)
//    
//    def progressViewVisibled = there was one(contentPane).setVisible(false)
//    
//    def progressViewVisibledBind = {
//      List(true, false) foreach ctrl.progressViewVisibled.:=
//      there was two(contentPane).setVisible(false) then one(contentPane).setVisible(true)
//    }
//    
//    def progressMessage = there was one(statusLabel).setText("")
//    
//    def progressMessageBind = {
//      List("text", "text2") foreach ctrl.progressMessage.:=
//      there was one(statusLabel).setText("text") then one(statusLabel).setText("text2")
//    }
//    
//    def progressMaximum = there was one(progressBar).setMaximum(0)
//    
//    def progressMaximumBind = {
//      List(20, 50) foreach ctrl.progressMaximum.:=
//      there was one(progressBar).setMaximum(20) then one(progressBar).setMaximum(50)
//    }
//    
//    def progressValue = there was one(progressBar).setValue(0)
//    def progressValueBind = {
//      List(10, 20) foreach ctrl.progressValue.:=
//      there was one(progressBar).setValue(10) then one(progressBar).setValue(20)
//    }
//    
//    def progressIndeterminate = there was one(progressBar).setIndeterminate(false)
//    def progressIndeterminateBind = {
//      List(true, false) foreach ctrl.progressIndeterminate.:=
//      there was two(progressBar).setIndeterminate(false) then
//        one(progressBar).setIndeterminate(true)
//    }
  }
  
  def bindMainView(c: MainViewController) = new {
    val ctrl = spy(c)
    ctrl.exhibitRoomListController returns spy(ctrl.exhibitRoomListController)
    ctrl.museumExhibitController returns spy(ctrl.museumExhibitController)
//    private val exhibitListCtrl = spy(ctrl.museumExhibitListController)
//    private val webResultCtrl = spy(ctrl.webServiceResultController)
//    
//    ctrl.exhibitRoomListController returns exhibitRoomListController
//    ctrl.museumExhibitListController returns exhibitListCtrl
//    ctrl.webServiceResultController returns webResultCtrl
//    ctrl.dataListController := exhibitListCtrl
//    doAnswer(_ => Unit).when(ctrl).bindProgressView(any, any, any)
//    
//    val view = spy(new MainView)
//    ctrl.bind(view)
//    
//    def exhibitRoomListController =
//      there was one(ctrl.exhibitRoomListController).bindTree(view.sourceList)
//    
//    def museumExhibitController =
//      there was one(ctrl.museumExhibitController).bind(view.exhibitListView)
//      
//    def bindProgressView = {
//      there was one(ctrl).bindProgressView(view.fileLoadingActivityPane,
//        view.fileLoadingProgress, view.fileLoadingStatus)
//    }
//    
//    def contentsModeHandler = {
//      ctrl.contentsMode setValue MainView.ContentsMode.LOCAL
//      ctrl.contentsMode setValue MainView.ContentsMode.NCBI
//      
//      there was
//        one(view).setContentsMode(MainView.ContentsMode.LOCAL) then
//        one(view).setContentsMode(MainView.ContentsMode.NCBI)
//    }
//    
//    def addBasicRoom = view.addListBox.getAction must_==
//      sourceListCtrl.addBasicRoomAction.peer
//    
//    def addGroupRoom = view.addSmartBox.getAction must_==
//      sourceListCtrl.addSamrtRoomAction.peer
//    
//    def addSmartRoom = view.addBoxFolder.getAction must_==
//      sourceListCtrl.addGroupRoomAction.peer
//    
//    def removeRoom = view.removeBoxButton.getAction must_==
//      sourceListCtrl.removeSelectedUserRoomAction.peer
  }
}
