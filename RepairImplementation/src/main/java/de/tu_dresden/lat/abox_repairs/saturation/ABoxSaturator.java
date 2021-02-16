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

	/**
	 * Returns the number of assertions that were added during the last call of saturate()
	 * @return
	 */
	int addedAssertions();

	/**
	 * Returns the number of individuals that were added during the last call of saturate()
	 */
	int addedIndividuals();

	/**
	 * Returns the number of seconds used for the last saturation.
	 * @return
	 */
	double getDuration();
}
