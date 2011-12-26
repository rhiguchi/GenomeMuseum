package jp.scid.genomemuseum.controller

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.GenomeMuseumGUI
import jp.scid.genomemuseum.view.MainFrameView
import jp.scid.genomemuseum.model.MuseumSchema

/**
 * 主画面と、その画面枠の操作を受け付け、操作反応を実行するオブジェクト。
 * 
 * @param application データやメッセージを取り扱うアプリケーションオブジェクト。
 * @param frameView 表示と入力を行う画面枠。
 */
class MainFrameViewController(
  application: GenomeMuseumGUI,
  frameView: MainFrameView
) extends GenomeMuseumController {
  // ビュー
  private def frame = frameView.frame
  private def menuBar = frameView.mainMenu
  
  // モデル
  /** この画面枠用のタイトル */
  val title = new ValueHolder("GenomeMuseum")
  /** 列設定用コントローラ */
  val viewSettingDialogCtrl = new ViewSettingDialogController(
      frameView.columnConfigPane, frameView.columnConfigDialog)
  
  /**
   * フレームの表示を行う。
   * 
   * @see MainFrameView#frame
   */
  def show() {
    frame.setVisible(true)
  }
  
  /** タイトルモデルの結合を行う */
  def bindTitle(viewTitle: ValueHolder[String]) {
    viewTitle.reactions += {
      case ValueChange(_, _, ctrlTitle: String) => ctrlTitle match {
        case "" => title := "GenomeMuseum"
        case _ => title := "GenomeMuseum - " + ctrlTitle
      }
    }
  }
  
  private def bindModels() {
    // タイトル
    ValueHolder.connect(title, frame, "title")
  }
  
  /** アクションの結合を行う */
  private def bindActions() {
    menuBar.open.action = application.openAction
    menuBar.quit.action = application.quitAction
    
    menuBar.cut.action = application.cutProxyAction
    menuBar.copy.action = application.copyProxyAction
    menuBar.paste.action = application.pasteProxyAction
    menuBar.selectAll.action = application.selectAllProxyAction
    // 列設定メニュー
    menuBar.columnVisibility.action = viewSettingDialogCtrl.showAction
  }
  
  // データ結合処理
  bindModels()
  bindActions()
}
