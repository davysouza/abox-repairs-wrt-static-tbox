package de.tu_dresden.lat.abox_repairs;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

public class OntologyAdaptation {
	
	private final OWLOntology ontology;
	private final Map<OWLNamedIndividual, Set<OWLClassExpression>> seedFunction;
	private final Map<OWLNamedIndividual, Integer> individualCounter;
	
	
	public OntologyAdaptation(OWLOntology inputOntology, Map<OWLNamedIndividual, Set<OWLClassExpression>> inputSeedFunction) {
		
		this.ontology = inputOntology;
		this.seedFunction=inputSeedFunction;
		this.individualCounter = new HashMap<>();
		Set<OWLNamedIndividual> setOfIndividuals = ontology.getIndividualsInSignature();
		for(OWLNamedIndividual individual: setOfIndividuals) {
			individualCounter.put(individual, 0);
		}
		
		
	}
	
	public OWLOntology getOntology() {
		return ontology;
	}
	
	
}
