package de.tu_dresden.lat.abox_repairs.generator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import com.google.common.collect.Sets; 

import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;

public class CanonicalRepairGenerator extends RepairGenerator {
	
	private static Logger logger = LogManager.getLogger(CanonicalRepairGenerator.class);
	
	public CanonicalRepairGenerator(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		
		super(inputOntology, inputSeedFunction);
	}
	
	@Override
	public void repair() throws OWLOntologyCreationException {
		// TODO Auto-generated method stub
		
		
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
	}

	@Override
	protected void generatingVariables() {
		// TODO Auto-generated method stub
		
		for(OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
			Set<OWLClassExpression> setOfAtoms = new HashSet<>();
			for(OWLClassExpression assertedConcept : reasonerWithTBox.instanceOf(individual)) {
				if(assertedConcept instanceof OWLClass || assertedConcept instanceof OWLObjectSomeValuesFrom) {
					setOfAtoms.add(assertedConcept);
				}
			}
			Set<Set<OWLClassExpression>> powerSet = Sets.powerSet(setOfAtoms);
			for(Set<OWLClassExpression> repairTypeCandidate: powerSet) {
				if(typeHandler.isPremiseSaturated(repairTypeCandidate, individual)) {
					RepairType type = typeHandler.newMinimisedRepairType(repairTypeCandidate);
					makeCopy(individual, type);
				}
			}
		}
	}

	

	@Override
	protected void makeCopy(OWLNamedIndividual ind, RepairType typ) {
		// TODO Auto-generated method stub
		
		individualCounter.put(ind, individualCounter.get(ind) + 1);
		OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(
				 ind.getIRI().getFragment() + 
				individualCounter.get(ind));
		seedFunction.put(freshIndividual, typ);

		
		setOfCollectedIndividuals.add(freshIndividual);
		
	}

	@Override
	protected void initialisation() {
		// TODO Auto-generated method stub
		
	}
}
