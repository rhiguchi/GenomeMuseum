package jp.scid.genomemuseum.model

trait MuseumScheme {
  def allMuseumExhibits(): List[MuseumExhibit]
  
  def store(entity: MuseumExhibit): Boolean
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
  
  def allMuseumExhibits() = transaction {
    from(museumExhibits)(select(_)).toList
  }
  
  def store(entity: MuseumExhibit) = transaction {
    if (entity.isPersisted)
      museumExhibits.update(entity)
    else
      museumExhibits.insert(entity)
    true
  }
}

private class EmptyMuseumScheme extends MuseumScheme {
  def allMuseumExhibits() = Nil
  def store(entity: MuseumExhibit) = true
}
