package de.tu_dresden.lat.abox_repairs;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairTypeHandler;


public class SeedFunctionHandler {
	// CHECK: check whether for which variables you may be able to minimise the skope where they are visible
	// If a variable is only used within one method, it should only be initialised there.
	// If a variable should not change its value once initialised, use the keyword "final"
	

	private Map<OWLNamedIndividual, RepairType> seedFunction;
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> seedFunctionCollector;
	private final ReasonerFacade reasonerWithTBox;
	private final ReasonerFacade reasonerWithoutTBox;
	
	private Random rand;
	
	public SeedFunctionHandler(ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox) {
		this.reasonerWithTBox = reasonerWithTBox;
		this.reasonerWithoutTBox = reasonerWithoutTBox;
	}
	
	public Map<OWLNamedIndividual, RepairType> getSeedFunction(Random random){
		
		return seedFunction;
	}


	/**
	 * TODO: the naming is misleading - this method does not actually compute the seed function.
	 * TODO: additional steps are performed by getSeedFunction()
	 */
	public void constructSeedFunction(RepairRequest repairRequest) {
		
		rand = new Random();
		
		constructHittingSet(repairRequest);
		
		seedFunction = new HashMap<>();
		Set<OWLNamedIndividual> setOfIndividuals = seedFunctionCollector.keySet();
		RepairTypeHandler typeHandler = new RepairTypeHandler(reasonerWithTBox, reasonerWithoutTBox);
		
		for(OWLNamedIndividual individual : setOfIndividuals) {
			
			RepairType type = typeHandler.convertToRandomRepairType(seedFunctionCollector.get(individual), rand);
			seedFunction.put(individual, type);
		}
		
	}
	
	private void constructHittingSet(RepairRequest repairRequest) {
		seedFunctionCollector = new HashMap<>();
		Set<OWLNamedIndividual> setOfIndividuals = repairRequest.keySet();
		for(OWLNamedIndividual individual : setOfIndividuals) {
			Set<OWLClassExpression> setOfConcepts = repairRequest.get(individual);
			System.out.println("Repair Request " + individual + " " + setOfConcepts);
			for(OWLClassExpression concept : setOfConcepts) {

				if(reasonerWithTBox.instanceOf(individual, concept) ) {

					if(concept instanceof OWLObjectIntersectionOf) {

						if(seedFunctionCollector.containsKey(individual)) {
							Set<OWLClassExpression> topLevelConjuncts = concept.asConjunctSet();
							Optional<OWLClassExpression> opt = reasonerWithoutTBox.findCoveringConcept(
									seedFunctionCollector.get(individual), topLevelConjuncts);
							if(opt.isPresent()) {
								seedFunctionCollector.get(individual).add(opt.get());
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
		
	}
	
	public Map<OWLNamedIndividual, RepairType> getSeedFunction(){
		return seedFunction;
	}
	
	
}
