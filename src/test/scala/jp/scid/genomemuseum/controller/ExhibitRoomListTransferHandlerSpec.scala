package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import javax.swing.{JTree, TransferHandler}
import java.awt.datatransfer.{Transferable, DataFlavor}
import TransferHandler.TransferSupport
import DataFlavor.javaFileListFlavor

import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom,
  MuseumStructure, MuseumExhibit}
import UserExhibitRoom.RoomType._

object ExhibitRoomListTransferHandlerSpec extends Mockito {
  case class ExhibitRoomTransferDataImpl(transferPath: IndexedSeq[ExhibitRoom] = IndexedSeq.empty) extends ExhibitRoomTransferData
}

class ExhibitRoomListTransferHandlerSpec extends Specification with Mockito {
  private type Handler = ExhibitRoomListTransferHandler
  import ExhibitRoomListTransferHandlerSpec._
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  import MuseumExhibitTransferData.{dataFlavor => exhibitListDataFlavor}
  
  def is = "ExhibitRoomListTransferHandler" ^
    "転出元がある転送" ^ canExportStateSpec(handlerWithTransferSource) ^ bt ^
    "転出元がない転送" ^ cannotExportStateSpec(handlerWithNoTransferSource) ^ bt ^
    "転送物が部屋の時" ^ importingRoomSpec(handlerWithDropTargetOfLocalSource) ^ bt ^
    "転送物が展示物の時" ^ importingExhibitsSpec(handlerWithDropTargetOfLocalSource) ^ bt ^
    "転送物がファイルの時" ^ importingFilesSpec(handlerWithDropTargetOfLocalSource) ^ bt ^
  end
  
  def controllerMock = {
    val ctrl = mock[ExhibitRoomListController]
    ctrl.sourceStructure returns new MuseumStructure
    ctrl
  }
  
  def handlerWithNoTransferSource = {
    val controller = controllerMock
    controller.transferSource returns None
    
    new Handler(controller)
  }
  
  def userRoomOf(roomType: RoomType) = {
    val room = mock[UserExhibitRoom]
    room.roomType returns roomType
    room
  }
  
  def handlerWithTransferSource = {
    val controller = controllerMock
    val roomMock = mock[ExhibitRoom]
    controller.transferSource returns Some(roomMock)
    
    new Handler(controller)
  }
  
  def handlerWithDropTargetOf(room: UserExhibitRoom) = {
    val controller = controllerMock
    val str = controller.sourceStructure
    val path = str.pathToRoot(str.userRoomsRoot) :+ room
    controller.getImportingTargetPath(any) returns path
    
    new Handler(controller)
  }
  
  def handlerWithDropTargetOfLocalSource = {
    val controller = controllerMock
    val str = controller.sourceStructure
    val path = str.pathToRoot(str.webSource)
    controller.getImportingTargetPath(any) returns path
    
    new Handler(controller)
  }
  
  def handlerWithDropTargetOfWebSource = {
    val controller = controllerMock
    val str = controller.sourceStructure
    val path = str.pathToRoot(str.localSource)
    controller.getImportingTargetPath(any) returns path
    
    new Handler(controller)
  }
  
  lazy val view = new JTree
  
  def canExportStateSpec(h: => Handler) =
    "getSourceActions が MOVE_OR_COPY" ! todo ^
    "createTransferable が ExhibitRoomTransferData を返す" ! todo
  
  def cannotExportStateSpec(h: => Handler) =
    "getSourceActions が NONE" ! todo ^
    "createTransferable が null" ! todo
  
  def importingToLocalSourceSpec(h: => Handler) =
    "ExhibitRoomTransferData.dataFlavor は受け入れ不可" ! todo ^
    "ExhibitRoomTransferData.dataFlavor は転入できない" ! todo ^
    "MuseumExhibitListTransferHandler.dataFlavor は受け入れ不可" ! todo ^
    "MuseumExhibitListTransferHandler.dataFlavor は転入できない" ! todo ^
    "javaFileListFlavor が受け入れ可" ! todo ^
    "javaFileListFlavor が転入できない" ! todo
  
  def importingToSWebSourceSpec(h: => Handler) =
    "ExhibitRoomTransferData.dataFlavor は受け入れ拒否" ! todo ^
    "ExhibitRoomTransferData.dataFlavor は転入できない" ! todo ^
    "MuseumExhibitListTransferHandler.dataFlavor は受け入れ拒否" ! todo ^
    "MuseumExhibitListTransferHandler.dataFlavor は転入できない" ! todo ^
    "javaFileListFlavor は受け入れ拒否" ! todo ^
    "javaFileListFlavor が転入できない" ! todo
  
  def importingToBasicRoomSpec(h: => Handler) =
    "ExhibitRoomTransferData.dataFlavor は受け入れ許可" ! todo ^
    "ExhibitRoomTransferData.dataFlavor が転入できる" ! todo ^
    "MuseumExhibitListTransferHandler.dataFlavor は受け入れ許可" ! todo ^
    "MuseumExhibitListTransferHandler.dataFlavor が転入できる" ! todo ^
    "javaFileListFlavor は受け入れ許可" ! todo ^
    "javaFileListFlavor が転入できる" ! todo
  
  def importingRoomSpec(h: => Handler) =
    "転入先が空の時、UserExhibitRoom の受け入れ許可" ! canImportRoom.userRoomToRoot ^
    "転入先が空の時、ExhibitRoom の受け入れ拒否" ! canImportRoom.otherRoomToRoot ^
    "転入先が空ではない時、操作クラスが true で許可" ! canImportRoom.toRootTrue ^
    "転入先が空ではない時、操作クラスが false で拒否" ! canImportRoom.toRootFalse
  
