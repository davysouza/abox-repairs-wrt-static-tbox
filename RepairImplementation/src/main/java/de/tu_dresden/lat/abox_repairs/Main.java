package de.tu_dresden.lat.abox_repairs;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.saturation.CanonicalModelGenerator;
import de.tu_dresden.lat.abox_repairs.saturation.ChaseGenerator;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;


public class Main {
	
	/**
	 * Check: usually, static fields (variables) should be avoided where possible.
	 * In this case: check whether they are really needed to be outside the main method,
	 * and otherwise, add them. 
	 */
	private OWLOntologyManager manager;
	private OWLOntology ontology;
	
	private Map<OWLNamedIndividual, RepairType> seedFunction;
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> repairRequest;
	 
	private Set<OWLOntology> importsClosure;
	private OWLEntityChecker entityChecker;
	private ManchesterOWLSyntaxParser parser;
	
	private ReasonerFacade reasonerWithTBox, reasonerWithoutTBox;
	
	private Scanner reader;
	
	
	public static void main(String args[]) throws IOException, OWLOntologyCreationException, SaturationException {
		
		Main m = new Main();
		
		int i = 0;
		while(i < args.length) {
			// Initialize ontology
			m.ontologyInitialisation(args, i);
			
			// Initialize parser
			m.parserInitialisation();
			
			// Initialize repair request
			m.repairRequestScanning(args, i);
			
			
			// Saturate the ontology
			if(args[i+2] instanceof String && args[i+2].equals("IQ")) {
//				m.iqSaturate();
			}
			else {
				m.cqSaturate();
			}
			
//			
			
			// Initialize reasoner 
			m.reasonerFacadeInitialisation();
			
			if(m.isCompliant(m.ontology, m.repairRequest)) {
				System.out.println("The ontology is compliant!");
			}
			else {
				System.out.println("The ontology is not compliant!");
				
				m.seedFunctionConstruction(m.repairRequest);
				Set<OWLNamedIndividual> setIndividuals = m.seedFunction.keySet();
				Iterator<OWLNamedIndividual> iteSetIndividuals = setIndividuals.iterator();
				System.out.println("Seed Function");
				while(iteSetIndividuals.hasNext()) {
					OWLNamedIndividual oni = iteSetIndividuals.next();
					System.out.println(oni);
					RepairType type = m.seedFunction.get(oni);
					System.out.println(type.getClassExpressions());
					System.out.println();
				}
				
				
				if(args[i+2] instanceof String && args[i+2].equals("IQ")) {
					m.IQRepair();
					
					System.out.println("Size of the IQ-repair:" + m.ontology.getIndividualsInSignature().size());
				}
				else {
					m.CQRepair();
					
					System.out.println("Size of the CQ-repair:" + m.ontology.getIndividualsInSignature().size());
				}
			
				
//				m.oldRepair();
				
			
				m.reasonerFacadeInitialisation();
				
				if(m.isCompliant(m.ontology, m.repairRequest)) {
					System.out.println("The ontology is now compliant");
				}
				else {
					System.out.println("The ontology is still not compliant");
				}
			}
			i+=3;
			if(i < args.length) System.out.println("\n" + "=================================================");
		}
		
		
	}
	
	private void ontologyInitialisation(String input[], int i) throws OWLOntologyCreationException, FileNotFoundException {
		manager = OWLManager.createOWLOntologyManager();
		ontology = manager.loadOntologyFromOntologyDocument(new File(input[i]));
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

	private void repairRequestScanning(String input[], int i) throws FileNotFoundException {
		
		reader = new Scanner(new File(input[i+1]));
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
		}
	}
	
	private void cqSaturate() throws SaturationException {
		ChaseGenerator chase = new ChaseGenerator();
		chase.saturate(ontology);
		ontology = chase.getOntology(); 
	}
	
	private void iqSaturate() throws SaturationException {
		CanonicalModelGenerator cmg = new CanonicalModelGenerator(reasonerWithTBox);
		cmg.saturate(ontology);
		ontology = cmg.getOntology(); 
	}
	
	private void seedFunctionConstruction(Map<OWLNamedIndividual, Set<OWLClassExpression>> inputRepairRequest) {
		SeedFunctionHandler seedFunctionHandler = new SeedFunctionHandler(reasonerWithTBox, reasonerWithoutTBox);
		seedFunctionHandler.constructSeedFunction(inputRepairRequest);
		seedFunction = seedFunctionHandler.getSeedFunction();
	}
	
	private boolean isCompliant(OWLOntology inputOntology, Map<OWLNamedIndividual, Set<OWLClassExpression>> inputRepairRequest) {
		boolean compliant = true;
		
		for(OWLNamedIndividual individual : inputRepairRequest.keySet()) {
			for(OWLClassExpression concept : inputRepairRequest.get(individual)) {
				if(reasonerWithTBox.instanceOf(individual, concept)) {
					return false;
				}
			}
			
		}
		
		return compliant;
	}
	
	
	private void CQRepair() throws OWLOntologyCreationException {
		CQRepairGenerator generator = new CQRepairGenerator(ontology, seedFunction);
		generator.setReasoner(reasonerWithTBox, reasonerWithoutTBox);
		generator.CQRepair();
		ontology = generator.getOntology();
	}
	
	private void IQRepair() throws OWLOntologyCreationException {
		IQRepairGenerator generator = new IQRepairGenerator(ontology, seedFunction);
		generator.setReasoner(reasonerWithTBox, reasonerWithoutTBox);
		generator.IQRepair();
		ontology = generator.getOntology();
	}
}
