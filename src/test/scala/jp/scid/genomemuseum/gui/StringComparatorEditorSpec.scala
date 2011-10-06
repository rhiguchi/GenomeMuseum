package jp.scid.genomemuseum.gui

import org.specs2.mutable._
import java.util.Comparator
import scala.swing.event.Event

class StringComparatorEditorSpec extends Specification {
  "StringComparatorEditor" should {
    "記述設定と記述取得" in {
      val editor = new StringComparatorEditor(stmt => ComparatorEditor.noOrder[String])
      
      editor.orderStatement must_== ""
      editor.orderStatement = "order"
      editor.orderStatement must_== "order"
    }
    
    "記述設定と比較器取得" in {
      val nameComp = new Comparator[String] {
        def compare(o1: String, o2: String) = 0
      }
      val addrComp = new Comparator[String] {
        def compare(o1: String, o2: String) = 0
      }
      val factory = (stmt: String) => stmt match {
        case "name" => nameComp
        case "addr" => addrComp
      }
      val editor = new StringComparatorEditor(factory)
      
      editor.orderStatement = "name"
      editor.comparator must_== nameComp
      editor.orderStatement = "addr"
      editor.comparator must_== addrComp
    }
    
    "記述設定とイベント" in {
      val nameComp = new Comparator[String] {
        def compare(o1: String, o2: String) = 0
      }
      val addrComp = new Comparator[String] {
        def compare(o1: String, o2: String) = 0
      }
      val factory = (stmt: String) => stmt match {
        case "name" => nameComp
        case "addr" => addrComp
      }
      val editor = new StringComparatorEditor(factory)
      var published = false
      editor.reactions += { case _ => published = true }
      
      published must beFalse
      editor.orderStatement = "name"
      published must beTrue
    }
  }
}

