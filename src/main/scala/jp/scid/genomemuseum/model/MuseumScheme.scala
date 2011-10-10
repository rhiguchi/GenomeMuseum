package jp.scid.genomemuseum.model

trait MuseumScheme {
  import MuseumScheme._
  
  def allMuseumExhibits(): List[MuseumExhibit]
  
  def store(entity: MuseumExhibit): Boolean
  def saveExhibits(entities: Iterable[MuseumExhibit]): Boolean = {
    var result = true
    entities map store foreach (result &= _)
    result
  }
  
  def exhibitRoomService: TreeDataService[ExhibitListBox]
}

object MuseumScheme {
  def fromH2Local(location: String): MuseumScheme =
    new MuseumSquerylScheme(location)
  def onMemory(): MuseumScheme = {
    import org.squeryl.PrimitiveTypeMode.transaction
    val scheme = new MuseumSquerylScheme("mem:")
    transaction {
      scheme.create
    }
    scheme
  }
  
  private lazy val emptyScheme = new EmptyMuseumScheme
  def empty: MuseumScheme = emptyScheme
}

private class MuseumSquerylScheme(dbLocation: String,
    user: String = "genomemuseum", password: String = "genomemuseum")
    extends org.squeryl.Schema with MuseumScheme {
  import org.squeryl.{SessionFactory, Session, adapters}
  import org.squeryl.PrimitiveTypeMode._
  import org.h2.jdbcx.JdbcConnectionPool
  
  Class.forName("org.h2.Driver")
  
  /** Squeryl のセッション作成時に使用する接続確立関数 */
  lazy val h2ds = JdbcConnectionPool.create("jdbc:h2:" + dbLocation, user, password)
  SessionFactory.concreteFactory = Some( () =>
    Session.create(h2ds.getConnection, new adapters.H2Adapter)
  )
  
  val museumExhibits = table[MuseumExhibit]("museum_exhibit")
  on(museumExhibits) { table => declare (
    table.name is (dbType("varchar")),
    table.accession is (dbType("varchar")),
    table.identifier is (dbType("varchar")),
    table.namespace is (dbType("varchar")),
    table.definition is (dbType("varchar")),
    table.source is (dbType("varchar")),
    table.organism is (dbType("varchar")),
    table.sequenceUnit is (dbType("varchar")),
    table.moleculeType is (dbType("varchar"))
  )}
  
  /** リストボックステーブル定義 */
  val exhibitRoom = table[ExhibitListBox]("exhibit_room")
  /** リストボックス親子階層 */
  val exhibitRoomRelation = oneToManyRelation(exhibitRoom, exhibitRoom)
    .via((p, c) => c.parentId === p.id)
  
  /** リストボックス用データアクセスサービス */
  val exhibitRoomService = new squeryl.TreeDataService(exhibitRoom) {
    protected def setParentTo(entity: ExhibitListBox, parent: ExhibitListBox) {
      entity.parentId = Some(parent.id)
    }
    
    protected def getParentValue(entity: ExhibitListBox): Option[Long] = {
      entity.parentId
    }
  }
  
  def allMuseumExhibits() = transaction {
    from(museumExhibits)(select(_)).toList
  }
  
  def store(entity: MuseumExhibit) = inTransaction {
    if (entity.isPersisted)
      museumExhibits.update(entity)
    else
      museumExhibits.insert(entity)
    true
  }
  
  override def saveExhibits(entities: Iterable[MuseumExhibit]) = inTransaction {
    super.saveExhibits(entities)
  }
}

private class EmptyMuseumScheme extends MuseumScheme {
  def allMuseumExhibits() = Nil
  def store(entities: MuseumExhibit) = true
  
  val exhibitRoomService = TreeDataService[ExhibitListBox]()
}
