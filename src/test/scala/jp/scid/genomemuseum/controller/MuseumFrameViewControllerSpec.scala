package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import javax.swing.RootPaneContainer

import jp.scid.genomemuseum.view.{MainView, MainViewMenuBar, ColumnVisibilitySetting}

class MuseumFrameViewControllerSpec extends Specification with Mockito {
  private type Controller = MuseumFrameViewController
  
  def is = "MuseumFrameViewController" ^
    "ビュー表示" ^ canShow ^ bt ^
    "コンテンツ設定" ^ rootPaneSpec ^ bt ^
//    "メニューバーアクション設定" ^ menuBarSpec(defaultMenuBar) ^ bt ^
    end
    
  class TestBase {
    val owner = mock[RootPaneContainer]
    lazy val ctrl = new Controller(owner)
  }
  
  implicit private def createController(owner: RootPaneContainer) =
    new Controller(owner)
  
  private def defaultMenuBar = {
    val ctrl = new Controller(rootPaneMock[RootPaneContainer])
    ctrl.mainMenu
  }
  
  private def rootPaneMock[A <% RootPaneContainer: ClassManifest] = {
    import javax.swing.JRootPane
    
    val rpcMock = mock[A]
    rpcMock.getRootPane returns mock[JRootPane]
    rpcMock
  }
  
  def canShow(implicit constructor: RootPaneContainer => Controller) =
    "conainter が Window の時は表示される" ! show(constructor).windowVisibled ^
    "conainter が Window ではない時は何も起きない" ! show(constructor).doNothing
  
  def rootPaneSpec(implicit constructor: RootPaneContainer => Controller) =
    "コンテンツが設定される" ! rootPane(constructor).contentPane ^
    "メニューバーが設定される" ! rootPane(constructor).menubar
  
  def menuBarSpec(menu: => MainViewMenuBar) =
    "部屋追加ボタンにアクション設定" ! menuBar(menu).newRoomActionsApplied
  
  def show(constructor: RootPaneContainer => Controller) = new TestBase {
    import javax.swing.{JWindow, JRootPane}
    
    def windowVisibled = {
      val rpcMock = rootPaneMock[JWindow]
      constructor(rpcMock).show()
      there was one(rpcMock).setVisible(true)
    }
    
    def doNothing = {
      val rpcMock = rootPaneMock[RootPaneContainer]
      constructor(rpcMock).show()
      success
    }
  }
  
  def rootPane(constructor: RootPaneContainer => Controller) = new TestBase {
    private def getContainerAndController = {
      val rcp = rootPaneMock[RootPaneContainer]
      val ctrl = constructor(rcp)
      Pair(rcp, ctrl)
    }
      
    def contentPane = {
      val (rcp, ctrl) = getContainerAndController
      there was one(rcp).setContentPane(ctrl.mainView.getContentPane)
    }
    
    def menubar = {
      val (rcp, ctrl) = getContainerAndController
      there was one(rcp.getRootPane).setJMenuBar(ctrl.mainMenu.container.peer)
    }
  }
  
  def menuBar(menu: MainViewMenuBar) = new TestBase {
    def newRoomActionsApplied =
      menu.newListBox.action.title must not beEmpty
  }
}
