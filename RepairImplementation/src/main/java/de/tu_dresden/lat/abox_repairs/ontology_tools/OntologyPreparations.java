package de.tu_dresden.lat.abox_repairs.ontology_tools;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Prepare ontology to be adhere to preconditions in the CADE-21 paper:
 *
 * 1. TBox is pure EL, 2. ABox is flat
 */
public class OntologyPreparations {

    public static void prepare(OWLOntology ontology) {
        OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();

        new ELRestrictor(factory).restrict(ontology);
        new ABoxFlattener(factory).flatten(ontology);
    }
}