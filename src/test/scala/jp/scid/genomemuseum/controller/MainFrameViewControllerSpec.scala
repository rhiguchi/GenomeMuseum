package jp.scid.genomemuseum.controller

import javax.swing.JFrame

import org.specs2._

import jp.scid.gui.ValueHolder
import jp.scid.genomemuseum.GenomeMuseumGUI
import jp.scid.genomemuseum.model.MuseumSchema
import jp.scid.genomemuseum.view.{MainFrameView, MainViewMenuBar}

class MainFrameViewControllerSpec extends Specification with mock.Mockito {
  def is = "MainFrameViewController" ^
    "フレームタイトルモデルと結合" ^ canConnectTitleModel(createHandler) ^
    "プロパティ" ^ propertiesSpec(createHandler) ^
    "フレームと結合" ^ canBindFrame(createHandler) ^
    "メニューバーと結合" ^ canBindMenuBar(createHandler) ^
    "MainFrameView と結合" ^ canBind(createHandler) ^
    end
  
  def createHandler() = new MainFrameViewController()
  
  def propertiesSpec(c: => MainFrameViewController) =
    "application 初期値" ! properties(c).application ^
    "application 設定" ! properties(c).applicationSet ^
    "mainViewController 初期値" ! properties(c).mainViewController ^
    "mainViewController 設定" ! properties(c).mainViewControllerSet ^
    bt
  
  def canBind(c: => MainFrameViewController) =
    "mainView を mainViewController と結合" ! bindMainFrameView(c).mainViewController ^
    "frame を bindFrame で結合" ! bindMainFrameView(c).bindFrame ^
    "mainMenu を bindMenuBar で結合" ! bindMainFrameView(c).bindMenuBar ^
    bt
  
  def canBindFrame(c: => MainFrameViewController) =
    "show() 呼び出しで表示できるようになる" ! bindFrame(c).visibleModel ^
    "タイトルモデルと結合される" ! bindFrame(c).titleModel ^
    bt

  def canBindMenuBar(c: => MainFrameViewController) =
    "ファイルメニュー" ^ fileMenuSpec(c) ^
    "編集メニュー" ^ editMenuSpec(c) ^
    bt
  
  def fileMenuSpec(c: => MainFrameViewController) = 
    "開く" ! fileMenu(c).open ^
    "終了" ! fileMenu(c).quit ^
    bt
  
  def editMenuSpec(c: => MainFrameViewController) = 
    "カット" ! editMenu(c).cut ^
    "コピー" ! editMenu(c).copy ^
    "ペースト" ! editMenu(c).paste ^
    "全てを選択" ! editMenu(c).selectAll ^
    bt
  
  def canConnectTitleModel(c: => MainFrameViewController) = 
    "モデルが空文字の時は GenomeMuseum と表示" ! connectTitle(c).empty ^
    "モデルに文字がある時は ハイフン を挟んで表示" ! connectTitle(c).someText ^
    bt
  
  private def spyMainFrameViewOf(frame: JFrame) = {
    val view = spy(new MainFrameView)
    view.frame returns frame
    view
  }
  
  // プロパティ
  def properties(ctrl: MainFrameViewController) = new {
    def application = ctrl.application must beNone
    def applicationSet = {
      val app = mock[GenomeMuseumGUI]
      ctrl.application = Some(app)
      ctrl.application must beSome(app)
    }
    
    def mainViewController = ctrl.mainViewController must beNone
    def mainViewControllerSet = {
      val mainViewCtrl = mock[MainViewController]
      ctrl.mainViewController = Some(mainViewCtrl)
      ctrl.mainViewController must beSome(mainViewCtrl)
    }
  }
  
  // MainFrameViewと結合
  def bindMainFrameView(c: MainFrameViewController) = new {
    val mainViewCtrl = mock[MainViewController]
    val ctrl = spy(c)
    ctrl.mainViewController = Some(mainViewCtrl)
    val view = new MainFrameView
    ctrl.bind(view)
    
    def mainViewController = there was one(mainViewCtrl).bind(view.mainView)
    def bindFrame = there was one(ctrl).bindFrame(view.frame)
    def bindMenuBar = there was one(ctrl).bindMenuBar(view.mainMenu)
  }
  
  // フレームと結合
  def bindFrame(ctrl: MainFrameViewController) = new {
    val frame = mock[JFrame]
    ctrl.bindFrame(frame)
    
    def visibleModel = {
      ctrl.show()
      there was one(frame).setVisible(true)
    }
    
    def titleModel = {
      List("title", "some", "") map ctrl.title.:=
      there was one(frame).setTitle("title")
      there was one(frame).setTitle("some")
      there was one(frame).setTitle("")
    }
  }
  
  // ファイルメニュー
  def fileMenu(ctrl: MainFrameViewController) = new {
    val app = new GenomeMuseumGUI
    ctrl.application = Some(app)
    
    val menuBar = new MainViewMenuBar
    ctrl.bindMenuBar(menuBar)
    
    def open = menuBar.open.action must_== app.openAction
    
    def quit = menuBar.quit.action must_== app.quitAction
  }
  
  // 編集メニュー
  def editMenu(ctrl: MainFrameViewController) = new {
    val app = new GenomeMuseumGUI
    ctrl.application = Some(app)
    
    val menuBar = new MainViewMenuBar
    ctrl.bindMenuBar(menuBar)
    
    def cut = menuBar.cut.action must_== app.cutProxyAction
    
    def copy = menuBar.copy.action must_== app.copyProxyAction
    
    def paste = menuBar.paste.action must_== app.pasteProxyAction
    
    def selectAll = menuBar.selectAll.action must_== app.selectAllProxyAction
  }
  
  // タイトルモデル結合
  def connectTitle(ctrl: MainFrameViewController) = new {
    val titleModel = new ValueHolder("")
    ctrl.connectTitle(titleModel)
    
    def empty = ctrl.title() must_== "GenomeMuseum"
    
    def someText = {
      titleModel := "title"
      ctrl.title() must_== "GenomeMuseum - title"
    }
  }
}
