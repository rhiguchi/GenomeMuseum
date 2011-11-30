package jp.scid.genomemuseum.controller

import org.jdesktop.application.Application

import jp.scid.genomemuseum.GenomeMuseumGUI

private[controller] object GenomeMuseumController {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[GenomeMuseumController])
  
  implicit def convertToScalaSwingAction(swingAction: javax.swing.Action)
      = new scala.swing.Action("") {
    import java.awt.event.ActionEvent
    
    override lazy val peer = swingAction
    override def apply() {
      val e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "apply")
      apply(e)
    }
    def apply(event: ActionEvent) = peer.actionPerformed(event)
  }
  
  lazy val applicationContext = {
    import util.control.Exception.catching
    
    def getInstance = Application.getInstance(classOf[GenomeMuseumGUI])
    
    val application = catching(classOf[IllegalStateException]).opt
        {getInstance}.getOrElse {
      java.beans.Beans.setDesignTime(true)
      getInstance
    }
    application.getContext()
  }
}

/**
 * GenomeMuseum の基底コントローラ
 */
private[controller] abstract class GenomeMuseumController {
  import GenomeMuseumController._
  
  /**
   * アクションマップを取得
   * 指定したコントローラのオブジェクトから、GenomeMuseumController までの
   * 階層内のマップを返す。
   */
  private def actionMapOf(controller: GenomeMuseumController) = applicationContext
    .getActionManager.getActionMap(classOf[GenomeMuseumController], controller)
  
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
}
