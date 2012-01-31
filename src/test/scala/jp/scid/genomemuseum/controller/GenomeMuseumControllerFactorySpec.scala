package jp.scid.genomemuseum.controller

import jp.scid.genomemuseum.{model, GenomeMuseumGUI}
import model.{MuseumExhibitServiceMock, UserExhibitRoomServiceMock, MuseumSchemaMock}

import org.specs2._

class GenomeMuseumControllerFactorySpec extends Specification with mock.Mockito {
  private type Factory = GenomeMuseumGUI => GenomeMuseumControllerFactory
  
  def is = "GenomeMuseumControllerFactory" ^
    "ソースリスト操作オブジェクト作成" ^ canCreateExhibitRoomListController(createFactory) ^
    "展示物リスト操作オブジェクト作成" ^ canCreateMuseumExhibitListController(createFactory) ^
    "ウェブ検索操作オブジェクト作成" ^ canCreateWebServiceResultController(createFactory) ^
    "主画面操作オブジェクト作成" ^ canCreateMainViewController(createFactory) ^
    "主画面枠操作管理オブジェクト作成" ^ canCreateMainFrameViewController(createFactory) ^
    end
  
  def createFactory(application: GenomeMuseumGUI) =
    new GenomeMuseumControllerFactory(application)
  
  def canCreateExhibitRoomListController(f: Factory) =
    "作成" ! createExhibitRoomListController(f).create ^
    "アプリケーションの roomService が設定されている" ! createExhibitRoomListController(f).roomService ^
    "loadManager が設定されている" ! createExhibitRoomListController(f).loadManager ^
    bt
  
  def canCreateMuseumExhibitListController(f: Factory) =
    "作成" ! createMuseumExhibitListController(f).create ^
    "アプリケーションの loadManager が設定されている" ! createMuseumExhibitListController(f).loadManager ^
    bt
  
  def canCreateWebServiceResultController(f: Factory) =
    "作成" ! createWebServiceResultController(f).create ^
    "アプリケーションの loadManager が設定されている" ! createWebServiceResultController(f).loadManager ^
    bt
  
  def canCreateMainViewController(f: Factory) =
    "作成" ! createMainViewController(f).create ^
    "ソースリスト操作オブジェクトが利用されている" ! createMainViewController(f).roomCtrl ^
    "展示物リスト操作オブジェクトが利用されている" ! createMainViewController(f).exhibitCtrl ^
    "ウェブ検索リスト操作オブジェクトが利用されている" ! createMainViewController(f).webCtrl ^
    bt
  
  def canCreateMainFrameViewController(f: Factory) =
    "作成" ! createMainFrameViewController(f).create ^
    "application が適用" ! createMainFrameViewController(f).application ^
    "mainViewCtrl が適用" ! createMainFrameViewController(f).mainViewController ^
    "mainViewCtrl.title がタイトルと接続" ! createMainFrameViewController(f).connectTitle ^
    bt
  
  class TestBase(f: Factory) {
    val loadManagerMock = mock[MuseumExhibitLoadManager]
    val roomServiceMcok = UserExhibitRoomServiceMock.of()
    val exhibitServiceMock = MuseumExhibitServiceMock.of()
    val schema = MuseumSchemaMock.of(roomServiceMcok, exhibitServiceMock)
    val app = mock[GenomeMuseumGUI]
    doAnswer(_ => schema).when(app).museumSchema
    doAnswer(_ => loadManagerMock).when(app).exhibitLoadManager
    
    val factory = spy(f(app))
  }
  
  /** ソースリスト操作オブジェクト */
  def createExhibitRoomListController(f: Factory) = new TestBase(f) {
    val ctrl = factory.createExhibitRoomListController()
    
    def create = ctrl must beAnInstanceOf[ExhibitRoomListController]
    
    def roomService = ctrl.roomService must_== roomServiceMcok
    
    def loadManager = ctrl.loadManager must_== loadManagerMock
  }
  
  /** 展示物リスト操作オブジェクト */
  def createMuseumExhibitListController(f: Factory) = new TestBase(f) {
    val ctrl = factory.createMuseumExhibitListController()
    
    def create = ctrl must beAnInstanceOf[MuseumExhibitListController]
    
    def loadManager = ctrl.loadManager must beSome(loadManagerMock) 
  }
  
  /** ウェブ検索操作オブジェクト */
  def createWebServiceResultController(f: Factory) = new TestBase(f) {
    val ctrl = factory.createWebServiceResultController()
    
    def create = ctrl must beAnInstanceOf[WebServiceResultController]
    
    def loadManager = ctrl.loadManager must beSome(loadManagerMock) 
  }
  
  /** 主画面操作オブジェクト */
  def createMainViewController(f: Factory) = new TestBase(f) {
//    val application = spy(app)
//    
//    application.createExhibitRoomListController returns
//      spy(app.createExhibitRoomListController)
//    application.createMuseumExhibitListController returns
//      spy(app.createMuseumExhibitListController)
//    application.createWebServiceResultController returns
//      spy(app.createWebServiceResultController)
    val ctrl = factory.createMainViewController()
    
    def create = ctrl must beAnInstanceOf[MainViewController]
    
    def roomCtrl = todo // ctrl.sourceListCtrl must_== application.createExhibitRoomListController

    def exhibitCtrl = todo //application.createMainViewController
//      .museumExhibitListCtrl must_== application.createMuseumExhibitListController

    def webCtrl = todo //application.createMainViewController
//      .webServiceResultCtrl must_== application.createWebServiceResultController
  }
  
  /** 主画面枠操作管理オブジェクト作成 */
  def createMainFrameViewController(f: Factory) = new TestBase(f) {
    def ctrl = factory.createMainFrameViewController
    
    def create = ctrl must beAnInstanceOf[MainFrameViewController]
    
    def application = ctrl.application must beSome(factory.application)
    
    def mainViewController = ctrl.mainViewController must beSome
    
    def connectTitle = {
      factory.newMainFrameViewController returns spy(new MainFrameViewController)
      val mainViewCtrl = factory.createMainViewController
      
      doAnswer(_ => mainViewCtrl).when(factory).createMainViewController
      there was one(ctrl).connectTitle(mainViewCtrl.title)
    }
  }
}
