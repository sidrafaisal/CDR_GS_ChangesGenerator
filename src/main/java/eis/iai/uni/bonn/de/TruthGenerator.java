package eis.iai.uni.bonn.de;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.apache.commons.lang.StringUtils;

public class TruthGenerator extends ICG {
	protected static Model truthmodel = ModelFactory.createDefaultModel();
	protected static Model conflictsmodel = ModelFactory.createDefaultModel();
	protected static Set<String> classes = new HashSet<String>();

	protected static void generateConflicts(String slice, String src, String tar, String truth, int i) throws IOException, OWLOntologyCreationException{
		System.out.println("Generating truth values..");
		conflictsmodel = ModelFactory.createDefaultModel();
		truthmodel = FileManager.get().loadModel(createfile(truth+i+".nt"), filesyntax);
		change_subject (src, tar, i);
		change_predicate (src, tar, i); 
		change_object (src, tar, i); 
		change_predicate_object (src, tar, i);
		truthmodel.write(new FileOutputStream(truth+i+".nt"), filesyntax);		
		ICG.fragment = (ICG.fragment.add(FileManager.get().loadModel(src+i+".nt", filesyntax)).add(FileManager.get().loadModel(tar+i+".nt", filesyntax)).
				add(truthmodel)).remove(conflictsmodel);
		ICG.fragment.write(new FileOutputStream(slice+(i+1)+".nt"), filesyntax);
		truthmodel.close();
		conflictsmodel.close();
	}


