package lt.lb.lucenejpa.model;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(LuceneBlob.class)
public abstract class LuceneBlob_ {

	public static volatile SingularAttribute<LuceneBlob, Long> id;
	public static volatile SingularAttribute<LuceneBlob, byte[]> content;

	public static final String ID = "id";
	public static final String CONTENT = "content";

}

