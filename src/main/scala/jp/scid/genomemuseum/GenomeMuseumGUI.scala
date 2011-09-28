package jp.scid.genomemuseum

import scala.swing.{Label, MainFrame}

class GenomeMuseumGUI {
  val mainFrame = new MainFrame {
    title = "GenomeMuseum"
    contents = new Label("Frame Test")
  }
  
  def start() {
    mainFrame.pack()
    mainFrame.visible = true
  }
}