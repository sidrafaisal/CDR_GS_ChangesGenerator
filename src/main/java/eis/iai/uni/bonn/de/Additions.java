package eis.iai.uni.bonn.de;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class Additions extends GS_ChangesGenerator {
	public static void create(int count) {
		int change_quota, change_subject_quota, change_predicate_quota, change_object_quota;
		Set<Statement> ss = new HashSet<Statement>();
		Iterator<Statement> stmtiter;

		if (!fragment.isEmpty() && count > 0) {
			count -= (change_quota = generator.nextInt(count)+1);
			while (count > 0 && change_quota > 0) {	// simple addition	
				StmtIterator stmtiter1 = fragment.listStatements(arbitrary_subject(), (Property) null, (RDFNode) null);
				if (stmtiter1.hasNext()) {
					Statement stmt = stmtiter1.next();
					fragment.remove(stmt);
					additions.add(stmt);					
					change_quota--;
				}
				stmtiter1.close();
			}	
			if ((count+=change_quota) > 0) {							
				count -= (change_object_quota = generator.nextInt(count)+1);
				if (change_object_quota > 0) {	// change_object
					Iterator<Property> fpi = fp.iterator();
					while (fpi.hasNext()){			
						ss.addAll(fragment.listStatements((Resource)null, fpi.next(), (RDFNode) null).toSet());	
						if(ss.size()==change_object_quota)
							break;
					}
					stmtiter = ss.iterator();
					while(stmtiter.hasNext()) {
						Statement stmt = stmtiter.next();	
						if (!already_used_triples.contains(stmt)) {
							already_used_triples.add(stmt); 
						Resource subject = stmt.getSubject();
						RDFNode newobject = arbitrary_literalobject(stmt.getObject());						
						Property property = stmt.getPredicate();
						if (generator.nextInt(2)==0) 
							subject = same_resource(subject);
						if (generator.nextInt(2)==0) 
							property = (Property) omodel.getOntProperty(property.toString()).getEquivalentProperty().asProperty();
										
						additions.add(ResourceFactory.createStatement(subject, property, newobject));	
						change_object_quota--;
					}
					}
					ss.clear();
					if (change_object_quota > 0) {
						StmtIterator stmtiter1 = fragment.listStatements((Resource)null, type_property, (RDFNode) null);
						while (stmtiter1.hasNext()) {
							Statement stmt = stmtiter1.next();
							if (!already_used_triples.contains(stmt)) {
								already_used_triples.add(stmt); 
							Resource subject = stmt.getSubject();
							if (generator.nextInt(2)==0) 
								subject = same_resource(subject);
							additions.add(ResourceFactory.createStatement(subject, stmt.getPredicate(), 
									(RDFNode) ResourceFactory.createResource(dc.get(stmt.getObject().toString()))));					
							change_object_quota--;						
						}
						}
						stmtiter1.close();
					}
				}
				if ((count += change_object_quota) > 0) {		
					count -= (change_subject_quota = generator.nextInt(count)+1);			
					if (change_subject_quota > 0) {	// change_subject
						Iterator<Property> fpi = ifp.iterator();
						while (fpi.hasNext()){			
							ss.addAll(fragment.listStatements((Resource)null, (Property) fpi.next(), (RDFNode) null).toSet());	
							if(ss.size()==change_subject_quota)
								break;
						}
						stmtiter = ss.iterator();
						while(stmtiter.hasNext()) {
							Statement stmt = stmtiter.next();
							if (!already_used_triples.contains(stmt)) {
								already_used_triples.add(stmt); 
							additions.add(ResourceFactory.createStatement(arbitrary_resource(stmt.getSubject()), stmt.getPredicate(), stmt.getObject()));	
							change_subject_quota--;
						}	
						}
						ss.clear();
					}
					if ((count += change_subject_quota) > 0) {	// change_predicate
						count -= (change_predicate_quota = generator.nextInt(count)+1);
						if (change_predicate_quota > 0) {	
							Set<Property> keys = dp.keySet();
							for (Property k: keys) {
								ss.addAll(fragment.listStatements((Resource)null, k, (RDFNode) null).toSet());	
								if(ss.size()==change_predicate_quota)
									break;							
							}
							stmtiter = ss.iterator();
							while(stmtiter.hasNext()) {
								Statement stmt = stmtiter.next();
								if (!already_used_triples.contains(stmt)) {
									already_used_triples.add(stmt); 
								Resource subject = stmt.getSubject();
								if (generator.nextInt(2)==0) 
									subject = same_resource(subject);
								additions.add(ResourceFactory.createStatement(subject, dp.get(stmt.getPredicate()), stmt.getObject()));	
								change_predicate_quota--;
							}	
							}
						}			
					}					
				}
			}
		}
	}
}