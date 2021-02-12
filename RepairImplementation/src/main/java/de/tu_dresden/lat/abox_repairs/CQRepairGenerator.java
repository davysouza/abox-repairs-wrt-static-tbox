package de.tu_dresden.lat.abox_repairs;

import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;

public class CQRepairGenerator extends RepairGenerator {
	
	private Queue<Pair<OWLNamedIndividual, OWLNamedIndividual>> queueOfPairs;
	
	public CQRepairGenerator(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		
		super(inputOntology, inputSeedFunction);
	}
	
	
	public void repair() throws OWLOntologyCreationException {
		
		// Variables Intitialisation
		for(OWLNamedIndividual individual : setOfOriginalIndividuals) {
			
			if(seedFunction.get(individual) != null) {
				individualCounter.put(individual, individualCounter.get(individual) + 1);
				OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(
						individual.getIRI().getFragment() + (individualCounter.get(individual)));
				
				RepairType type = typeHandler.newMinimisedRepairType(new HashSet<>());
				seedFunction.put(freshIndividual, type);
				Set<OWLNamedIndividual> setOfCopies = originalToCopy.get(individual);
				setOfCopies.add(freshIndividual);
				originalToCopy.put(individual, setOfCopies);
				copyToOriginal.put(freshIndividual, individual);
				setOfCollectedIndividuals.add(freshIndividual);
			}
			
			else {
				RepairType type = typeHandler.newMinimisedRepairType(new HashSet<>());
				seedFunction.put(individual, type);
			}
			
		}
		
		generatingVariables();
		
		System.out.println("\nAfter generating necessary variables");
		for(OWLNamedIndividual ind : setOfCollectedIndividuals) {
			System.out.println("- " + ind);
			if(seedFunction.get(ind)!= null) {
				System.out.println(seedFunction.get(ind).getClassExpressions());
				System.out.println();
			}
			
		}
		
		generatingMatrix();
		
		System.out.println("\nAfter generating matrix");
		newOntology.axioms().forEach(ax -> System.out.println(ax));
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
			
			OWLNamedIndividual originalInd1 = setOfOriginalIndividuals.contains(ind1)? ind1 : copyToOriginal.get(ind1);
			OWLNamedIndividual originalInd2 = setOfOriginalIndividuals.contains(ind2)? ind2 : copyToOriginal.get(ind2);
			
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
							Set<RepairType> setOfRepairTypes = typeHandler.findCoveringRepairTypes(type2, successorSet);
							
							for(RepairType newType : setOfRepairTypes) {
								boolean individualAlreadyExists = false;
								for(OWLNamedIndividual copy : originalToCopy.get(originalInd2)) {
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
		copyToOriginal.put(freshIndividual, ind);
		
		Set<OWLNamedIndividual> setOfCopies = originalToCopy.get(ind);
		setOfCopies.add(freshIndividual);
		originalToCopy.put(ind, setOfCopies);
		
		for(OWLNamedIndividual individual : setOfCollectedIndividuals) {
			Pair<OWLNamedIndividual, OWLNamedIndividual> pair1 = Pair.of(individual, freshIndividual);
			Pair<OWLNamedIndividual, OWLNamedIndividual> pair2 = Pair.of(freshIndividual, individual);
			queueOfPairs.add(pair1);
			queueOfPairs.add(pair2);
		}

		
		setOfCollectedIndividuals.add(freshIndividual);
	}
}
