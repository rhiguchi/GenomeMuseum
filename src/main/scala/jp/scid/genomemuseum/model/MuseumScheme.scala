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
  
  def exhibitRoomService: ExhibitRoomService
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
  
  trait ExhibitRoomService {
    def save(room: ExhibitListBox)
    def rootItems: List[ExhibitListBox]
    def childrenFor(parent: ExhibitListBox): List[ExhibitListBox]
  }
  
  object ExhibitRoomService {
    class EmptyExhibitRoomService extends ExhibitRoomService {
      def save(room: ExhibitListBox) {}
      def rootItems = Nil
      def childrenFor(parent: ExhibitListBox) = Nil
    }
    
    val empty: ExhibitRoomService = new EmptyExhibitRoomService
  }
}

private class MuseumSquerylScheme(dbLocation: String,
    user: String = "genomemuseum", password: String = "genomemuseum")
    extends org.squeryl.Schema with MuseumScheme {
  import org.squeryl.{SessionFactory, Session, adapters}
  import org.squeryl.PrimitiveTypeMode._
  import org.h2.jdbcx.JdbcConnectionPool
  import MuseumScheme.ExhibitRoomService
  
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
  val exhibitRoomService = new ExhibitRoomService {
    private val table = exhibitRoom
    
    def save(room: ExhibitListBox) = inTransaction {
      room.parentId match {
        case Some(0) => room.parentId = None
        case _ =>
      }
      if (room.isPersisted)
        table.update(room)
      else
        table.insert(room)
    }
    
    def rootItems = transaction {
      from(exhibitRoom)(e => where(e.parentId.isNull) select(e)).toList
    }
    
    def childrenFor(parent: ExhibitListBox) = transaction {
      from(exhibitRoom)(e => where(e.parentId === parent.id) select(e)).toList
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
  import MuseumScheme.ExhibitRoomService
  def allMuseumExhibits() = Nil
  def store(entities: MuseumExhibit) = true
  
  val exhibitRoomService = ExhibitRoomService.empty
}
