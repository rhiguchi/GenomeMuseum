package jp.scid.bio

import java.io.{InputStream, Reader, BufferedReader}

import java.text.ParseException
import collection.mutable.{ListBuffer, Buffer}
import Fasta._

/**
 * {@code Fasta} オブジェクトを文字列から作成する構文解析クラス
 */
class FastaParser extends BioFileParser[Fasta] {
  
  val headerParser = Fasta.Header
  /**
   * 文字列情報から {@code Fasta} オブジェクトを生成する
   * @param source 生成もとのテキスト。 ヘッダ行までは無視される。
   * @return 作成された {@code Fasta} オブジェクト
   * @throws ParseException {@code source} に解析できない文字列が含まれていた場合。
   */
  @throws(classOf[ParseException])
  def parseFrom(source: Iterator[String]): Fasta = {
    val bufferedSource = source.buffered
    
    def nonSectionHead(line: String) = ! isSectionHead(line)
    
    val unknownStart = readElementTail(ListBuffer.empty[String],
      bufferedSource, nonSectionHead _)
    
    if (bufferedSource.isEmpty)
      throw new ParseException("It is not FASTA source", 0)
    
    createFrom(bufferedSource)
  }
  
  @throws(classOf[ParseException])
  def parseFrom(source: Reader): Fasta = {
    val bufSource = new io.BufferedSource(null)(null) {
      override def reader() = null
      override val bufferedReader = new BufferedReader(source)
    }
    parseFrom(bufSource.getLines());
  }
  
  @throws(classOf[ParseException])
  private def createFrom(source: BufferedIterator[String]) = {
    val header = Header.parseFrom(source.next)
    var sequence = Sequence()
    
    def nonSectionHead(line: String) = ! isSectionHead(line)
    
    if (source.hasNext && nonSectionHead(source.head)) {
      val seqLines = readElementTail(
        ListBuffer(source.next), source, nonSectionHead _)
      sequence = Sequence.parseFrom(seqLines)
    }
    
    Fasta(header, sequence)
  }
  
  /**
   * 指定した要素形式から、次の要素形式が検出されるまでの文字列を読み出す
   * {@code format#detectNextFrom} が {@code Some} を返すまで読み出す。
   * @param accumu 読み出した文字列の追加先
   * @param source 文字列の読み出し元
   * @param elementContinuing 要素文字列が継続しているかの判定関数
   * @return {@code accumu} オブジェクト
   */
  @annotation.tailrec
  private def readElementTail(accumu: Buffer[String], source: BufferedIterator[String], 
      elementContinuing: String => Boolean): Buffer[String] =
    if (source.hasNext && elementContinuing(source.head)) {
      accumu += source.next
      readElementTail(accumu, source, elementContinuing)
    }
    else accumu
  
  /** セクションの始まりを表す行であるか */
  private def isSectionHead(line: String) =
    Header.Head unapply line
}
