package de.tu_dresden.lat.abox_repairs.saturation;

import org.semanticweb.owlapi.model.OWLOntology;

public class DummySaturator implements ABoxSaturator {
    @Override
    public void saturate(OWLOntology ontology) throws SaturationException {
        // does nothing
    }

    @Override
    public int addedAssertions() {
        return 0;
    }

    @Override
    public int addedIndividuals() {
        return 0;
    }

    @Override
    public double getDuration() {
        return 0;
    }
}
