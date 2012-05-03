package jp.scid.genomemuseum.model;

import java.util.List;

/**
 * 永続化オブジェクト操作クラス
 * @author ryusuke
 *
 * @param <E> 永続化オブジェクトクラス
 */
public interface EntityService<E> {
    /**
     * 新規の永続化オブジェクトインスタンスを作成する。
     * 
     * 作成時点ではまだ永続化されない。
     * 
     * @return 永続化オブジェクトインスタンス
     */
    E newElement();
    
    /**
     * 永続化オブジェクトインスタンスを保存する。
     * 
     * まだ永続化されていないオブジェクトは永続化が行われ、
     * 永続化されたオブジェクトは識別子に基づき変更が更新される。
     * 
     * @param element 永続化オブジェクトインスタンス
     * @return 保存に成功したときは {@code true}
     */
    boolean store(E element);
    
    /**
     * オブジェクトの永続化を解除する。
     * 
     * @param element 永続化を解除する要素。
     * @return 削除に成功したときは {@code true}。
     */
    boolean delete(E element);
    
    /**
     * 永続化オブジェクトを識別子から取得する。
     * 
     * @param id 識別子
     * @return 永続化オブジェクト
     */
    E find(long id);
    
    /**
     * 永続かオブジェクトを検索する。
     * 
     * @param whereSql SQL形式の検索クエリ
     * @param params 検索クエリの要素
     * @return 検索クエリに該当した要素リスト。
     */
    List<E> search(String whereSql, Object... params);
}
