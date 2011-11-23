package jp.scid.genomemuseum.gui

import org.specs2._
import mock._

import java.util.Date

import jp.scid.gui.DataListModel
import jp.scid.genomemuseum.model.ListDataService

class ListDataServiceSourceSpec extends Specification with Mockito {
  def is = "ListDataServiceSource" ! pending
//    "サービスの要素をモデルへ適用" ! s1 ^
//    "再読み込み" ! s2 ^
//    "選択要素削除" ! s3 ^
//    "要素の追加" ^
//      "サービスの create コール" ! s4 ^
//      "モデルのソースに新しい要素が適用" ! s5 ^
//    bt ^ "要素の更新" ^
//      "サービスの save コール" ! s6 ^
//      "モデルの項目の更新" ! s7
//  
//  val model = new DataListModel[Symbol] with ListDataServiceSource[Symbol]
//  
//  val elementsA = List('element1, 'element2, 'element3)
//  val elementsB = elementsA :+ 'element4
//  
//  val service = mock[ListDataService[Symbol]]
//  service.allElements returns elementsA
//  
//  model.dataService = service
//  val s1_source = model.source.toList
//  
//  // 項目の更新
//  service.allElements returns elementsB
//  model.reloadSource()
//  val s2_source = model.source.toList
//  
//  // 要素選択後、削除
//  model.selections = List('element2, 'element4)
//  model.removeSelections()
//  val s3_source = model.source.toList
//  val elementC = List('element1, 'element3)
//  
//  // 要素の追加
//  val newElm1 = Symbol("")
//  val newElm2 = 'newElement
//  service.create() returns (newElm1, newElm2)
//  
//  model.createElement()
//  model.createElement()
//  val s5_source = model.source.toList
//  val elementD = elementC :+ newElm1 :+ newElm2
//  
//  // 要素更新通知
//  service.indexOf(newElm2) returns 0
//  model.updateElement(newElm2)
//  val s7_source = model.source.toList
//  
//  def s1 = s1_source must_== elementsA
//  
//  def s2 = s2_source must_== elementsB
//  
//  def s3_1 = there was one(service).remove('element2)
//  def s3_2 = there was one(service).remove('element4)
//  def s3_3 = s3_source must_== elementC
//  def s3 = s3_1 and s3_2 and s3_3
//  
//  def s4_1 = there was two(service).create()
//  def s4 = s4_1 
//  
//  def s5 = s5_source must_== elementD
//  
//  def s6 = there was one(service).save(newElm2)
//  
//  def s7 = s7_source.head must_== newElm2
}
