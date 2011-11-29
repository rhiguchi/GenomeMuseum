package jp.scid.gui

import org.specs2._

import java.util.Comparator
import java.util.Collections

import ca.odell.glazedlists.GlazedLists

import jp.scid.gui.event.OrderStatementChanged

class StringSortableSpec extends Specification {
  def is = "StringSortable" ^
    "並び替え記述指定" ^ orderStatementSpec ^ bt ^
    "イベント発行" ^ canPublishOrderChangedEvent(emptyModel) ^ bt ^
    end
  
  def canPublishOrderChangedEvent[A](m: => DataListModel[A] with StringSortable[A]) =
    "イベントオブジェクト" ! publishOrderChangedEvent(m).s1 ^
    "記述文字列" ! publishOrderChangedEvent(m).s2
  
  def orderStatementSpec =
    "設定と取得" ! orderStatement.s1 ^
    "並び替えが行われる" ! orderStatement.s2 ^
    "comparatorFor から取得" ! orderStatement.s3
  
  def emptyModel = new DataListModel[String] with StringSortable[String] {
    val c = GlazedLists.comparableComparator[String]
    def comparatorFor(orderStatement: String) = orderStatement match {
      case "order" => c
      case "reverse" => Collections.reverseOrder(c)
      case _ => null
    }
  }
  
  def publishOrderChangedEvent[A](m: DataListModel[A] with StringSortable[A]) = new Object {
    var eventSource: Option[DataListModel[_] with StringSortable[_]] = None
    var eventValue: Option[String] = None
    
    m.reactions += {
      case OrderStatementChanged(source, newValue) =>
        eventSource = Some(source)
        eventValue = Some(newValue)
    }
    
    m.orderStatement = "stmt"
      
    def s1 = eventSource must beSome(m)
    
    def s2 = eventValue must beSome("stmt")
  }
  
  def orderStatement = new Object {
    val model = emptyModel
    val items = List("b", "a", "c")
    model.source = items
    
    def s1 = {
      model.orderStatement = "order"
      model.orderStatement must_== "order"
    }
    
    def s2 = {
      model.orderStatement = "order"
      model.viewSource must contain("a", "b", "c").only.inOrder
    }
    
    def s3 = {
      model.orderStatement = "reverse"
      model.viewSource must contain("c", "b", "a").only.inOrder
    }
  }
}
