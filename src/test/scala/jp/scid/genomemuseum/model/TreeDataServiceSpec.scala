package jp.scid.genomemuseum.model

import org.specs2.mock.Mockito

object TreeDataServiceSpec extends Mockito {
  def makeMock[A](service: TreeDataService[A]) = {
    service.getChildren(any) returns Nil
    service.getParent(any) returns None
    service
  }
}