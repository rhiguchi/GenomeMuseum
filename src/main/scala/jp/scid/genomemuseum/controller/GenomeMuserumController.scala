package jp.scid.genomemuseum.controller

import org.jdesktop.application.Application

import jp.scid.genomemuseum.GenomeMuseumGUI

private[controller] object GenomeMuseumController {
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
  protected[controller] def actionMapOf(controller: GenomeMuseumController) = applicationContext
    .getActionManager.getActionMap(classOf[GenomeMuseumController], controller)
  
  /**
   * リソースマップを取得
   * 指定したコントローラのオブジェクトから、GenomeMuseumController までの
   * 階層内のマップを返す。
   */
  protected[controller] def resourceMapOf(obj: AnyRef) = applicationContext
    .getResourceManager.getResourceMap(obj.getClass, classOf[Object])
}

/**
 * GenomeMuseum の基底コントローラ
 */
private[controller] abstract class GenomeMuseumController {
  import GenomeMuseumController._
  import GenomeMuseumGUI.convertToScalaSwingAction
  
  /**
   * このコントローラのアクションを取得する。
   */
  protected[controller] def getAction(name: String) = {
    val actionMap = actionMapOf(this)
    logger.debug("Get '%s' action from '%s'."
      .format(name, actionMap.getActionsClass))
    val action = actionMap.get(name)
    
    if (action == null)
        logger.warn("Action '%s' is not defined on '%s'."
          .format(name, actionMap.getActionsClass))
    
    convertToScalaSwingAction(action)
  }
  
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
}
