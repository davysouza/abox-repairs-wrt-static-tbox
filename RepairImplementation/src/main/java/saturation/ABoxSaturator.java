package saturation;

import org.semanticweb.owlapi.model.OWLOntology;

public interface ABoxSaturator {

	/**
	 * Saturates ontology by adding axioms.
	 * @throws SaturationException
	 */
	void saturate(OWLOntology ontology) throws SaturationException;
}
