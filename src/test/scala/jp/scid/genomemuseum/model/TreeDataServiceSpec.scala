package jp.scid.genomemuseum.model

import org.specs2.mutable._

class TreeDataServiceSpec extends Specification {
  // 項目クラスは ExhibitRoom
  def defaultTreeDataService = {
    TreeDataService[ExhibitRoom]()
  }
  "TreeDataService" should {
    "項目数の取得" in {
      val service = defaultTreeDataService
      service.count must_== 0
    }
    
    "項目追加" in {
      val service = defaultTreeDataService
      val room1 = ExhibitRoom("room1")
      val room2 = ExhibitRoom("room2")
      service add room1 
      service add room2 
      
      "項目数の増加" in {
        service.count must_== 2
      }
      
      "ルート項目への追加" in {
        val items = service.rootItems
        items.size must_== 2
        items must contain(room1)
        items must contain(room2)
      }
    }
    
    "子項目追加" in {
      val service = defaultTreeDataService
      val parent = ExhibitRoom("parent")
      val child1 = ExhibitRoom("child1")
      val child2 = ExhibitRoom("child2")
      service add parent 
      service.add(child1, Some(parent))
      service.add(child2, Some(parent))
      
      "項目数の増加" in {
        service.count must_== 3
      }
      
      "子項目の取得" in {
        val items = service.getChildren(parent)
        items.size must_== 2
        items must contain(child1)
        items must contain(child2)
      }
      
      "親要素取得" in {
        service.getParent(child1) must_== Some(parent)
        service.getParent(child2) must_== Some(parent)
        service.getParent(parent) must_== None
      }
    }
    
    "要素削除" in {
      val service = defaultTreeDataService
      val parent = ExhibitRoom("A")
      val parent2 = ExhibitRoom("B")
      service add parent 
      service add parent2
      service.remove(parent2)
      val child1 = ExhibitRoom("A-A")
      val child2 = ExhibitRoom("A-B")
      service.add(child1, Some(parent))
      service.add(child2, Some(parent))
      service.add(ExhibitRoom("A-A-A"), Some(child1))
      service.add(ExhibitRoom("A-A-B"), Some(child1))
      
      "減少数の取得と項目数の減少" in {
        service.count must_== 5
        service.remove(child1) must_== 3
        service.count must_== 2
      }
    }
  }
}

