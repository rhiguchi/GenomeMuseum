package jp.scid.genomemuseum.controller

import jp.scid.genomemuseum.GenomeMuseumGUI

/**
 * GenomeMuseum の操作オブジェクトを作成するファクトリ
 */
class GenomeMuseumControllerFactory(val application: GenomeMuseumGUI) {
  private def userExhibitRoomService = application.museumSchema.userExhibitRoomService
  private def museumExhibitService = application.museumSchema.museumExhibitService
  private def exhibitLoadManager = application.exhibitLoadManager
  
  /**
   * ソースリスト操作オブジェクトを作成する。
   * 
   * {@code loadManager} が設定される。
   */
  protected[genomemuseum] def createExhibitRoomListController() =
    new ExhibitRoomListController(userExhibitRoomService, exhibitLoadManager)

  /**
   * 展示物リスト操作オブジェクトを作成する。
   * 
   * {@code loadManager} が設定される。
   */
  protected[genomemuseum] def createMuseumExhibitListController() =
    new MuseumExhibitListController(museumExhibitService, exhibitLoadManager)

  /**
   * ウェブ検索操作オブジェクトを作成する。
   * 
   * {@code loadManager} が設定される。
   */
  protected[genomemuseum] def createWebServiceResultController() =
    new WebServiceResultController(exhibitLoadManager)
  
  /**
   * 主画面操作オブジェクトを作成する。
   * 
   * @param mainView 入出力画面オブジェクト
   */
  protected[genomemuseum] def createMainViewController() = {
    new MainViewController(userExhibitRoomService, museumExhibitService, exhibitLoadManager)
  }
  
  /**
   * 主画面枠の操作対応オブジェクトを作成する。
   */
  def createMainFrameViewController() = {
    val frameViewCtrl = newMainFrameViewController()
    frameViewCtrl.application = Some(application)
    
    val mainViewCtrl = createMainViewController
    frameViewCtrl.mainViewController = Some(mainViewCtrl)
    frameViewCtrl.connectTitle(mainViewCtrl.title)
    
    frameViewCtrl
  }
  
  /** 主画面枠を作成 */
  private[controller] def newMainFrameViewController() = new MainFrameViewController()
}
