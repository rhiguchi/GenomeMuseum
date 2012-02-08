package jp.scid.genomemuseum.controller

import jp.scid.genomemuseum.GenomeMuseumGUI

object GenomeMuseumController {
  def apply(controller: AnyRef) = new GenomeMuseumController {
    override private[controller] def controllerObject = controller
    
    def getResource(key: String) = super.getResource(key)(resourceMap)
    
    def getAction(name: String) = super.getAction(name)(actionMap)
  }
}

/**
 * GenomeMuseum の基底コントローラ
 */
abstract class GenomeMuseumController extends ApplicationController {
  protected val applicationClass = classOf[GenomeMuseumGUI]
}
