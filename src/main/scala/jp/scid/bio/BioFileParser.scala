package jp.scid.bio

import java.text.ParseException

trait BioFileParser[A <: BioData] {
  @throws(classOf[ParseException])
  def parseFrom(source: Iterator[String]): A
}
