package jp.scid.genomemuseum

import java.io.File
import util.control.Exception.allCatch

import org.specs2._

import jp.scid.gui.ValueHolder
import controller.{GenomeMuseumController, MainViewController, MainFrameViewController,
  ExhibitRoomListController, MuseumExhibitListController, WebServiceResultController}
import model.{MuseumSchema, MuseumExhibitService, DefaultMuseumExhibitFileLibrary,
  UriFileStorage}
import view.{MainFrameView, MainViewMenuBar}
import GenomeMuseumGUI._
import RunMode._

@org.junit.runner.RunWith(classOf[runner.JUnitRunner])
class GenomeMuseumGUISpec extends Specification with mock.Mockito {
  def is = "GenomeMuseumGUI" ^
    "初期化動作" ^ initializeSpec(appSimple) ^
    "プロパティ" ^ propertiesSpec(appSimple) ^
    "スキーマブジェクト" ^ museumSchemaSpec(appSimple) ^
    "アクション" ^ actionsSpec(appSimple) ^
    "ファイルライブラリオブジェクト" ^ exhibitFileLibrarySpec(appSimple) ^
    "読み込み操作オブジェクト" ^ exhibitLoadManagerSpec(appSimple) ^
    "メニューバーと結合" ^ canBindMenuBar(appSimple) ^
    "主画面操作の作成" ^ canCreateMainViewController(appSimple) ^
    "主画面操作の作成" ^ canCreateMainFrameViewController(appSimple) ^
    "起動処理" ^ startupSpec(appSimple) ^
    end
  
  def appSimple = new GenomeMuseumGUI()
  
  def initializeSpec(app: => GenomeMuseumGUI) = sequential ^
    "applicationHome が一時ディレクトリ上に設定される" ! initialize(app).setsApplicationHomeOnTmpdir ^
    "--LocalLibrary引数" ^ initializeArgLocalLibrarySpec(app) ^
    bt
  
  def initializeArgLocalLibrarySpec(app: => GenomeMuseumGUI) =
    "applicationHome がユーザーホーム以下に設定される" ! initializeArgLocalLibrary(app).setsApplicationHome ^
    "databaseSource が applicationHome 以下に設定される" ! initializeArgLocalLibrary(app).databaseSource ^
    "fileStorageDir が applicationHome 以下に設定される" ! initializeArgLocalLibrary(app).fileStorageDir ^
    bt
  
  def propertiesSpec(app: => GenomeMuseumGUI) =
    "databaseSource 初期値" ! properties(app).databaseSourceGet ^
    "databaseSource 設定と取得" ! properties(app).databaseSourceSet ^
    "fileStorageDir 初期値" ! properties(app).fileStorageDirGet ^
    "fileStorageDir 設定と取得" ! properties(app).fileStorageDirSet ^
    bt
  
  def museumSchemaSpec(app: => GenomeMuseumGUI) =
    "作成" ! museumSchema(app).create ^
    "ローカルファイルに作成" ! museumSchema(app).toFile ^
    bt
  
  def actionsSpec(app: => GenomeMuseumGUI) =
    "cutProxyAction" ! actions(app).cutProxyAction ^
    "copyProxyAction" ! actions(app).copyProxyAction ^
    "pasteProxyAction" ! actions(app).pasteProxyAction ^
    "selectAllProxyAction" ! actions(app).selectAllProxyAction ^
    "openAction" ! actions(app).openAction ^
    "quitAction" ! actions(app).quitAction ^
    bt
  
  def exhibitFileLibrarySpec(app: => GenomeMuseumGUI) =
    "fileStorageDir が None の時は None" ! exhibitFileLibrary(app).noLibrary ^
    "fileStorageDir のディレクトリで作成される" ! exhibitFileLibrary(app).fromAttr ^
    bt
  
  def exhibitLoadManagerSpec(app: => GenomeMuseumGUI) =
//    "作成" ! exhibitLoadManager(app).create ^
//    "dataService に museumSchema のものが利用される" ! exhibitLoadManager(app).dataService ^
//    "fileLibrary に exhibitFileLibrary が利用される" ! exhibitLoadManager(app).fileLibrary ^
    bt

  def canBindMenuBar(c: => GenomeMuseumGUI) =
    "ファイルメニュー" ^ canBindFileMenu(c) ^
    "編集メニュー" ^ canBindEditMenu(c) ^
    bt
  
  def canBindFileMenu(c: => GenomeMuseumGUI) = 
    "開く" ! bindFileMenu(c).open ^
    "終了" ! bindFileMenu(c).quit ^
    bt
  
  def canBindEditMenu(c: => GenomeMuseumGUI) = 
    "カット" ! bindEditMenu(c).cut ^
    "コピー" ! bindEditMenu(c).copy ^
    "ペースト" ! bindEditMenu(c).paste ^
    "全てを選択" ! bindEditMenu(c).selectAll ^
    bt
  
  def canCreateMainViewController(app: => GenomeMuseumGUI) =
    "モデルと結合される" ! createMainViewController(app).setMuseumStructure ^
    "読み込みマネージャと結合される" ! createMainViewController(app).setExhibitLoadManager ^
    bt
  
  def canCreateMainFrameViewController(app: => GenomeMuseumGUI) =
    "主画面操作を使って構築" ! createMainFrameViewController(app).usesMainViewCtrlFactory ^
    bt
  
  def startupSpec(app: => GenomeMuseumGUI) =
    "主画面枠と結合" ! startup(app).bindView ^
    "メニューバーと結合" ! startup(app).bindMenuBar ^
    "画面枠を表示" ! startup(app).showsFrame ^
    bt
  
