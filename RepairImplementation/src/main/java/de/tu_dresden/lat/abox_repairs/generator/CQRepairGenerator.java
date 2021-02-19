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
	
	
	public void repair() throws OWLOntologyCreationException {
		
		initialisation();
		
		long startTimeVariables = System.nanoTime();
		
		generatingVariables();
		
		double timeVariables = (double)(System.nanoTime() - startTimeVariables)/1_000_000_000;
		
		logger.info("Time for generating variables: " + timeVariables);
		
		logger.debug("\nAfter generating necessary variables");
		for(OWLNamedIndividual ind : setOfCollectedIndividuals) {
			logger.debug("- " + ind);
			if(seedFunction.get(ind)!= null) {
				logger.debug(seedFunction.get(ind).getClassExpressions());
				logger.debug("");
			}
			
		}
		
		long startTimeMatrix = System.nanoTime();
		
		generatingMatrix();
		
		double timeMatrix = (double)(System.nanoTime() - startTimeMatrix)/1_000_000_000;
		
		logger.info("Time for generating Matrix: " + timeMatrix);
		
		logger.debug("\nAfter generating matrix");
		newOntology.axioms().forEach(ax -> logger.debug(ax));
	}
	
	protected void initialisation() {
		// Variables Intitialisation
		
		anonymousDetector = AnonymousVariableDetector.newInstance(true, Main.RepairVariant.CQ);
		
		setOfCollectedIndividuals = setOfSaturationIndividuals.stream()
				.filter(ind -> anonymousDetector.isNamed(ind))
				.collect(Collectors.toSet());
		
		for(OWLNamedIndividual individual : setOfSaturationIndividuals) {
			
			if(seedFunction.get(individual) != null) {
				individualCounter.put(individual, individualCounter.get(individual) + 1);
				OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(
						individual.getIRI().getFragment() + (individualCounter.get(individual)));
				
				RepairType type = typeHandler.newMinimisedRepairType(new HashSet<>());
				seedFunction.put(freshIndividual, type);
				Set<OWLNamedIndividual> setOfCopies = objectToCopies.get(individual);
				setOfCopies.add(freshIndividual);
				objectToCopies.put(individual, setOfCopies);
				copyToObject.put(freshIndividual, individual);
				setOfCollectedIndividuals.add(freshIndividual);
			}
			
			else {
				RepairType type = typeHandler.newMinimisedRepairType(new HashSet<>());
				seedFunction.put(individual, type);
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
			
			OWLNamedIndividual originalInd1 = setOfSaturationIndividuals.contains(ind1)? ind1 : copyToObject.get(ind1);
			OWLNamedIndividual originalInd2 = setOfSaturationIndividuals.contains(ind2)? ind2 : copyToObject.get(ind2);
			
			for(OWLObjectProperty role: ontology.getObjectPropertiesInSignature()) {
				OWLObjectPropertyAssertionAxiom roleAssertion = factory
						.getOWLObjectPropertyAssertionAxiom(role, originalInd1, originalInd2);
				
				if(ontology.containsAxiom(roleAssertion)) {
					RepairType type1 = seedFunction.get(ind1);
					RepairType type2 = seedFunction.get(ind2);
					
					if(type1 != null) {
						Set<OWLClassExpression> successorSet = computeSuccessorSet(
								type1,role, originalInd2);
						
						Set<OWLClassExpression> coveringSet = type2 != null? 
								type2.getClassExpressions() : new HashSet<>();
						
						if(!reasonerWithTBox.isCovered(successorSet, coveringSet)) {
							Set<RepairType> setOfRepairTypes = 
									typeHandler.findCoveringRepairTypes(type2, successorSet, originalInd2);
							
							for(RepairType newType : setOfRepairTypes) {
								boolean individualAlreadyExists = false;
								for(OWLNamedIndividual copy : objectToCopies.get(originalInd2)) {
									if((seedFunction.get(copy) == null && newType.getClassExpressions().isEmpty()) ||
										(seedFunction.get(copy) != null && seedFunction.get(copy).equals(newType))) {
										individualAlreadyExists = true;
										break;
									}
								}
								if(!individualAlreadyExists) {
									makeCopy(originalInd2, newType);
								}
							}
						}
					}
				}
			}
		}
	}
	
	protected void makeCopy(OWLNamedIndividual ind, RepairType typ) {
		individualCounter.put(ind, individualCounter.get(ind) + 1);
		OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(
				 ind.getIRI().getFragment() + 
				individualCounter.get(ind));
		seedFunction.put(freshIndividual, typ);
		copyToObject.put(freshIndividual, ind);
		
		Set<OWLNamedIndividual> setOfCopies = objectToCopies.get(ind);
		setOfCopies.add(freshIndividual);
		objectToCopies.put(ind, setOfCopies);
		
		for(OWLNamedIndividual individual : setOfCollectedIndividuals) {
			Pair<OWLNamedIndividual, OWLNamedIndividual> pair1 = Pair.of(individual, freshIndividual);
			Pair<OWLNamedIndividual, OWLNamedIndividual> pair2 = Pair.of(freshIndividual, individual);
			Pair<OWLNamedIndividual, OWLNamedIndividual> pair3 = Pair.of(freshIndividual, freshIndividual);
			queueOfPairs.add(pair1);
			queueOfPairs.add(pair2);
			queueOfPairs.add(pair3);
		}

		
		setOfCollectedIndividuals.add(freshIndividual);
	}
}
