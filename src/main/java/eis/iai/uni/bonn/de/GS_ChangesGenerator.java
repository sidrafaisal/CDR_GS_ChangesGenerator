package eis.iai.uni.bonn.de;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.ontology.FunctionalProperty;
import org.apache.jena.ontology.InverseFunctionalProperty;
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
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class GS_ChangesGenerator {
	protected static Model fragment, additions, deletions, fragment_additions, already_used_triples;
	protected static OntModel omodel;
	protected static List<Property> predicates;
	protected static List<Resource> list_resources, list_subjects;
	protected static List<RDFNode> list_node;
	protected static String filesyntax = "NT";
	protected static Random generator = new Random();
	protected static OWLDataFactory fac; 
	protected static OWLReasoner reasoner;
	protected static OWLOntologyManager manager;
	protected static Set<Property> fp = new HashSet<Property>(), ifp = new HashSet<Property>();
	protected static Map<Property,Property> dp = new HashMap<Property,Property>();
	protected static Map<String,String> dc = new HashMap<String,String>();
	protected static Set<OntClass> ocs;
	protected static Property type_property = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
			sameas_property = ResourceFactory.createProperty("http://www.w3.org/2002/07/owl#sameAs");
	protected static int no_of_files = 2;

	public static void main (String [] args) {	
		try {
			System.out.println("Configuring..");
			(omodel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF)).read(FileManager.get().open(args[1]), null);
			fac = (manager = OWLManager.createOWLOntologyManager()).getOWLDataFactory();
			reasoner = new Reasoner.ReasonerFactory().createNonBufferingReasoner(manager.loadOntologyFromOntologyDocument(new File(args[1])));
			parse_ontology();

			String [] consumers = {args[2], args[3]};
			GenerateNumberAndTypeOfChanges(args[0], consumers, args[4], Integer.parseInt(args[5]));		
			//VerifyChanges(args[0], consumers, args[4]);
		} catch (IOException | NumberFormatException | OWLOntologyCreationException | ParseException e) {
			e.printStackTrace();
		} 
	}

	public static void VerifyChanges(String slice, String [] output, String gs) {

		Model m_gs = FileManager.get().loadModel(gs+".nt", filesyntax);

		fragment = FileManager.get().loadModel(slice+".nt", filesyntax);
		for (int i = 0; i < no_of_files; i++) {
			for (int consumers = 0; consumers < 2; consumers++) {
				additions = FileManager.get().loadModel(output[consumers]+"_add_"+i+".nt", filesyntax);
				deletions = FileManager.get().loadModel(output[consumers]+"_del_"+i+".nt", filesyntax);	
				fragment.add(additions.listStatements()).remove(deletions.listStatements());
			}
		}
		Model m_m1 = ModelFactory.createDefaultModel();
		Model m_m2 = ModelFactory.createDefaultModel();
		m_m1 = m_m1.add(m_gs.listStatements()).remove(fragment.listStatements());
		m_m2 = m_m2.add(fragment.listStatements()).remove(m_gs.listStatements());		

		if (!m_m1.isEmpty()) {
			System.out.println("nempty - gs-frag");
			StmtIterator iter = m_m1.listStatements();
			while (iter.hasNext()) {
				System.out.println(iter.next());
			}
		}
		if (!m_m2.isEmpty()) {
			System.out.println("nempty - frag-gs");
			StmtIterator iter = m_m2.listStatements();
			while (iter.hasNext()) {
				System.out.println(iter.next());
			}
		}
		m_m1.close();
		m_m2.close();
		deletions.close(); 
		additions.close();
		fragment.close();
	}
	public static void GenerateNumberAndTypeOfChanges(String slice, String [] output, String gs, int hours) throws IOException, OWLOntologyCreationException, ParseException {
		// create gold standard
		File file = new File(gs+".nt");
		if(file.exists())			
			file.delete();
		file.createNewFile();			
		FileUtils.copyFile(new File(slice+".nt"), file);

		//create changes
		fragment = FileManager.get().loadModel(slice+".nt", filesyntax);
		fragment_additions = ModelFactory.createDefaultModel();
		already_used_triples = ModelFactory.createDefaultModel();
		list_subjects = fragment.listSubjects().toList();
		list_resources = fragment.listResourcesWithProperty((Property)null).toList();
		list_node = fragment.listObjects().toList();

		int arr[][] = new int[hours][];
		for (int i = 1; i <= hours; i++) {
			int no = getPoisson(2);//number
			if (no == 0) {
				i--;
				continue;
			}
			arr[i - 1] = getUniform(no, 3);//type
		}
		System.out.println("Generating changes..");
		no_of_files = arr.length;
		for (int i = 0; i < no_of_files; i++) {
			for (int consumers = 0; consumers < 2; consumers++) {
				additions = FileManager.get().loadModel(createfile(output[consumers]+"_add_"+i+".nt"), filesyntax);
				deletions = FileManager.get().loadModel(createfile(output[consumers]+"_del_"+i+".nt"), filesyntax);	
				for (int k = 0; k < 3; k++) {
					for (int j = 0; j < arr[i].length; j++) {
						int changeType  = arr[i][j];	
						if(changeType==k && k==0) 	
							Deletions.create(5); 
						else if(changeType==k && k==1)
							Modifications.create(5);
						else if(changeType==k && k==2)
							Additions.create(5); 															
					}
				}
				additions.write(new FileOutputStream(output[consumers]+"_add_"+i+".nt"), filesyntax).close();
				deletions.write(new FileOutputStream(output[consumers]+"_del_"+i+".nt"), filesyntax).close();
			}
		}
		fragment.add(fragment_additions).write(new FileOutputStream(slice+".nt"), filesyntax).close();;
		fragment_additions.close();	
		already_used_triples.close(); 
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

	public static int[] getUniform(int _param, int numberOfChangeTypes) {
		int[] a = new int[_param];
		Random generator = new Random();
		for (int i = 0; i < a.length; i++) 
			a[i] = generator.nextInt(numberOfChangeTypes);
		return a;
	}

	protected static Resource arbitrary_resource() {
		return list_resources.get(generator.nextInt(list_resources.size()));
	}

	protected static Resource arbitrary_resource(RDFNode original_resource) {
		Resource newresource = arbitrary_resource();
		for (int k = 0; k<10 && original_resource.equals(newresource); k++) 
			newresource = arbitrary_resource();
		if (original_resource.equals(newresource))
			newresource = ResourceFactory.createResource("http://dbpedia.org/resource/Bertolt");
		return newresource;		
	}

	protected static RDFNode arbitrary_literalobject() {	
		RDFNode node = list_node.get(generator.nextInt(list_node.size()));
		while (!node.isLiteral())
			node = list_node.get(generator.nextInt(list_node.size()));
		return node;
	}

	protected static RDFNode arbitrary_literalobject(RDFNode original_object) {	
		RDFNode n = arbitrary_literalobject();
		for (int k = 0; k<10 && original_object.equals(n); k++) 
			n = arbitrary_literalobject();
		if (original_object.equals(n)) 
			n = (RDFNode)(NodeFactory.createLiteral("xyz"));		
		return n;
	}

	protected static Resource same_resource(Resource resource) {	
		ExtendedIterator<Statement> stmtiter = fragment.listStatements(resource, sameas_property, (RDFNode)null).andThen(
				fragment.listStatements((Resource) null, sameas_property, (RDFNode)resource));
		Resource new_resource = resource;
		while(stmtiter.hasNext()) {
			Statement stmt = stmtiter.next();		
			if(!stmt.getSubject().equals(stmt.getObject())) { 
				if(stmt.getSubject().equals(resource))
					new_resource = stmt.getObject().asResource();
				else if(stmt.getObject().asResource().equals(resource))
					new_resource = stmt.getSubject();
				stmtiter.close();
				break;				
			}
		}
			return new_resource;
		}
		protected static Resource arbitrary_subject() {
			return list_subjects.get(generator.nextInt(list_subjects.size()));
		}

		protected static String createfile (String fname) throws IOException {
			File file = new File(fname);
			if(file.exists())			
				file.delete();
			file.createNewFile();
			return fname;
		}

		protected static Set<Property> readfile (String fname, Set<Property> s) throws IOException {
			BufferedReader br = new BufferedReader(new FileReader(fname));
			String line;
			while ((line = br.readLine()) != null) 
				s.add(ResourceFactory.createProperty(line));	
			br.close();
			return s;
		}

		protected static <T> void writefile (String fname, Set<T> s) throws IOException{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fname));			
			String content = "";
			Iterator<T> iter = s.iterator();
			while (iter.hasNext()) 
				content += iter.next() + "\n";
			bw.write(content);
			bw.close();
		}

		@SuppressWarnings("unchecked")
		protected static <T> Map<T,T> readfile (String fname, Map<T,T> s) throws IOException{
			BufferedReader br = new BufferedReader(new FileReader(fname));
			String line;
			while ((line = br.readLine()) != null) {
				int index = line.indexOf("\t");
				if (fname=="dp.txt")
					s.put((T) ResourceFactory.createProperty(line.substring(0,index)), (T) ResourceFactory.createProperty(line.substring(index+1, line.length())));
				else
					s.put((T) line.substring(0,index), (T) line.substring(index+1, line.length()));					
			}
			br.close();	
			return s;
		}

		protected static <T> void writefile (String fname, Map<T,T> s) throws IOException{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fname));		
			String content = "";
			Iterator<T> iter = s.keySet().iterator();
			while (iter.hasNext()) {
				T k = iter.next();
				content += k + "\t"+ s.get(k) + "\n";
			}
			bw.write(content);
			bw.close();
		}

		protected static void parse_ontology () throws OWLOntologyCreationException, IOException {	
			if(new File("fp.txt").exists()) 
				fp = readfile("fp.txt", fp);				
			else {
				ExtendedIterator<FunctionalProperty> iter = omodel.listFunctionalProperties();
				while(iter.hasNext())
					fp.add(iter.next());
				writefile("fp.txt", fp);
			}

			if((new File("ifp.txt")).exists()) 
				ifp = readfile("ifp.txt", ifp);				
			else {
				ExtendedIterator<InverseFunctionalProperty> iter = omodel.listInverseFunctionalProperties();
				while(iter.hasNext())
					ifp.add(iter.next());			
				writefile("ifp.txt", ifp);
			}
			///////////////////////////////// getDisjointProperties
			if(new File("dp.txt").exists()) 
				dp = readfile("dp.txt", dp);
			else {	
				ExtendedIterator<OntProperty> ont = omodel.listAllOntProperties();
				while(ont.hasNext()) {
					OntProperty p = ont.next();
					RDFNode n  = p.getPropertyValue(ResourceFactory.createProperty("http://www.w3.org/2002/07/owl#propertyDisjointWith"));
					if (n!=null)
						dp.put(p, ResourceFactory.createProperty(n.toString()));			
				}
				writefile("dp.txt", dp);
			}
			///////////////////////////////// getDisjointClasses
			ocs = omodel.listClasses().toSet();
			if(new File("dc.txt").exists()) 
				dc = readfile("dc.txt", dc);
			else {
				Iterator<OntClass> iter_ocs = ocs.iterator();
				while(iter_ocs.hasNext()) {
					OWLClass oc = fac.getOWLClass(IRI.create(iter_ocs.next().toString()));
					Set<OWLClass> disclass = reasoner.getDisjointClasses(oc).getFlattened();
					for (OWLClass c : disclass) 
						dc.put(oc.getIRI().toString(), c.getIRI().toString());			
				}
				writefile("dc.txt", dc);
			}
		}
	}
