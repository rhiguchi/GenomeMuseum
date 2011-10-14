package jp.scid.genomemuseum.model.squeryl

import org.specs2._
import java.util.Date

/**
 * ツリー構造のデータを扱うためのトレイト
 */
class TreeDataServiceSpec extends Specification {
  def is = "squeryl.TreeDataService" ^ pending
    "要素の追加" ! MyTest().s1 ^
    "ルート要素の取得" ! MyTest().s2 ^
    "子要素の取得" ! MyTest().s3 ^
    "親要素の取得" ! MyTest().s4 ^
    "要素の更新" ! MyTest().s5 ^
    "要素の削除" ! MyTest().s6.pendingUntilFixed ^
    "要素の再起的削除" ! MyTest().s7.pendingUntilFixed
  
  private trait H2MemDB extends H2MemDBSession {
    import org.squeryl.PrimitiveTypeMode.transaction
    
    private val schema = new TestSchema()
    transaction {
      schema.create
    }
    
    protected val service = new TreeDataService(schema.sampleTable) {
      def getParentValue(entity: TestEntity) = entity.parentId
      protected def setParentTo(entity: TestEntity, parent: TestEntity) {
        entity.parentId = Some(parent.id)
      }
      val lastModified = new Date()
    }
  }
  
  private case class MyTest() extends H2MemDB {
    assert(service.count == 0)
    
    val e1 = TestEntity("e1")
    val e2 = TestEntity("e2")
    val rootEntities = List(e1, e2, TestEntity("e3"))
    val e1Children = List(TestEntity("e1-1"), TestEntity("e1-2"))
    
    // ルート要素
    rootEntities.foreach(service.add(_))
    // 子要素
    e1Children.foreach(service.add(_, Some(e1)))
    
    def s1 = service.count must_== 5
    
    def s2 = service.rootItems must have size(3)
    
    def s3 = service.getChildren(e1) must contain(
      e1Children.head, e1Children.tail.head) and have size(2)
    
    def s4 = service.getParent(e1Children.head) must_== Some(e1) and
      (service.getParent(e1Children.tail.head) must_== Some(e1)) and
      (service.getParent(e1) must beNone)
    
    def s5 = {
      e2.name = "e2_updated"
      service.save(e2)
      e2.name must_== "e2_updated"
    }
    
    def s6 = {
      service.remove(e2) must_== 1 and
        (service.count must_== 4)
    }
    
    def s7 = {
      service.remove(e1) must_== 1 and
        (service.count must_== 4)
    }
  }
  
}
  
private trait H2MemDBSession extends specification.After {
  import org.squeryl.{SessionFactory, Session, adapters}
  import org.h2.jdbcx.JdbcConnectionPool
  Class.forName("org.h2.Driver")
  
  private lazy val cp = JdbcConnectionPool.create("jdbc:h2:mem:" , "", "")
  SessionFactory.concreteFactory = Some( () =>
    Session.create(cp.getConnection, new adapters.H2Adapter)
  )
  
  def after = {
    cp.dispose()
  }
}

private class TestSchema() extends org.squeryl.Schema {
  import org.squeryl.PrimitiveTypeMode._
  
  val sampleTable = table[TestEntity]
  
  val treeNode = oneToManyRelation(sampleTable, sampleTable)
    .via((s,c) => s.parentId === c.id)
    
  treeNode.foreignKeyDeclaration.constrainReference(onDelete cascade)
}

import org.squeryl.KeyedEntity

private case class TestEntity(
  var name: String = "",
  var parentId: Option[Long] = None
) extends KeyedEntity[Long] {
  var id: Long = 0
  
  def this() = this("", Option(0))
}
