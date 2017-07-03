package eis.iai.uni.bonn.de;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class ICG {
	protected static Model fragment, additions;
	protected static OntModel omodel;
	protected static List<Property> predicates;
	protected static List<Resource> list_resources;
	protected static String filesyntax;
	protected static Random generator = new Random();
	protected static OWLDataFactory fac; 
	protected static OWLReasoner reasoner;
	protected static OWLOntologyManager manager;
	protected static Property dfrom = ResourceFactory.createProperty("http://www.w3.org/2002/07/owl#differentFrom"),
			type_property = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
			same = ResourceFactory.createProperty("http://www.w3.org/2002/07/owl#sameAs");
	protected static Map<OntProperty, Node> disjoint = new HashMap<OntProperty, Node>();
	public static ArrayList<String> disjoint_list = new ArrayList<String>();
	
	// fragment, vocabulory, outputfile, hours
	public static void main (String [] args) {	
		try {
			System.out.println("Configuring..");

			filesyntax = "NT";
			(omodel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF)).read(FileManager.get().open(args[1]), null);
			fac = (manager = OWLManager.createOWLOntologyManager()).getOWLDataFactory();
			reasoner = new Reasoner.ReasonerFactory().createNonBufferingReasoner(manager.loadOntologyFromOntologyDocument(new File(args[1])));
			getDisjointClasses();
			Iterator<OntProperty> ont = omodel.listAllOntProperties().toSet().iterator();
			while(ont.hasNext()) {
				OntProperty p = ont.next();
				RDFNode n  = p.getPropertyValue(ResourceFactory.createProperty("http://www.w3.org/2002/07/owl#propertyDisjointWith"));
				if (n!=null)
					disjoint.put(p, n.asNode());
			}

			/* Additions  */
			String [] consumers = {args[2], args[3]};
			GenerateNumberAndTypeOfChanges(args[0], consumers, args[4], Integer.parseInt(args[5]));			
		} catch (IOException | NumberFormatException | ParseException | OWLOntologyCreationException e) {
			e.printStackTrace();
		} 
	}
	

	public static void GenerateNumberAndTypeOfChanges(String slice, String [] output, String truth, int hours) throws IOException, OWLOntologyCreationException, ParseException {
		System.out.println("Generating changes..");
		int changeType;
		int arr[][] = new int[hours][];
		for (int i = 1; i <= hours; i++) {
			int no = getPoisson(2);//number
			if (no == 0) {
				i--;
				continue;
			}
			arr[i - 1] = getUniform(no);//type
		}

		for (int i = 0; i < arr.length; i++) {
			fragment = FileManager.get().loadModel(slice+i+".nt", filesyntax);
			list_resources = fragment.listResourcesWithProperty((Property)null).toList();
			for (int consumers = 0; consumers < 2; consumers++) {
			additions = FileManager.get().loadModel(createfile(output[consumers]+i+".nt"), filesyntax);	
				for (int j = 0; j < arr[i].length; j++) {
				changeType  = arr[i][j];	
				if(changeType==1) 	
					Subject.change_subject(3); 						// change subject
				else if(changeType==2) 				
					Predicate.change_predicate(3, false, false); 	// change predicate
				else if(changeType==3) 				
					Object.change_object(3, false);					// change object
				else if(changeType==4) 				
					Predicate.change_predicate(3, true, false); 	// change subject and predicate
				else if(changeType==5) 					
					Object.change_object(3, true); 					// change subject and object
				else if(changeType==6) 					
					Predicate.change_predicate(3, false, true); 	// change predicate and object
				else if(changeType==7) 
					Predicate.change_predicate(3, true, true); 		// change subject, predicate and object
			}			
		additions.write(new FileOutputStream(output[consumers]+i+".nt"), filesyntax);
		additions.close();
		}
			TruthGenerator.generateConflicts(slice, output[0], output[1], truth, i);
	}
		fragment.close();
	}
	public static int getPoisson(double lambda) {
		double L = Math.exp(-lambda);
		double p = 1.0;
		int k = 0;
		do {
			k++;
			p *= Math.random();
		} while (p > L);
		return k - 1;
	}

	public static int[] getUniform(int _param) {
		int N = _param;
		int numberOfChangeTypes = 11;
		int[] a = new int[N];
		Random generator = new Random();
		for (int i = 0; i < a.length; i++) {
			a[i] = generator.nextInt(numberOfChangeTypes) + 1;
		}
		return a;
	}

	protected static void additions_into_changeset(Node s, Node p, Node o) {
		additions.add(additions.asStatement(Triple.create(s, p, o)));
	}

	protected static Resource arbitrary_resource() {
		return list_resources.get(generator.nextInt(list_resources.size()));
	}
	
	protected static String createfile (String fname) throws IOException {
		File file = new File(fname);
		if(file.exists())			
			file.delete();
		file.createNewFile();
		return fname;
	}
	/////////////////////////////////disjoint class 
	private static void getDisjointClasses() throws OWLOntologyCreationException, IOException  {
		ExtendedIterator<OntClass> ocs = omodel.listClasses();
		while(ocs.hasNext()) {
			OntClass oc = ocs.next();
			NodeSet<OWLClass> disclass = reasoner.getDisjointClasses(fac.getOWLClass(IRI.create(oc.toString())));
			for (OWLClass c : disclass.getFlattened()) {
				//System.out.println(oc.toString() + "\t" + c.getIRI().toString()+"....dcfun");
				disjoint_list.add(oc.toString() + "\t" + c.getIRI().toString());			
			}
		}
	}
}
