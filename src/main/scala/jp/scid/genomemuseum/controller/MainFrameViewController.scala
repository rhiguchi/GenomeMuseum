package jp.scid.genomemuseum.controller

import javax.swing.JFrame

import jp.scid.gui.ValueHolder
import jp.scid.gui.model.{ValueModel, ValueModels, TransformValueModel}
import jp.scid.gui.control.{StringPropertyBinder, AbstractValueController, BooleanPropertyBinder}
import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.GenomeMuseumGUI
import jp.scid.genomemuseum.view.MainFrameView
import jp.scid.genomemuseum.view.{MainFrameView, MainViewMenuBar}

import ValueModels.{newValueModel, newBooleanModel}

/**
 * 主画面と、その画面枠の操作を受け付け、操作反応を実行するオブジェクト。
 * 
 * @param mainViewController この画面枠に使用する主画面操作器
 */
class MainFrameViewController(val mainViewController: MainViewController) extends GenomeMuseumController {
  
  def this() {
    this(new MainViewController)
  }
  
  // プロパティモデル
  /** 画面枠の表示プロパティ */
  val visible = ValueModels.newBooleanModel(false)
  
  /** 画面枠タイトルプロパティ */
  val title = new TransformValueModel[String, String](mainViewController.title, new TitleTransformer)
  
  // コントローラ
  /** 列設定用コントローラ */
//  val viewSettingDialogCtrl = new ViewSettingDialogController(
//      frameView.columnConfigPane, frameView.columnConfigDialog)
  
  // 結合
  /** この画面枠用のタイトルプロパティ */
  private val titleBinder = new StringPropertyBinder(title)
  
  /** この画面枠用の表示プロパティ */
  private val visibleBinder = new BooleanPropertyBinder(visible)
  
  /**
   * フレームの表示を行う。
   * 
   * @see MainFrameView#frame
   */
  def show() {
    visible setValue true
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
    /** タイトルモデルの結合を行う */
    titleBinder.bindFrameTitle(frame)
    visibleBinder.bindVisible(frame)
    
    frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE)
  }
  
  /**
   * メニューバー とアクションを結合する
   */
  def bindMenuBar(menuBar: MainViewMenuBar) {
    menuBar.resourceMap.injectComponents(menuBar.container.peer)
    // 列設定メニュー
//    menuBar.columnVisibility.action = viewSettingDialogCtrl.showAction
  }
  
  class TitleTransformer extends TransformValueModel.Transformer[String, String] {
    def apply(subject: String) = subject match {
      case "" => "GenomeMuseum"
      case title => "GenomeMuseum - " + title
    }
  }
}
