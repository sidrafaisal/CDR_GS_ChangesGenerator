package eis.iai.uni.bonn.de;

import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class Predicate extends Main{
	
	// change predicate
	protected static void change_predicate(int count) {
		int disjoint_property_quota = generator.nextInt(count) + 1;	
		count = count - disjoint_property_quota;
		for (OntProperty property : disjoint.keySet()) {
			StmtIterator stmt_iter = fragment.listStatements((Resource)null, property, (RDFNode)null);
			if (stmt_iter.hasNext()) {
				Statement stmt = stmt_iter.next();
				additions.add(additions.asStatement(Triple.create(stmt.getSubject().asNode(), disjoint.get(property), stmt.getObject().asNode())));						 						 								
				disjoint_property_quota--;
				if (disjoint_property_quota <= 0)
					break;
			}
		}
		if (disjoint_property_quota > 0)
			count = count + disjoint_property_quota;
		for (int i = 0; i < count; i++) {
			StmtIterator stmt_iter = fragment.listStatements(list_resources.get(generator.nextInt(list_resources.size()) + 1), (Property) null, (RDFNode) null);
			if (stmt_iter.hasNext()) {
				Statement stmt = stmt_iter.next();
				OntProperty p = omodel.getOntProperty(stmt.getPredicate().toString());
				OntProperty new_property = p; 
				int rand = generator.nextInt(4);
				
				if (rand==0)
					new_property = p.getSubProperty();
				else if (rand==1)
					new_property = p.getSuperProperty();
				else if (rand==2)
					new_property = p.getEquivalentProperty();
				else if (rand==3)
					new_property = p.getInverse();
				
				additions.add(additions.asStatement(Triple.create(stmt.getSubject().asNode(), new_property.asNode(), stmt.getObject().asNode())));						 						 								
		}
		stmt_iter.close();
		}		
	}
}
