package de.tu_dresden.lat.abox_repairs;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.NullReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;


public class Main {
	
	/**
	 * Check: usually, static fields (variables) should be avoided where possible.
	 * In this case: check whether they are really needed to be outside the main method,
	 * and otherwise, add them. 
	 */
	private OWLOntologyManager manager;
	private OWLOntology ontology;
	private IRI iri;
	private OWLDataFactory factory;
	
	private Map<OWLNamedIndividual, RepairType> seedFunction;
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> repairRequest;
	private Map<OWLNamedIndividual, Integer> individualCounter; 
	
	private Set<OWLOntology> importsClosure;
	private OWLEntityChecker entityChecker;
	private ManchesterOWLSyntaxParser parser;
	
	private ElkReasonerFactory reasonerFactory; // CHECK: can you use OWLReasonerFactory here?
	private OWLReasoner reasoner; // CHECK: sure we need two reasoners?
	private OWLReasonerConfiguration conf;
	
	private ReasonerFacade reasonerFacade;
	
	private Scanner reader;
	
	
	public static void main(String args[]) throws IOException, OWLOntologyCreationException {
		
		Main m = new Main();
		
		// Initialize ontology
		m.ontologyInitialisation(args);
		
		// Initialize reasoner
		m.reasonerInitialisation();
		
		// Initialize parser
		m.parserInitialisation();
		
		// Initialize repair request
		m.repairRequestScanning(args);
		
	
		if(m.getRepairRequest().isEmpty()) {
			System.out.println("The ontology is compliant!");
		}
		else {
			System.out.println("The ontology is not compliant!");
			// Construct seedFunction
			m.seedFunctionConstruction();
			
			
			
			Set<OWLNamedIndividual> setIndividuals = m.seedFunction.keySet();
			Iterator<OWLNamedIndividual> iteSetIndividuals = setIndividuals.iterator();
			while(iteSetIndividuals.hasNext()) {
				OWLNamedIndividual oni = iteSetIndividuals.next();
				System.out.println(oni);
				RepairType type = m.seedFunction.get(oni);
				System.out.println(type.getClassExpressions());
				System.out.println();
			}
			
//			Optional<IRI> opt = m.ontology.getOntologyID().getOntologyIRI();
//			m.iri = opt.get();
//			m.factory = m.ontology.getOWLOntologyManager().getOWLDataFactory();
//			
//			Set<OWLNamedIndividual> setOfIndividuals = m.ontology.getIndividualsInSignature();			
//			m.individualCounter = new HashMap<>();
//			CopiedIndividualsHandler copyHandler = new CopiedIndividualsHandler(m.ontology);
//			for(OWLNamedIndividual individual: setOfIndividuals) {
//				m.individualCounter.put(individual, 1);
//				OWLNamedIndividual freshIndividual = m.factory.getOWLNamedIndividual(
//						m.iri + "#" + individual.getIRI().getFragment() + m.individualCounter.get(individual));
//				
//				copyHandler.copyIndividual(individual,freshIndividual);
//			}
//			
//			m.reasonerFacade = new ReasonerFacade(m.ontology);
//			RepairRules rules = new RepairRules(m.reasonerFacade);
//			rules.repair(m.ontology, m.seedFunction);
//			m.ontology = rules.getOntology();
//			
//			Set<OWLNamedIndividual> setOfIndividuals2 = m.ontology.getIndividualsInSignature();
//			System.out.println();
//			for(OWLNamedIndividual individual: setOfIndividuals2) {
//				System.out.println("Result " + individual);
//				Set<OWLClassAssertionAxiom> ocaa = m.ontology.getClassAssertionAxioms(individual);
//				System.out.println(ocaa);
//				Set<OWLObjectPropertyAssertionAxiom> oopaa = m.ontology.getObjectPropertyAssertionAxioms(individual);
//				System.out.println(oopaa);
//			}
		}	
	}
	
	private void ontologyInitialisation(String input[]) throws OWLOntologyCreationException, FileNotFoundException {
		manager = OWLManager.createOWLOntologyManager();
		ontology = manager.loadOntologyFromOntologyDocument(new File(input[0]));
	}
	
	private void reasonerInitialisation() {
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
	}
	
	private void parserInitialisation() {
		importsClosure = ontology.getImportsClosure();
		entityChecker = new ShortFormEntityChecker(
		        new BidirectionalShortFormProviderAdapter(manager, importsClosure, 
		            new SimpleShortFormProvider()));
		
		parser = OWLManager.createManchesterParser();
		parser.setDefaultOntology(ontology);
	    parser.setOWLEntityChecker(entityChecker);
	    
	    
	}

	private void repairRequestScanning(String input[]) throws FileNotFoundException {
		
		reader = new Scanner(new File(input[1]));
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
	}
	
	private void seedFunctionConstruction() {
		ReasonerFacade facade1 = new ReasonerFacade(ontology);
		ReasonerFacade facade2 = new ReasonerFacade(ontology);
		SeedFunctionHandler seedFunctionHandler = new SeedFunctionHandler(reasoner, facade1, facade2);
		seedFunctionHandler.constructSeedFunction(getRepairRequest());
		seedFunction = seedFunctionHandler.getSeedFunction();
	}
	
	
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> getRepairRequest(){
		return repairRequest;
	}
	
	
}
