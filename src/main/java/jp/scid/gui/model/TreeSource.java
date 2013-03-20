package jp.scid.gui.model;

import java.util.List;

/**
 * 同じ方の要素からなるツリー構造の定義
 * @author Ryusuke Higuchi
 *
 * @param <T> 要素の型
 */
public interface TreeSource<T> extends ValueModel<T> {
    /**
     * 子要素リストを返す。
     * 
     * {@link SourceTreeModel} では {@link ca.odell.glazedlists.EventList} を返すことで、
     * 要素の変化を監視し、その変化がツリー構造に適用される。
     * 
     * @param parent 親要素
     * @return 子要素のリスト。
     */
    List<T> getChildren(T parent);
    
    /**
     * 要素が末端の葉要素であるかを返す
     * @param node 調べる要素
     * @return 葉要素である場合は {@code true} 。
     */
    boolean isLeaf(T node);
    
    /**
     * 要素に編集が行われたことを通知する。
     * @param node 変更が加えられた要素
     * @param editorValue 編集内容。
     */
    void updateNodeValue(T node, Object editorValue);
}
