package eis.iai.uni.bonn.de;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class Subject extends ICG {

	protected static void change_subject(int count) {
		for (int i = 0; i < count; i++) {
			StmtIterator stmtiter = fragment.listStatements(arbitrary_resource(), (Property) null, (RDFNode) null);
			while (stmtiter.hasNext()) {
				Statement stmt = stmtiter.next();
				additions_into_changeset(arbitrary_resource().asNode(), stmt.getPredicate().asNode(), stmt.getObject().asNode());
				break;
			}
			stmtiter.close();
		}
	}
}