  /** 初期化処理 */
  def initialize(application: GenomeMuseumGUI) = new {
    application.initialize(Array.empty[String])
    
    def setsApplicationHomeOnTmpdir =
      application.applicationHome.getAbsolutePath must startWith(System.getProperty("java.io.tmpdir"))
  }
  
  /** 初期化処理 LocalLibrary 引数 */
  def initializeArgLocalLibrary(application: GenomeMuseumGUI) = new {
    application.initialize(Array("--LocalLibrary"))
    
    def setsApplicationHome =
      application.applicationHome.getAbsolutePath must startWith(System.getProperty("user.home"))
    
    def fileStorageDir = application.fileStorageDir must
      beSome(new File(application.applicationHome, "BioFiles"))
    
    def databaseSource = application.databaseSource must_==
      new File(application.applicationHome, "Library/lib").toString
  }
  
  /** プロパティ */
  def properties(application: GenomeMuseumGUI) = new {
    def databaseSourceGet = application.databaseSource must startWith("mem:")
    
    def databaseSourceSet = {
      application.databaseSource = "test/path"
      application.databaseSource must_== "test/path"
    }
    
    def fileStorageDirGet = application.fileStorageDir must beNone
    def fileStorageDirSet = {
      val file = new File("file")
      application.fileStorageDir = Some(file)
      application.fileStorageDir must beSome(file)
    }
  }
  
  /** スキーマオブジェクト */
  def museumSchema(app: GenomeMuseumGUI) = new {
    def create = app.museumSchema must not beNull
    
    def toFile = {
      val tempFile = File.createTempFile("h2database", null)
      app.databaseSource = "file:" + tempFile.getAbsolutePath
      app.museumSchema
      tempFile.delete
      new File(tempFile.getPath + ".h2.db").exists must beTrue
    }
  }
  
  /** スキーマオブジェクト */
  def actions(app: GenomeMuseumGUI) = new {
    def cutProxyAction = app.cutProxyAction.name must_== "cut"
    def copyProxyAction = app.copyProxyAction.name must_== "copy"
    def pasteProxyAction = app.pasteProxyAction.name must_== "paste"
    def selectAllProxyAction = app.selectAllProxyAction.name must_== "selectAll"
    def openAction = app.openAction.name must_== "open"
    def quitAction = app.quitAction.name must_== "quit"
  }
  
  /** ファイルライブラリオブジェクト作成 */
  def exhibitFileLibrary(app: GenomeMuseumGUI) = new {
    def noLibrary = {
      app.fileStorageDir = None
      app.exhibitFileLibrary must beNone
    }
    
    def fromAttr = {
      val dir = createTempDir()
      app.fileStorageDir = Some(dir)
      app.exhibitFileLibrary must beSome[DefaultMuseumExhibitFileLibrary].which(_.baseDir == dir)
    }
  }
  
  /** 読み込み操作オブジェクト */
//  def exhibitLoadManager(app: GenomeMuseumGUI) = new {
//    val application = spy(app)
//    
//    def create = app.exhibitLoadManager must not beNull
//    
//    def dataService = {
//      val schema = mock[MuseumSchema]
//      val exhibitService = mock[MuseumExhibitService]
//      schema.museumExhibitService returns exhibitService
//      
//      doAnswer(_ => schema).when(application).museumSchema
//      application.exhibitLoadManager.dataService must_== exhibitService
//    }
//    
//    def fileLibrary = {
//      val libraryMock = mock[DefaultMuseumExhibitFileLibrary]
//      doAnswer(_ => Some(libraryMock)).when(application).exhibitFileLibrary
//      application.exhibitLoadManager.fileLibrary must beSome(libraryMock)
//    }
//  }
  
  /** 主画面操作の作成 */
  def createMainViewController(app: GenomeMuseumGUI) = new {
    val ctrl = app.createMainViewController
    
    def setMuseumStructure = ctrl.museumStructure must_== app.museumStructure
    def setExhibitLoadManager = todo
  }
  
  /** 主画面枠操作の作成 */
  def createMainFrameViewController(app: GenomeMuseumGUI) = new {
    val mainViewCtrl = app.createMainViewController
    
    val application = spy(app)
    application.createMainViewController returns mainViewCtrl
    
    val ctrl = application.createMainFrameViewController
    
    def usesMainViewCtrlFactory = ctrl.mainViewController must_== mainViewCtrl
  }
  
  /** 起動時処理 */
  def startup(app: GenomeMuseumGUI) = new {
    val mainFrameViewCtrl = mock[MainFrameViewController]
    val mainFrameView = app.createMainFrameView
    
    val application = spy(app)
    application.createMainFrameViewController returns mainFrameViewCtrl
    application.createMainFrameView returns mainFrameView
    
    application.startup()
    
    def bindView = there was one(mainFrameViewCtrl).bind(mainFrameView)
    
    def bindMenuBar = there was one(application).bindMenuBar(mainFrameView.mainMenu)
    
    def showsFrame = there was one(mainFrameViewCtrl).show()
  }
  
  /** ファイルメニュー */
  def bindFileMenu(application: GenomeMuseumGUI) = new {
    val menuBar = new MainViewMenuBar
    application.bindMenuBar(menuBar)
    
    def open = menuBar.open.action must_== application.openAction
    
    def quit = menuBar.quit.action must_== application.quitAction
  }
  
  /** 編集メニュー */
  def bindEditMenu(application: GenomeMuseumGUI) = new {
    val menuBar = new MainViewMenuBar
    application.bindMenuBar(menuBar)
    
    def cut = menuBar.cut.action must_== application.cutProxyAction
    
    def copy = menuBar.copy.action must_== application.copyProxyAction
    
    def paste = menuBar.paste.action must_== application.pasteProxyAction
    
    def selectAll = menuBar.selectAll.action must_== application.selectAllProxyAction
  }
}
