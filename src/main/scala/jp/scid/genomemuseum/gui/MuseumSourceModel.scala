package jp.scid.genomemuseum.gui

import jp.scid.gui.tree.{DataTreeModel, SourceTreeModel, TreeSource}
import jp.scid.genomemuseum.model.{MuseumStructure, ExhibitRoom,
  UserExhibitRoom, UserExhibitRoomService}
import UserExhibitRoom.RoomType._
import DataTreeModel.Path

/**
 * ExhibitRoom のツリーモデルを保持するクラス。
 * 階層関係の追加、移動、削除を司る。
 */
class MuseumSourceModel(source: MuseumStructure) extends DataTreeModel(source) {
  import MuseumSourceModel._
  
  var basicRoomDefaultName = "Basic Room"
  var groupRoomDefaultName = "Group Room"
  var smartRoomDefaultName = "Smart Room"
  
  private var currentDataService: Option[UserExhibitRoomService] = None
  
  /**
   * 未使用の名前を検索する。
   * {@code baseName} の名前を持つ部屋がサービス中に存在するとき、
   * 連番をつけて次の名前を検索する。
   * @param baseName 基本の名前
   * @return 他と重複しない、部屋の名前。
   */
  private def findRoomNewName(baseName: String) = {
    def searchNext(index: Int): String = {
      val candidate = baseName + " " + index
      if (dataService.nameExists(candidate)) {
        searchNext(index + 1)
      }
      else
        candidate
    }
    
    if (dataService.nameExists(baseName)) {
      searchNext(1)
    }
    else
      baseName
  }
  
  /** 選択された UserExhibitRoom を削除 */
  def removeSelectedUserRoom() {
    // 削除対象の Room を取得
    val rooms = selectedPaths.sortWith(_.length > _.length)
      .map(_.last).collect{ case e: UserExhibitRoom => e }
    rooms foreach removeRoom
  }
  
  /**
   * 選択中のパスに部屋を追加する。
   * @return 追加された場所を示すパス
   */
  def addUserRoomToSelectedPath(roomType: RoomType) = {
    val baseName = roomType match {
      case BasicRoom => basicRoomDefaultName
      case GroupRoom => groupRoomDefaultName
      case SmartRoom => smartRoomDefaultName
    }
    val name = findRoomNewName(baseName)
    
    // 親を取得
    val parentPathOp = selectedPath.map(findAncestorGroupRoom) match {
      case Some(Path()) => None
      case other => other
    }
    val parent = parentPathOp.map(_.last.asInstanceOf[UserExhibitRoom])
    
    val room = addRoom(roomType, name, parent)
    selectPath(parentPathOp.getOrElse(pathForUserRooms) :+ room)
    
    room
  }
  
  /**
   * 部屋をサービスに追加する。
   * @param roomType 部屋の種類
   * @param name 表示名
   * @param parent 親要素
   * @return 追加に成功した場合、そのオブジェクトが返る。
   * @throws IllegalArgumentException
   *         {@code parent#roomType} が {@code GroupRoom} 以外の時
   * @throws IllegalStateException dataService が 設定されていない時
   * @see UserExhibitRoom
   */
  def addRoom(roomType: RoomType, name: String,
      parent: Option[UserExhibitRoom]): UserExhibitRoom = {
    parent match {
      case Some(elm) if elm.roomType != GroupRoom =>
        throw new IllegalArgumentException("roomType of parent must be GroupRoom")
      case _ =>
    }
    
    if (currentDataService.isEmpty)
      throw new IllegalStateException("to add room require a dataService but not served")
    
    val newRoom = dataService.addRoom(roomType, name, parent)
    fireSomeUserExhibitRoomWereInserted(parent)
    newRoom
  }
  
  /**
   * 新しい親へ移動する
   * @param element 移動する要素
   * @param newParent 異動先となる親要素。ルート項目にする時は None 。
   * @throws IllegalArgumentException 指定した親が GroupRoom ではない時
   * @throws IllegalStateException 指定した親が要素自身か、子孫である時
   */
  def moveRoom(element: UserExhibitRoom, newParent: Option[UserExhibitRoom]) {
    newParent match {
      case Some(parent @ UserExhibitRoom.RoomType(GroupRoom)) =>
        val elmPath = source.pathToRoot(element)
        val destPath = source.pathToRoot(parent)
        if (destPath.startsWith(elmPath))
          throw new IllegalStateException("'%s' is not allowed to move to '%s'"
            .format(elmPath, destPath))
        elmPath(elmPath.size - 2)
      case None => dataServiceRoot
      case _ => throw new IllegalArgumentException("parent must be a GroupRoom")
    }
    
    val oldParent = source.userExhibitRoomSource.getParent(element)
    dataService.setParent(element, newParent)
    fireSomeUserExhibitRoomWereRemoved(oldParent)
    fireSomeUserExhibitRoomWereInserted(newParent)
  }
  
  /**
   * 現在のユーザーボックスのデータソースを取得
   */
  def dataService = currentDataService.get
  
  /**
   * ユーザーボックスのデータソースを設定する。
   */
  def dataService_=(newDataService: UserExhibitRoomService) {
    currentDataService = Option(newDataService)
    source.userExhibitRoomSource = newDataService
    reloadWithDataService()
  }
  
  /**
   * 部屋を親から削除する。
   * {@code room} に子要素が存在する時は、その要素もサービスから除外される。
   * @param room 削除する要素
   */
  def removeRoom(room: UserExhibitRoom) {
    val parent = dataService.getParent(room)
    dataService.remove(room)
    fireSomeUserExhibitRoomWereRemoved(parent)
  }
  
  /** データサービス更新時にイベント送出に使用するルート要素 */
  private def dataServiceRoot = source.userRoomsRoot
  
  /** サービスからデータを読み込み直す */
  def reloadWithDataService() {
    sourceTreeModel.reset(dataServiceRoot)
  }
  
  /** ローカルライブラリノードへのパス */
  val pathForLocalLibrary: Path[ExhibitRoom] =
    source.pathToRoot(source.localSource)
  
  /** ライブラリノードへのパス */
  def pathForLibraries: Path[ExhibitRoom] =
    source.pathToRoot(source.sourcesRoot)
  
  /** ユーザールームルートへのパス */
  def pathForUserRooms: Path[ExhibitRoom] =
    source.pathToRoot(source.userRoomsRoot)
  
  /** ローカルライブラリノードを選択状態にする */
  private def selectPathLocalLibrary() {
    selectPath(pathForLocalLibrary)
  }
  
  /** TreeModel に要素削除イベントを発行させる */
  private def fireSomeUserExhibitRoomWereRemoved(parent: Option[UserExhibitRoom]) {
    sourceTreeModel.someChildrenWereRemoved(parent.getOrElse(dataServiceRoot))
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
