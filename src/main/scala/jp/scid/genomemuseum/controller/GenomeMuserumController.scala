package jp.scid.genomemuseum.controller

import org.slf4j.LoggerFactory

private[controller] object GenomeMuseumController {
  object Logger {
    def apply[A: ClassManifest]() = {
      val c = implicitly[ClassManifest[A]].erasure
      LoggerFactory.getLogger(c)
    }
  }
  
  implicit def convertToScalaSwingAction(swingAction: javax.swing.Action)
      = new scala.swing.Action("") {
    import java.awt.event.ActionEvent
    
    override lazy val peer = swingAction
    override def apply() {
      val e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "apply")
      apply(e)
    }
    def apply(event: ActionEvent) = peer.actionPerformed(event)
  }
}
