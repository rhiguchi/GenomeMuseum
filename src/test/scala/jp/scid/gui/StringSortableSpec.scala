package jp.scid.gui

import org.specs2._

import java.util.Comparator

class StringSortableSpec extends Specification {
  def is = "StringSortable" ^
    "初期状態" ! s1 ^
    "並び替え記述指定1" ! s2 ^
    "並び替え記述指定2" ! s3
  
  class TestBase {
    val stringComparator = new Comparator[String] {
      def compare(o1: String, o2: String) = {
        o1.compareToIgnoreCase(o2)
      }
    }
    
    val reverseStringComparator = new Comparator[String] {
      def compare(o1: String, o2: String) = {
        o2.compareToIgnoreCase(o1)
      }
    }
    
    val model = new DataListModel[String] with StringSortable[String] {
      def comparatorFor(orderStatement: String) = orderStatement match {
        case "reverse" => reverseStringComparator
        case _ => stringComparator
      }
    }
    
    model.source = List("apple", "SAMPLE", "bread")
    
    def viewSource = model.viewSource
  }
  
  def base = new TestBase {
    def sort = {
      model sortWith "any"
      viewSource
    }
    
    def reverse = {
      model sortWith "reverse"
      viewSource
    }
  }
  
  def s1 = base.viewSource must contain("apple", "SAMPLE", "bread").only.inOrder
  
  def s2 = base.sort must contain("apple", "bread", "SAMPLE").only.inOrder
  
  def s3 = base.reverse must contain("SAMPLE", "bread", "apple").only.inOrder
}
