package jp.scid.genomemuseum.gui

import collection.mutable.Publisher
import collection.script.{Message, Include, Update, Remove, Reset}

import org.specs2._
import mock._

import jp.scid.genomemuseum.model.{TreeDataService, UserExhibitRoom, ExhibitRoom}
import jp.scid.gui.tree.SourceTreeModel

class SourceTreeModelAdapterSpec extends Specification with Mockito {
  
  private type Service = TreeDataService[UserExhibitRoom] with Publisher[Message[UserExhibitRoom]]
  private type Factory = SourceTreeModel[ExhibitRoom] => SourceTreeModelAdapter[ExhibitRoom, UserExhibitRoom]
  
  def is = "SourceTreeModelAdapter" ^
    "イベント接続" ^ canAdapt(adapterOf) ^
    "接続解除" ^ canDisconnect(adapterOf) ^
    end
  
  def adapterOf(treeModel: SourceTreeModel[ExhibitRoom]) = {
    new SourceTreeModelAdapter[ExhibitRoom, UserExhibitRoom](treeModel)
  }
  
  def roomMock() = {
    mock[UserExhibitRoom]
  }
  
  def canAdapt(f: Factory) =
    "Include で someChildrenWereInserted 発行" ! publish(f).callsInserted ^
    "Update で nodeChanged 呼び出し" ! publish(f).callsChanged ^
    "Remove で nodeRemoved 呼び出し" ! publish(f).callsRemoved ^
    "Reset で reset 呼び出し" ! publish(f).callsReset ^
    bt
  
  def canDisconnect(f: Factory) =
    "解除するとメソッドを呼び出さない" ! disconnect(f).notCall ^
    bt
  
  
  private[gui] class TestPublisher extends TreeDataService[UserExhibitRoom] with Publisher[Message[UserExhibitRoom]] {
    val roomA = roomMock()
    val roomA_A = roomMock()
    
    override def publish(msg: Message[UserExhibitRoom]) = super.publish(msg)
    
    def getChildren(parent: Option[UserExhibitRoom]): Iterable[UserExhibitRoom] = Nil
    def getParent(element: UserExhibitRoom) = {
      element match {
        case `roomA_A` => Some(roomA)
        case _ => None
      }
    }
    def save(element: UserExhibitRoom) {}
  }
  
  abstract class TestBase(f: Factory) {
    val treeModel = mock[SourceTreeModel[ExhibitRoom]]
    private[gui] val publisher = new TestPublisher
    val adapter = f(treeModel)
    adapter.connect(publisher)
  }
  
  def publish(f: Factory) = new TestBase(f) {
    def callsInserted = {
      publisher.publish(new Include(publisher.roomA_A))
      there was one(treeModel).someChildrenWereInserted(publisher.roomA)
    }
    
    def callsChanged = {
      publisher.publish(new Update(publisher.roomA))
      there was one(treeModel).nodeChanged(publisher.roomA)
    }
    
    def callsRemoved = {
      publisher.publish(new Remove(publisher.roomA))
      there was one(treeModel).nodeRemoved(publisher.roomA)
    }
    
    def callsReset = {
      publisher.publish(new Reset)
      there was one(treeModel).reset()
    }
  }
  
  def disconnect(f: Factory) = new TestBase(f) {
    adapter.disconnect()
    
    def notCall = {
      publisher.publish(new Include(publisher.roomA_A))
      there was no(treeModel).someChildrenWereInserted(publisher.roomA)
    }
  }
}
