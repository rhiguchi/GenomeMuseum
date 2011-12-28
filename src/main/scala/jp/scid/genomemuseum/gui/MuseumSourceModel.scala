package jp.scid.genomemuseum.gui

import collection.script.{Message, Include, Update, Remove}

import jp.scid.gui.tree.{DataTreeModel, SourceTreeModel}
import jp.scid.genomemuseum.model.{MuseumStructure, ExhibitRoom, UserExhibitRoom}
import UserExhibitRoom.RoomType
import RoomType._
import DataTreeModel.Path

/**
 * ExhibitRoom のツリーモデルを保持するクラス。
 * 階層関係の追加、移動、削除を司る。
 */
class MuseumSourceModel(val source: MuseumStructure) extends DataTreeModel(source) {
  import MuseumSourceModel._
  
  /** 変更イベントの結合 */
  source.subscribe(new source.Sub {
    def notify(pub: source.Pub, event: Message[ExhibitRoom]) = event match {
      case Include(loc, room) => source.pathToRoot(room).reverse match {
        case Seq(_, parent, _*) => sourceTreeModel.someChildrenWereInserted(parent)
        case _ =>
      }
      case Update(loc, room) => sourceTreeModel.nodeChanged(room)
      case Remove(loc, room) => sourceTreeModel.nodeRemoved(room)
      case _ =>
    }
  })
  
  /** 部屋作成時の親となる部屋を返す */
  private def findInsertPath = {
    selectedPath.getOrElse(IndexedSeq.empty).reverse.dropWhile {
      case UserExhibitRoom(room @ RoomType(GroupRoom)) => false
      case _ => true
    } match {
      case path @ Seq(parent: UserExhibitRoom, _*) => (Some(parent), path.reverse)
      case _ => (None, pathForUserRooms)
    }
  }
  
  /**
   * 部屋をサービスに追加する。
   * 
   * @param roomType 部屋の種類
   * @return 新しい部屋までのパス
   * @see UserExhibitRoom
   */
  def addRoom(roomType: RoomType) = {
    val (parent, path) = findInsertPath
    val newRoom = source.addRoom(roomType, parent)
    fireSomeUserExhibitRoomWereInserted(parent)
    path :+ newRoom
  }
  
  /**
   * 新しい親へ移動する
   * @param element 移動する要素
   * @param newParent 異動先となる親要素。ルート項目にする時は None 。
   * @throws IllegalArgumentException 指定した親が GroupRoom ではない時
   * @throws IllegalStateException 指定した親が要素自身か、子孫である時
   */
  def moveRoom(element: UserExhibitRoom, newParent: Option[UserExhibitRoom]) {
    sourceTreeModel.nodeRemoved(element)
    source.moveRoom(element, newParent)
    fireSomeUserExhibitRoomWereInserted(newParent)
  }
  
  /**
   * 部屋を親から削除する。
   * {@code room} に子要素が存在する時は、その要素もサービスから除外される。
   * @param room 削除する要素
   */
  def removeSelections() {
    selectedPaths.sortBy(_.length).reverse.map(_.reverse).foreach {
      case Seq(room: UserExhibitRoom, tail @ _*) =>
        source.removeRoom(room)
        sourceTreeModel.nodeRemoved(room)
      case _ =>
    }
  }
  
  /** データサービス更新時にイベント送出に使用するルート要素 */
  private def dataServiceRoot = source.userRoomsRoot
  
  /** サービスからデータを読み込み直す */
  def reloadWithDataService() {
    sourceTreeModel.reset(dataServiceRoot)
  }
  
  /** ローカルライブラリノードへのパス */
  def pathForLocalLibrary: Path[ExhibitRoom] = source.pathToRoot(source.localSource)
  
  /** ユーザールームルートへのパス */
  def pathForUserRooms: Path[ExhibitRoom] = source.pathToRoot(source.userRoomsRoot)
  
  /** ローカルライブラリノードを選択状態にする */
  def selectPathLocalLibrary() {
    selectPath(pathForLocalLibrary)
  }
  
  /** TreeModel に要素追加イベントを発行させる */
  private def fireSomeUserExhibitRoomWereInserted(parent: Option[UserExhibitRoom]) {
    sourceTreeModel.someChildrenWereInserted(parent.getOrElse(dataServiceRoot))
  }
}

private object MuseumSourceModel {
  /** 最も近い GroupRoom である親を探索 */
  def findAncestorGroupRoom(path: Path[ExhibitRoom]) = {
    path.reverse.dropWhile {
      case UserExhibitRoom.RoomType(GroupRoom) => false
      case _ => true
    }
    .reverse
  }
}
