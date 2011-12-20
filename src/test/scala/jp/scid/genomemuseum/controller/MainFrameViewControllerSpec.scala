package jp.scid.genomemuseum.controller

import javax.swing.JFrame

import org.specs2._

import jp.scid.gui.ValueHolder
import jp.scid.genomemuseum.GenomeMuseumGUI
import jp.scid.genomemuseum.model.MuseumSchema
import jp.scid.genomemuseum.view.MainFrameView

class MainFrameViewControllerSpec extends Specification with mock.Mockito {
  private type Factory = (ApplicationActionHandler, MainFrameView) => MainFrameViewController
  
  def is = "MainFrameViewController" ^
    "フレームの表示" ^ canShow(createHandler) ^
    "フレームタイトルの更新" ^ canUpdateFrameTitle(createHandler) ^
    "メニューバーアクション" ^ menuBarActionsSpec(createHandler) ^
    end
  
  def createHandler(h: ApplicationActionHandler, v: MainFrameView) =
    new MainFrameViewController(h, v)
  
  def canUpdateFrameTitle(f: Factory) = 
    "frame#setTitle への適用" ! updateFrameTitle(f).appliedView ^
    "mainCtrl#title から適用" ! updateFrameTitle(f).fromMainCtrl ^
    bt
  
  def canShow(f: Factory) = 
    "frameView#frame の setVisible を true" ! show(f).frameVisibled ^
    bt
  
  def menuBarActionsSpec(f: Factory) = 
    "ファイルメニュー" ^ fileMenuSpec(f) ^
    "編集メニュー" ^ editMenuSpec(f) ^
    bt
  
  def fileMenuSpec(f: Factory) = 
    "開く" ! fileMenu(f).open ^
    "終了" ! fileMenu(f).quit ^
    bt
  
  def editMenuSpec(f: Factory) = 
    "カット" ! fileMenu(f).cut ^
    "コピー" ! fileMenu(f).copy ^
    "ペースト" ! fileMenu(f).paste ^
    "全てを選択" ! fileMenu(f).selectAll ^
    bt
  
  private def mockApplicationActionHandler = {
    val openAction, quitAction, cutProxyAction, copyProxyAction,
      pasteProxyAction, selectAllProxyAction = swing.Action(""){}
    val handler = spyApplicationActionHandler
    handler.openAction returns openAction
    handler.quitAction returns quitAction
    handler.cutProxyAction returns cutProxyAction
    handler.copyProxyAction returns copyProxyAction
    handler.pasteProxyAction returns pasteProxyAction
    handler.selectAllProxyAction returns selectAllProxyAction
    handler
  }
  
  private def spyApplicationActionHandler = {
    val gui = new GenomeMuseumGUI
    spy(new ApplicationActionHandler(gui))
  }
  
  private def spyMainFrameViewOf(frame: JFrame) = {
    val view = spy(new MainFrameView)
    view.frame returns frame
    view
  }
  
  // フレームタイトル
  def updateFrameTitle(f: Factory) = new {
    val frameMock = mock[JFrame]
    val ctrl = f(mockApplicationActionHandler, spyMainFrameViewOf(frameMock))
    
    def appliedView = {
      List("title", "some", "") map ctrl.title.:=
      got {
        frameMock.setTitle("GenomeMuseum - title")
        frameMock.setTitle("GenomeMuseum - some")
        frameMock.setTitle("GenomeMuseum - ")
      }
    }
    
    def fromMainCtrl = {
      List("Room", "", "NCBI") map ctrl.mainCtrl.title.:=
      there was one(frameMock).setTitle("GenomeMuseum - Room") then
      one(frameMock).setTitle("GenomeMuseum") then
      one(frameMock).setTitle("GenomeMuseum - NCBI")
    }
  }
  
  // 表示
  def show(f: Factory) = new {
    val frameMock = mock[JFrame]
    val hander = mockApplicationActionHandler
    val view = spyMainFrameViewOf(frameMock)
    val ctrl = f(hander, view)
    
    ctrl.show()
    
    def frameVisibled = there was one(frameMock).setVisible(true)
  }
  
  // ファイルメニュー
  def fileMenu(f: Factory) = new {
    val view = new MainFrameView
    val handler = mockApplicationActionHandler
    val ctrl = f(handler, view)
    
    def menuBar = view.mainMenu
    
    def roomListCtrl = ctrl.mainCtrl.sourceListCtrl
    
    def open = menuBar.open.action must_== handler.openAction
    
    def quit = menuBar.quit.action must_== handler.quitAction
    
    def cut = menuBar.cut.action must_== handler.cutProxyAction
    
    def copy = menuBar.copy.action must_== handler.copyProxyAction
    
    def paste = menuBar.paste.action must_== handler.pasteProxyAction
    
    def selectAll = menuBar.selectAll.action must_== handler.selectAllProxyAction
  }
}
