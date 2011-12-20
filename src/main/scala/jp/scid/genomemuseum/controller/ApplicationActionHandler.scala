package jp.scid.genomemuseum.controller

import java.io.File
import java.awt.event.ActionEvent

import jp.scid.genomemuseum.GenomeMuseumGUI
import jp.scid.genomemuseum.view.ApplicationViews

import org.jdesktop.application.Action

import GenomeMuseumController.actionMapOf

/**
 * アプリケーションの入力操作を取り扱うオブジェクト。
 * 
 * {@link #dataSchema} や {@link #fileStorage} は {@code parent} の
 * ファクトリメソッドからそれぞれ作成される。
 * 
 * @param parent アプリケーション
 */
class ApplicationActionHandler(parent: GenomeMuseumGUI) extends ApplicationController {
  // ビュー
  /** アプリケーションビュー */
  lazy val view: ApplicationViews = parent.createApplicationViews()
  /** メインビュー */
  private def mainFrameView = view.mainVrameView
  /** 『開く』用ファイル選択ダイアログ */
  private def openDialog = view.openDialog
  
  // モデル
  /** データモデル */
  val dataSchema = parent.createMuseumSchema
  
  // アクション
  /** {@code chooseAndLoadFile} を呼び出すアクション */
  lazy val openAction = getAction("open")
  /** {@link GenomeMuseumGUI#exit} を呼び出すアクション */
  lazy val quitAction = getAction("quit")
  /** 切り取りプロキシアクション */
  lazy val cutProxyAction = parent.getAction("cut")
  /** コピープロキシアクション */
  lazy val copyProxyAction = parent.getAction("copy")
  /** 貼付けプロキシアクション */
  lazy val pasteProxyAction = parent.getAction("paste")
  /** 全てを選択プロキシアクション */
  lazy val selectAllProxyAction = parent.getAction("selectAll")
  
  // コントローラ
  /** ファイル管理オブジェクト */
  val fileStorage = parent.createFileStorage
  /** 読み込み管理オブジェクト */
  lazy val loadManager = createMuseumExhibitLoadManager()
  /** 主画面枠操作 */
  lazy val mainFrameViewCtrl = createMainFrameViewController()
  
  /** 主画面枠を表示する */
  def showMainFrameView() {
    mainFrameViewCtrl.show()
  }
  
  /**
   * ファイル選択ダイアログを表示し、選ばれたファイルをバイオデータとして読み込みを行う。
   * 
   * @see MuseumExhibitLoadManager#loadExhibit(File)
   */
  @Action(name="open")
  def chooseAndLoadFile() {
    val dialog = openDialog
    dialog.setVisible(true)
    
    Option(dialog.getFile).map(new File(dialog.getDirectory, _))
      .foreach(loadManager.loadExhibit)
  }
  
  /**
   * アプリケーションの終了処理を開始する。
   * 
   * @see GenomeMuseumGUI#exit(ActionEvent)
   */
  @Action(name="quit")
  def exitApplication(actionEvent: ActionEvent) {
    parent.exit(actionEvent)
  }
  
  /**
   * バイオデータの読み込み管理オブジェクトを作成する。
   */
  protected[controller] def createMuseumExhibitLoadManager() = {
    new MuseumExhibitLoadManager(dataSchema.museumExhibitService, Some(fileStorage))
  }
  
  /**
   * 主画面枠の操作対応オブジェクトを作成する。
   */
  protected[controller] def createMainFrameViewController() = {
    new MainFrameViewController(this, mainFrameView)
  }
}
