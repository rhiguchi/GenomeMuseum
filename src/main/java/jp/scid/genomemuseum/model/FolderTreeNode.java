package jp.scid.genomemuseum.model;

/**
 * フォルダの階層構造の1ノード
 * 
 * @author higuchi
 *
 */
public interface FolderTreeNode {

    FolderContainer getParentContainer();

    void remove();
}