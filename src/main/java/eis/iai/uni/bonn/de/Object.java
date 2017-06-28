package eis.iai.uni.bonn.de;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.FunctionalProperty;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;

public class Object extends Main{

	// change object
	private static int change_object_values(Statement stmt1, int count) throws ParseException {
		RDFNode object_to_replace = stmt1.getObject();
		if (object_to_replace.isResource()) {
			int rand = generator.nextInt(2);
			if (rand==0) {
				ExtendedIterator<Statement> eiter = fragment.listStatements(object_to_replace.asResource(), dfrom, (RDFNode)null).andThen(
						fragment.listStatements((Resource)null, dfrom, object_to_replace));
				if (eiter.hasNext()) {
					Statement stmt2 = eiter.next();
					Node different_resource = stmt2.getObject().asNode();
					if (stmt2.getSubject().equals(stmt1.getObject().asResource()))
						different_resource = stmt2.getSubject().asNode();
					additions.add(additions.asStatement(Triple.create(stmt1.getSubject().asNode(), stmt1.getPredicate().asNode(), different_resource)));						 
				} 
			} else {
				Resource random_resource = list_resources.get(generator.nextInt(list_resources.size()) + 1);
				additions.add(additions.asStatement(Triple.create(stmt1.getSubject().asNode(), stmt1.getPredicate().asNode(), random_resource.asNode())));						 						 
			}
			--count;
		} else {
			if (stmt1.getPredicate().getURI().toLowerCase().contains("date")) {			
				DateFormat sdf = new SimpleDateFormat("YYYY-mm-dd");
				sdf.setLenient(true);
				String 	newobject = new SimpleDateFormat("MMMMM dd, yyyy").format(sdf.parse(stmt1.getObject().toString()));						 
				additions.add(additions.asStatement(Triple.create(stmt1.getSubject().asNode(), stmt1.getPredicate().asNode(), additions.createLiteral(newobject).asNode())));						
				--count;
			} else if (object_to_replace.toString().length()<20 && object_to_replace.toString().contains(",")) {
				String object = object_to_replace.toString().replaceAll("\"", "").replaceAll(" ", "");
				int i = object.indexOf(",");
				String newobject = object.substring(i+1, object.length()) + "," + object.substring(0, i);
				additions.add(additions.asStatement(Triple.create(stmt1.getSubject().asNode(), stmt1.getPredicate().asNode(), additions.createLiteral(newobject).asNode())));						 						 
				--count;
			} 
			//97-a,65-A
			StringBuilder orig = new StringBuilder(object_to_replace.toString());
			int maskSize = generator.nextInt(orig.length()+1);
			for (int i = 0; i < maskSize; i++) {
				int pos = generator.nextInt(orig.length());
				int ac = (int)orig.charAt(pos); 
				if ((ac>=65 && ac<=89) || (ac>=97 && ac<=121))
					orig.setCharAt(pos, ((char) (ac+1)));
			}
			additions.add(additions.asStatement(Triple.create(stmt1.getSubject().asNode(), stmt1.getPredicate().asNode(), additions.createLiteral(orig.toString()).asNode())));						 						 								
			--count;
		}
		return count;
	}

	protected static void change_object(int count) throws ParseException {
		int functionl_property_quota = generator.nextInt(count) + 1;	
		count = count - functionl_property_quota;	
		ExtendedIterator<FunctionalProperty> functional_properties = omodel.listFunctionalProperties();
		L1: while (functional_properties.hasNext()) {
			StmtIterator stmt_iter = fragment.listStatements((Resource)null, functional_properties.next(), (RDFNode)null);
			while (stmt_iter.hasNext()) {
				if ((functionl_property_quota = change_object_values(stmt_iter.next(), functionl_property_quota)) <= 0) {
					stmt_iter.close();
					break L1;
				}
			}
		}
		if (functionl_property_quota > 0)
			count = count + functionl_property_quota;
		for (int i = 0; i < count; i++) {
			StmtIterator stmt_iter = fragment.listStatements(list_resources.get(generator.nextInt(list_resources.size()) + 1), (Property) null, (RDFNode) null);
			if (stmt_iter.hasNext()) 
				change_object_values(stmt_iter.next(), 0);
			stmt_iter.close();
		}
	}

}