  def importingExhibitsSpec(h: => Handler) =
    "転入先が空の時、受け入れ拒否" ! canImportExhibit.falseToRoot ^
    "転入先が BasicRoom の時、受け入れ許可" ! canImportExhibit.trueToBasicRoom ^
    "転入先が GroupRoom の時、受け入れ拒否" ! canImportExhibit.falseToGroupRoom ^
    "転入先が SmartRoom の時、受け入れ拒否" ! canImportExhibit.falseToSmartRoom
  
  def importingFilesSpec(h: => Handler) =
    "転入先が空の時、受け入れ拒否" ! canImportFiles.falseToRoot ^
    "転入先が BasicRoom の時、受け入れ許可" ! canImportFiles.trueToBasicRoom ^
    "転入先が GroupRoom の時、受け入れ拒否" ! canImportFiles.falseToGroupRoom ^
    "転入先が SmartRoom の時、受け入れ拒否" ! canImportFiles.falseToSmartRoom ^
    "転入先が LocalSource の時、受け入れ許可" ! canImportFiles.trueToLocalSource
  
  def importingToSmartRoomSpec(h: => Handler) =
    "ExhibitRoomTransferData.dataFlavor は受け入れ拒否" ! todo ^
    "ExhibitRoomTransferData.dataFlavor は転入できない" ! todo ^
    "MuseumExhibitListTransferHandler.dataFlavor は受け入れ拒否" ! todo ^
    "MuseumExhibitListTransferHandler.dataFlavor は転入できない" ! todo ^
    "javaFileListFlavor は受け入れ拒否" ! todo ^
    "javaFileListFlavor が転入できない" ! todo
  
  
  def canImportRoom = new Object {
    val ctrl = controllerMock
    // ターゲット無し（ViewPort 想定）
    ctrl.getImportingTargetPath(any) returns IndexedSeq.empty
    
    val handler = new Handler(ctrl)
    
    def transferSupportOf(room: ExhibitRoom) = {
      val t = mock[Transferable]
      t.isDataFlavorSupported(exhibitRoomDataFlavor) returns true
      t.getTransferData(exhibitRoomDataFlavor) returns ExhibitRoomTransferDataImpl(IndexedSeq(room))
      
      new TransferSupport(view, t)
    }
    
    def userRoomToRoot = {
      handler.canImport(transferSupportOf(userRoomOf(BasicRoom))) must beTrue
    }
    
    def otherRoomToRoot = {
      handler.canImport(transferSupportOf(mock[ExhibitRoom])) must beFalse
    }
    
    def toRootTrue = {
      val room = mock[ExhibitRoom]
      ctrl.getImportingTargetPath(any) returns IndexedSeq(room)
      ctrl.canMove(any, any) returns true
      handler.canImport(transferSupportOf(room)) must beTrue
    }
    
    def toRootFalse = {
      val room = mock[ExhibitRoom]
      ctrl.getImportingTargetPath(any) returns IndexedSeq(room)
      ctrl.canMove(any, any) returns false
      handler.canImport(transferSupportOf(room)) must beFalse
    }
  }
  
  def canImportExhibit = new Object {
    val ctrl = controllerMock
    val handler = new Handler(ctrl)
    
    def transferSupportOfExhibits = {
      val t = mock[Transferable]
      t.isDataFlavorSupported(exhibitListDataFlavor) returns true
      t.getTransferData(exhibitListDataFlavor) returns IndexedSeq.empty[MuseumExhibit]
      
      new TransferSupport(view, t)
    }
    
    def falseToRoot = {
      ctrl.getImportingTargetPath(any) returns IndexedSeq.empty
      handler.canImport(transferSupportOfExhibits) must beFalse
    }
    
    def trueToBasicRoom = {
      ctrl.getImportingTargetPath(any) returns IndexedSeq(userRoomOf(BasicRoom))
      handler.canImport(transferSupportOfExhibits) must beTrue
    }
    
    def falseToGroupRoom = {
      ctrl.getImportingTargetPath(any) returns IndexedSeq(userRoomOf(GroupRoom))
      handler.canImport(transferSupportOfExhibits) must beFalse
    }
    
    def falseToSmartRoom = {
      ctrl.getImportingTargetPath(any) returns IndexedSeq(userRoomOf(SmartRoom))
      handler.canImport(transferSupportOfExhibits) must beFalse
    }
  }
  
  def canImportFiles = new Object {
    val ctrl = controllerMock
    val handler = new Handler(ctrl)
    
    def transferSupportOfFiles = {
      val t = mock[Transferable]
      t.isDataFlavorSupported(javaFileListFlavor) returns true
      t.getTransferData(javaFileListFlavor) returns new java.util.ArrayList[java.io.File]
      
      new TransferSupport(view, t)
    }
    
    def falseToRoot = {
      ctrl.getImportingTargetPath(any) returns IndexedSeq.empty
      handler.canImport(transferSupportOfFiles) must beFalse
    }
    
    def trueToBasicRoom = {
      ctrl.getImportingTargetPath(any) returns IndexedSeq(userRoomOf(BasicRoom))
      handler.canImport(transferSupportOfFiles) must beTrue
    }
    
    def falseToGroupRoom = {
      ctrl.getImportingTargetPath(any) returns IndexedSeq(userRoomOf(GroupRoom))
      handler.canImport(transferSupportOfFiles) must beFalse
    }
    
    def falseToSmartRoom = {
      ctrl.getImportingTargetPath(any) returns IndexedSeq(userRoomOf(SmartRoom))
      handler.canImport(transferSupportOfFiles) must beFalse
    }
    
    def trueToLocalSource = {
      ctrl.getImportingTargetPath(any) returns IndexedSeq(ctrl.sourceStructure.localSource)
      handler.canImport(transferSupportOfFiles) must beTrue
    }
  }
}