	/*	Change Subject, Predicate and Object */
	private static void change_predicate_object (String src, String tar, int i) {
		Model m1 = FileManager.get().loadModel(src+i+".nt", filesyntax).add(FileManager.get().loadModel(tar+i+".nt", filesyntax));
		Model m2 = FileManager.get().loadModel(src+i+".nt", filesyntax);
		Model m3 = FileManager.get().loadModel(tar+i+".nt", filesyntax);

		/* domain
		m.add rule : ( domain_Of(A, B, UID) & fromDataset(S, A, O) & TypefromConsumer(S, D) & disjointfrom(D,B)) >> type(S,B), weight : weightMap["R2"];
		m.add rule : ( domain_Of(A, B, UID) & fromConsumer(S, A, O) & TypefromConsumer(S, D) & disjointfrom(D,B)) >> type(S,B), weight : weightMap["R3"];
		 */
		StmtIterator stmt_iter1 = m1.listStatements((Resource)null,type_property,(RDFNode)null); 
		while(stmt_iter1.hasNext()) {
			Statement stmt1 = stmt_iter1.next();
			ExtendedIterator<Statement> stmt_iter2 = fragment.listStatements(stmt1.getSubject(), (Property)null, (RDFNode)null).andThen(
					m1.listStatements(stmt1.getSubject(), (Property)null, (RDFNode)null));
			while(stmt_iter2.hasNext()) {	
				Statement stmt2 = stmt_iter2.next();
				OntProperty property = omodel.getOntProperty(stmt2.getPredicate().toString());
				if (property.getDomain()!=null && isDisjoint(property.getDomain(), stmt1.getObject().asResource())) {
					conflictsmodel.add(stmt1);
					conflictsmodel.add(stmt2);
					truthmodel.add(ResourceFactory.createStatement(stmt1.getSubject(), type_property, property.getDomain()));
				}
			}
			/* range
			m.add rule : ( range_Of(A, B, UID) & fromDataset(S, A, O) & TypefromConsumer(O, D) & disjointfrom(D,B)) >> type(O,B), weight : weightMap["R4"];
			m.add rule : ( range_Of(A, B, UID) & fromConsumer(S, A, O) & TypefromConsumer(O, D) & disjointfrom(D,B)) >> type(O,B), weight : weightMap["R5"];
			 */	
			stmt_iter2 = fragment.listStatements((Resource)null, (Property)null, (RDFNode)stmt1.getSubject()).andThen(
					m1.listStatements((Resource)null, (Property)null, (RDFNode)stmt1.getSubject()));
			while(stmt_iter2.hasNext()) {	
				Statement stmt2 = stmt_iter2.next();
				OntProperty property = omodel.getOntProperty(stmt2.getPredicate().toString());
				if (property.getRange()!=null && isDisjoint(property.getRange(), stmt1.getObject().asResource())) {
					conflictsmodel.add(stmt1);
					conflictsmodel.add(stmt2);
					truthmodel.add(ResourceFactory.createStatement(stmt1.getSubject().asResource(), type_property, property.getRange()));
				}
			}
		}
		/*equivalent property
		m.add rule : (eqv_property(A,B, UID) & fromDataset(S, A, P) & fromConsumer_1(S, B, N) & fromConsumer_2(S, B, O) & nsame(N,O)) >> relatedTo(S, B, P), weight : weightMap["R6"];
		m.add rule : (eqv_property(A,B, UID) & fromDataset(S, A, P) & fromConsumer_1(S, B, N) & fromConsumer_2(S, B, O) & diffrom(N,O)) >> relatedTo(S, B, P), weight : weightMap["R7"];
		m.add rule : (eqv_property(A,B, UID) & fromDataset(S, A, P) & fromConsumer_1(S, B, N) & fromConsumer_2(S, B, O) & dissimiliar(N,O)) >> relatedTo(S, B, P), weight : weightMap["R8"];
		 */
		StmtIterator stmt_iter2 = m2.listStatements(); 
		while(stmt_iter2.hasNext()) {
			Statement stmt2 = stmt_iter2.next();
			StmtIterator stmt_iter3 = m3.listStatements(stmt2.getSubject(), stmt2.getPredicate(), (RDFNode)null);
			while(stmt_iter3.hasNext()) {	
				Statement stmt3 = stmt_iter3.next();
				Boolean check = false;

				if (stmt2.getObject().isResource() && stmt3.getObject().isResource() && stmt2.getObject()!=stmt3.getObject()) {
					Statement check_s1 = ResourceFactory.createStatement(stmt2.getObject().asResource(), dfrom, stmt3.getObject());
					Statement check_s2 = ResourceFactory.createStatement(stmt3.getObject().asResource(), dfrom, stmt2.getObject());
					Statement check_s3 = ResourceFactory.createStatement(stmt2.getObject().asResource(), same, stmt3.getObject());
					Statement check_s4 = ResourceFactory.createStatement(stmt3.getObject().asResource(), same, stmt2.getObject());

					if (fragment.contains(check_s1) || fragment.contains(check_s2) || m1.contains(check_s1) || m1.contains(check_s2) ||
							!(fragment.contains(check_s3) || fragment.contains(check_s4) || m1.contains(check_s3) || m1.contains(check_s4))) 
						check = true;
				} else if (disimilar (stmt2.getObject(), stmt3.getObject())) 
					check = true;				

				if (check) {
					OntProperty p = omodel.getOntProperty(stmt2.getPredicate().toString());
					ExtendedIterator<? extends OntProperty> ep_iter = p.listEquivalentProperties();
					while (ep_iter.hasNext()) {
						Property ep  = ep_iter.next();
						if (p != ep) {
							StmtIterator stmt_iter4 = fragment.listStatements(stmt2.getSubject(), ep, (RDFNode)null);
							while (stmt_iter4.hasNext()) {
								conflictsmodel.add(stmt3);
								conflictsmodel.add(stmt2);
								truthmodel.add(ResourceFactory.createStatement(stmt2.getSubject(), stmt2.getPredicate(), stmt_iter4.next().getObject()));						
							}
						}
					}
					/*sub property
					m.add rule : (sub_propertyOf(A,B, UID) & fromDataset(S, B, P) & fromConsumer_1(S, A, N) & fromConsumer_2(S, A, O) & nsame(N,O)) >> relatedTo(S, A, P), weight : weightMap["R9"];
					m.add rule : (sub_propertyOf(A,B, UID) & fromDataset(S, B, P) & fromConsumer_1(S, A, N) & fromConsumer_2(S, A, O) & diffrom(N,O)) >> relatedTo(S, A, P), weight : weightMap["R10"];
					m.add rule : (sub_propertyOf(A,B, UID) & fromDataset(S, B, P) & fromConsumer_1(S, A, N) & fromConsumer_2(S, A, O) & dissimiliar(N,O)) >> relatedTo(S, A, P), weight : weightMap["R11"];
					 */
					ExtendedIterator<? extends OntProperty> sp_iter = p.listSuperProperties();
					while (sp_iter.hasNext()) {
						Property sp  = sp_iter.next();
						if (p != sp) {
							StmtIterator stmt_iter4 = fragment.listStatements(stmt2.getSubject(), sp, (RDFNode)null);
							while (stmt_iter4.hasNext()) {
								conflictsmodel.add(stmt3);
								conflictsmodel.add(stmt2);
								truthmodel.add(ResourceFactory.createStatement(stmt2.getSubject(), stmt2.getPredicate(), stmt_iter4.next().getObject()));						
							}
						}
					}
					/* change s,p,o - same resources(can be used in combination with other rules??)
					m.add rule : (fromDataset(T, A, P) & fromConsumer_1(S, A, N) & fromConsumer_2(S, A, O) & sameas(T,S) & nsame(N,O)) >> relatedTo(S, A, P), weight : weightMap["R12"];
					m.add rule : (fromDataset(T, A, P) & fromConsumer_1(S, A, N) & fromConsumer_2(S, A, O) & sameas(T,S) & diffrom(N,O)) >> relatedTo(S, A, P), weight : weightMap["R13"];
					m.add rule : (fromDataset(T, A, P) & fromConsumer_1(S, A, N) & fromConsumer_2(S, A, O) & sameas(T,S) & dissimiliar(N,O)) >> relatedTo(S, A, P), weight : weightMap["R14"];
					 */
					ExtendedIterator<Statement> sameres_iter = fragment.listStatements(stmt2.getSubject(), same, (RDFNode)null).andThen(
							fragment.listStatements((Resource)null, same, (RDFNode)stmt2.getSubject())).andThen(
									m1.listStatements(stmt2.getSubject(), same, (RDFNode)null).andThen(
											m1.listStatements((Resource)null, same, (RDFNode)stmt2.getSubject())));
					while (sameres_iter.hasNext()) {
						Statement stmt4 = sameres_iter.next();
						Resource same_res = null;
						if (stmt4.getSubject()!=stmt2.getSubject())
							same_res = stmt4.getSubject();
						else if(stmt4.getObject().asResource() != stmt2.getSubject())
							same_res = stmt4.getObject().asResource();
						StmtIterator stmt_iter4 = fragment.listStatements(same_res, stmt2.getPredicate(), (RDFNode)null);
						while (stmt_iter4.hasNext()) {
							conflictsmodel.add(stmt3);
							conflictsmodel.add(stmt2);
							truthmodel.add(ResourceFactory.createStatement(stmt2.getSubject(), stmt2.getPredicate(), stmt_iter4.next().getObject()));
						}
						/*		m.add rule : (eqv_property(A,B, UID) & fromDataset(T, B, P) & fromConsumer_1(S, A, N) & fromConsumer_2(S, A, O) & sameas(T,S) & nsame(N,O)) >> relatedTo(S, A, P), weight : weightMap["R15"];
			m.add rule : (eqv_property(A,B, UID) & fromDataset(T, B, P) & fromConsumer_1(S, A, N) & fromConsumer_2(S, A, O) & sameas(T,S) & diffrom(N,O)) >> relatedTo(S, A, P), weight : weightMap["R16"];
						 */
						ep_iter = p.listEquivalentProperties();
						while (ep_iter.hasNext()) {
							Property ep  = ep_iter.next();
							if (p != ep) {
								StmtIterator stmt_iter5 = fragment.listStatements(same_res, ep, (RDFNode)null);
								while (stmt_iter5.hasNext()) {
									conflictsmodel.add(stmt3);
									conflictsmodel.add(stmt2);
									truthmodel.add(ResourceFactory.createStatement(stmt2.getSubject(), stmt2.getPredicate(), stmt_iter5.next().getObject()));						
								}
							}
						}
						/*
			m.add rule : (sub_propertyOf(A,B, UID) & fromDataset(T, B, P) & fromConsumer_1(S, A, N) & fromConsumer_2(S, A, O) & sameas(T,S) & nsame(N,O)) >> relatedTo(S, A, P), weight : weightMap["R17"];
			m.add rule : (sub_propertyOf(A,B, UID) & fromDataset(T, B, P) & fromConsumer_1(S, A, N) & fromConsumer_2(S, A, O) & sameas(T,S) & diffrom(N,O)) >> relatedTo(S, A, P), weight : weightMap["R18"];
						 */
						sp_iter = p.listSuperProperties();
						while (sp_iter.hasNext()) {
							Property sp  = sp_iter.next();
							if (p != sp) {
								StmtIterator stmt_iter6 = fragment.listStatements(same_res, sp, (RDFNode)null);
								while (stmt_iter6.hasNext()) {
									conflictsmodel.add(stmt3);
									conflictsmodel.add(stmt2);
									truthmodel.add(ResourceFactory.createStatement(stmt2.getSubject(), stmt2.getPredicate(), stmt_iter6.next().getObject()));						
								}
							}
						}
					}
				} 
			} 				
		}
		m1.close();
		m2.close();
		m3.close();
	}


