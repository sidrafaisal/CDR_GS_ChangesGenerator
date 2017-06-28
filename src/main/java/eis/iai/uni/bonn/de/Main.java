package eis.iai.uni.bonn.de;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.jena.graph.Node;
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

public class Main {
	protected static Model fragment, additions;
	protected static OntModel omodel;
	protected static List<Property> predicates;
	protected static List<Resource> list_resources;
	protected static String filesyntax;
	protected static Random generator = new Random();
	protected static Property dfrom = ResourceFactory.createProperty("http://www.w3.org/2002/07/owl#differentFrom");
	protected static Map<OntProperty, Node> disjoint = new HashMap<OntProperty, Node>();
	
	// fragment, vocabulory, outputfile, filesyntax, hours,
	public static void main (String [] args) {	
		try {
			System.out.println("Setting up..");
			filesyntax = args[3];
			fragment = FileManager.get().loadModel(args[0], filesyntax);
			(omodel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF)).read(FileManager.get().open(args[1]), null);
			list_resources = fragment.listResourcesWithProperty((Property)null).toList();
			Iterator<OntProperty> ont = omodel.listAllOntProperties().toSet().iterator();
			while(ont.hasNext()) {
				OntProperty p = ont.next();
				RDFNode n  = p.getPropertyValue(ResourceFactory.createProperty("http://www.w3.org/2002/07/owl#propertyDisjointWith"));
				if (n!=null)
					disjoint.put(p, n.asNode());
			}
			//resources = fragment.listResourcesWithProperty((Property)null);
			additions = FileManager.get().loadModel(createfile(args[2]), filesyntax);

			System.out.println("Generating changes..");

			/* Additions  */
			Subject.change_subject(3);
			Predicate.change_predicate(3);
			Object.change_object(3);
			
			additions.write(new FileOutputStream(args[2]), filesyntax);
			additions.close();
			fragment.close();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}

	}

	// change subject and predicate
	// change subject and object
	// change subject, predicate and object

	protected static String createfile (String fname) throws IOException {
		File file = new File(fname);
		if(file.exists())			
			file.delete();
		file.createNewFile();
		return fname;
	}
}
