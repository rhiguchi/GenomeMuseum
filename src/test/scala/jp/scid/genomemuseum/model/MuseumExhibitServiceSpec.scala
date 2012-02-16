package jp.scid.genomemuseum.model

import collection.script.Message

object MuseumExhibitServiceMock extends org.specs2.mock.Mockito {
  def of(elements: MuseumExhibit*) = {
    val s = spy(new EmptyMuseumExhibitService)
    s.exhibitList returns elements.toList.asInstanceOf[List[s.ElementClass]]
    s.getExhibits(any) returns Nil
    s
  }
}

class EmptyMuseumExhibitService extends MuseumExhibitService {
  type ElementClass = MuseumExhibit
  
  def exhibitList: List[MuseumExhibit] = Nil
  def create() = MuseumExhibitMock.of("mock")
  def save(element: MuseumExhibit) {}
  def remove(element: MuseumExhibit) = false
  def getExhibits(room: UserExhibitRoom): List[MuseumExhibit] = Nil
  def addElement(room: UserExhibitRoom, item: MuseumExhibit) {}
}