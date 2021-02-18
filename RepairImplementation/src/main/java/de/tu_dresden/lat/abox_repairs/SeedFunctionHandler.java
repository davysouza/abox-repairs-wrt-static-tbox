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
	 * @param repairRequest a repair request
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
						/**
						 * If the repair request contains a conjunction, we do not have to repair all conjuncts, but
						 * just one.
						 */

						if(hittingSetFunction.containsKey(individual)) {

							// in case we already added something to the individual, there are two possibilities
							// for the conjunction:
							// if we can find a covering concept for the conjunction (no TBox), we add it to the hitting set
							// otherwise, we pick a random conjunct, and add this to the repairtype
							//
							// the covering concept is a conjunct that subsumes something that is already in the hitting set.
							// why do we want to add it in this case?

							Set<OWLClassExpression> topLevelConjuncts = concept.asConjunctSet();
							Optional<OWLClassExpression> opt = reasonerWithoutTBox.findCoveringConcept(
									hittingSetFunction.get(individual), topLevelConjuncts);
							if(opt.isPresent()) {
								hittingSetFunction.get(individual).add(opt.get());

								// the following line looks suspicous: we are replacing the value under "individual"
								// with the value under "individual" - the map should not be changed.
								// either you wanted to do something else, or the following line can go.
								hittingSetFunction.put(individual, hittingSetFunction.get(individual));
							}
							else { // no covering concept returned
								List<OWLClassExpression> topLevelConjunctList = new ArrayList<OWLClassExpression>(topLevelConjuncts);
								int randomIndex = rand.nextInt(topLevelConjunctList.size());
								// small remark: we are converting a set into a list, and then using our random
								// generator to pick a random element. As the order of sets can be different on
								// different runs, this may still lead to different behaviour for the same seed function
								OWLClassExpression conceptTemp = topLevelConjunctList.get(randomIndex);
								hittingSetFunction.get(individual).add(conceptTemp);


								// the following line looks suspicous: we are replacing the value under "individual"
								// with the value under "individual" - the map should not be changed.
								// either you wanted to do something else, or the following line can go.
								hittingSetFunction.put(individual, hittingSetFunction.get(individual));
							}
						}
						else { // hittingSetFunction does not contain individual as key

							// in this case, we pick a random conjunct, and add this one to the hitting set function
							List<OWLClassExpression> topLevelConjunctList = new ArrayList<OWLClassExpression>(concept.asConjunctSet());
							int randomIndex = rand.nextInt(topLevelConjunctList.size());
							OWLClassExpression conceptTemp = topLevelConjunctList.get(randomIndex);
							Set<OWLClassExpression> initHittingSet = new HashSet<OWLClassExpression>();
							initHittingSet.add(conceptTemp);
							hittingSetFunction.put(individual, initHittingSet);
						}
					}
					else { // concept is not a conjunction
						if(!hittingSetFunction.containsKey(individual))
							hittingSetFunction.put(individual, new HashSet<>());

						hittingSetFunction.get(individual).add(concept);

						// Simplified from the following code:
						/*
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
						 */
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
