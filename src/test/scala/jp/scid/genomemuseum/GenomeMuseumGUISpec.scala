package jp.scid.genomemuseum

import java.io.File
import util.control.Exception.allCatch

import org.specs2._

import controller.{GenomeMuseumController, MainViewController, MainFrameViewController,
  ApplicationActionHandler}
import model.{MuseumSchema, LibraryFileManager, MuseumExhibitService}
import view.ApplicationViews
import GenomeMuseumGUI._
import RunMode._

@org.junit.runner.RunWith(classOf[runner.JUnitRunner])
class GenomeMuseumGUISpec extends Specification with mock.Mockito {
  def is = "GenomeMuseumGUI" ^
    "初期化動作" ^ initializeSpec(appSimple) ^
    "プロパティ" ^ propertiesSpec(appSimple) ^
    "スキーマの作成" ^ createsSchemaSpec(appSimple) ^
    "ファイル管理オブジェクトの作成" ^ createsFileStorageSpec(appSimple) ^
    "ビューの作成" ^ createApplicationViewsSpec(appSimple) ^
    "操作オブジェクトの作成" ^ createApplicationHandlerSpec(appSimple) ^
    "起動処理" ^ startupSpec(appSimple) ^
    "オブジェクト" ^ companionObjectSpec ^
    end
  
  def appSimple = {
    new GenomeMuseumGUI(Testing)
  }
  
  def initializeSpec(app: => GenomeMuseumGUI) = sequential ^
    "applicationHome が一時ディレクトリ上に設定される" ! initialize(app).setsApplicationHomeOnTmpdir ^
    "fileStorageDir が設定される" ! initialize(app).setsFileStorageDir ^
    "--LocalLibrary引数" ^ initializeArgLocalLibrarySpec(app) ^
    bt
  
  def initializeArgLocalLibrarySpec(app: => GenomeMuseumGUI) =
    "applicationHome がユーザーホーム以下に設定される" ! initializeArgLocalLibrary(app).setsApplicationHome ^
    "schemaFileSource が設定される" ! initializeArgLocalLibrary(app).setsFileStorageDir ^
    bt
  
  def propertiesSpec(app: => GenomeMuseumGUI) =
    "schemaFileSource 初期値" ! properties(app).schemaFileSourceGet ^
    "schemaFileSource 設定と取得" ! properties(app).schemaFileSourceSet ^
    "fileStorageDir 初期値" ! properties(app).fileStorageDirGet ^
    "fileStorageDir 設定と取得" ! properties(app).fileStorageDirSet ^
    bt
  
  def createsSchemaSpec(app: => GenomeMuseumGUI) =
    "作成" ! createMuseumSchema(app).returnsSchema ^
    "ローカルファイルに作成" ! createMuseumSchema(app).toFile ^
    bt
  
  def createsFileStorageSpec(app: => GenomeMuseumGUI) =
    "作成" ! createsFileStorage(app).returnsStorage ^
    "fileStorageDir がディレクトリとされる" ! createsFileStorage(app).fileStorageDir ^
    bt
  
  def createApplicationViewsSpec(app: => GenomeMuseumGUI) =
    "作成" ! createApplicationViews(app).returnsView ^
    bt
  
  def createApplicationHandlerSpec(app: => GenomeMuseumGUI) =
    "作成" ! createApplicationHandler(app).returnsHandler ^
    bt
  
  def startupSpec(app: => GenomeMuseumGUI) =
    "主画面枠が表示される" ! startup(app).showMainFrameView ^
    bt
  
  def companionObjectSpec =
    "コンテキストの取得" ! companionObject.getContext
  
  /** 初期化処理 */
  def initialize(application: GenomeMuseumGUI) = new Object {
    application.initialize(Array.empty[String])
    
    def setsApplicationHomeOnTmpdir =
      application.applicationHome.getAbsolutePath must startWith(System.getProperty("java.io.tmpdir"))
    
    def setsFileStorageDir = application.fileStorageDir must
      beSome(new File(application.applicationHome, "BioFiles"))
  }
  
  /** 初期化処理 LocalLibrary 引数 */
  def initializeArgLocalLibrary(application: GenomeMuseumGUI) = new Object {
    application.initialize(Array("--LocalLibrary"))
    
    def setsApplicationHome = pending
//      application.applicationHome.getAbsolutePath must startWith(System.getProperty("user.home"))
    
    def setsFileStorageDir = application.fileStorageDir must
      beSome(new File(application.applicationHome, "BioFiles"))
    
    def schemaFileSource = application.fileStorageDir must
      beSome(new File(application.applicationHome, "Library/lib"))
  }
  
  /** プロパティ */
  def properties(application: GenomeMuseumGUI) = new Object {
    def schemaFileSourceGet = application.schemaFileSource must beNone
    
    def schemaFileSourceSet = {
      val file = new File("file")
      application.schemaFileSource = Some(file)
      application.schemaFileSource must beSome(file)
    }
    
    def fileStorageDirGet = application.fileStorageDir must beNone
    def fileStorageDirSet = {
      val file = new File("file")
      application.fileStorageDir = Some(file)
      application.fileStorageDir must beSome(file)
    }
  }
  
  /** スキーマ作成 */
  def createMuseumSchema(app: GenomeMuseumGUI) = new Object {
    def returnsSchema = app.createMuseumSchema must not beNull
    
    def toFile = {
      val tempFile = File.createTempFile("h2database", null)
      app.schemaFileSource = Some(tempFile)
      app.createMuseumSchema
      tempFile.delete
      new File(tempFile.getPath + ".h2.db").exists must beTrue
    }
  }
  
  /** ファイル管理オブジェクト作成 */
  def createsFileStorage(app: GenomeMuseumGUI) = new Object {
    def returnsStorage = app.createFileStorage must not beNull
    
    def fileStorageDir = {
      app.fileStorageDir = Some(createTempDir())
      app.createFileStorage.baseDir must_== app.fileStorageDir.get
    }
  }
  
  /** ビュー作成 */
  def createApplicationViews(app: GenomeMuseumGUI) = new Object {
    def returnsView =
      app.createApplicationViews() must not beNull
  }
  
  /** アプリケーション操作オブジェクトの作成 */
  def createApplicationHandler(app: GenomeMuseumGUI) = new Object {
    def returnsHandler =
      app.createApplicationHandler() must not beNull
  }
  
  /** 起動時処理 */
  def startup(app: GenomeMuseumGUI) = new Object {
    val applicationHandlerMock = mock[ApplicationActionHandler]
    
    val application = spy(app)
    doAnswer(_ => applicationHandlerMock).when(application)
      .createApplicationHandler()
    
    application.startup
    
    def showMainFrameView =
      there was one(applicationHandlerMock).showMainFrameView
  }
  
  def companionObject = new Object {
    def getContext = {
      allCatch.opt(GenomeMuseumGUI.applicationContext) must beSome
    }
  }
}
