package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import jp.scid.genomemuseum.view
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumSchema,
  MuseumExhibitService, UserExhibitRoomService, RoomExhibitService}
import view.MainView
import MuseumExhibitListController.TableSource._

class MainViewControllerSpec extends Specification with Mockito {
  def is = "MainViewController" ^
    "mainView" ^ pending
      "sourceList にモデルが設定" ! initial.s1 ^
      "dataTable にモデルが設定" ! initial.s2 ^
      "初期状態で LocalSource 表示モード" ! initial.s3 ^
    bt ^ "ソース選択" ^
      "ウェブソース選択で WebSource 表示モード" ! sourceSelect.s1 ^
      "ローカルソース選択で LocalSource 表示モード" ! sourceSelect.s2 ^
      "WebSource モードで部屋を選択すると LocalSource 表示モード" ! sourceSelect.s3 ^
    bt ^ "sourceListCtrl 状態" ^
      "userExhibitRoomService が dataSchema#userExhibitRoomService である" ! slctrl.s1 ^
    bt ^ "dataTableCtrl 状態" ^
      "localDataService が dataSchema#museumExhibitService である" ! dtctrl.s1 ^
      "部屋を選択するとその部屋の RoomExhibitService が localDataService に設定される" ! dtctrl.s2
  
  class TestBase {
    val mainview = new MainView
    mainview.sourceList.setModel(null)
    val ctrl = new MainViewController(mainview)
    
    def tableSource = ctrl.dataTableCtrl.tableSource
    
    def sourceStructure = ctrl.sourceListCtrl.sourceStructure
  }
  
  lazy val initial = new TestBase {
    def s1 = mainview.sourceList.getModel must not beNull
    
    def s2 = mainview.dataTable.getModel.getColumnCount must be_>=(10)
    
    def s3 = tableSource must_== LocalSource
  }
  
  lazy val sourceSelect = new TestBase {
    // ウェブソースを選択
    val s1_mode = sourceSelectAndGetCurrentSource(sourceStructure.webSource)
    // ローカルソースを選択
    val s2_mode = sourceSelectAndGetCurrentSource(sourceStructure.localSource)
    // WebSource モードで部屋を選択
    ctrl.dataTableCtrl.tableSource = WebSource
    val s3_mode = sourceSelectAndGetCurrentSource(UserExhibitRoom("room"))
    
    /** ソースを選択して現在のテーブルソース型を返す */
    def sourceSelectAndGetCurrentSource(room: ExhibitRoom) = {
      ctrl.sourceListCtrl.selectedRoom := room
      tableSource
    }
    
    def s1 = s1_mode must_== WebSource
    
    def s2 = s2_mode must_== LocalSource
    
    def s3 = s3_mode must_== LocalSource
  }
  
  class WithSource extends TestBase {
    import MainViewControllerSpec.makeTreeDataServiceMock
    // MuseumSchema 用 UserExhibitRoomService 構築
    val userExhibitRoomService = mock[UserExhibitRoomService]
    makeTreeDataServiceMock(userExhibitRoomService)
    // MuseumSchema 用 MuseumExhibitService 構築
    val museumExhibitService = mock[MuseumExhibitService]
    museumExhibitService.allElements returns Nil
    
    val roomExhibitService = mock[RoomExhibitService]
    museumExhibitService.allElements returns Nil
    
    // dataSchema 用 MuseumSchema 構築
    val schema = mock[MuseumSchema]
    schema.museumExhibitService returns museumExhibitService
    schema.userExhibitRoomService returns userExhibitRoomService
    schema.roomExhibitService(any) returns roomExhibitService
    
    // dataSchema 設定
    ctrl.dataSchema = schema
  }
  
  lazy val slctrl = new WithSource {
    def s1 = ctrl.sourceListCtrl.userExhibitRoomService must_== userExhibitRoomService
  }
  
  lazy val dtctrl = new WithSource {
    private val initialLocalDataService = ctrl.dataTableCtrl.localDataService
    
    // 部屋の service を構築
    val testRoom = UserExhibitRoom("testRoom")
    val testRoomExhibitService = mock[RoomExhibitService]
    schema.roomExhibitService(testRoom) returns testRoomExhibitService
    
    // 部屋を選択
    ctrl.sourceListCtrl.selectedRoom := testRoom
    private val testRoomLocalDataService = ctrl.dataTableCtrl.localDataService
    
    def s1 = initialLocalDataService must_== museumExhibitService
    
    def s2 = testRoomLocalDataService must_== testRoomExhibitService
  }
}

object MainViewControllerSpec extends Mockito {
  import jp.scid.genomemuseum.model.TreeDataService
  
  def makeTreeDataServiceMock[A](service: TreeDataService[A]) {
    service.getChildren(any) returns Nil
    service.getParent(any) returns None
  }
}