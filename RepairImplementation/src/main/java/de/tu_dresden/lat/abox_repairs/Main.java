package de.tu_dresden.lat.abox_repairs;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;

import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;


public class Main {
	
	/**
	 * Check: usually, static fields (variables) should be avoided where possible.
	 * In this case: check whether they are really needed to be outside the main method,
	 * and otherwise, add them. 
	 */
	private static OWLOntologyManager manager;
	private static OWLOntology ontology;
	private static Set<OWLClassExpression> policySet;
	private static Map<OWLNamedIndividual, Set<OWLClassExpression>> seedFunction;
	
	public static void main(String args[]) throws IOException, OWLOntologyCreationException {
		
		manager = OWLManager.createOWLOntologyManager(); 
		ontology = manager.loadOntologyFromOntologyDocument(new File(args[0]));
		
		// Reading a policy file
		File policyFile = new File(args[1]);
		
		
		PolicyRepair policy = new PolicyRepair(ontology, manager, policyFile);
		
		if(policy.repairRequest.isEmpty()) {
			System.out.println("The ontology is compliant!");
		}
		else {
			System.out.println("The ontology is not compliant!");
			seedFunction = policy.constructSeedFunction();
			
			Set<OWLNamedIndividual> setIndividuals = seedFunction.keySet();
			Iterator<OWLNamedIndividual> iteSetIndividuals = setIndividuals.iterator();
			while(iteSetIndividuals.hasNext()) {
				OWLNamedIndividual oni = iteSetIndividuals.next();
				System.out.println(oni);
				Set<OWLClassExpression> setAtoms = seedFunction.get(oni);
				System.out.println(setAtoms);
				System.out.println();
			}
			
			
		}
		
		 // Generate a seed function
		
		
		OWLDataFactory df = OWLManager.getOWLDataFactory();
		
		
	}
}
