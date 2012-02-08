package jp.scid.genomemuseum.controller

import org.jdesktop.application.{Application, ApplicationContext,
  ApplicationActionMap, ResourceMap, ApplicationAction => SAFAction}

import java.awt.event.ActionEvent

object ApplicationController {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[ApplicationController])
  
  /**
   * アプリケーションのコンテキストオブジェクトを取得
   */
  protected[controller] def applicationContextOf[C <: Application](appClass: Class[C]) = {
    import util.control.Exception.catching
    
    def getApplication = Application.getInstance(appClass)
    
    val application = catching(classOf[IllegalStateException]).opt
        {getApplication}.getOrElse {
      java.beans.Beans.setDesignTime(true)
      getApplication
    }
    application.getContext()
  }

  /**
   * アクションマップを取得。
   * 
   * 指定したオブジェクトと、その継承階層のすべてのアクションを含んだマップを返す。
   */
  def actionMapOf(controller: AnyRef)(implicit context: ApplicationContext) =
    context.getActionManager.getActionMap(classOf[Object], controller)
  
  /**
   * リソースマップを取得。
   * 
   * 指定したオブジェクトと、その継承階層のすべてのリソースを含んだマップを返す。
   */
  def resourceMapOf(controller: AnyRef)(implicit context: ApplicationContext) =
    context.getResourceManager.getResourceMap(controller.getClass, classOf[Object])
  
  /**
   * アプリケーションのアクションクラス。
   */
  class ApplicationAction(swingAction: javax.swing.Action, val name: String) extends swing.Action("") {
    override lazy val peer = swingAction
    def apply() {
      val e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "apply")
      apply(e)
    }
    
    def apply(event: ActionEvent) = peer.actionPerformed(event)
  }
  
  /**
   * アプリケーションのリソースクラス。
   */
  class ApplicationResource(val key: String, resourceMap: ResourceMap) {
    def apply(args: AnyRef*) = resourceMap.getString(key, args)
    
    override def toString() = {
      "ApplicationResource[key=" + key + ", map=" + resourceMap.getBundleNames + "]"
    }
  }
  
  /**
   * Swing のアクションを scala.swing のアクションに変換する
   */
  implicit def convertToScalaSwingAction(swingAction: javax.swing.Action) = {
    val name = swingAction match {
      case action: SAFAction => action.getName
      case _ => swingAction.getValue("Name").asInstanceOf[String]
    }
    new ApplicationAction(swingAction, name)
  }
  
  /**
   * アクションマップからアクションを取得する。
   * 
   * {@code actionMap} に {@code name} キーのアクションが存在しないときは
   * 警告ログが出力される。
   */
  def getAction(name: String, actionMap: ApplicationActionMap) = {
    logger.debug("Get '%s' action from '%s'."
      .format(name, actionMap.getActionsClass))
    val action = actionMap.get(name)
    
    if (action == null)
        logger.warn("Action '%s' is not defined on '%s'."
          .format(name, actionMap.getActionsClass))
    
    convertToScalaSwingAction(action)
  }
  
  /**
   * リソースマップからリソースを取得する。
   * 
   * {@code actionMap} に {@code name} キーのアクションが存在しないときは
   * 警告ログが出力される。
   */
  def getResource(key: String, resourceMap: ResourceMap) = {
    if (logger.isDebugEnabled)
      logger.debug("Get '%s' resource from '%s'."
        .format(key, resourceMap.getBundleNames))
    
    if (!resourceMap.containsKey(key))
        logger.warn("Resource '%s' is not defined on '%s'."
          .format(key, resourceMap.getBundleNames))
    
    new ApplicationResource(key, resourceMap)
  }
}

/**
 * アプリケーションのコントローラクラスの作成に便利なユーティリティメソッドを含んだクラス。
 */
trait ApplicationController {
  import ApplicationController.{actionMapOf, resourceMapOf, applicationContextOf,
    getAction => getApplicationAction, getResource => getApplicationResource}
  
  private[controller] def controllerObject(): AnyRef = this
  
  /**
   * このコントローラを利用するアプリケーションのクラス。
   * 
   * このクラスをもとにして、コンテキストオブジェクトを取得し、
   * アクションマップやリソースマップが利用できる。
   */
  protected def applicationClass: Class[_ <: Application]
  
  /** アプリケーションのコンテキストオブジェクトを取得 */
  implicit private def applicationContext = applicationContextOf(applicationClass)

  /** このコントローラのアクションマップ */
  implicit protected[controller] def actionMap = actionMapOf(controllerObject)
  
  /** このコントローラのリソースマップ */
  implicit protected[controller] def resourceMap = resourceMapOf(controllerObject)
  
  /**
   * このコントローラのアクションを取得する。
   */
  protected[controller] def getAction(name: String)(implicit actionMap: ApplicationActionMap) =
    getApplicationAction(name, actionMap)
  
  /**
   * このコントローラのリソースを取得する。
   */
  protected[controller] def getResource(key: String)(implicit resourceMap: ResourceMap) =
    getApplicationResource(key, resourceMap)
  
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
