package de.tu_dresden.inf.lat.abox_repairs.generator;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.inf.lat.abox_repairs.saturation.AnonymousVariableDetector;
import de.tu_dresden.inf.lat.abox_repairs.repair_manager.RepairManagerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.*;

public class IQRepairGenerator extends RepairGenerator {

	private static Logger logger = LogManager.getLogger(IQRepairGenerator.class);

	private Queue<OWLIndividual> queueOfIndividuals;
	
		
	public IQRepairGenerator(OWLOntology inputOntology) {
		
		super(inputOntology);
	}
	
	
	@Override
	protected void initialise() {
		super.initialise();
		
		anonymousDetector = AnonymousVariableDetector.newInstance(true, RepairManagerBuilder.RepairVariant.IQ);
		
		setOfCollectedIndividuals = inputObjectNames.stream()
//									.filter(ind -> anonymousDetector.isNamed(ind))
									.filter(ind -> ind.isOWLNamedIndividual())
									.collect(Collectors.toSet());
	}

	/* The below code is a bit difficult to understand, but I believe that the current version now does the job
	*  correctly. */
	protected void generateVariables() {
		queueOfIndividuals = new PriorityQueue<>(setOfCollectedIndividuals);

		while(!queueOfIndividuals.isEmpty()) {
			OWLIndividual individual = queueOfIndividuals.poll();
			
			OWLIndividual originalIndividual = inputObjectNames.contains(individual) ?
					individual : copyToObject.get(individual);
			
			Set<OWLObjectPropertyAssertionAxiom> setOfRoleAssertions = ontology
					.getObjectPropertyAssertionAxioms(originalIndividual);
			
			for(OWLObjectPropertyAssertionAxiom roleAssertion : setOfRoleAssertions) {
				
				OWLIndividual originalObject = roleAssertion.getObject();
				RepairType type = objectToRepairType.get(individual);
				
				Set<OWLClassExpression> successorSet = computeSuccessorSet(
						type,(OWLObjectProperty) roleAssertion.getProperty(),originalObject);
			
				RepairType emptyType = typeHandler.newMinimisedRepairType(new HashSet<>());
				Set<RepairType> setOfRepairTypes = typeHandler.findCoveringRepairTypes(emptyType, successorSet, originalObject);
				
				for(RepairType newType : setOfRepairTypes) {
					if(!objectToCopies.get(originalObject).stream().anyMatch(copy -> newType.equals(objectToRepairType.get(copy)))) {
						OWLAnonymousIndividual copy = createCopy(originalObject, newType);
						queueOfIndividuals.add(copy);
					}
				}
			}
			
		}
	}
}
