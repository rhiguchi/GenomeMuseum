package jp.scid.genomemuseum.controller

import org.specs2._

import jp.scid.genomemuseum.GenomeMuseumGUI

object GenomeMuseumControllerSpec extends mock.Mockito {
  private[controller] def spyApplicationActionHandler = {
    val gui = new GenomeMuseumGUI
    spy(new ApplicationActionHandler(gui))
  }
}