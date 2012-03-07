package jp.scid.genomemuseum.view

import java.util.ResourceBundle
import scala.swing.{MenuBar, Menu, MenuItem, Separator}
import org.jdesktop.application.ResourceMap

class MainViewMenuBar {
  val resourceMap =
    new ResourceMap(null, classOf[MainViewMenuBar].getClassLoader,
      "jp.scid.genomemuseum.view.MainViewMenuBar")
  
  val container = new MenuBar()
    
  val fileMenu = createMenu("file")
  val editMenu = createMenu("edit")
  val viewMenu = createMenu("view")
    
  // for fileMenu
  val newListBox = createMenuItem("newListBox")
  val newSmartBox = createMenuItem("newSmartBox")
  val newGroupBox = createMenuItem("newGroupBox")
  val open = createMenuItem("open")
  val quit = createMenuItem("quit")
    
  // for editMenu
  val undo = createMenuItem("undo")
  val cut = createMenuItem("cut")
  val copy = createMenuItem("copy")
  val paste = createMenuItem("paste")
  val delete = createMenuItem("delete")
  val selectAll = createMenuItem("selectAll")
  
  // for viewMenu
  val columnVisibility = createMenuItem("columnVisibility")
    
  container.contents += (fileMenu, editMenu, viewMenu)
  fileMenu.contents += (newListBox, newSmartBox, newGroupBox, new Separator,
    open, new Separator, quit)
  editMenu.contents += (undo, new Separator,
    cut, copy, paste, delete, selectAll)
  viewMenu.contents += (columnVisibility)
  
  private def createMenuItem(name: String) = {
    val menu = new MenuItem(getResourceText(name))
    menu.name = name
    menu
  }
  
  private def createMenu(name: String) = {
    val menu = new Menu(getResourceText(name))
    menu.name = name
    menu
  }
  
  private def getResourceText(name: String) = resourceMap.getString(name + ".text")
}
