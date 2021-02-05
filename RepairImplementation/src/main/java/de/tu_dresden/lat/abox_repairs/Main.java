package de.tu_dresden.lat.abox_repairs;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
	 
	
	private Set<OWLOntology> importsClosure;
	private OWLEntityChecker entityChecker;
	private ManchesterOWLSyntaxParser parser;
	
	//private ElkReasonerFactory reasonerFactory; // CHECK: can you use OWLReasonerFactory here?
//	private OWLReasoner reasoner; // CHECK: sure we need two reasoners?
	//private OWLReasonerConfiguration conf;
	
	private ReasonerFacade reasonerWithTBox, reasonerWithoutTBox;
	
	private Scanner reader;
	
	
	public static void main(String args[]) throws IOException, OWLOntologyCreationException {
		
		Main m = new Main();
		
		// Initialize ontology
		m.ontologyInitialisation(args);
		
		// Initialize parser
		m.parserInitialisation();
		
		// Initialize repair request
		m.repairRequestScanning(args);
		
		// Initialize reasoner 
		m.reasonerFacadeInitialisation();
		
		
		// TODO we need the reasoner when parsing the repair request, and we need the repair request
		// to initialise the reasoner facades. Might be worth decoupling this, so that we do not 
		// need to use the reasoner object at all. (For example, filter out useless repair requests in the end,
		// or just leave them in.)
		
		if(m.isCompliant(m.ontology, m.repairRequest)) {
			System.out.println("The ontology is compliant!");
		}
		else {
			System.out.println("The ontology is not compliant!");
			
			m.seedFunctionConstruction(m.repairRequest);
			Set<OWLNamedIndividual> setIndividuals = m.seedFunction.keySet();
			Iterator<OWLNamedIndividual> iteSetIndividuals = setIndividuals.iterator();
			while(iteSetIndividuals.hasNext()) {
				OWLNamedIndividual oni = iteSetIndividuals.next();
				System.out.println(oni);
				RepairType type = m.seedFunction.get(oni);
				System.out.println(type.getClassExpressions());
				System.out.println();
			}
			
			RepairGenerator generator = new RepairGenerator(m.ontology, m.seedFunction);
			generator.setReasoner(m.reasonerWithTBox, m.reasonerWithoutTBox);
			generator.repair();
			m.ontology = generator.getOntology();
			System.out.println("Size " + m.ontology.getIndividualsInSignature().size());
			
			m.reasonerFacadeInitialisation();
			
			if(m.isCompliant(m.ontology, m.repairRequest)) {
				System.out.println("The ontology is now compliant");
			}
			else {
				System.out.println("The ontology is still not compliant");
			}
		}
		
		
		
	}
	
	private void ontologyInitialisation(String input[]) throws OWLOntologyCreationException, FileNotFoundException {
		manager = OWLManager.createOWLOntologyManager();
		ontology = manager.loadOntologyFromOntologyDocument(new File(input[0]));
	}
	


	private void reasonerFacadeInitialisation() throws OWLOntologyCreationException {
		List<OWLClassExpression> additionalExpressions = new LinkedList<>();

		for(Collection<OWLClassExpression> exps:repairRequest.values()){
			for(OWLClassExpression exp: exps){
				additionalExpressions.add(exp);
				additionalExpressions.addAll(exp.getNestedClassExpressions());
			}
		}

		reasonerWithTBox = ReasonerFacade.newReasonerFacadeWithTBox(ontology, additionalExpressions);
		reasonerWithoutTBox = ReasonerFacade.newReasonerFacadeWithoutTBox(ontology, additionalExpressions);
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
		repairRequest = new HashMap<>();
		while(reader.hasNextLine()) {
			String policy = reader.nextLine();
		    parser.setStringToParse(policy.trim());
			
		    OWLClassAssertionAxiom axiom = (OWLClassAssertionAxiom) parser.parseAxiom();
		    
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
		    
//		    if(reasoner.isEntailed(axiom)) {
//		    	
//		    }
		}
	}
	
	private void seedFunctionConstruction(Map<OWLNamedIndividual, Set<OWLClassExpression>> inputRepairRequest) {
		SeedFunctionHandler seedFunctionHandler = new SeedFunctionHandler(reasonerWithTBox, reasonerWithoutTBox);
		seedFunctionHandler.constructSeedFunction(inputRepairRequest);
		seedFunction = seedFunctionHandler.getSeedFunction();
	}
	
	private boolean isCompliant(OWLOntology inputOntology, Map<OWLNamedIndividual, Set<OWLClassExpression>> inputRepairRequest) {
		boolean check = true;
		
//		List<OWLClassExpression> additionalExpressions = new LinkedList<>();
//
//		for(Collection<OWLClassExpression> exps:inputRepairRequest.values()){
//			for(OWLClassExpression exp: exps){
//				additionalExpressions.add(exp);
//				additionalExpressions.addAll(exp.getNestedClassExpressions());
//			}
//		}
//
//		ReasonerFacade reasoner = ReasonerFacade.newReasonerFacadeWithTBox(inputOntology, additionalExpressions);
		
		for(OWLNamedIndividual individual : inputRepairRequest.keySet()) {
			for(OWLClassExpression concept : inputRepairRequest.get(individual)) {
				if(reasonerWithTBox.instanceOf(individual, concept)) {
					System.out.println(individual + " " + concept);
					check = false;
					break;
				}
			}
		}
		
		return check;
	}
	
//	private Map<OWLNamedIndividual, Set<OWLClassExpression>> getRepairRequest(){
//		return repairRequest;
//	}
//	
//	private Map<OWLNamedIndividual, RepairType> getSeedFunction(){
//		return seedFunction;
//	}
	
	
}
