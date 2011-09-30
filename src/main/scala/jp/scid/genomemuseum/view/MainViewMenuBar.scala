package jp.scid.genomemuseum.view

import java.util.ResourceBundle
import scala.swing.{MenuBar, Menu, MenuItem, Separator}

class MainViewMenuBar {
  val container = new MenuBar()
    
  val fileMenu = new Menu("file")
  val editMenu = new Menu("edit")
    
  // for fileMenu
  val open = new MenuItem("open")
  val quit = new MenuItem("quit")
    
  // for editMenu
  val undo = new MenuItem("undo")
  val cut = new MenuItem("cut")
  val copy = new MenuItem("copy")
  val paste = new MenuItem("paste")
  val delete = new MenuItem("delete")
  val selectAll = new MenuItem("selectAll")
    
  container.contents += (fileMenu, editMenu)
  fileMenu.contents += (open, new Separator, quit)
  editMenu.contents += (undo, new Separator,
    cut, copy, paste, delete, selectAll)
  
  // リソース読み込み
  reloadResources()
  
  def reloadResources() {
    reloadResources(ResourceBundle.getBundle(classOf[MainViewMenuBar].getName))
  }
  
  def reloadResources(res: ResourceBundle) {
    import collection.JavaConverters._
    import java.lang.Boolean.parseBoolean
    import scala.swing.AbstractButton
    
    def injectResourceTo(b: AbstractButton, resPrefix: String) {
      val resKeys = res.getKeys.asScala.filter(_.startsWith(resPrefix)).toList
      resKeys.foreach { resKey =>
        resKey.substring(resPrefix.length) match {
          case ".text" => b.text = res.getString(resKey)
          case ".enabled" => b.enabled = parseBoolean(res.getString(resKey))
          case _ => // TODO warnings
        }
      }
    }
    
    def injectResourceFromText(btns: AbstractButton*) {
      btns foreach { btn => injectResourceTo(btn, btn.text) }
    }
    
    injectResourceFromText(fileMenu, editMenu)
    injectResourceFromText(open, quit)
    injectResourceFromText(undo, cut, copy, paste, delete, selectAll)
  }
}
