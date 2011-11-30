package jp.scid.genomemuseum.gui

import org.specs2._
import mock._

import jp.scid.gui.DataListModel.waitEventListProcessing

import jp.scid.genomemuseum.model.{MuseumExhibitService, MuseumExhibit,
  MuseumExhibitServiceSpec}

class ExhibitTableModelSpec extends Specification with Mockito {
  def is = "ExhibitTableModel" ^
    "再読み込み" ^ canReloadSource(modelWithService) ^ bt ^
    "要素の削除" ^ canRemove(modelWithServiceAndElement) ^ bt ^
    "選択要素の削除" ^ canRemoveSelections(modelWithServiceAndElement) ^ bt ^
    end
  
  def modelWithService = {
    val model = new ExhibitTableModel
    val service = mock[MuseumExhibitService]
    MuseumExhibitServiceSpec.makeMock(service)
    model.dataService = service
    model
  }
  
  def modelWithServiceAndElement = {
    val model = modelWithService
    val service = model.dataService
    val e1, e2, e3, e4, e5 = mock[MuseumExhibit]
    service.allElements returns List(e1, e2, e3, e4, e5).map(_.asInstanceOf[service.ElementClass])
    model.reloadSource
    waitEventListProcessing()
    model.select(e2, e3)
    model
  }
  
  def canReloadSource(model: => ExhibitTableModel) =
    "サービスから読み込まれる" ! serviceOf(model).reload
  
  def canRemove(model: => ExhibitTableModel) =
    "サービスから削除" ! serviceOf(model).remove ^
    "ソースから削除" ! serviceOf(model).removeFromSource
  
  def canRemoveSelections(model: => ExhibitTableModel) =
    "サービスから削除" ! serviceOf(model).removeSelection ^
    "ソースから削除" ! serviceOf(model).removeSelectionFromSource
  
  def serviceOf(model: ExhibitTableModel) = new Object {
    def reload = {
      val service = model.dataService
      val e1, e2, e3, e4, e5 = mock[MuseumExhibit]
      service.allElements returns List(e1, e2, e3, e4, e5).map(_.asInstanceOf[service.ElementClass])
      model.reloadSource()
      model.source must contain(e1, e2, e3, e4, e5).only.inOrder
    }
    
    def remove = {
      val e1 = model.source.head
      model.removeElement(e1)
      val s = one(model.dataService)
      there was s.remove(e1.asInstanceOf[s.ElementClass])
    }
    
    def removeFromSource = {
      val service = model.dataService
      val e1 = model.source.head
      model.removeElement(e1)
      model.source must not contain(e1)
    }
    
    def removeSelection = {
      val service = model.dataService
      val List(e1, e2) = model.selections
      model.removeSelections()
      val s = one(service)
      there was s.remove(e1.asInstanceOf[s.ElementClass]) then
        s.remove(e2.asInstanceOf[s.ElementClass])
    }
    
    def removeSelectionFromSource = {
      val service = model.dataService
      val List(e1, e2) = model.selections
      model.removeSelections()
      model.source must not contain(e1, e2)
    }
  }
}
