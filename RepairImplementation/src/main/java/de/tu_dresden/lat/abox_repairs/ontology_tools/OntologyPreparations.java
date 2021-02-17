package de.tu_dresden.lat.abox_repairs.ontology_tools;

import de.tu_dresden.lat.abox_repairs.repair_types.RepairTypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Prepare ontology to be adhere to preconditions in the CADE-21 paper:
 *
 * 1. TBox is pure EL, 2. ABox is flat
 */
public class OntologyPreparations {

    private static Logger logger = LogManager.getLogger(OntologyPreparations.class);

    public static void prepare(OWLOntology ontology) {
        OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();

        new ELRestrictor(factory).restrict(ontology);
        new ABoxFlattener(factory).flatten(ontology);

        // restrict signature to used names
        // this way, we avoid names that are not used in any axiom to occur in a repair type or somewhere else
        ontology.removeAxioms(ontology.getAxioms(AxiomType.DECLARATION));
    }
}