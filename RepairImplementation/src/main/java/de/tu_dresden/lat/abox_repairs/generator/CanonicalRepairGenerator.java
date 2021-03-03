package de.tu_dresden.lat.abox_repairs.generator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;

import com.google.common.collect.Sets; 

import de.tu_dresden.lat.abox_repairs.repair_type.RepairType;

/**
 * Careful: this does not compute the canonical repair, but only the variables that would be needed by it.
 */
public class CanonicalRepairGenerator extends RepairGenerator {
	
	private static Logger logger = LogManager.getLogger(CanonicalRepairGenerator.class);
	
	public CanonicalRepairGenerator(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		//super(inputOntology, inputSeedFunction);
		super(inputOntology, Collections.emptyMap());

		setOfCollectedIndividuals = new HashSet<>();
	}
	

	@Override
	protected void generateVariables() {
		
		for(OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
			Set<OWLClassExpression> setOfAtoms = new HashSet<>();
			for(OWLClassExpression assertedConcept : reasonerWithTBox.instanceOf(individual)) {
				if(atomForRepairType(assertedConcept)) {
					setOfAtoms.add(assertedConcept);
				}
			}
			Set<Set<OWLClassExpression>> powerSet = Sets.powerSet(setOfAtoms);
			for(Set<OWLClassExpression> repairTypeCandidate: powerSet) {
				if(typeHandler.isPremiseSaturated(repairTypeCandidate, individual)) {
					RepairType type = typeHandler.newMinimisedRepairType(repairTypeCandidate);
					createCopy(individual, type);
				}
			}
		}
	}

	private boolean atomForRepairType(OWLClassExpression exp){
		/* As you want to test whether 'exp' is an atom, you must replace the condition
		*  !reasonerWithTBox.equivalentToOWLThing(exp)
		*  with !reasonerWithoutTBox.equivalentToOWLThing(exp) */
		return (exp instanceof OWLClass || exp instanceof OWLObjectSomeValuesFrom)
				&& !reasonerWithoutTBox.equivalentToOWLThing(exp);
	}

	


	@Override
	protected void initialise() {
		
	}
}
