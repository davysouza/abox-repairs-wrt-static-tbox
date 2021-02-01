import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.NullReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.apache.log4j.BasicConfigurator;


public class PolicyRepair {
	Scanner reader;
	Set<OWLOntology> importsClosure;
	OWLEntityChecker entityChecker;
	ManchesterOWLSyntaxParser parser;
	OWLOntology ontology;
	OWLOntologyManager manager;

	
	
//	Set<OWLNamedIndividual> setOfIndividuals;
	Set<OWLAxiom> setOfPolicy;
	Map<OWLNamedIndividual, Set<OWLClassExpression>> seedFunction;
	Map<OWLNamedIndividual, Set<OWLClassExpression>> repairRequest;
	
	OWLReasonerFactory orf;
	ElkReasoner elk;
	OWLDataFactory df;
	ReasonerProgressMonitor progressMonitor;
	FreshEntityPolicy freshEntityPolicy;
	long timeOut;
	IndividualNodeSetPolicy individualNodeSetPolicy;
	OWLReasonerConfiguration conf;
	ElkReasonerFactory reasonerFactory;
	OWLReasoner reasoner;
	
	
	public PolicyRepair(OWLOntology ontology1, OWLOntologyManager manager1, File repairRequestFile) throws FileNotFoundException {
		ontology = ontology1;
		manager = manager1;
		reader = new Scanner(repairRequestFile);
		
		
		df = OWLManager.getOWLDataFactory();
		
		// Set a configuration for the reasoner
		BasicConfigurator.configure();
		ReasonerProgressMonitor progressMonitor = new NullReasonerProgressMonitor();
		FreshEntityPolicy freshEntityPolicy = FreshEntityPolicy.ALLOW;
		long timeOut = Integer.MAX_VALUE;
		IndividualNodeSetPolicy individualNodeSetPolicy = IndividualNodeSetPolicy.BY_NAME;
		conf = new SimpleConfiguration(progressMonitor, freshEntityPolicy, timeOut, individualNodeSetPolicy);
		
		// Instantiate an Elk Reasoner Factory
		reasonerFactory =  new ElkReasonerFactory();
		reasoner = reasonerFactory.createNonBufferingReasoner(ontology, conf);
	
//		setOfIndividuals = ontology.getIndividualsInSignature();
		importsClosure = ontology.getImportsClosure();
		entityChecker = new ShortFormEntityChecker(
		        new BidirectionalShortFormProviderAdapter(manager, importsClosure, 
		            new SimpleShortFormProvider()));
		
		parser = OWLManager.createManchesterParser();
		parser.setDefaultOntology(ontology);
	    parser.setOWLEntityChecker(entityChecker);
	    
//	    setOfConcepts = new HashSet<OWLClassExpression>();
	    repairRequest = new HashMap<OWLNamedIndividual, Set<OWLClassExpression>>();
		while(reader.hasNextLine()) {
			String policy = reader.nextLine();
		    parser.setStringToParse(policy.trim());
			
		    OWLClassAssertionAxiom axiom = (OWLClassAssertionAxiom) parser.parseAxiom();
		    
		    if(reasoner.isEntailed(axiom)) {
		    	OWLNamedIndividual assertedIndividual = (OWLNamedIndividual) axiom.getIndividual();
			    
			    if(repairRequest.containsKey(assertedIndividual)) {
			    	Set<OWLClassExpression> setOfClasses = repairRequest.get(assertedIndividual);
			    	setOfClasses.add(axiom.getClassExpression());
			    	repairRequest.put(assertedIndividual, setOfClasses);
			    }
			    else {
			    	Set<OWLClassExpression> setOfClasses = new HashSet<OWLClassExpression>();
			    	setOfClasses.add(axiom.getClassExpression());
			    	repairRequest.put(assertedIndividual, setOfClasses);
			    }
		    }
		}
//		System.out.println(repairRequest);
	}
	
	public Map<OWLNamedIndividual, Set<OWLClassExpression>> seedFunctionConstruction() {
		seedFunction = new HashMap<OWLNamedIndividual, Set<OWLClassExpression>>();
		Set<OWLNamedIndividual> setOfIndividuals = repairRequest.keySet();
		Iterator<OWLNamedIndividual> iteratorOfIndividuals = setOfIndividuals.iterator();
		while(iteratorOfIndividuals.hasNext()) {
			OWLNamedIndividual individual = iteratorOfIndividuals.next();
			Set<OWLClassExpression> setOfConcepts = repairRequest.get(individual);
			Iterator<OWLClassExpression> iteratorOfConcepts = setOfConcepts.iterator();
			while(iteratorOfConcepts.hasNext()) {
				OWLClassExpression concept = iteratorOfConcepts.next();
				if(concept instanceof OWLObjectIntersectionOf) {
					if(seedFunction.containsKey(individual)) {
						Set<OWLClassExpression> topLevelConjuncts = concept.asConjunctSet();
						if(Collections.disjoint(seedFunction.get(individual), topLevelConjuncts)) {
							List<OWLClassExpression> topLevelConjunctList = new ArrayList<OWLClassExpression>(topLevelConjuncts);
							Random rand = new Random();
							int randomIndex = rand.nextInt(topLevelConjunctList.size());
							OWLClassExpression conceptTemp = topLevelConjunctList.get(randomIndex);
							seedFunction.get(individual).add(conceptTemp);
							seedFunction.put(individual, seedFunction.get(individual));
						}
					}
					else {
						List<OWLClassExpression> topLevelConjunctList = new ArrayList<OWLClassExpression>(concept.asConjunctSet());
						Random rand = new Random();
						int randomIndex = rand.nextInt(topLevelConjunctList.size());
						OWLClassExpression conceptTemp = topLevelConjunctList.get(randomIndex);
						Set<OWLClassExpression> setOfConceptsTemp = new HashSet<OWLClassExpression>();
						setOfConceptsTemp.add(conceptTemp);
						seedFunction.put(individual, setOfConceptsTemp);
					}
				}
				else {
					if(seedFunction.containsKey(individual)) {
						if(!seedFunction.get(individual).contains(concept)) {
							Set<OWLClassExpression> setOfConceptsTemp = seedFunction.get(individual);
							setOfConceptsTemp.add(concept);
							seedFunction.put(individual, setOfConceptsTemp);
						}
					}
					else {
						Set<OWLClassExpression> setOfConceptsTemp = new HashSet<OWLClassExpression>();
						setOfConceptsTemp.add(concept);
						seedFunction.put(individual, setOfConceptsTemp);
						
					}
				}
				
			}
		}
		return seedFunction;
		
		
	}
}
