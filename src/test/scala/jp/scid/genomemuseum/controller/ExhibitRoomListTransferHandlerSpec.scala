package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler}
import java.awt.datatransfer.{Transferable, DataFlavor}
import java.io.File

import org.specs2._
import mock._

import TransferHandler.TransferSupport
import DataFlavor.javaFileListFlavor

import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibitService,
  MuseumStructure, MuseumExhibit, MuseumExhibitTransferData, UserExhibitRoomSpec}
import jp.scid.genomemuseum.gui.{MuseumSourceModel}
import UserExhibitRoom.RoomType._

object ExhibitRoomListTransferHandlerSpec extends Mockito {
}

class ExhibitRoomListTransferHandlerSpec extends Specification with Mockito {
  private type Handler = ExhibitRoomListTransferHandler
  private type Factory = MuseumSourceModel => ExhibitRoomListTransferHandler
  
  import ExhibitRoomListTransferHandlerSpec._
  import MuseumExhibitTransferData.{dataFlavor => exhibitListDataFlavor}
  
  def is = "ExhibitRoomListTransferHandler" ^
    "転入可能判定" ^ canImportSpec ^ bt ^
    "転入操作" ^ canImportData(handlerOf) ^ bt ^
  end
  
  def spiedSourceListModel = {
    spy(new MuseumSourceModel(new MuseumStructure))
  }
  
  def handlerWithNoTransferSource = {
    new Handler(spiedSourceListModel)
  }
  
  def userRoomOf(roomType: RoomType) = {
    val room = mock[UserExhibitRoom]
    room.roomType returns roomType
    room
  }
  
  def handlerWithTransferSource = {
    val roomMock = mock[ExhibitRoom]
    
    new Handler(spiedSourceListModel)
  }
  
  def createHandler = {
    new Handler(createModelMock)
  }
  
  def handlerOf(model: MuseumSourceModel) = {
    new Handler(model)
  }
  
  def createModelMock = {
    val model = spiedSourceListModel
    // 移動は何も起きない
    doAnswer {arg => }.when(model).moveRoom(any, any)
    // 子孫部屋判定を false
    doAnswer {arg => false}.when(model).isDescendant(any, any)
    model
  }
  
  lazy val view = new JTree
  
  def canExportStateSpec(h: => Handler) =
    "getSourceActions が MOVE_OR_COPY" ! todo ^
    "createTransferable が ExhibitRoomTransferData を返す" ! todo
  
  def cannotExportStateSpec(h: => Handler) =
    "getSourceActions が NONE" ! todo ^
    "createTransferable が null" ! todo
  
  def canImportSpec =
    "MuseumExhibitTransferData は許可しない" ! canImport.returnsFalseByExhibits ^
    "転入部屋が GroupRoom の時は MuseumExhibitTransferData を許可" ! canImport.returnsTrueByExhibitsOnGroupRoom ^
    "転入部屋が転出部屋と同一の時は MuseumExhibitTransferData を許可しない" ! canImport.falseByEqualRoom ^
    "転入部屋が GroupRoom で、子孫の時は MuseumExhibitTransferData を許可しない" ! canImport.falseByAncestor ^
    "転入部屋が BasicRoom の時は MuseumExhibitTransferData を許可" ! canImport.trueToBasicRoom ^
    "転入部屋が ローカルソースの時はファイルを許可" ! todo ^
    "転入部屋が BasicRoom の時はファイルを許可" ! todo ^
    bt
  
  def canImportData(f: Factory) =
    "BasicRoom へ転入" ^ canImportToBasicRoom(f) ^
    "GroupRoom へ転入" ^ canImportToGroupRoom(f) ^
    "SmartRoom へ転入はできない" ! importDataToSmartRoom.returnsFalse ^
    "ルート へ転入" ^ canImportToUserRoomRoot(f) ^
    bt
  
  def canImportToBasicRoom(f: Factory) =
    "UserExhibitRoom を転入" ! importDataToBasicRoom(f).fromRoom ^
    "addElement を呼び出す" ! importDataToBasicRoom(f).callsAddElement ^
    "ソースの部屋無しを転入" ! importDataToBasicRoom(f).fromAnonRoom ^
    "同じ部屋は転入できない" ! importDataToBasicRoom(f).falseBySameRoom ^
    "ファイルを転入" ! importDataToBasicRoom(f).byFile ^
    "ファイルを読み込みクラスで実行" ! importDataToBasicRoom(f).importsFiles ^
    bt
  
