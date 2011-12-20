package jp.scid.genomemuseum.controller

import org.jdesktop.application.{Application, ApplicationActionMap}

import jp.scid.genomemuseum.GenomeMuseumGUI
import jp.scid.genomemuseum.model.MuseumSchema

private[controller] object GenomeMuseumController {
  import GenomeMuseumGUI.convertToScalaSwingAction
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[GenomeMuseumController])
  
  private def applicationContext = GenomeMuseumGUI.applicationContext
  
  private def setActionTo(binds: (javax.swing.AbstractButton, swing.Action)*) {
    binds foreach { pair => pair._1 setAction pair._2.peer }
  }
  
  /**
   * アクションマップを取得
   * 指定したコントローラのオブジェクトから、GenomeMuseumController までの
   * 階層内のマップを返す。
   */
  protected[controller] def actionMapOf(controller: AnyRef) = applicationContext
    .getActionManager.getActionMap(classOf[Object], controller)
  
  /**
   * リソースマップを取得
   * 指定したコントローラのオブジェクトから、GenomeMuseumController までの
   * 階層内のマップを返す。
   */
  protected[controller] def resourceMapOf(obj: AnyRef) = applicationContext
    .getResourceManager.getResourceMap(obj.getClass, classOf[Object])
  
  /**
   * アクションマップからアクションを取得する。
   * 
   * {@code actionMap} に {@code name} キーのアクションが存在しないときは
   * 警告ログが出力される。
   */
  private[controller] def getAction(name: String, actionMap: ApplicationActionMap) = {
    logger.debug("Get '%s' action from '%s'."
      .format(name, actionMap.getActionsClass))
    val action = actionMap.get(name)
    
    if (action == null)
        logger.warn("Action '%s' is not defined on '%s'."
          .format(name, actionMap.getActionsClass))
    
    convertToScalaSwingAction(action): swing.Action
  }
}

private[controller] trait ApplicationController {
  import GenomeMuseumController._
  
  implicit private[controller] val actionMap = actionMapOf(this)
  
  /**
   * このコントローラのアクションを取得する。
   */
  protected[controller] def getAction(name: String)(implicit actionMap: ApplicationActionMap) = {
    GenomeMuseumController.getAction(name, actionMap)
  }
  
  /** scala.swing.Action から peer のアクションを取得する暗黙変換 */
  private[controller] implicit def getJavaSwingAction(scalaSwingAction: swing.Action) =
    scalaSwingAction.peer
  
  /**
   * ボタンにアクションを設定する。
   */
  private[controller] def bindAction(binds: (javax.swing.AbstractButton, javax.swing.Action)*) {
    binds foreach { pair => pair._1 setAction pair._2 }
  }
}

/**
 * GenomeMuseum の基底コントローラ
 */
private[controller] abstract class GenomeMuseumController(
  app: ApplicationActionHandler
) extends ApplicationController {
  import GenomeMuseumController._
  import GenomeMuseumGUI.convertToScalaSwingAction
  
  @deprecated("", "")
  def this() = this(null)
  
  /**
   * このコントローラのリソースマップを取得する。
   */
  protected[controller] def resourceMap = resourceMapOf(this)
  
  /**
   * リソースマップから文字列を取得する。
   * キーが存在しない時は警告ログが出力される。
   */
  protected[controller] def getResourceString(key: String, params: AnyRef*) = {
    if (!resourceMap.containsKey(key))
      logger.warn("Resource '%s' is not defined on '%s'."
        .format(key, resourceMap.getBundleNames))
      
    resourceMap.getString(key, params)
  }
  
  /** データスキーマを取得する。 */
  protected[controller] def museumSchema: MuseumSchema = app.dataSchema
  
  /** データローダーを取得する。 */
  protected[controller] def loadManager: MuseumExhibitLoadManager = app.loadManager
  
  /** ファイル管理オブジェクトを取得する */
  protected[controller] def fileStorage = app.fileStorage
}
