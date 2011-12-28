package jp.scid.genomemuseum.controller

import javax.swing.JFrame

import org.specs2._

import jp.scid.gui.ValueHolder
import jp.scid.genomemuseum.GenomeMuseumGUI
import jp.scid.genomemuseum.model.MuseumSchema
import jp.scid.genomemuseum.view.{MainFrameView, MainViewMenuBar}

class MainFrameViewControllerSpec extends Specification with mock.Mockito {
  private type Factory = (GenomeMuseumGUI) => MainFrameViewController
  
  def is = "MainFrameViewController" ^
    "フレームと結合" ^ canBindFrame(createHandler) ^
    "メニューバーと結合" ^ canBindMenuBar(createHandler) ^
    "フレームタイトルモデルの結合" ^ canBindTitleModel(createHandler) ^
    end
  
  def createHandler(h: GenomeMuseumGUI) =
    new MainFrameViewController(h)
  
  implicit def constructByDefault(f: Factory): MainFrameViewController = {
    val gmGui = new GenomeMuseumGUI
    new MainFrameViewController(gmGui)
  }
  
  def canBindFrame(f: Factory) =
    "show() 呼び出しで表示できるようになる" ! bindFrame(f).visibleModel ^
    "タイトルモデルと結合される" ! bindFrame(f).titleModel ^
    bt

  def canBindMenuBar(f: Factory) =
    "ファイルメニュー" ^ fileMenuSpec(f) ^
    "編集メニュー" ^ editMenuSpec(f) ^
    bt
  
  def canBindTitleModel(f: Factory) = 
    "モデルが空文字の時は GenomeMuseum と表示" ! bindTitle(f).empty ^
    "モデルに文字がある時は ハイフン を挟んで表示" ! bindTitle(f).someText ^
    bt
  
  def fileMenuSpec(f: Factory) = 
    "開く" ! fileMenu(f).open ^
    "終了" ! fileMenu(f).quit ^
    bt
  
  def editMenuSpec(f: Factory) = 
    "カット" ! editMenu(f).cut ^
    "コピー" ! editMenu(f).copy ^
    "ペースト" ! editMenu(f).paste ^
    "全てを選択" ! editMenu(f).selectAll ^
    bt
  
  private def spyMainFrameViewOf(frame: JFrame) = {
    val view = spy(new MainFrameView)
    view.frame returns frame
    view
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
    val menuBar = new MainViewMenuBar
    ctrl.bindMenuBar(menuBar)
    
    private def app = ctrl.application
    
    def open = menuBar.open.action must_== app.openAction
    
    def quit = menuBar.quit.action must_== app.quitAction
  }
  
  // 編集メニュー
  def editMenu(ctrl: MainFrameViewController) = new {
    val menuBar = new MainViewMenuBar
    ctrl.bindMenuBar(menuBar)
    
    private def app = ctrl.application
    
    def cut = menuBar.cut.action must_== app.cutProxyAction
    
    def copy = menuBar.copy.action must_== app.copyProxyAction
    
    def paste = menuBar.paste.action must_== app.pasteProxyAction
    
    def selectAll = menuBar.selectAll.action must_== app.selectAllProxyAction
  }
  
  // タイトルモデル結合
  def bindTitle(ctrl: MainFrameViewController) = new {
    val titleModel = new ValueHolder("")
    ctrl.bindTitle(titleModel)
    
    def empty = ctrl.title() must_== "GenomeMuseum"
    
    def someText = {
      titleModel := "title"
      ctrl.title() must_== "GenomeMuseum - title"
    }
  }
}
