package de.tu_dresden.lat.abox_repairs;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairTypeHandler;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;


public class SeedFunctionHandler {
	
	
	private Map<OWLNamedIndividual, RepairType> seedFunction;
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> hittingSetFunction;
	private RepairRequest repairRequest;
	private OWLOntology ontology;
	private RepairTypeHandler typeHandler;
	private AnonymousVariableDetector detector;
	private final ReasonerFacade reasonerWithTBox;
	private final ReasonerFacade reasonerWithoutTBox;

	
	
	public SeedFunctionHandler(OWLOntology ontology, RepairRequest repairRequest, Main.RepairVariant variant,
			ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox) {
		
		this.ontology = ontology;
		this.repairRequest = repairRequest;
		
		if(variant.equals(Main.RepairVariant.IQ))
			detector = AnonymousVariableDetector.newInstance(true, Main.RepairVariant.IQ);
		else 
			detector = AnonymousVariableDetector.newInstance(true, Main.RepairVariant.CQ);
		this.reasonerWithTBox = reasonerWithTBox;
		this.reasonerWithoutTBox = reasonerWithoutTBox;
		this.typeHandler = new RepairTypeHandler(reasonerWithTBox, reasonerWithoutTBox);
	}
	
	
	/**
	 * 
	 * Given a repair request, the method constructs a seed function that 
	 * maps each individual in the repair request to a repair type
	 *
	 * 
	 */
	
	public Map<OWLNamedIndividual, RepairType> computeRandomSeedFunction() {
		
		computeRandomHittingSet();
		
		seedFunction = new HashMap<>();
		Set<OWLNamedIndividual> setOfMappedIndividuals = hittingSetFunction.keySet();
		
		
		for(OWLNamedIndividual individual : setOfMappedIndividuals) {
			
			RepairType type = typeHandler.convertToRandomRepairType(hittingSetFunction.get(individual), individual);
			seedFunction.put(individual, type);
		}
		
		Set<OWLNamedIndividual> setOfRemainingIndividuals = ontology.getIndividualsInSignature().stream()
															.filter(ind -> !setOfMappedIndividuals.contains(ind) &&
																			detector.isNamed(ind))
															.collect(Collectors.toSet());
		
		for(OWLNamedIndividual individual : setOfRemainingIndividuals) {
			RepairType type = typeHandler.newMinimisedRepairType(new HashSet<>());
			seedFunction.put(individual, type);
		}
		
		return seedFunction;
	}
	
	/**
	 *
	 * Given a repair request, for each individual in the request, the method
	 * constructs a hitting set of the top-level conjuncts of concepts that are 
	 * asserted to the individual.
	 * 
	 * @param repairRequest a repair request
	 */
	private void computeRandomHittingSet() {
		hittingSetFunction = new HashMap<>();
		
		for(OWLNamedIndividual individual : repairRequest.individuals()) {
			
			for(OWLClassExpression concept : repairRequest.get(individual)) {
				
				if(reasonerWithTBox.instanceOf(individual, concept) ) {
					
					if(concept instanceof OWLObjectIntersectionOf) {
						OWLClassExpression atom = concept.asConjunctSet().stream()
								.filter(con -> !reasonerWithTBox.equivalentToOWLThing(con))
								.findAny().orElseThrow(() -> new IllegalArgumentException("Invalid Repair Request"));
								
						concept = atom;
						
					}
					if(!hittingSetFunction.containsKey(individual))
						hittingSetFunction.put(individual, new HashSet<>());

					hittingSetFunction.get(individual).add(concept);
					
				}
			}
			
		}		
	}
	
//	/**
//	 * 
//	 * @return the computed seed function
//	 */
//	public Map<OWLNamedIndividual, RepairType> getSeedFunction(){
//		return seedFunction;
//	}
	
	
}
