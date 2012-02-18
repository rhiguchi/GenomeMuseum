package jp.scid.genomemuseum.controller

import javax.swing.JFrame

import org.specs2._

import jp.scid.gui.ValueHolder
import jp.scid.genomemuseum.GenomeMuseumGUI
import jp.scid.genomemuseum.model.MuseumSchema
import jp.scid.genomemuseum.view.{MainFrameView, MainViewMenuBar}

class MainFrameViewControllerSpec extends Specification with mock.Mockito {
  def is = "MainFrameViewController" ^
    "画面枠を表示する" ^ canShow(createHandler) ^
    "画面枠タイトルと主画面タイトルの結合" ^ titleSpec(createHandler) ^
    "フレームと結合" ^ canBindFrame(createHandler) ^
    "メニューバーと結合" ^ canBindMenuBar(createHandler) ^
    "MainFrameView と結合" ^ canBind(createHandler) ^
    end
  
  def createHandler() = new MainFrameViewController()
  
  def canShow(c: => MainFrameViewController) =
    "表示モデルが true になる" ! show(c).visibleModel ^
    bt
  
  def titleSpec(c: => MainFrameViewController) =
    "主画面のタイトルがブランクだと GenomeMuseum となる" ! title(c).blank ^
    "主画面のタイトルがあると 「GenomeMuseum - 文字列」となる" ! title(c).someString ^
    bt
  
  def canBindFrame(c: => MainFrameViewController) =
    "表示できるようになる" ! bindFrame(c).visibleModel ^
    "タイトルモデルと結合される" ! bindFrame(c).titleModel ^
    bt
  
  def canBindMenuBar(c: => MainFrameViewController) =
    "列設定ダイアログと結合" ! todo ^
    bt
  
  def canBind(c: => MainFrameViewController) =
    "mainViewController と結合される" ! bind(c).toMainViewController ^
    "frame を bindFrame で結合" ! bind(c).bindFrame ^
    "mainMenu を bindMenuBar で結合" ! bind(c).bindMenuBar ^
    bt
  
  /** 画面枠を表示する */
  def show(ctrl: MainFrameViewController) = new {
    ctrl.show()
    
    def visibleModel = ctrl.visible.getValue must_== true
  }
  
  /** 画面枠タイトルと主画面タイトルの結合 */
  def title(ctrl: MainFrameViewController) = new {
    ctrl.mainViewController.title := ""
    
    def blank = ctrl.title() must_== "GenomeMuseum"
    
    def someString = {
      ctrl.mainViewController.title := "title"
      ctrl.title() must_== "GenomeMuseum - title"
    }
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
  
  // MainFrameViewと結合
  def bind(c: MainFrameViewController) = new {
    val mainViewCtrl = mock[MainViewController]
    val ctrl = spy(c)
    ctrl.mainViewController returns mainViewCtrl
    doAnswer{_ => }.when(ctrl).bindFrame(any)
    doAnswer{_ => }.when(ctrl).bindMenuBar(any)
    
    val view = new MainFrameView
    ctrl.bind(view)
    
    def toMainViewController = there was one(mainViewCtrl).bind(view.mainView)
    def bindFrame = there was one(ctrl).bindFrame(view.frame)
    def bindMenuBar = there was one(ctrl).bindMenuBar(view.mainMenu)
  }
}
