package eis.iai.uni.bonn.de;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
//import org.apache.jena.ontology.OntProperty;
//import org.apache.jena.util.iterator.ExtendedIterator;

public class Modifications extends GS_ChangesGenerator{
	public static void create(int count) {
		Set<Statement> ss = new HashSet<Statement>();

		//pick arbitrary triples
		for (int i = 0; i < count; i++ ) {	
			StmtIterator stmtiter1 = fragment.listStatements(arbitrary_subject(), (Property) null, (RDFNode) null);
			while (stmtiter1.hasNext()) {
				Statement stmt = stmtiter1.next();
				if (!already_used_triples.contains(stmt)) {
					ss.add(stmt);	
					break;
				}
			}
			stmtiter1.close();
		}
		// create triples to modify 
		Iterator<Statement>  stmtiter = ss.iterator();
		while (stmtiter.hasNext()){
			Statement stmt1 = null, stmt = stmtiter.next();
			while (stmt1 == null) {
				int i = generator.nextInt(3);
				if (i == 0) {		// change_object
					if (stmt.getObject().isLiteral())
						stmt1 = ResourceFactory.createStatement(stmt.getSubject(), stmt.getPredicate(), arbitrary_literalobject(stmt.getObject()));
					else 
						stmt1 = ResourceFactory.createStatement(stmt.getSubject(), stmt.getPredicate(), arbitrary_resource(stmt.getObject()));
				} else if (i == 1) // change_subject
					stmt1 = ResourceFactory.createStatement(arbitrary_resource(stmt.getSubject()), stmt.getPredicate(), stmt.getObject());
				/*else {			// change_property
					ExtendedIterator<? extends OntProperty> op = omodel.getOntProperty(stmt.getPredicate().toString()).listSuperProperties();
					if (op.hasNext())
						stmt1 = ResourceFactory.createStatement(stmt.getSubject(),op.next(), stmt.getObject());
				}*/
			}
			fragment.remove(stmt);
			additions.add(stmt);
			 fragment_additions.add(stmt1);
			deletions.add(stmt1);
		}	
	}
}
