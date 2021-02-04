package de.tu_dresden.lat.abox_repairs;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairTypeHandler;


public class SeedFunctionHandler {
	// CHECK: check whether for which variables you may be able to minimise the skope where they are visible
	// If a variable is only used within one method, it should only be initialised there.
	// If a variable should not change its value once initialised, use the keyword "final"
	

	private Map<OWLNamedIndividual, RepairType> seedFunction;
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> seedFunctionCollector;
	//private OWLReasoner reasoner;
	private final ReasonerFacade reasonerWithTBox;
	private final ReasonerFacade reasonerWithoutTBox;
	private final OWLDataFactory factory;
	
	public SeedFunctionHandler(ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox) {
		this.reasonerWithTBox = reasonerWithTBox;
		this.reasonerWithoutTBox = reasonerWithoutTBox;
		this.factory = OWLManager.getOWLDataFactory();
	}
	
	public Map<OWLNamedIndividual, RepairType> getSeedFunction(){
		seedFunction = new HashMap<>();
		Set<OWLNamedIndividual> setOfIndividuals = seedFunctionCollector.keySet();
		RepairTypeHandler typeHandler = new RepairTypeHandler(reasonerWithTBox, reasonerWithoutTBox);
		
		for(OWLNamedIndividual individual : setOfIndividuals) {
			
//			System.out.println(seedFunctionCollector.get(individual));
			RepairType type = typeHandler.newMinimisedRepairType(seedFunctionCollector.get(individual));
			seedFunction.put(individual, type);
		}
		return seedFunction;
	}

	public void constructSeedFunction(Map<OWLNamedIndividual, Set<OWLClassExpression>> repairRequest) {
		
		Random rand = new Random();

		seedFunctionCollector = new HashMap<>();
		Set<OWLNamedIndividual> setOfIndividuals = repairRequest.keySet();
		for(OWLNamedIndividual individual : setOfIndividuals) {
			Set<OWLClassExpression> setOfConcepts = repairRequest.get(individual);
			System.out.println("Repair Request " + individual + " " + setOfConcepts);
			for(OWLClassExpression concept : setOfConcepts) {
				if(concept instanceof OWLObjectIntersectionOf) {
					if(seedFunctionCollector.containsKey(individual)) {
						Set<OWLClassExpression> topLevelConjuncts = concept.asConjunctSet();
						OWLClassExpression tempConcept = atLeastOneCovered(
								seedFunctionCollector.get(individual), topLevelConjuncts);
						if(tempConcept != null) {
//							System.out.println("I got you");
							seedFunctionCollector.get(individual).add(tempConcept);
							seedFunctionCollector.put(individual, seedFunctionCollector.get(individual));
						}
						else {
							List<OWLClassExpression> topLevelConjunctList = new ArrayList<OWLClassExpression>(topLevelConjuncts);
							int randomIndex = rand.nextInt(topLevelConjunctList.size());
							OWLClassExpression conceptTemp = topLevelConjunctList.get(randomIndex);
							seedFunctionCollector.get(individual).add(conceptTemp);
							seedFunctionCollector.put(individual, seedFunctionCollector.get(individual));
						}
					}
					else {
						List<OWLClassExpression> topLevelConjunctList = new ArrayList<OWLClassExpression>(concept.asConjunctSet());
						int randomIndex = rand.nextInt(topLevelConjunctList.size());
						OWLClassExpression conceptTemp = topLevelConjunctList.get(randomIndex);
						Set<OWLClassExpression> setOfConceptsTemp = new HashSet<OWLClassExpression>();
						setOfConceptsTemp.add(conceptTemp);
						seedFunctionCollector.put(individual, setOfConceptsTemp);
					}
				}
				else {
					if(seedFunctionCollector.containsKey(individual)) {
						if(!seedFunctionCollector.get(individual).contains(concept)) {
							Set<OWLClassExpression> setOfConceptsTemp = seedFunctionCollector.get(individual);
							setOfConceptsTemp.add(concept);
							seedFunctionCollector.put(individual, setOfConceptsTemp);
						}
					}
					else {
						Set<OWLClassExpression> setOfConceptsTemp = new HashSet<OWLClassExpression>();
						setOfConceptsTemp.add(concept);
						seedFunctionCollector.put(individual, setOfConceptsTemp);
					}
				}
			}
		}
	}
	
	private OWLClassExpression atLeastOneCovered(Set<OWLClassExpression> set1, Set<OWLClassExpression> set2) {
		
		for(OWLClassExpression atom1 : set1) {
			for(OWLClassExpression atom2 : set2) {
				//OWLAxiom axiom = factory.getOWLSubClassOfAxiom(atom1, atom2);
				if(reasonerWithTBox.subsumedBy(atom1, atom2)) {
					return atom2;
				}
			}
		}
		
		return null;
	}
}
