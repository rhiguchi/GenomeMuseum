package jp.scid.genomemuseum.controller

import org.specs2._
import jp.scid.genomemuseum.view.FileContentView

class FileContentViewerSpec extends Specification {
  def is = "FileContentViewer" ^
    "source" ^
      "document に格納される" ! souce.s1 ^
      "document の更新" ! souce.s2 ^
      "キャレット位置が 0" ! souce.s3
  
  trait TestBase {
    val sourceTest = """line1
                       |line2
                       |line3
                       |""".stripMargin
    val source = sourceTest.split("\n").toIterator
    
    val view = new FileContentView()
    
    val viewer = new FileContentViewer(view)
    
    def document = viewer.document
    
    viewer.source = source
  }
  
  def souce = new TestBase {
    val sourceTest2 = List("SOURCE").toIterator
    
    def s1 = document.getText(0, document.getLength) must_== sourceTest
    
    def s2 = {
      viewer.source = sourceTest2
      document.getText(0, 6) must_== "SOURCE"
    }
    
    def s3 = view.textArea.getCaretPosition must_== 0
  }
}
