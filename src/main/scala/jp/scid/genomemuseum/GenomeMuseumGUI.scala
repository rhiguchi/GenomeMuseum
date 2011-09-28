package jp.scid.genomemuseum

import java.awt.{BorderLayout}
import view.MainView
import scala.swing.{MainFrame}

class GenomeMuseumGUI {
  lazy val mainFrame = new MainFrame {
    val mainView = new MainView
    title = "GenomeMuseum"
    peer.getContentPane.setLayout(new BorderLayout)
    peer.getContentPane.add(mainView.contentPane, "Center")
  }
  
  def start() {
    mainFrame.pack()
    mainFrame.peer setLocationRelativeTo null
    mainFrame.visible = true
  }
}