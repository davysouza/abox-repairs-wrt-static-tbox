package de.tu_dresden.lat.abox_repairs.generator;

import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.ontology_tools.ELRestrictor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;

public class IQRepairGenerator extends RepairGenerator {

	private static Logger logger = LogManager.getLogger(IQRepairGenerator.class);

	private Queue<OWLNamedIndividual> queueOfIndividuals;
	
		
	public IQRepairGenerator(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		
		super(inputOntology, inputSeedFunction);
	}
	
	
	
	protected void initialisation() {
		
		anonymousDetector = AnonymousVariableDetector.newInstance(true, Main.RepairVariant.IQ);
		
		setOfCollectedIndividuals = setOfSaturatedIndividuals.stream()
									.filter(ind -> anonymousDetector.isNamed(ind))
									.collect(Collectors.toSet());
		
		 
	}
	
	protected void generatingVariables() {
		
		
		queueOfIndividuals = new PriorityQueue<>(setOfCollectedIndividuals);

		while(!queueOfIndividuals.isEmpty()) {
			OWLNamedIndividual individual = queueOfIndividuals.poll();
			
			OWLNamedIndividual originalIndividual = setOfSaturatedIndividuals.contains(individual) ?
					individual : copyToObject.get(individual);
			
			Set<OWLObjectPropertyAssertionAxiom> setOfRoleAssertions = ontology
					.getObjectPropertyAssertionAxioms(originalIndividual);
			
			for(OWLObjectPropertyAssertionAxiom roleAssertion : setOfRoleAssertions) {
				
				OWLNamedIndividual originalObject = (OWLNamedIndividual) roleAssertion.getObject();
				RepairType type = seedFunction.get(individual);
				
				Set<OWLClassExpression> successorSet = computeSuccessorSet(
						type,(OWLObjectProperty) roleAssertion.getProperty(),originalObject);
			
				RepairType emptyType = typeHandler.newMinimisedRepairType(new HashSet<>());
				Set<RepairType> setOfRepairTypes = typeHandler.findCoveringRepairTypes(emptyType, successorSet, originalObject);
				
				for(RepairType newType : setOfRepairTypes) {
					if(!objectToCopies.get(originalObject).stream().anyMatch(copy -> newType.equals(seedFunction.get(copy)))) {
						OWLNamedIndividual copy = createCopy(originalObject, newType);
						queueOfIndividuals.add(copy);
					}
				}
			}
			
		}
		
	}



}
