package jp.scid.genomemuseum.controller

import java.awt.FileDialog
import java.io.File

import org.specs2._

import jp.scid.genomemuseum.{view, model, GenomeMuseumGUI}
import view.ApplicationViews
import model.{MuseumSchema, LibraryFileManager}
  
class ApplicationActionHandlerSpec extends Specification with mock.Mockito {
  private type Factory = GenomeMuseumGUI => ApplicationActionHandler
  
  def is = "ApplicationActionHandler" ^
    "コンストラクタ" ^ constructorSpec(createHandler) ^
    "ファイルを選んで読み込む" ^ canChooseAndLoad(createHandler) ^ 
    "主画面枠の表示" ^ canShowMainFrameView(createHandler) ^
    "openAction" ^ openActionSpec(createHandler) ^
    "quitAction" ^ quitActionSpec(createHandler) ^
    end
  
  
  def createHandler(parent: GenomeMuseumGUI) = {
    new ApplicationActionHandler(parent)
  }
  
  def constructorSpec(f: Factory) =
    "スキーマを作成" ! constructor(f).createsSchema ^
    "ファイル管理オブジェクトを作成" ! constructor(f).createsFileStorage ^
    "ファイル読み込みオブジェクト作成" ! constructor(f).createsLoader ^
    "主画面枠操作オブジェクト作成" ! constructor(f).createsController ^
    bt
  
  def canChooseAndLoad(f: Factory) =
    "ビューのダイアログを表示" ! chooseAndLoad(f).showsDialog ^
    "ファイルが選択されたときは読み込み処理を呼び出す" ! chooseAndLoad(f).callsLoad ^
    "ファイルが選択されなかったときは読み込まない" ! chooseAndLoad(f).notCallsLoad ^
    bt
  
  def canShowMainFrameView(f: Factory) =
    "コントローラの show をコール" ! showMainFrameView(f).show ^
    bt
  
  def openActionSpec(f: Factory) =
    "chooseAndLoad をコール" ! openAction(f).calls ^
    bt
  
  def quitActionSpec(f: Factory) =
    "Application#exit をコール" ! quitAction(f).calls ^
    bt
  
  private def mockApplication() = {
    val schemaMock = mock[MuseumSchema]
    val fileStorageMock = mock[LibraryFileManager]
    val view = new ApplicationViews
    
    val application = mock[GenomeMuseumGUI]
    application.createMuseumSchema returns schemaMock
    application.createFileStorage returns fileStorageMock
    application.createApplicationViews returns view
  }
  
  def constructor(f: Factory) = new {
    val schemaMock = mock[MuseumSchema]
    val fileStorageMock = mock[LibraryFileManager]
    val loadManagerMock = mock[MuseumExhibitLoadManager]
    val mainFrameViewCtrlMock = mock[MainFrameViewController]
    val view = new ApplicationViews
    
    val application = mock[GenomeMuseumGUI]
    application.createMuseumSchema returns schemaMock
    application.createFileStorage returns fileStorageMock
    application.createApplicationViews returns view
    
    
    val handler = spy(f(application))
    doAnswer(_ => loadManagerMock).when(handler).createMuseumExhibitLoadManager
    doAnswer(_ => mainFrameViewCtrlMock).when(handler).createMainFrameViewController
    
    def createsSchema = handler.dataSchema must_== schemaMock
    
    def createsFileStorage = handler.fileStorage must_== fileStorageMock
    
    def createsLoader = handler.loadManager must_== loadManagerMock
    
    def createsController = handler.mainFrameViewCtrl must_== mainFrameViewCtrlMock
  }
  
  private def spyHandler(h: ApplicationActionHandler) = {
    val loadManagerMock = mock[MuseumExhibitLoadManager]
    val mainFrameViewCtrlMock = mock[MainFrameViewController]
    val handler = spy(h)
    doAnswer(_ => loadManagerMock).when(handler).createMuseumExhibitLoadManager
    doAnswer(_ => mainFrameViewCtrlMock).when(handler).createMainFrameViewController
    handler
  }
  
  def chooseAndLoad(f: Factory) = new {
    val openDialogMock = mock[FileDialog]
    openDialogMock.getDirectory returns "dir"
    val view = spy(new ApplicationViews)
    doAnswer(_ => openDialogMock).when(view).openDialog
    
    val application = mock[GenomeMuseumGUI]
    application.createApplicationViews returns view
    
    val loadManagerMock = mock[MuseumExhibitLoadManager]
    val handler = spy(f(application))
    doAnswer(_ => loadManagerMock).when(handler).createMuseumExhibitLoadManager
    
    def showsDialog = {
      handler.chooseAndLoadFile()
      there was one(openDialogMock).setVisible(true)
    }
    
    def callsLoad = {
      openDialogMock.getFile returns "file"
      handler.chooseAndLoadFile()
      there was one(loadManagerMock).loadExhibit(new File("dir", "file"))
    }
    
    def notCallsLoad = {
      handler.chooseAndLoadFile()
      there was no(loadManagerMock).loadExhibit(any[File])
    }
  }
  
  def showMainFrameView(f: Factory) = new {
    val application = mock[GenomeMuseumGUI]
    val mainFrameViewCtrlMock = mock[MainFrameViewController]
    val handler = spy(f(application))
    doAnswer(_ => mainFrameViewCtrlMock).when(handler).createMainFrameViewController
    
    handler.showMainFrameView
    
    def show = there was one(mainFrameViewCtrlMock).show()
  }
  
  def openAction(f: Factory) = new {
    val application = mock[GenomeMuseumGUI]
    application.createApplicationViews returns new ApplicationViews
    
    val handler = spy(f(application))
    doAnswer(_ => ()).when(handler).chooseAndLoadFile
    
    handler.openAction.apply()
    
    def calls = there was one(handler).chooseAndLoadFile
  }
  
  def quitAction(f: Factory) = new {
    val application = mock[GenomeMuseumGUI]
    doAnswer(_ => ()).when(application).exit(any)
    
    val handler = spy(f(application))
    
    handler.quitAction.apply()
    
    def calls = there was one(application).exit(any)
  }
}
