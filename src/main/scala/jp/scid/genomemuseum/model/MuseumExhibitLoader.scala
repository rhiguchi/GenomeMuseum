package jp.scid.genomemuseum.model

import java.net.URL
import java.io.{File, IOException, InputStream, FileInputStream,
  BufferedInputStream, ByteArrayInputStream}
import java.text.ParseException

import collection.mutable.{Buffer, ListBuffer}
import actors.{DaemonActor, Actor, Future, Futures, TIMEOUT}
import Actor.State

import jp.scid.bio.{BioFileParser, GenBankParser, FastaParser, BioData,
  GenBank, Fasta}

class MuseumExhibitLoader extends DaemonActor {
  import MuseumExhibitLoader._
  
  val maxPool = 8
  
  private var currentLoading = 0
  
  private def loadActor(q: Query, sender: Actor) = Actor.actor {
    val msg = loadTo(q.service, q.source) match {
      case Some(exhibit) => LoadSuccess(exhibit, q)
      case None => LoadFailure(q)
    }
    sender ! msg
  }
  
  def act() {
    if (currentLoading <= 0) reactWithin(500) {
      case q @ Query(service, source) =>
        currentLoading += 1
        loadActor(q, this)
        act()
      case TIMEOUT =>
        println("TIMEOUT")
        exit
    }
    else reactWithin(0) {
      case Stop() =>
        reply("")
        exit
      case LoadSuccess(exhibit, q) =>
        currentLoading -= 1
        println("LoadSuccess: " + currentLoading)
        act()
      case LoadFailure(q, cause) =>
        currentLoading -= 1
        println("LoadFailure: " + currentLoading)
        act()
      case TIMEOUT =>
        if (currentLoading < maxPool) react {
          case q @ Query(service, source) =>
            currentLoading += 1
            loadActor(q, this)
            act()
        }
        else {
          Thread.sleep(100)
        }
        act()
    }
  }
  
  /**
   * ファイルからデータを読み込み、そのデータをサービスへ格納する。
   */
  def query(service: MuseumExhibitService, source: File) {
    this ! Query(service, source)
    
    getState match {
      case State.New => start()
      case State.Terminated => restart()
      case _ =>
    }
  }
  
  def stop() = {
    this !! Stop()
  }
  
  private val parsers = List(GenBnakSource, FastaSource)
  
  private[model] def loadTo(service: MuseumExhibitService, source: File) = {
    val exhibitOp = parsers.find(_.canParse(source)).map { parser =>
      val e = service.create
      parser.makeExhibit(e, source)
      service.save(e)
      e
    }
    
    exhibitOp
  }
  
  private[model] abstract class ExhibitFileLoader[A <: BioData](parser: BioFileParser[A]) {
    protected def makeExhibit(e: MuseumExhibit, data: List[A])
    
    def canParse(file: File) = {
      using(new FileInputStream(file)) { data =>
        parseBioDataFrom(data, parser).nonEmpty
      }
    }
    
    def makeExhibit(e: MuseumExhibit, file: File) {
      val source = io.Source.fromFile(file).getLines
      val data = loadSections(ListBuffer.empty[A], source, parser).toList
      makeExhibit(e, data)
    }
  }
  
  private[model] object GenBnakSource extends ExhibitFileLoader(new GenBankParser) {
    protected def makeExhibit(e: MuseumExhibit, sections: List[GenBank]) {
      val data = sections.head
      e.name = data.locus.name
      e.sequenceLength = data.locus.sequenceLength
      e.accession = data.accession.primary
      e.identifier = data.version.identifier
      e.namespace = data.locus.division
      e.version = getVersionNumber(data.version.number)
      e.definition = data.definition.value
      e.source = data.source.value
      e.organism = data.source.taxonomy :+ data.source.organism mkString "\n"
      e.date = data.locus.date
    }
  }
  
  private[model] object FastaSource extends ExhibitFileLoader(new FastaParser) {
    protected def makeExhibit(e: MuseumExhibit, sections: List[Fasta]) {
      val data = sections.head
      e.name = data.header.name
      e.sequenceLength = data.sequence.value.length
      e.accession = data.header.accession
      e.identifier = data.header.identifier
      e.namespace = data.header.namespace
      e.version = getVersionNumber(data.header.version)
      e.definition = data.header.description
    }
  }
  
  /** バージョン値 */
  private def getVersionNumber(value: Int) =
    if (value == 0) None else Some(value)
  
  private def parseBioDataFrom[A <: BioData](inst: InputStream, parser: BioFileParser[A]): Option[A] = {
    val dataOp = try {
      val source = scala.io.Source.fromInputStream(inst).getLines
      val bioData = parser.parseFrom(source)
      Some(bioData)
    }
    catch {
      case e: ParseException => None
    }
    dataOp
  }
    
  /** ソースから全てのセクションを読み込む */
  private def loadSections[A <: BioData](sections: Buffer[A],
      source: Iterator[String], parser: BioFileParser[A]): Buffer[A] = {
    source.hasNext match {
      case true =>
        sections += parser.parseFrom(source)
        loadSections(sections, source, parser)
      case false =>
        sections
    }
  }
  
  /** 指定したバイト数分、ストリームの先頭を読み込む */
  @throws(classOf[IOException])
  private def readHeadFrom(source: InputStream, length: Int = 8192) = {
    val buf = new Array[Byte](length)
    val inst = new BufferedInputStream(source, length)
    val read = inst.read(buf)
    
    val head = new ByteArrayInputStream(buf, 0, read)
    head
  }
 
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}

private object MuseumExhibitLoader {
  abstract class Message
  case class Query(service: MuseumExhibitService, source: File) extends Message
  case class LoadSuccess(entity: MuseumExhibit, query: Query) extends Message
  case class LoadFailure(query: Query, cause: Option[Exception] = None) extends Message
  case class Stop() extends Message
}
