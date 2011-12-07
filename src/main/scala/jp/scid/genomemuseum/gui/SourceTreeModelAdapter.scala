package jp.scid.genomemuseum.gui

import collection.mutable.Publisher
import collection.script.{Message, Include, Update, Remove}

import jp.scid.genomemuseum.model.{TreeDataService, UserExhibitRoom, ExhibitRoom}
import jp.scid.gui.tree.SourceTreeModel

class SourceTreeModelAdapter[A <: AnyRef, B <: A](treeModel: SourceTreeModel[A])
    extends PublisherAdapter[Message[B]] {
  type ObservableService = TreeDataService[B] with Publisher[Message[B]]
  
  protected def notifyModel(service: ObservableService, message: Message[B]) {
    message match {
      case Include(loc, room) =>
        service.getParent(room) foreach (p => treeModel.someChildrenWereInserted(p))
      case Update(loc, room) => treeModel.nodeChanged(room)
      case Remove(loc, room) => treeModel.nodeRemoved(room)
      case _ => treeModel.reset()
    }
  }
}

private[gui] trait PublisherAdapter[Msg] {
  type ObservableService <: Publisher[Msg]
  
  private var disconnector = () => {}
  
  def connect(service: ObservableService) {
    disconnect()
    
    val subscriber = new service.Sub {
      def notify(pub: service.Pub, message: Msg) {
        notifyModel(service, message)
      }
    }
    
    service subscribe subscriber
    disconnector = () => service removeSubscription subscriber
  }
  
  def disconnect() = disconnector()
  
  protected def notifyModel(service: ObservableService, message: Msg)
}