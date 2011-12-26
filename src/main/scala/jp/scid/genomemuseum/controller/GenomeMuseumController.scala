package jp.scid.genomemuseum.controller

import jp.scid.genomemuseum.GenomeMuseumGUI

/**
 * GenomeMuseum の基底コントローラ
 */
abstract class GenomeMuseumController extends ApplicationController {
  protected val applicationClass = classOf[GenomeMuseumGUI]
}
