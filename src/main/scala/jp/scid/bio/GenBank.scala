package jp.scid.bio

import java.io.InputStream

case class GenBank (
  var locus: String = ""
)

object GenBank {
  def fromInputStream(stream: InputStream) = {
    GenBank("NC_001773")
  }
}