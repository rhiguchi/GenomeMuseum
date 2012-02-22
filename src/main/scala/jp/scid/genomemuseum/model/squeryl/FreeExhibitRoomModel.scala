package jp.scid.genomemuseum.model.squeryl

import jp.scid.genomemuseum.model.{FreeExhibitRoomModel => IFreeExhibitRoomModel,
  MuseumExhibit => IMuseumExhibit}

/**
 * 自由に展示物を入れ替えできる部屋のモデル
 * 
 * @param roomId 部屋の ID
 * @param table 部屋内容テーブル
 */
class FreeExhibitRoomModel extends ExhibitRoomModel with IFreeExhibitRoomModel {
  /** 部屋の中身 */
  var contentList: java.util.List[RoomExhibit] = null

  /** 部屋の中身を指定して初期化 */
  def this(contentList: java.util.List[RoomExhibit]) {
    this()
    this.contentList = contentList
  }
  
  /**
   * 要素を追加する。
   * 
   * 要素を ID で DB からルックアップし、存在するときは追加される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def addContent(element: IMuseumExhibit) =
    contentList.add(RoomExhibit(roomId, element.id))
  
  /**
   * インデックスの展示物を置換する。
   * 
   * 要素を ID で DB からルックアップし、存在するときは追加される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def setContent(index: Int, element: IMuseumExhibit) = {
    val oldExhibit = get(index)
    val content = contentList.get(index)
    content.exhibitId = element.id
    contentList.set(index, content)
    oldExhibit
  }
  
  /**
   * 指定項目番目の要素を除去する。
   */
  def removeContent(index: Int) = {
    val removed = get(index)
    contentList.remove(index)
    removed
  }
}
