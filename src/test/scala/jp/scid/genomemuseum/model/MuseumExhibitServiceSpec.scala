package jp.scid.genomemuseum.model

import collection.script.Message

object MuseumExhibitServiceMock extends org.specs2.mock.Mockito {
  def of(elements: MuseumExhibit*) = {
    val s = mock[MuseumExhibitService]
    s.exhibitList returns elements.toList.asInstanceOf[List[s.ElementClass]]
//    s.getExhibits(any) returns Nil
    s
  }
}