  def canImportToGroupRoom(f: Factory) =
    "UserExhibitRoom を転入" ! importDataToGroupRoom(f).fromRoom ^
    "moveRoom を呼び出す" ! importDataToGroupRoom(f).callsMoveRoom ^
    "ソースの部屋無しは転入できない" ! importDataToGroupRoom(f).fromAnonRoom ^
    "同じ部屋は転入できない" ! importDataToGroupRoom(f).falseBySameRoom ^
    "自分の子孫は転入できない" ! importDataToGroupRoom(f).falseByDesc ^
    bt
  
  def canImportToUserRoomRoot(f: Factory) =
    "UserExhibitRoom を転入" ! importDataToRoot(f).fromRoom ^
    "ソースの部屋無しは転入できない" ! importDataToRoot(f).fromAnonRoom ^
    "ルートには転入できない" ! importDataToRoot(f).fromRoot ^
    bt
  
  def roomOf = UserExhibitRoomSpec.mockOf _
  
  def canImport = new {
    val handler = spy(createHandler)
    
    
    val t = mock[MuseumExhibitTransferData]
    t.isDataFlavorSupported(exhibitListDataFlavor) returns true
    t.sourceRoom returns None
    
    val ts = new TransferSupport(mock[JTree], t)
    
    def returnsFalseByExhibits = {
      handler.canImport(ts) must beFalse
    }
    
    def returnsTrueByExhibitsOnGroupRoom = {
      t.sourceRoom returns Some(roomOf(BasicRoom))
      handler.getImportingTarget(any) returns Some(roomOf(GroupRoom))
      handler.canImport(ts) must beTrue
    }
    
    def falseByEqualRoom = {
      val room = roomOf(GroupRoom)
      handler.getImportingTarget(any) returns Some(room)
      t.sourceRoom returns Some(room)
      handler.canImport(ts) must beFalse
    }
    
    def falseByAncestor = {
      val room1, room2 = roomOf(GroupRoom)
      handler.isDescendant(any, any) returns true
      handler.getImportingTarget(any) returns Some(room1)
      t.sourceRoom returns Some(room2)
      handler.canImport(ts) must beFalse
    }
    
    def trueToBasicRoom = {
      handler.getImportingTarget(any) returns Some(roomOf(BasicRoom))
      handler.canImport(ts) must beTrue
    }
  }
  
  /** 部屋の転送データ作成 */
  private def creaateExhibitTransferData(sourceRoom: Option[UserExhibitRoom]) = {
    val t = mock[MuseumExhibitTransferData]
    t.isDataFlavorSupported(exhibitListDataFlavor) returns true
    t.getTransferData(exhibitListDataFlavor) returns t
    t.sourceRoom returns sourceRoom
    t.museumExhibits returns Nil
    t
  }
  
  private def creaateTransferSupport(sourceRoom: UserExhibitRoom) = {
    new TransferSupport(mock[JTree], creaateExhibitTransferData(Some(sourceRoom)))
  }
  private def creaateTransferSupport(sourceRoom: UserExhibitRoom, exhibits: Seq[MuseumExhibit]) = {
    val t = creaateExhibitTransferData(Some(sourceRoom))
    t.museumExhibits returns exhibits.toList
    new TransferSupport(mock[JTree], t)
  }
  
  private def creaateTransferData(files: Seq[File]) = {
    import scala.collection.JavaConverters._
    
    val t = mock[MuseumExhibitTransferData]
    t.isDataFlavorSupported(javaFileListFlavor) returns true
    t.getTransferData(javaFileListFlavor) returns files.toList.asJava
    t
  }
  
  /** 転送元部屋の作成 */
  implicit private def sourceRoomOf(roomType: RoomType) = Some(roomOf(roomType))
  
  /** TransferSupport 暗黙変換 */
  implicit private def createTransferSupport(t: MuseumExhibitTransferData) = {
    new TransferSupport(mock[JTree], t)
  }
  
  def exhibitTransferDataOf(sourceRoom: Option[UserExhibitRoom], exhibits: Seq[MuseumExhibit]) = {
    val t = mock[MuseumExhibitTransferData]
    t.isDataFlavorSupported(exhibitListDataFlavor) returns true
    t.getTransferData(exhibitListDataFlavor) returns t
    t.sourceRoom returns sourceRoom
    t.museumExhibits returns exhibits.toList
    t
  }
  
