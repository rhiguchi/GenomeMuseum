package jp.scid.genomemuseum.controller

import javax.swing.RootPaneContainer

import org.jdesktop.application


import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.{view, model}
import view.{MainView, MainViewMenuBar, ColumnVisibilitySetting}
import model.MuseumSchema
import GenomeMuseumController.{convertToScalaSwingAction}

object MuseumFrameViewController {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[MuseumFrameViewController])
}

/**
 * フレームビューコントローラ
 */
class MuseumFrameViewController(
  rootPaneContainer: RootPaneContainer
) {
  import MuseumFrameViewController._
  // ビュー
  /** メインビュー */
  val mainView = new MainView
  /** メニューバー */
  val mainMenu = new MainViewMenuBar
  /** 列表示設定 */
  val columnConfigPane = new ColumnVisibilitySetting
  /** 列表示ダイアログ */
  lazy val columnConfigDialog = new swing.Dialog {
    import java.awt
    import javax.swing.JDialog
    
    override lazy val peer = rootPaneContainer match {
      case window: awt.Window => new JDialog(window) with InterfaceMixin
      case _ => new JDialog with InterfaceMixin
    }

    contents = new swing.Panel{
      override lazy val peer = columnConfigPane.contentPane
    }
  }
  
  // コントローラ
  /** メインビュー用コントローラ */
  val mainCtrl = new MainViewController(mainView)
  /** 列設定用コントローラ */
  lazy val viewSettingDialogCtrl = new ViewSettingDialogController(
      columnConfigPane, columnConfigDialog) {
    mainMenu.columnVisibility.action = showAction
  }
  
  // モデル
  /** 現在のスキーマ */
  var currentSchema: Option[MuseumSchema] = None
  /** 現在のメインビューのタイトル */
  def mainCtrlTitle = mainCtrl.title
  
  // モデルバインド
  // タイトルの設定
  mainCtrlTitle.reactions += {
    case _: ValueChange[_] => updateFrameTitle()
  }
  
  // アクション
  @application.Action(name = "show")
  def show() {
    logger.debug("show")
    rootPaneContainer match {
      case window: java.awt.Window =>
        window setVisible true
      case _ =>
    }
  }
  
  // プロパティ
  /** データスキーマを取得する */
  def dataSchema = currentSchema.get
  
  /** データスキーマを設定する */
  def dataSchema_=(newSchema: MuseumSchema) {
    mainCtrl.dataSchema = newSchema
    currentSchema = Option(newSchema)
  }
  
  private def bindActionsTo(menu: MainViewMenuBar) {
    // 列設定メニュー
//    menu.columnVisibility.action = viewSettingDialogCtrl.showAction
    // 部屋追加ボタン
    val sourceListCtrl = mainCtrl.sourceListCtrl
    menu.newListBox.action = sourceListCtrl.addBasicRoomAction
    menu.newSmartBox.action = sourceListCtrl.addSamrtRoomAction
    menu.newGroupBox.action = sourceListCtrl.addGroupRoomAction
    
    menu.reloadResources()
  }
  
  /** ルートペインにこのコントローラのビューを適用する */
  private def makeRootPaneContainer(container: RootPaneContainer) {
    container setContentPane mainView.getContentPane
    container.getRootPane setJMenuBar mainMenu.container.peer
    
    rootPaneContainer match {
      case window: java.awt.Window =>
        window.pack
        window.setLocationRelativeTo(null)
      case _ =>
    }
    
    updateFrameTitle()
  }
  
  /** ルートペインのタイトルを更新する */
  private[controller] def updateFrameTitle() {
    val title = "GenomeMuseum - " + mainCtrlTitle()
    rootPaneContainer match {
      case frame: java.awt.Frame => frame.setTitle(title)
      case _ =>
    }
  }
  
  // ビューの構築
  makeRootPaneContainer(rootPaneContainer)
}
