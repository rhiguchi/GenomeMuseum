/**
 * This class is generated by jOOQ
 */
package jp.scid.genomemuseum.model.sql.tables.records;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value    = {"http://www.jooq.org", "2.2.0"},
                            comments = "This class is generated by jOOQ")
public class MuseumExhibitRecord extends org.jooq.impl.UpdatableRecordImpl<jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord> {

	private static final long serialVersionUID = -270143395;

	/**
	 * An uncommented item
	 * 
	 * PRIMARY KEY
	 */
	public void setId(java.lang.Long value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.ID, value);
	}

	/**
	 * An uncommented item
	 * 
	 * PRIMARY KEY
	 */
	public java.lang.Long getId() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.ID);
	}

	/**
	 * An uncommented item
	 * 
	 * PRIMARY KEY
	 */
	public java.util.List<jp.scid.genomemuseum.model.sql.tables.records.CollectionBoxItemRecord> fetchCollectionBoxItemList() {
		return create()
			.selectFrom(jp.scid.genomemuseum.model.sql.tables.CollectionBoxItem.COLLECTION_BOX_ITEM)
			.where(jp.scid.genomemuseum.model.sql.tables.CollectionBoxItem.COLLECTION_BOX_ITEM.EXHIBIT_ID.equal(getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.ID)))
			.fetch();
	}

	/**
	 * An uncommented item
	 */
	public void setName(java.lang.String value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.NAME, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.String getName() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.NAME);
	}

	/**
	 * An uncommented item
	 */
	public void setSequenceLength(java.lang.Integer value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.SEQUENCE_LENGTH, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.Integer getSequenceLength() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.SEQUENCE_LENGTH);
	}

	/**
	 * An uncommented item
	 */
	public void setAccession(java.lang.String value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.ACCESSION, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.String getAccession() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.ACCESSION);
	}

	/**
	 * An uncommented item
	 */
	public void setNamespace(java.lang.String value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.NAMESPACE, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.String getNamespace() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.NAMESPACE);
	}

	/**
	 * An uncommented item
	 */
	public void setVersion(java.lang.Integer value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.VERSION, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.Integer getVersion() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.VERSION);
	}

	/**
	 * An uncommented item
	 */
	public void setDefinition(java.lang.String value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.DEFINITION, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.String getDefinition() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.DEFINITION);
	}

	/**
	 * An uncommented item
	 */
	public void setSourceText(java.lang.String value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.SOURCE_TEXT, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.String getSourceText() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.SOURCE_TEXT);
	}

	/**
	 * An uncommented item
	 */
	public void setOrganism(java.lang.String value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.ORGANISM, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.String getOrganism() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.ORGANISM);
	}

	/**
	 * An uncommented item
	 */
	public void setDate(java.sql.Date value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.DATE, value);
	}

	/**
	 * An uncommented item
	 */
	public java.sql.Date getDate() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.DATE);
	}

	/**
	 * An uncommented item
	 */
	public void setSequenceUnit(java.lang.Integer value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.SEQUENCE_UNIT, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.Integer getSequenceUnit() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.SEQUENCE_UNIT);
	}

	/**
	 * An uncommented item
	 */
	public void setMoleculeType(java.lang.String value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.MOLECULE_TYPE, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.String getMoleculeType() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.MOLECULE_TYPE);
	}

	/**
	 * An uncommented item
	 */
	public void setFileType(java.lang.Integer value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.FILE_TYPE, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.Integer getFileType() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.FILE_TYPE);
	}

	/**
	 * An uncommented item
	 */
	public void setFileUri(java.lang.String value) {
		setValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.FILE_URI, value);
	}

	/**
	 * An uncommented item
	 */
	public java.lang.String getFileUri() {
		return getValue(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT.FILE_URI);
	}

	/**
	 * Create a detached MuseumExhibitRecord
	 */
	public MuseumExhibitRecord() {
		super(jp.scid.genomemuseum.model.sql.tables.MuseumExhibit.MUSEUM_EXHIBIT);
	}
}