  class RoomImportingTestBase(factory: Factory, val targetRoom: Option[UserExhibitRoom]) {
    val model = createModelMock
    
    val handler = spy(factory(model))
    // 転送先
    handler.getImportingTarget(any) returns targetRoom
    // 展示物サービスの設定
    val service = mock[MuseumExhibitService]
    handler.exhibitService = Some(service)
    // ファイル読み込み
    val loader = mock[MuseumExhibitLoadManager]
    handler.loadManager = Some(loader)
    
    // 転送展示物
    val exhibits = 0 to 3 map (_ => mock[MuseumExhibit])
    
    // 転送データ
    val roomDataList = List(BasicRoom, SmartRoom, GroupRoom) map (t => exhibitTransferDataOf(t, exhibits))
    
    // 空の転送データ
    val emptySourceRoom = exhibitTransferDataOf(None, exhibits)
  }
  
  def importDataToBasicRoom(f: Factory) = new RoomImportingTestBase(f, BasicRoom) {
    // 追加呼び出し
    var elms: List[MuseumExhibit] = Nil
    doAnswer{ case arg: Array[_] => elms = elms :+ arg(1).asInstanceOf[MuseumExhibit] }.when(service).addElement(any, any)
    
    def fromRoom = roomDataList map (ts => handler.importData(ts)) must not contain(false)
    
    def callsAddElement = {
      roomDataList foreach (ts => handler.importData(ts))
      elms must_== (exhibits ++ exhibits ++ exhibits)
    }
    
    def fromAnonRoom = handler.importData(emptySourceRoom) must beTrue
    
    def falseBySameRoom = {
      val t = exhibitTransferDataOf(targetRoom, exhibits)
      handler.importData(t) must beFalse
    }
    
    def byFile = {
      val files = 0 to 9 map (i => new File("file" + i))
      val t = creaateTransferData(files)
      handler.importData(t) must beTrue
    }
    
    def importsFiles = {
      val files = 0 to 2 map (i => new File("file" + i))
      val t = creaateTransferData(files)
      handler.importData(t)
      there was one(loader).loadExhibit(files(0)) then
        one(loader).loadExhibit(files(1)) then
        one(loader).loadExhibit(files(2))
    }
  }
  
  def importDataToGroupRoom(f: Factory) = new RoomImportingTestBase(f, GroupRoom) {
    def fromRoom = roomDataList map (d => handler.importData(d)) must not contain(false)
    
    def callsMoveRoom = {
      val room = roomOf(BasicRoom)
      handler.importData(exhibitTransferDataOf(Some(room), Nil))
      there was one(model).moveRoom(room, targetRoom)
    }
    
    def fromAnonRoom = handler.importData(emptySourceRoom) must beFalse
    
    def falseBySameRoom = {
      // 子孫部屋判定を true
      model.isDescendant(any, any) returns true
      handler.importData(exhibitTransferDataOf(targetRoom, Nil)) must beFalse
    }
    
    def falseByDesc = {
      // 子孫部屋判定を常に true
      model.isDescendant(any, any) returns true
      roomDataList map (d => handler.importData(d)) must not contain(true)
    }
  }
  
  def importDataToRoot(f: Factory) = new {
    val model = createModelMock
    val handler = spy(new Handler(model))
    // 最上層への転送
    handler.getImportingTarget(any) returns None
    
    def fromRoom = {
      val tss = List(BasicRoom, SmartRoom, GroupRoom) map (rt => creaateTransferSupport(roomOf(rt)))
      tss map handler.importData must not contain(false)
    }
    
    def fromAnonRoom = {
      val ts = new TransferSupport(mock[JTree], creaateExhibitTransferData(None))
      handler.importData(ts) must beFalse
    }
    
    def fromRoot = {
      // 子孫部屋判定を常に true
      model.isDescendant(any, any) returns true
      todo
      val tss = List(BasicRoom, SmartRoom, GroupRoom) map (rt => creaateTransferSupport(roomOf(rt)))
      tss map handler.importData must not contain(true)
    }
  }
  
  def importDataToSmartRoom = new {
    val handler = spy(createHandler)
    // SmartRoom への転送
    val targetRoom = roomOf(SmartRoom)
    handler.getImportingTarget(any) returns Some(targetRoom)
    
    def returnsFalse = {
      val tss = List(BasicRoom, SmartRoom, GroupRoom) map (rt => creaateTransferSupport(roomOf(rt)))
      tss map handler.importData must not contain(true)
    }
  }
}
