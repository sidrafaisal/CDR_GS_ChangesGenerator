package eis.iai.uni.bonn.de;

import java.text.ParseException;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;

public class Predicate extends ICG{
	
	protected static void change_predicate(int count, Boolean change_subject, Boolean change_object) throws ParseException {
		int disjoint_property_quota = generator.nextInt(count) + 1;	
		count = count - disjoint_property_quota;
		for (OntProperty property : disjoint.keySet()) {
			StmtIterator stmt_iter = fragment.listStatements((Resource)null, property, (RDFNode)null);
			if (stmt_iter.hasNext()) {
				Statement stmt = stmt_iter.next();
				Node new_subject = stmt.getSubject().asNode(), new_property = disjoint.get(property), new_object = stmt.getObject().asNode();
				if (change_subject==true) 
					new_subject = arbitrary_resource().asNode();
				if (change_object==true) 
					new_object	= Object.change_object_values(stmt);	

				additions_into_changeset(new_subject, new_property, new_object);						 						 								
				disjoint_property_quota--;
				if (disjoint_property_quota <= 0)
					break;
			}
		}
		if (disjoint_property_quota > 0)
			count = count + disjoint_property_quota;
		for (int i = 0; i < count; i++) {
			StmtIterator stmt_iter = fragment.listStatements(arbitrary_resource(), (Property) null, (RDFNode) null);
			if (stmt_iter.hasNext()) {
				Statement stmt = stmt_iter.next();
				OntProperty p = omodel.getOntProperty(stmt.getPredicate().toString());
				OntProperty new_property = p; 
				int rand = generator.nextInt(4);
				OntProperty subprop = getSubProperty(p, false), ep =  getEqvProperty(p), supprop = getSubProperty(p, true);
				
				if (rand==0 && subprop!=null)//p.hasSubProperty((Property)null, false))
					new_property = subprop;
				else if (rand==1 && supprop!=null)//p.hasSuperProperty((Property)null, false))
					new_property = supprop;
				else if (rand==2 && ep!=null)//p.hasEquivalentProperty((Property)null))
					new_property = ep;
				else if (rand==3 && p.getInverse()!=null)//p.hasInverse())
					new_property = p.getInverse();

				Node new_subject = stmt.getSubject().asNode(), new_object = stmt.getObject().asNode();
				if (change_subject==true) 
					new_subject = arbitrary_resource().asNode();
				if (change_object==true) 
					new_object	= Object.change_object_values(stmt);
				additions_into_changeset(new_subject, new_property.asNode(), new_object);						 						 								
		}
		stmt_iter.close();
		}		
	}
	
	protected static OntProperty getEqvProperty(Property property)	{
		OntProperty op = omodel.getOntProperty(property.toString());			 
		ExtendedIterator<? extends OntProperty> eps = null;
		OntProperty ep = null;

		if (op != null) {
			eps = op.listEquivalentProperties();
			while(eps.hasNext()) {
				OntProperty p = eps.next();
				if (!p.equals(op)){
					ep = p;
					break;
				}
			}
		}
		return ep;
	}
	
	protected static OntProperty getSubProperty(Property property, boolean superproperty)	{
		OntProperty op = omodel.getOntProperty(property.toString());			 
		ExtendedIterator<? extends OntProperty> sps = null;
		Set<? extends OntProperty> s_eps = null;
		OntProperty sp = null;
		if( op != null) {
			if (!superproperty)
				sps = op.listSubProperties();
			else
				sps = op.listSuperProperties();
			s_eps = op.listEquivalentProperties().toSet();
			while(sps.hasNext()) {
				OntProperty p = sps.next();
				if (!(p.equals(op) || s_eps.contains(p))) {			
					sp = p;
					break;
				}
			}
		}
		return sp;
	}
}
