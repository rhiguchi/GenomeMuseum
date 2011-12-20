package jp.scid.genomemuseum.view

import java.awt.FileDialog

class ApplicationViews() {
  val mainVrameView = new MainFrameView
  
  val openDialog = new FileDialog(null.asInstanceOf[java.awt.Frame], "", FileDialog.LOAD)
}
