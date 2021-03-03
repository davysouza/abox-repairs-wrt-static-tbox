package de.tu_dresden.lat.abox_repairs.generator;

import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import de.tu_dresden.lat.abox_repairs.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import de.tu_dresden.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;

public class IQRepairGenerator extends RepairGenerator {

	private static Logger logger = LogManager.getLogger(IQRepairGenerator.class);

	private Queue<OWLNamedIndividual> queueOfIndividuals;
	
		
	public IQRepairGenerator(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		
		super(inputOntology, inputSeedFunction);
	}
	
	
	@Override
	protected void initialise() {
		
		anonymousDetector = AnonymousVariableDetector.newInstance(true, Main.RepairVariant.IQ);
		
		setOfCollectedIndividuals = inputObjectNames.stream()
									.filter(ind -> anonymousDetector.isNamed(ind))
									.collect(Collectors.toSet());
	}

	/* The below code is a bit difficult to understand, but I believe that the current version now does the job
	*  correctly. */
	protected void generateVariables() {
		
		
		queueOfIndividuals = new PriorityQueue<>(setOfCollectedIndividuals);

		while(!queueOfIndividuals.isEmpty()) {
			OWLNamedIndividual individual = queueOfIndividuals.poll();
			
			OWLNamedIndividual originalIndividual = inputObjectNames.contains(individual) ?
					individual : copyToObject.get(individual);
			
			Set<OWLObjectPropertyAssertionAxiom> setOfRoleAssertions = ontology
					.getObjectPropertyAssertionAxioms(originalIndividual);
			
			for(OWLObjectPropertyAssertionAxiom roleAssertion : setOfRoleAssertions) {
				
				OWLNamedIndividual originalObject = (OWLNamedIndividual) roleAssertion.getObject();
				RepairType type = objectToRepairType.get(individual);
				
				Set<OWLClassExpression> successorSet = computeSuccessorSet(
						type,(OWLObjectProperty) roleAssertion.getProperty(),originalObject);
			
				RepairType emptyType = typeHandler.newMinimisedRepairType(new HashSet<>());
				Set<RepairType> setOfRepairTypes = typeHandler.findCoveringRepairTypes(emptyType, successorSet, originalObject);
				
				for(RepairType newType : setOfRepairTypes) {
					if(!objectToCopies.get(originalObject).stream().anyMatch(copy -> newType.equals(objectToRepairType.get(copy)))) {
						OWLNamedIndividual copy = createCopy(originalObject, newType);
						queueOfIndividuals.add(copy);
					}
				}
			}
			
		}
		
	}



}
