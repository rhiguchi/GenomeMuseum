

/* Create Tables */

CREATE TABLE BOX_TREE_NODE
(
	id bigint NOT NULL AUTO_INCREMENT,
	-- 0 - 親無し
	-- それ以外の数字 - 親ノードのID
	parent_id bigint,
	name varchar DEFAULT 'New Box' NOT NULL,
	-- 1 - Collection Box
	-- 2 - Group Box
	-- 3- Smart Box
	node_type integer DEFAULT 1 NOT NULL,
	table_property_id bigint UNIQUE,
	PRIMARY KEY (id)
);


CREATE TABLE COLLECTION_BOX_ITEM
(
	id bigint NOT NULL AUTO_INCREMENT,
	exhibit_id bigint NOT NULL,
	box_id bigint NOT NULL,
	PRIMARY KEY (id)
);


-- バイオデータファイルを表すテーブル
CREATE TABLE MUSEUM_EXHIBIT
(
	id bigint NOT NULL AUTO_INCREMENT,
	-- 名前
	name varchar NOT NULL,
	sequence_length integer NOT NULL,
	accession varchar NOT NULL,
	namespace varchar NOT NULL,
	version integer NOT NULL,
	definition varchar NOT NULL,
	source_text varchar NOT NULL,
	organism varchar NOT NULL,
	date date,
	sequence_unit integer NOT NULL,
	molecule_type varchar NOT NULL,
	file_type integer NOT NULL,
	file_uri varchar NOT NULL,
	PRIMARY KEY (id)
);


CREATE TABLE TABLE_VIEW_PROPERTY
(
	id bigint NOT NULL AUTO_INCREMENT,
	-- カンマ区切りの列識別名文字列
	visibled_columns varchar NOT NULL,
	-- 例：name desc
	order_statement varchar NOT NULL,
	-- 選択されている行数を文字列に永続化したもの
	selected_rows varchar NOT NULL,
	PRIMARY KEY (id)
);



/* Create Foreign Keys */

ALTER TABLE BOX_TREE_NODE
	ADD CONSTRAINT FK_BOX_PARENT_ID FOREIGN KEY (parent_id)
	REFERENCES BOX_TREE_NODE (id)
	ON UPDATE CASCADE
	ON DELETE CASCADE
;


ALTER TABLE COLLECTION_BOX_ITEM
	ADD CONSTRAINT FK_BOX_ITEM_BOX_ID FOREIGN KEY (box_id)
	REFERENCES BOX_TREE_NODE (id)
	ON UPDATE CASCADE
	ON DELETE CASCADE
;


ALTER TABLE COLLECTION_BOX_ITEM
	ADD CONSTRAINT FK_BOX_ITEM_EXHIBIT_ID FOREIGN KEY (exhibit_id)
	REFERENCES MUSEUM_EXHIBIT (id)
	ON UPDATE CASCADE
	ON DELETE CASCADE
;


ALTER TABLE BOX_TREE_NODE
	ADD CONSTRAINT FK_BOX_TABLE_PROPERTY_ID FOREIGN KEY (table_property_id)
	REFERENCES TABLE_VIEW_PROPERTY (id)
	ON UPDATE CASCADE
	ON DELETE CASCADE
;



