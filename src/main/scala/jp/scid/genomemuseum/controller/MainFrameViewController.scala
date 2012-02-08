package jp.scid.genomemuseum.controller

import javax.swing.JFrame

import jp.scid.gui.ValueHolder
import jp.scid.gui.model.ValueModel
import jp.scid.gui.control.AbstractValueController
import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.GenomeMuseumGUI
import jp.scid.genomemuseum.view.MainFrameView
import jp.scid.genomemuseum.view.{MainFrameView, MainViewMenuBar}

/**
 * 主画面と、その画面枠の操作を受け付け、操作反応を実行するオブジェクト。
 * 
 * @param application データやメッセージを取り扱うアプリケーションオブジェクト。
 * @param frameView 表示と入力を行う画面枠。
 */
class MainFrameViewController(val mainViewController: MainViewController) extends GenomeMuseumController {
  
  def this() {
    this(new MainViewController)
  }
  
  // コントローラ
  /** アプリケーションアクションの参照 */
  var application: Option[GenomeMuseumGUI] = None
  
  // モデル
  /** この画面枠用のタイトル */
  val title = new ValueHolder("GenomeMuseum")
  /** 画面枠の表示モデル */
  val frameVisible = new ValueHolder(false)
  /** 列設定用コントローラ */
//  val viewSettingDialogCtrl = new ViewSettingDialogController(
//      frameView.columnConfigPane, frameView.columnConfigDialog)
  
  /**
   * フレームの表示を行う。
   * 
   * @see MainFrameView#frame
   */
  def show() {
    frameVisible := true
  }
  
  /** タイトルモデルの結合を行う */
  def connectTitle(viewTitle: ValueModel[String]) {
    val controller = new AbstractValueController[String] {
      def processValueChange(value: String) = value match {
        case "" => title := "GenomeMuseum"
        case _ => title := "GenomeMuseum - " + value
      }
    }
    controller.setModel(viewTitle)
  }
  
  /**
   * ビュー MainFrameView とモデルの結合を行う
   */
  def bind(view: MainFrameView) {
    mainViewController.bind(view.mainView)
    bindFrame(view.frame)
    bindMenuBar(view.mainMenu)
  }
  
  /**
   * JFrame とモデルを結合する
   */
  def bindFrame(frame: JFrame) {
    ValueHolder.connect(title, frame, "title")
    ValueHolder.connect(frameVisible, frame, "visible")
  }
  
  /**
   * メニューバー とアクションを結合する
   */
  def bindMenuBar(menuBar: MainViewMenuBar) {
    application foreach { application =>
      menuBar.open.action = application.openAction
      menuBar.quit.action = application.quitAction
      
      menuBar.cut.action = application.cutProxyAction
      menuBar.copy.action = application.copyProxyAction
      menuBar.paste.action = application.pasteProxyAction
      menuBar.selectAll.action = application.selectAllProxyAction
    }
    // 列設定メニュー
//    menuBar.columnVisibility.action = viewSettingDialogCtrl.showAction
  }
}
