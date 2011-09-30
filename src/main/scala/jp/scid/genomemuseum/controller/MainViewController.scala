package jp.scid.genomemuseum.controller

import jp.scid.genomemuseum.view
import view.{MainView, MainViewMenuBar}
import javax.swing.JFrame

class MainViewController(
  mainView: MainView,
  frameOfMainView: JFrame,
  menu: MainViewMenuBar
) {
  val tableCtrl = new ExhibitTableController(mainView.dataTable)
  
  def showFrame() {
    frameOfMainView.pack
    frameOfMainView setLocationRelativeTo null
    frameOfMainView setVisible true
  }
}