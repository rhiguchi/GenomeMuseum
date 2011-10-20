package jp.scid.bio.ws

import actors.{Futures, Future}
import WebServiceAgent.{Identifier, EntryValues}

/**
 * {@code WebServiceAgent} を用いた検索結果データを非同期取得して保持するクラス。
 * 
 * @constructor データソースを作成する。
 * @param agent データの取得元
 * @param query 検索文字列。 {@code agent} に用いられる。
 * @param size このイテレータの要素数
 */
class WebSourceIterator(agent: WebServiceAgent, val query: String, override val size: Int)
    extends Iterator[Future[(Identifier, Future[EntryValues])]] {
  import collection.mutable.Queue
  
  /** 現在まで取得した結果数 */
  private var offset = 0
  /** 一回に取得する結果数 */
  var defaultLimit = 20
  /** 取得した結果キュー */
  val queue = Queue.empty[Future[(Identifier, Future[EntryValues])]]
  
  def hasNext = queue.nonEmpty || remainingCount > 0
  
  def next = {
    if (queue.isEmpty && remainingCount > 0) {
      // 取得数を決める
      val limit = if (defaultLimit < remainingCount) defaultLimit else remainingCount
      // 結果を取得し、その結果を記録
      val elms = loadNext(offset, limit)
      offset = offset + elms.size
      queue ++= elms
    }
    
    queue.dequeue
  }
  
  /**
   * {@code agent} から検索結果を取得してオブジェクトとして返す
   * @param offset 検索時の開始位置
   * @param count 取得項目数
   * @return 取得要素の Future。項目数は {@code count} と同じとなる。
   */
  private def loadNext(offset: Int, count: Int) = {
    // identifier 取得
    val idsFut = agent.searchIdentifiers(query, offset, count)
    
    // EntryValues は identifier が取得できてないと取れないので future で再ラップ
    val fvFut: Future[IndexedSeq[EntryValues]] = Futures.future {
      agent.getFieldValuesFor(idsFut.apply).apply
    }
    
    // count 数の Future を生成
    Range(0, count) map { index => 
      Futures.future {
        val identifiers = idsFut.apply
        // identifier が、limit 数より少なく取得される可能性を考慮
        identifiers.size match {
          case idCount if index < idCount =>
            val identifier = identifiers(index)
            val valuesFut = Futures.future {
              fvFut.apply()(index)
            }
            Pair(identifier, valuesFut)
          case _ =>
            emptyElement
        }
      }
    }
  }
  
  /** {@code size} より取得結果が少なかった時のための空要素 */
  private lazy val emptyElement =
    Pair(Identifier.empty, Futures.future { EntryValues.empty })
  
  /** 読み出すことのできる残りの要素数 */
  private def remainingCount = size - offset
}

