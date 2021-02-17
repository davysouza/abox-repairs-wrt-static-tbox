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
	
	
	private Map<OWLNamedIndividual, RepairType> seedFunction;
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> hittingSetFunction;
	private final ReasonerFacade reasonerWithTBox;
	private final ReasonerFacade reasonerWithoutTBox;
	
	private Random rand;
	
	public SeedFunctionHandler(ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox) {
		this.reasonerWithTBox = reasonerWithTBox;
		this.reasonerWithoutTBox = reasonerWithoutTBox;
	}
	
	
	/**
	 * 
	 * Given a repair request, the method constructs a seed function that 
	 * maps each individual in the repair request to a repair type
	 * 
	 * @param a repair request
	 * 
	 */
	
	public void constructSeedFunction(RepairRequest repairRequest) {
		
		rand = new Random();
		
		constructHittingSet(repairRequest);
		
		seedFunction = new HashMap<>();
		Set<OWLNamedIndividual> setOfIndividuals = hittingSetFunction.keySet();
		RepairTypeHandler typeHandler = new RepairTypeHandler(reasonerWithTBox, reasonerWithoutTBox);
		
		for(OWLNamedIndividual individual : setOfIndividuals) {
			
			RepairType type = typeHandler.convertToRandomRepairType(hittingSetFunction.get(individual), rand);
			seedFunction.put(individual, type);
		}
		
	}
	
	/**
	 *
	 * Given a repair request, for each individual in the request, the method
	 * constructs a hitting set of the top-level conjuncts of concepts that are 
	 * asserted to the individual.
	 * 
	 * @param a repair request
	 * 
	 *  
	 */
	private void constructHittingSet(RepairRequest repairRequest) {
		hittingSetFunction = new HashMap<>();
		
		Set<OWLNamedIndividual> setOfIndividuals = repairRequest.keySet();
		for(OWLNamedIndividual individual : setOfIndividuals) {
			Set<OWLClassExpression> setOfConcepts = repairRequest.get(individual);
			
			System.out.println("Repair Request " + individual + " " + setOfConcepts);
			for(OWLClassExpression concept : setOfConcepts) {

				if(reasonerWithTBox.instanceOf(individual, concept) ) {

					if(concept instanceof OWLObjectIntersectionOf) {

						if(hittingSetFunction.containsKey(individual)) {
							Set<OWLClassExpression> topLevelConjuncts = concept.asConjunctSet();
							Optional<OWLClassExpression> opt = reasonerWithoutTBox.findCoveringConcept(
									hittingSetFunction.get(individual), topLevelConjuncts);
							if(opt.isPresent()) {
								hittingSetFunction.get(individual).add(opt.get());
								hittingSetFunction.put(individual, hittingSetFunction.get(individual));
							}
							else {
								List<OWLClassExpression> topLevelConjunctList = new ArrayList<OWLClassExpression>(topLevelConjuncts);
								int randomIndex = rand.nextInt(topLevelConjunctList.size());
								OWLClassExpression conceptTemp = topLevelConjunctList.get(randomIndex);
								hittingSetFunction.get(individual).add(conceptTemp);
								hittingSetFunction.put(individual, hittingSetFunction.get(individual));
							}
						}
						else {
							List<OWLClassExpression> topLevelConjunctList = new ArrayList<OWLClassExpression>(concept.asConjunctSet());
							int randomIndex = rand.nextInt(topLevelConjunctList.size());
							OWLClassExpression conceptTemp = topLevelConjunctList.get(randomIndex);
							Set<OWLClassExpression> initHittingSet = new HashSet<OWLClassExpression>();
							initHittingSet.add(conceptTemp);
							hittingSetFunction.put(individual, initHittingSet);
						}
					}
					else {
						if(hittingSetFunction.containsKey(individual)) {
							if(!hittingSetFunction.get(individual).contains(concept)) {
								Set<OWLClassExpression> currentHittingSet = hittingSetFunction.get(individual);
								currentHittingSet.add(concept);
								hittingSetFunction.put(individual, currentHittingSet);
							}
						}
						else {
							Set<OWLClassExpression> initHittingSet = new HashSet<OWLClassExpression>();
							initHittingSet.add(concept);
							hittingSetFunction.put(individual, initHittingSet);
						}
					}
				}
			}
		}
		
	}
	
	/**
	 * 
	 * @return the computed seed function
	 */
	public Map<OWLNamedIndividual, RepairType> getSeedFunction(){
		return seedFunction;
	}
	
	
}