	/* Change Object */		
	private static void change_object (String src, String tar, int i) {
		/* disjoint classes
		 * m.add rule : (TypefromDataset(S, B) & TypefromConsumer(S, D) & disjointfrom(D,B)) >> type(S,B), weight : weightMap["R1"];
		 */	
		Model m1 = FileManager.get().loadModel(src+i+".nt", filesyntax).add(FileManager.get().loadModel(tar+i+".nt", filesyntax));
		StmtIterator stmt_iter1 = m1.listStatements((Resource)null,type_property,(RDFNode)null); 
		while(stmt_iter1.hasNext()) {
			Statement stmt1 = stmt_iter1.next();
			StmtIterator stmt_iter2 = fragment.listStatements(stmt1.getSubject(), type_property, (RDFNode)null);
			while(stmt_iter2.hasNext()) {	
				Statement stmt2 = stmt_iter2.next();
				if (stmt1.getObject() != stmt2.getObject() && isDisjoint(stmt1.getObject().asResource(), stmt2.getObject().asResource())) {
					conflictsmodel.add(stmt1);
					conflictsmodel.add(stmt2);
					truthmodel.add(stmt2);
				}
			}
		}

		StmtIterator stmt_iter3 = m1.listStatements(); 
		while(stmt_iter3.hasNext()) {
			Statement stmt3 = stmt_iter3.next();
			/* 	m.add rule : (fromDataset(S, A, P) & fromConsumer(S, A, P1) & diffrom(P,P1)) >> relatedTo(S, A, P), weight : weightMap["R24"];
					m.add rule : (funProperty(A, UID) & fromDataset(S, A, P) & fromConsumer(S, A, P1) & nsame(P,P1)) >> relatedTo(P, same, P1), weight : weightMap["R22"];
		m.add rule : (nfunProperty(A, UID) & fromDataset(S, A, P) & fromConsumer(S, A, N) & similiar(N,P)) >> relatedTo(S, A, P), weight : weightMap["R19"];
			m.add rule : (funProperty(A, UID) & fromDataset(S, A, P) & fromConsumer(S, A, P1) & disimiliar(P,P1)) >> relatedTo(S, A, P), weight : weightMap["R27"];
	m.add rule : (funProperty(A, UID) & fromDataset(S, A, P) & fromConsumer(S, A, P1) & similiar(P,P1)) >> relatedTo(S, A, P), weight : weightMap["R28"];
			 */
			StmtIterator stmt_iter4 = fragment.listStatements(stmt3.getSubject(), stmt3.getPredicate(), (RDFNode)null);
			while (stmt_iter4.hasNext()) {	
				Statement stmt4 = stmt_iter4.next();
				RDFNode object3 = stmt3.getObject(), object4 = stmt4.getObject();
				if (object3 != object4) {
					if (object3.isResource() && object4.isResource()) {
						Statement check_s1 = ResourceFactory.createStatement(object3.asResource(), dfrom, object4);
						Statement check_s2 = ResourceFactory.createStatement(object4.asResource(), dfrom, object3);
						Statement check_s3 = ResourceFactory.createStatement(object3.asResource(), same, object4);
						Statement check_s4 = ResourceFactory.createStatement(object4.asResource(), same, object3);

						if (fragment.contains(check_s1) || fragment.contains(check_s2) || m1.contains(check_s1) || m1.contains(check_s2)) {
							conflictsmodel.add(stmt3);
							conflictsmodel.add(stmt4);
							truthmodel.add(stmt4);
						} else if (omodel.getOntProperty(stmt3.getPredicate().toString()).isFunctionalProperty() && !(fragment.contains(check_s3) || fragment.contains(check_s4) || m1.contains(check_s3) || m1.contains(check_s4))) {
							conflictsmodel.add(stmt3);
							conflictsmodel.add(stmt4);
							truthmodel.add(check_s3);
						} } else if (!omodel.getOntProperty(stmt3.getPredicate().toString()).isFunctionalProperty() && similar(stmt3.getObject(),stmt4.getObject())) {
							conflictsmodel.add(stmt3);
							conflictsmodel.add(stmt4);
							truthmodel.add(stmt4);
						} else if (omodel.getOntProperty(stmt3.getPredicate().toString()).isFunctionalProperty() && disimilar(stmt3.getObject(),stmt4.getObject())) {
							conflictsmodel.add(stmt3);
							conflictsmodel.add(stmt4);
							truthmodel.add(stmt4);
						} else if (omodel.getOntProperty(stmt3.getPredicate().toString()).isFunctionalProperty() && similar(stmt3.getObject(),stmt4.getObject())) {
							conflictsmodel.add(stmt3);
							conflictsmodel.add(stmt4);
							truthmodel.add(stmt4);
						}
				}
			}
		} 
		m1.close();
	}	

