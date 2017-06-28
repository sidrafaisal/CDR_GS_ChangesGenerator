package eis.iai.uni.bonn.de;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class Subject extends Main {
	// change subject 
	protected static void change_subject(int count) {
		for (int i = 0; i < count; i++) {
			Resource res_to_replace = list_resources.get(generator.nextInt(list_resources.size()) + 1);
			StmtIterator stmtiter = fragment.listStatements(res_to_replace, (Property) null, (RDFNode) null);
			Resource res_with_replace = list_resources.get(generator.nextInt(list_resources.size()) + 1);
			while (stmtiter.hasNext()) {
				Statement stmt = stmtiter.next();
				additions.add(additions.asStatement(Triple.create(res_with_replace.asNode(), stmt.getPredicate().asNode(), stmt.getObject().asNode())));
				break;
			}
			stmtiter.close();
		}
	}
}
