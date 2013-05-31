package jp.scid.genomemuseum.model;

import jp.scid.bio.store.folder.CollectionType;

/**
 * 子フォルダを複数保持するノードの構造。
 * 
 * フォルダの追加と削除ができる。
 * @author higuchi
 *
 */
public interface FolderContainer {

    FolderTreeNode addChild(CollectionType type);
    
    boolean canMove(FolderTreeNode node);
}