package jp.scid.genomemuseum.gui

import collection.mutable.Publisher

private[gui] trait PublisherAdapter[Msg] {
  type ObservableService <: Publisher[Msg]
  
  private var disconnector = () => {}
  
  def connect(service: ObservableService) = {
    disconnect()
    
    val subscriber = new service.Sub {
      def notify(pub: service.Pub, message: Msg) {
        notifyModel(service, message)
      }
    }
    
    service subscribe subscriber
    disconnector = () => service removeSubscription subscriber
    this
  }
  
  def disconnect() = {
    disconnector()
    this
  }
  
  protected def notifyModel(service: ObservableService, message: Msg)
}
