package lt.lb.lucenejpa.model;

import java.util.Date;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(LuceneFile.class)
public abstract class LuceneFile_ {

	public static volatile SingularAttribute<LuceneFile, String> fileName;
	public static volatile SingularAttribute<LuceneFile, Boolean> temp;
	public static volatile SingularAttribute<LuceneFile, String> fileKind;
	public static volatile SingularAttribute<LuceneFile, Long> fileSize;
	public static volatile SingularAttribute<LuceneFile, Long> id;
	public static volatile SingularAttribute<LuceneFile, String> folderName;
	public static volatile SingularAttribute<LuceneFile, Date> lastModified;
	public static volatile SingularAttribute<LuceneFile, String> fileOrigin;
	public static volatile SingularAttribute<LuceneFile, LuceneBlob> luceneBlob;

	public static final String FILE_NAME = "fileName";
	public static final String TEMP = "temp";
	public static final String FILE_KIND = "fileKind";
	public static final String FILE_SIZE = "fileSize";
	public static final String ID = "id";
	public static final String FOLDER_NAME = "folderName";
	public static final String LAST_MODIFIED = "lastModified";
	public static final String FILE_ORIGIN = "fileOrigin";
	public static final String LUCENE_BLOB = "luceneBlob";

}

