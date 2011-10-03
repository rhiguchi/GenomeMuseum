package jp.scid.genomemuseum.model

import net.liftweb.mapper
import mapper.{Schemifier}

trait MuseumScheme {
  def allMuseumExhibits(): List[MuseumExhibit]
}

object MuseumScheme {
  def fromH2Local(location: String): MuseumScheme =
    new H2DatabaseMuseumScheme(location)
  def onMemory(): MuseumScheme =
    new H2DatabaseMuseumScheme("mem:")
}

private class H2DatabaseMuseumScheme(location: String) extends MuseumScheme {
  import mapper.{DB, DefaultConnectionIdentifier, Schemifier, MapperRules}
  val vendor = new DBVendor(location)
  DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
  Schemifier.schemify(true, Schemifier.neverF _, MuseumExhibit)
  
  def allMuseumExhibits() = MuseumExhibit.findAll
}

import mapper.{ConnectionIdentifier, ConnectionManager}
import net.liftweb.common.{Box, Empty, Full}

class DBVendor(dbLocation: String = "mem:") extends ConnectionManager {
  import java.sql.{Connection, DriverManager}
  
  private val driverName = "org.h2.Driver"

  private var pool: List[Connection] = Nil
  private var poolSize = 0
  private val maxPoolSize = 4
 
  def dbUrl = "jdbc:h2:" + dbLocation
  
  private def createOne: Box[Connection] = try {
    Class.forName(driverName)
 
    val dm = DriverManager.getConnection(dbUrl, "genomemuseum", "genomemuseum")
 
    Full(dm)
  } catch {
    case e: Exception => e.printStackTrace; Empty
  }
 
  def newConnection(name: ConnectionIdentifier): Box[Connection] =
    synchronized {
      pool match {
        case Nil if poolSize < maxPoolSize =>
          val ret = createOne
          poolSize = poolSize + 1
          ret.foreach(c => pool = c :: pool)
          ret
 
        case Nil => wait(1000L); newConnection(name)
        case x :: xs => try {
          x.setAutoCommit(false)
          Full(x)
        } catch {
          case e => try {
            pool = xs
            poolSize = poolSize - 1
            x.close
            newConnection(name)
          } catch {
            case e => newConnection(name)
          }
        }
      }
    }
 
  def releaseConnection(conn: Connection): Unit = synchronized {
    pool = conn :: pool
    notify
  }
  
}