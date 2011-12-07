package jp.scid.genomemuseum

import org.specs2._
import GenomeMuseumGUI.RunMode._
import util.control.Exception.allCatch

@org.junit.runner.RunWith(classOf[runner.JUnitRunner])
class GenomeMuseumGUISpec extends Specification {
  def is = "GenomeMuseumGUI" ^
    "初期化動作" ^ initializeSpec(appSimple) ^ bt ^
    "起動動作" ^ startupSpec(appSimple) ^ bt ^
    "オブジェクト" ^ companionObjectSpec ^ bt ^
    end
  
  def appSimple = {
    new GenomeMuseumGUI(Testing)
  }
  
  def initializeSpec(app: => GenomeMuseumGUI) =
    "引数指定無し" ! initialize(app).noArg
  
  def startupSpec(app: => GenomeMuseumGUI) =
    "引数指定無し initialize 後" ! startup(app).basic
  
  def companionObjectSpec =
    "コンテキストの取得" ! companionObject.getContext
  
  def initialize(app: GenomeMuseumGUI) = new Object {
    def noArg = {
      app.initialize(new Array[String](0))
      success
    }
  }
  
  def startup(app: GenomeMuseumGUI) = new Object {
    app.initialize(new Array[String](0))
    
    def basic = {
      app.startup
      success
    }
  }
  
  def companionObject = new Object {
    def getContext = {
      allCatch.opt(GenomeMuseumGUI.applicationContext) must beSome
    }
  }
}
