package de.tu_dresden.lat.abox_repairs.seed_function;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;
import de.tu_dresden.lat.abox_repairs.repair_request.RepairRequest;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.lat.abox_repairs.repair_type.RepairTypeHandler;
import org.semanticweb.owlapi.model.OWLOntology;
import de.tu_dresden.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.lat.abox_repairs.repair_type.RepairTypeHandler;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;


public class SeedFunctionHandler {
	
	//private Map<OWLNamedIndividual, RepairType> seedFunction;
	//private Map<OWLNamedIndividual, Set<OWLClassExpression>> hittingSetFunction;
	//private RepairRequest repairRequest;
	private OWLOntology ontology;
	private final RepairTypeHandler typeHandler;
	private final AnonymousVariableDetector detector;
	private final ReasonerFacade reasonerWithTBox;
	private final ReasonerFacade reasonerWithoutTBox;

	
	
	public SeedFunctionHandler(
			OWLOntology ontology,
			//RepairRequest repairRequest, RepairManager.RepairVariant variant,
			ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox,
			AnonymousVariableDetector detector) {
		
		this.ontology = ontology;
		//this.repairRequest = repairRequest;

		this.detector=detector;

		this.reasonerWithTBox = reasonerWithTBox;
		this.reasonerWithoutTBox = reasonerWithoutTBox;
		this.typeHandler = new RepairTypeHandler(reasonerWithTBox, reasonerWithoutTBox);
	}
	
	
	/**
	 * 
	 * Given a repair request, the method constructs a seed function that 
	 * maps each individual in the repair request to a repair type
	 *
	 * Assumption: every individual in repairRequest is named
	 * @return
	 */
	
	public SeedFunction computeRandomSeedFunction(RepairRequest repairRequest) {
		
		Map<OWLNamedIndividual, Set<OWLClassExpression>> hittingSetFunction =
				computeRandomHittingSet(repairRequest);

		SeedFunction seedFunction = new SeedFunction();
		Set<OWLNamedIndividual> setOfMappedIndividuals = hittingSetFunction.keySet();
		
		
		for(OWLNamedIndividual individual : setOfMappedIndividuals) {
			
			RepairType type = typeHandler.convertToRandomRepairType(hittingSetFunction.get(individual), individual);
			seedFunction.put(individual, type);
		}
		
		Set<OWLNamedIndividual> setOfRemainingIndividuals = ontology.getIndividualsInSignature()
															.stream()
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
	 */
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> computeRandomHittingSet(RepairRequest repairRequest) {
		Map<OWLNamedIndividual, Set<OWLClassExpression>> hittingSetFunction = new HashMap<>(); // TODO use Multimap
		
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
		return hittingSetFunction;
	}	
}
