package jp.scid.genomemuseum.controller

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
  
  bindActions(menu)
  
  def showFrame() {
    frameOfMainView.pack
    frameOfMainView setLocationRelativeTo null
    frameOfMainView setVisible true
  }
  
  def openFile() {
    println("openFile")
  }
  
  def bindActions(menu: MainViewMenuBar) {
    menu.open.action = openAction
  }
}