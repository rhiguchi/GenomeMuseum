package jp.scid.genomemuseum.gui

import java.util.Date

import collection.script.Message

import ca.odell.glazedlists.gui.TableFormat

import jp.scid.gui.StringFilterable
import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitService,
  UserExhibitRoom}

/**
 * データサービスから展示物を取得して表示するテーブルモデル
 * 
 * @param dataService モデルに表示するサービス。
 * @param tableFormat 表表示のモデル
 */
class ExhibitTableModel(dataService: MuseumExhibitService, tableFormat: TableFormat[MuseumExhibit])
    extends DataTableModel[MuseumExhibit](tableFormat)
    with StringFilterable[MuseumExhibit] with TableColumnSortable[MuseumExhibit] {
  
  def this(dataService: MuseumExhibitService) = this(dataService, new ExhibitTableFormat)
  
  /** 現在の部屋 */
  private var currentUserExhibitRoom: Option[UserExhibitRoom] = None
  /** サービス変化を監視してモデルの再読み込みを行うアダプタ */
  private val reloadingHandler = EventQueuePublisherAdapter(dataService) { _ =>
    reloadSource()
  }
  
  /** ソースの再読み込み */
  protected[gui] def reloadSource() {
    source = userExhibitRoom match {
      case Some(room) => dataService.getExhibits(room)
      case None => dataService.allElements
    }
  }
  
  // プロパティ
  /** 展示物を表示する部屋 */
  def userExhibitRoom = currentUserExhibitRoom
  
  def userExhibitRoom_=(room: Option[UserExhibitRoom]) {
    currentUserExhibitRoom = room
    reloadSource()
  }
  
  protected def getFilterString(base: java.util.List[String], e: MuseumExhibit) {
    base add e.name
    base add e.source
  }
  
  reloadSource()
}
