package jp.scid.genomemuseum.controller

import java.util.ResourceBundle
import jp.scid.genomemuseum.view
import view.{MainView, MainViewMenuBar}
import javax.swing.JFrame
import scala.swing.Action

class MainViewController(
  mainView: MainView,
  frameOfMainView: JFrame,
  menu: MainViewMenuBar
) {
  val tableCtrl = new ExhibitTableController(mainView.dataTable)
  val openAction = Action("Open") { openFile }
  val quitAction = Action("Quit") { quitApplication }
  
  bindActions(menu)
  reloadResources()
  
  def showFrame() {
    frameOfMainView.pack
    frameOfMainView setLocationRelativeTo null
    frameOfMainView setVisible true
  }
  
  def openFile() {
    println("openFile")
  }
  
  def quitApplication() {
    System.exit(0)
  }
  
  def bindActions(menu: MainViewMenuBar) {
    menu.open.action = openAction
    menu.quit.action = quitAction
  }
  
  /** リソースを設定する */
  private def reloadResources() {
    reloadResources(ResourceBundle.getBundle(
      classOf[MainViewController].getName))
  }
  
  private def reloadResources(res: ResourceBundle) {
    val rm = new ResourceManager(res)
    rm.injectTo(openAction, "openAction")
    rm.injectTo(quitAction, "quitAction")
    menu.reloadResources
  }
}

class ResourceManager(res: ResourceBundle) {
  import collection.JavaConverters._
  import java.lang.Boolean.parseBoolean
  import javax.swing.KeyStroke.getKeyStroke
  
  def injectTo(action: Action, keyPrefix: String) {
    val resKeys = res.getKeys.asScala.filter(_.startsWith(keyPrefix))
    if (resKeys.isEmpty)
      throw new IllegalArgumentException(
        "No resource which starts with '%s' found.".format(keyPrefix))
    
    resKeys.foreach { resKey =>
      resKey.substring(keyPrefix.length) match {
        case ".title" => action.title = res.getString(resKey)
        case ".enabled" => action.enabled = parseBoolean(res.getString(resKey))
        case ".accelerator" =>
          action.accelerator = Some(getKeyStroke(res.getString(resKey)))
        case ".toolTip" => action.toolTip = res.getString(resKey)
        case _ => // TODO log warnings
          println("unsupported key: " + resKey)
      }
    }
  }
}