	/*	Change Predicate 
	m.add rule : (disjoint_property(A,B, UID) & fromDataset(S, A, P) & fromConsumer(S, B, P)) >> relatedTo(S, A, P), weight : weightMap["R21"];
	 */
	private static void change_predicate (String src, String tar, int i) {
		Model m1 = FileManager.get().loadModel(src+i+".nt", filesyntax).add(FileManager.get().loadModel(tar+i+".nt", filesyntax));
		StmtIterator stmt_iter1 = m1.listStatements(); 
		while(stmt_iter1.hasNext()) {
			Statement stmt1 = stmt_iter1.next();
			Node n = disjoint.get(omodel.getOntProperty(stmt1.getPredicate().toString()));
			if (n!=null) {
				Property dp = ResourceFactory.createProperty(n.toString());
				StmtIterator stmt_iter2 = fragment.listStatements(stmt1.getSubject(), dp, stmt1.getObject());
				while (stmt_iter2.hasNext()) {
					Statement stmt2 = stmt_iter2.next();
					conflictsmodel.add(stmt1);
					conflictsmodel.add(stmt2);
					truthmodel.add(stmt2);
				}
			}
		}
		m1.close();
	}



	/*	Change Subject 
	m.add rule : (invFunProperty(A, UID) & fromDataset(S, A, P) & fromConsumer(S1, A, P) & diffrom(S,S1)) >> relatedTo(S, A, P), weight : weightMap["R25"];
	m.add rule : (invFunProperty(A, UID) & fromDataset(S, A, P) & fromConsumer(S1, A, P) & nsame(S,S1)) >> relatedTo(S, same, S1), weight : weightMap["R26"];
	 */
	private static void change_subject (String src, String tar, int i) {
		Model m1 = FileManager.get().loadModel(src+i+".nt", filesyntax).add(FileManager.get().loadModel(tar+i+".nt", filesyntax));
		StmtIterator stmt_iter1 = m1.listStatements(); 
		while(stmt_iter1.hasNext()) {
			Statement stmt1 = stmt_iter1.next();
			Property p = stmt1.getPredicate();
			if (omodel.getOntProperty(p.toString()).isInverseFunctionalProperty()) {
				StmtIterator stmt_iter2 = fragment.listStatements((Resource)null, p, stmt1.getObject());
				while (stmt_iter2.hasNext()) {
					Statement stmt2 = stmt_iter2.next();
					if (stmt1.getSubject() != stmt2.getSubject()) {
						Statement check_s1 = ResourceFactory.createStatement(stmt1.getSubject(), dfrom, (RDFNode) stmt2.getSubject());
						Statement check_s2 = ResourceFactory.createStatement(stmt2.getSubject(), dfrom, (RDFNode) stmt1.getSubject());
						Statement check_s3 = ResourceFactory.createStatement(stmt1.getSubject(), same, (RDFNode) stmt2.getSubject());
						Statement check_s4 = ResourceFactory.createStatement(stmt2.getSubject(), same, (RDFNode) stmt1.getSubject());
						if (fragment.contains(check_s1) || fragment.contains(check_s2) || m1.contains(check_s1) || m1.contains(check_s2)) {
							conflictsmodel.add(stmt1);
							conflictsmodel.add(stmt2);
							truthmodel.add(stmt2);
						} else if (!(fragment.contains(check_s3) || fragment.contains(check_s4) || m1.contains(check_s3) || m1.contains(check_s4))) {
							conflictsmodel.add(stmt1);
							conflictsmodel.add(stmt2);
							truthmodel.add(check_s3);
						}
					}
				}
			}
		}
		m1.close();
	}


