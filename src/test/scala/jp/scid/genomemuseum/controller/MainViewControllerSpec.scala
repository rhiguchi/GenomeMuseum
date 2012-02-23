package jp.scid.genomemuseum.controller

import org.specs2._

import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.{view, model}
import model.{ExhibitRoom, UserExhibitRoom, MuseumSchema}
import view.MainView
import MainView.ContentsMode

class MainViewControllerSpec extends Specification with mock.Mockito {
  def is = "MainViewController" ^
    "データビューの表示モード" ^ contentsModeSpec(new MainViewController) ^
    "データビューの表示モードの変更" ^ canSetContentsMode(new MainViewController) ^
    "検索フィールド文字列" ^ searchTextModelSpec(new MainViewController) ^
    "タイトル文字列" ^ titleSpec(new MainViewController) ^
    "展示物リスト操作機" ^ museumExhibitListControllerSpec(new MainViewController) ^
    "ビュー結合" ^ canBindMainView(new MainViewController) ^
    end
  
  def contentsModeSpec(c: => MainViewController) =
    "初期値はローカルデータ表示モード" ! contentsMode(c).initial ^
    bt
  
  def canSetContentsMode(c: => MainViewController) =
    "プロパティが変更される" ! setContentsMode(c).contentsMode ^
    bt
  
  def searchTextModelSpec(c: => MainViewController) =
    "最初はモデル結合なし" ! searchTextModel(c).initial ^
    "LOCAL モードでは local と結合" ! searchTextModel(c).localMode ^
    "NCBI モードでは web と結合" ! searchTextModel(c).ncbiMode ^
    bt
  
  def titleSpec(c: => MainViewController) =
    "最初はモデル結合なし" ! title(c).initial ^
    "LOCAL モードでは local と結合" ! title(c).localMode ^
    "NCBI モードでは web と結合" ! title(c).ncbiMode ^
    bt
  
  def museumExhibitListControllerSpec(f: => MainViewController) =
//    "ソースの UserExhibitRoom が userExhibitRoom に適用" ! selectedRoom(f).userExhibitRoom ^
//    "ローカルソース選択で userExhibitRoom は None となる" ! selectedRoom(f).localSourceToNone ^
//    "ウェブソースを選択しても userExhibitRoom は変化しない" ! selectedRoom(f).webSource ^
    bt
  
  def canBindMainView(f: => MainViewController) =
    "ソースリストと結合" ! bindMainView(f).exhibitRoomListController ^
    "コンテンツビューと結合" ! bindMainView(f).museumExhibitController ^
    "ウェブ検索と結合" ! bindMainView(f).webServiceResultController ^
    "検索フィールドの結合" ! bindMainView(f).searchTextController ^
    "コンテンツモードを変更することができる。" ! bindMainView(f).contentsModeHandler ^
    "addBasicRoom ボタンアクション" ! bindMainView(f).addBasicRoom ^
    "addGroupRoom ボタンアクション" ! bindMainView(f).addGroupRoom ^
    "addSmartRoom ボタンアクション" ! bindMainView(f).addSmartRoom ^
    "removeRoom ボタンアクション" ! bindMainView(f).removeRoom ^
    bt
  
  /** データビューの表示モード */
  def contentsMode(ctrl: MainViewController) = new {
    private def model = ctrl.contentsMode
    
    def initial = model() must_== ContentsMode.LOCAL
  }
  
  /** データビューの表示モードの変更 */
  def setContentsMode(ctrl: MainViewController) = new {
    private def setNcbi() = ctrl.setContentsMode(ContentsMode.NCBI)
    private def setLocal() = ctrl.setContentsMode(ContentsMode.LOCAL)
    
    def contentsMode = {
      setNcbi()
      val a = ctrl.contentsMode()
      setLocal()
      val b = ctrl.contentsMode()
      Seq(b, a) must_== Seq(ContentsMode.LOCAL, ContentsMode.NCBI)
    }
  }
  
  /** 検索フィールド文字列 */
  def searchTextModel(ctrl: MainViewController) = new {
    private def model = ctrl.searchText
    
    def initial = model.getSubject must beNull
    
    def localMode = {
      ctrl.setContentsMode(ContentsMode.LOCAL)
      model.getSubject must_== ctrl.museumExhibitController.title
    }
    
    def ncbiMode = {
      ctrl.setContentsMode(ContentsMode.NCBI)
      model.getSubject must_== ctrl.webServiceResultController.searchTextModel
    }
  }
  
  /** タイトル文字列 */
  def title(ctrl: MainViewController) = new {
    private def model = ctrl.title
    
    def initial = model.getSubject must beNull
    
    def localMode = {
      ctrl.setContentsMode(ContentsMode.LOCAL)
      model.getSubject must_== ctrl.museumExhibitController.searchText
    }
    
    def ncbiMode = {
      ctrl.setContentsMode(ContentsMode.NCBI)
      model.getSubject must_== ctrl.webServiceResultController.taskMessage
    }
  }
  
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
  
  
  /** ビュー結合 */
  def bindMainView(c: MainViewController) = new {
    val ctrl = spy(c)
    ctrl.exhibitRoomListController returns spy(c.exhibitRoomListController)
    ctrl.museumExhibitController returns spy(c.museumExhibitController)
    ctrl.webServiceResultController returns spy(c.webServiceResultController)
    ctrl.searchTextController returns spy(c.searchTextController)
    
    private def sourceListCtrl = ctrl.exhibitRoomListController
    
    val view = spy(new MainView)
    ctrl.bind(view)
    
    def exhibitRoomListController =
      there was one(ctrl.exhibitRoomListController).bindTree(view.sourceList)
    
    def museumExhibitController =
      there was one(ctrl.museumExhibitController).bind(view.exhibitListView)
    
    def webServiceResultController =
      there was one(ctrl.webServiceResultController).bindTable(view.websearchTable)
    
    def searchTextController =
      there was one(ctrl.searchTextController).bindTextComponent(view.quickSearchField) then
        one(ctrl.searchTextController).setModel(view.quickSearchField.getDocument)
    
    def contentsModeHandler = {
      ctrl.setContentsMode(ContentsMode.NCBI)
      ctrl.setContentsMode(ContentsMode.LOCAL)
      there was
        atLeastOne(view).setContentsMode(MainView.ContentsMode.LOCAL) then
        atLeastOne(view).setContentsMode(MainView.ContentsMode.NCBI)
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
}
