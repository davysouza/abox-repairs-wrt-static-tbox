import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

public class OntologyAdaptation {
	
	OWLOntology ontology;
	Map<OWLNamedIndividual, Set<OWLClassExpression>> seedFunction;
	Map<OWLNamedIndividual, Integer> individualCounter;
	
	
	public OntologyAdaptation(OWLOntology inputOntology, Map<OWLNamedIndividual, Set<OWLClassExpression>> inputSeedFunction) {
		
		ontology = inputOntology;
		individualCounter = new HashMap<OWLNamedIndividual, Integer>();
		Set<OWLNamedIndividual> setOfIndividuals = ontology.getIndividualsInSignature();
		for(OWLNamedIndividual individual: setOfIndividuals) {
			individualCounter.put(individual, 0);
		}
		
		
	}
	
	public OWLOntology getOntology() {
		return ontology;
	}
	
	
}
