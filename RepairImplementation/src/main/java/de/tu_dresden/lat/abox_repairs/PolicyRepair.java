package de.tu_dresden.lat.abox_repairs;
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
	// CHECK: check whether for which variables you may be able to minimise the skope where they are visible
	// If a variable is only used within one method, it should only be initialised there.
	// If a variable should not change its value once initialised, use the keyword "final"
	private Set<OWLOntology> importsClosure;
	private OWLEntityChecker entityChecker;
	private ManchesterOWLSyntaxParser parser;
	private final OWLOntology ontology;
	private final OWLOntologyManager manager;

	
	
//	Set<OWLNamedIndividual> setOfIndividuals;
	private Set<OWLAxiom> setOfPolicy;
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> seedFunction;
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> repairRequest;
	
	private OWLReasonerFactory orf;
	private ElkReasoner elk; // CHECK: can you use OWLReasoner here?
	private OWLDataFactory df;
	private ReasonerProgressMonitor progressMonitor; // CHECK: should this really be visible anywhere in the class?
	private FreshEntityPolicy freshEntityPolicy;
	private long timeOut;
	private IndividualNodeSetPolicy individualNodeSetPolicy;
	private OWLReasonerConfiguration conf;
	private ElkReasonerFactory reasonerFactory; // CHECK: can you use OWLReasonerFactory here?
	private OWLReasoner reasoner; // CHECK: sure we need two reasoners?
	
	
	public PolicyRepair(OWLOntology ontology, OWLOntologyManager manager, File repairRequestFile) throws FileNotFoundException {
		this.ontology = ontology;
		this.manager = manager;
		final Scanner reader = new Scanner(repairRequestFile);
				
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
	
	public Map<OWLNamedIndividual, Set<OWLClassExpression>> constructSeedFunction() {
		Random rand = new Random();

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
							
							int randomIndex = rand.nextInt(topLevelConjunctList.size());
							OWLClassExpression conceptTemp = topLevelConjunctList.get(randomIndex);
							seedFunction.get(individual).add(conceptTemp);
							seedFunction.put(individual, seedFunction.get(individual));
						}
					}
					else {
						List<OWLClassExpression> topLevelConjunctList = new ArrayList<OWLClassExpression>(concept.asConjunctSet());
						
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
