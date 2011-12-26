package jp.scid.genomemuseum.gui

import collection.script.Include

import org.specs2._

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitService,
  UserExhibitRoom, EmptyMuseumExhibitService, MuseumExhibitMock, UserExhibitRoomMock}
import UserExhibitRoom.RoomType._

class ExhibitTableModelSpec extends Specification with mock.Mockito {
  private type Factory = MuseumExhibitService => ExhibitTableModel
  
  def is = "ExhibitTableModel" ^
    "データソース" ^ sourceSpec(createModel) ^
    end
  
  def createModel(service: MuseumExhibitService) = {
    new ExhibitTableModel(service)
  }
  
  def sourceSpec(f: Factory) =
    "service#allElements が適用" ! source(f).elements ^
    "service のイベントで再読み込み" ! source(f).eventReaction ^
    "部屋を指定するとその部屋のデータを表示" ! source(f).roomContents ^
    bt
  
  def source(f: Factory) = new {
    val e1, e2, e3, e4 = MuseumExhibitMock.of("mock")
    val r1, r2 = UserExhibitRoomMock.of(BasicRoom)
    val service = spy(new EmptyMuseumExhibitService)
    service.allElements returns List(e1, e2, e3)
    service.getExhibits(r1) returns List(e4, e3)
    
    val model = f(service)
    
    def elements = model.source must contain(e1, e2, e3).only.inOrder
      
    def eventReaction = {
      service.allElements returns List(e1, e2, e3, e4)
      service.publish(new Include(e4))
      model.source must contain(e1, e2, e3, e4).only.inOrder
    }
    
    def roomContents = {
      model.userExhibitRoom = Some(r1)
      val source1 = model.source
      model.userExhibitRoom = Some(r2)
      val source2 = model.source
      (source1, source2) must_== (List(e4, e3), Nil)
    }
  }
}
