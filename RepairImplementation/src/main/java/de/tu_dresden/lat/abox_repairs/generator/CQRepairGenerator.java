package de.tu_dresden.lat.abox_repairs.generator;

import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;

public class CQRepairGenerator extends RepairGenerator {

	private static Logger logger = LogManager.getLogger(CQRepairGenerator.class);

	private Queue<Pair<OWLNamedIndividual, OWLNamedIndividual>> queueOfPairs;
	
	public CQRepairGenerator(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		
		super(inputOntology, inputSeedFunction);
	}
	
	
	protected void initialisation() {
		// Variables Intitialisation
		
		anonymousDetector = AnonymousVariableDetector.newInstance(true, Main.RepairVariant.CQ);
		
		setOfCollectedIndividuals = setOfSaturatedIndividuals.stream()
				.filter(ind -> anonymousDetector.isNamed(ind))
				.collect(Collectors.toSet());
		
		for(OWLNamedIndividual individual : setOfSaturatedIndividuals) {
			
			if(anonymousDetector.isAnonymous(individual) || 
				!seedFunction.get(individual).getClassExpressions().isEmpty()) {				
				createCopy(individual,typeHandler.newMinimisedRepairType(new HashSet<>()));
				individualCounter.put(individual, individualCounter.get(individual) + 1);
			}
			
		}
	}
	
	protected void generatingVariables() {
		queueOfPairs = 
				new PriorityQueue<Pair<OWLNamedIndividual, OWLNamedIndividual>>();
		
		for(OWLNamedIndividual ind1 : setOfCollectedIndividuals) {
			for (OWLNamedIndividual ind2 : setOfCollectedIndividuals) {
				Pair<OWLNamedIndividual, OWLNamedIndividual> pair = Pair.of(ind1, ind2);
				queueOfPairs.add(pair);
			}
		}
		
		while(!queueOfPairs.isEmpty()) {
			Pair<OWLNamedIndividual, OWLNamedIndividual> p = queueOfPairs.poll();
			OWLNamedIndividual ind1 = p.getLeft();
			OWLNamedIndividual ind2 = p.getRight();
			
			OWLNamedIndividual originalInd1 = setOfSaturatedIndividuals.contains(ind1)? ind1 : copyToObject.get(ind1);
			OWLNamedIndividual originalInd2 = setOfSaturatedIndividuals.contains(ind2)? ind2 : copyToObject.get(ind2);
			
			for(OWLObjectProperty role: ontology.getObjectPropertiesInSignature()) {
				
				OWLObjectPropertyAssertionAxiom roleAssertion = factory
						.getOWLObjectPropertyAssertionAxiom(role, originalInd1, originalInd2);
				
				if(ontology.containsAxiom(roleAssertion)) {
					RepairType type1 = seedFunction.get(ind1);
					RepairType type2 = seedFunction.get(ind2);
					
					Set<OWLClassExpression> successorSet = computeSuccessorSet(
							type1,role, originalInd2);
					
					Set<OWLClassExpression> coveringSet = type2.getClassExpressions();
					
					if(!reasonerWithTBox.isCovered(successorSet, coveringSet)) {
						Set<RepairType> setOfRepairTypes = 
								typeHandler.findCoveringRepairTypes(type2, successorSet, originalInd2);
						
						for(RepairType newType : setOfRepairTypes) {
							if(!objectToCopies.get(originalInd2).stream().anyMatch(copy -> newType.equals(seedFunction.get(copy)))) {
								OWLNamedIndividual copy = createCopy(originalInd2, newType);
								
								for(OWLNamedIndividual individual : setOfCollectedIndividuals) {
									Pair<OWLNamedIndividual, OWLNamedIndividual> pair1 = Pair.of(individual, copy);
									Pair<OWLNamedIndividual, OWLNamedIndividual> pair2 = Pair.of(copy, individual);
									queueOfPairs.add(pair1);
									queueOfPairs.add(pair2);
								}
							}
							
						}
					}
				}
			}
		}
	}
	
	
}