	public static boolean isDisjoint(Resource res1, Resource res2) {
		String str1 = res1.getURI(), str2 = res2.getURI();
		if (str1.equals(str2))
			return false;
		else if (disjoint_list.contains(str1+ "\t" + str2) || disjoint_list.contains(str2+ "\t" + str1))
			return true;
		else
			return false;
	}

	public static Boolean similar(RDFNode n1, RDFNode n2) {
		String a = n1.toString(), b = n2.toString();
		if (a.startsWith("http") && b.startsWith("http") )
			return false;

		int maxLen = Math.max(a.length(), b.length());
		if (maxLen == 0)
			return false;
		double sim = 1.0 - (StringUtils.getLevenshteinDistance(a, b) / maxLen);
		if (sim > 0.5)
			return true;
		else
			return false;
	}

	public static Boolean disimilar (RDFNode n1, RDFNode n2) {
		String a = n1.toString(), b = n2.toString();
		if ((a.matches("[0-9.-/]+") && b.matches("[0-9a-zA-Z.-/]+")) || (a.matches("[0-9a-zA-Z.-/]+") && b.matches("[0-9.-/]+")) || a.startsWith("http") || b.startsWith("http"))
			return false;
		int maxLen = Math.max(a.length(), b.length());
		if (maxLen == 0)
			return false;
		double sim = 1.0 - (StringUtils.getLevenshteinDistance(a, b) / maxLen);

		if (sim < 0.5)
			return true;
		else 
			return false;
	}

}