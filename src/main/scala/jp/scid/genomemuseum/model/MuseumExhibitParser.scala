package jp.scid.genomemuseum.model

import java.text.ParseException
import java.io.{File, FileInputStream, IOException}
import jp.scid.bio.{BioFileParser, GenBankParser, FastaParser, BioData,
  GenBank, Fasta}
import collection.mutable.{ListBuffer, Buffer}

/**
 * ファイルから MuseumExhibit を作成するクラス
 */
class MuseumExhibitParser {
  import MuseumExhibitParser._
  
  /**
   * ファイルから MuseumExhibit を作成する
   */
  @throws(classOf[IOException])
  @throws(classOf[ParseException])
  def parseFrom(file: File): Option[MuseumExhibit] = {
    val loader = file match {
      case GenBankFileLoader() => Some(GenBankFileLoader)
      case FastaFileLoader() => Some(FastaFileLoader)
      case _ => None
    }
    
    val exhibitOp = loader map { loader => createMuseumExhibitFrom(file, loader) }
    
    exhibitOp
  }
  
  /**
   * ビルダを使用してファイルから MuseumExhibit を作成する
   */
  private def createMuseumExhibitFrom(file: File,
      builder: MuseumExhibitBuilder[_]): MuseumExhibit = {
    val data = using(new FileInputStream(file)) { inst =>
      val source = io.Source.fromInputStream(inst)
      builder.createFrom(source.getLines)
    }
    
    data
  }
  
  /**
   * GenBank ファイルから MuseumExhibit を作成する
   */
  private object GenBankFileLoader extends MuseumExhibitBuilder[GenBank] {
    val parser = new GenBankParser
    
    def unapply(file: File): Boolean =
      file.getName.endsWith(".gbk")
    
    def convertToMuseumExhibit(sections: List[GenBank]) = {
      val data = sections.head
      
      val exhibit = MuseumExhibit(
        name = data.locus.name,
        sequenceLength = data.locus.sequenceLength,
        accession = data.accession.primary,
        identifier = data.version.identifier,
        namespace = data.locus.division,
        version = getVersionNumber(data.version.number),
        definition = data.definition.value,
        source = data.source.value,
        organism = data.source.taxonomy :+ data.source.organism mkString "\n",
        date = data.locus.date
      )
      
      exhibit
    }
  }
  
  /**
   * FASTA ファイルから MuseumExhibit を作成する
   */
  private object FastaFileLoader extends MuseumExhibitBuilder[Fasta] {
    val parser = new FastaParser
    
    def unapply(file: File): Boolean =
      file.getName.endsWith(".faa") || file.getName.endsWith(".fna") || 
        file.getName.endsWith(".ffn") || file.getName.endsWith(".fasta")
        
    def convertToMuseumExhibit(sections: List[Fasta]) = {
      val data = sections.head
      
      val exhibit = MuseumExhibit(
          name = data.header.name,
          sequenceLength = data.sequence.value.length,
          accession = data.header.accession,
          identifier = data.header.identifier,
          namespace = data.header.namespace,
          version = getVersionNumber(data.header.version),
          definition = data.header.description
        )
      
      exhibit
    }
  }
    
  
  /** バージョン値 */
  private def getVersionNumber(value: Int) =
    if (value == 0) None else Some(value)
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}

object MuseumExhibitParser {
  private abstract class MuseumExhibitBuilder[A <: BioData] {
    def parser: BioFileParser[A]
    
    /**
     * ファイルが読み込み可能か調べる
     */
    def unapply(file: File): Boolean
    
    @throws(classOf[ParseException])
    def createFrom(source: Iterator[String]): MuseumExhibit = {
      val sections = loadSections(ListBuffer.empty[A], source, parser).toList
      
      if (sections.isEmpty)
        throw new ParseException("Cannot detect bio data", 0)
      
      convertToMuseumExhibit(sections)
    }
    
    /**
     * セクションリストを MuseumExhibit に変換
     */
    protected def convertToMuseumExhibit(sections: List[A]): MuseumExhibit
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
  
}