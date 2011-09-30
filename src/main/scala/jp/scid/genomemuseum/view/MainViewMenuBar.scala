package jp.scid.genomemuseum.view

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
}
