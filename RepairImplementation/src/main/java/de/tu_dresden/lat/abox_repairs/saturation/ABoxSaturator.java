package de.tu_dresden.lat.abox_repairs.saturation;

import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author Patrick Koopmann
 */
public interface ABoxSaturator {

	/**
	 * Saturates ontology by adding axioms.
	 * @throws SaturationException
	 */
	void saturate(OWLOntology ontology) throws SaturationException;
}
