package de.tu_dresden.lat.abox_repairs.generator;

import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;


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

public class IQRepairGenerator extends RepairGenerator {

	private static Logger logger = LogManager.getLogger(IQRepairGenerator.class);

	private Queue<OWLNamedIndividual> queueOfIndividuals;
	
	public IQRepairGenerator(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		
		super(inputOntology, inputSeedFunction);
	}
	
	
	public void repair() throws OWLOntologyCreationException {
		
		
		long startTimeVariables = System.nanoTime();
		
		generatingVariables();
		
		double timeVariables = (double)(System.nanoTime() - startTimeVariables)/1_000_000_000;
		
		logger.info("Time for generating variables: " + timeVariables);
		
		logger.debug("After generating necessary variables");
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
		
		logger.debug("\nAfter building the matrix");
		newOntology.axioms().forEach(ax -> logger.debug("- " + ax.toString()));
	
	}
	
	
	protected void generatingVariables() {
		queueOfIndividuals = new PriorityQueue<>(setOfCollectedIndividuals);

		while(!queueOfIndividuals.isEmpty()) {
			OWLNamedIndividual individual = queueOfIndividuals.poll();
			
			OWLNamedIndividual originalIndividual = setOfOriginalIndividuals.contains(individual) ?
					individual : copyToOriginal.get(individual);
			
			Set<OWLObjectPropertyAssertionAxiom> setOfRoleAssertions = ontology
					.getObjectPropertyAssertionAxioms(originalIndividual);
			
			for(OWLObjectPropertyAssertionAxiom roleAssertion : setOfRoleAssertions) {
				
				OWLNamedIndividual originalObject = (OWLNamedIndividual) roleAssertion.getObject();
				RepairType type = seedFunction.get(individual);
				if(type != null) {
					Set<OWLClassExpression> successorSet = computeSuccessorSet(
							type,(OWLObjectProperty) roleAssertion.getProperty(), 
							originalObject);
			
					RepairType emptyType = typeHandler.newMinimisedRepairType(new HashSet<>());
					if(successorSet.isEmpty()) {
						boolean individualAlreadyExists = false;
						for(OWLNamedIndividual copy : originalToCopy.get(originalObject)) {
							if (seedFunction.get(copy) == null || 
								(seedFunction.get(copy) != null && seedFunction.get(copy).equals(emptyType))) {
									individualAlreadyExists = true;
									break;
							}
						}
						
						if(!individualAlreadyExists) {
							makeCopy(originalObject, emptyType);
						}
					}
					else {
						Set<RepairType> setOfRepairTypes = typeHandler.findCoveringRepairTypes(emptyType, successorSet);
						if(!setOfRepairTypes.isEmpty()) {
							for(RepairType newType : setOfRepairTypes) {
								boolean individualAlreadyExists = false;
								for(OWLNamedIndividual copy : originalToCopy.get(originalObject)) {
									if(seedFunction.get(copy) != null) {
										if(seedFunction.get(copy).equals(newType)) {
											individualAlreadyExists = true;
											break;
										}
									}
								}
								if(!individualAlreadyExists) {
									makeCopy(originalObject, newType);
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
		
		queueOfIndividuals.add(freshIndividual);
		
		setOfCollectedIndividuals.add(freshIndividual);
	}

}
