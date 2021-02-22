package de.tu_dresden.lat.abox_repairs.ontology_tools;

import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.util.Optional;

public class Prover {

    private Prover() {
    }

    public static Optional<OWLOntology> getJustification(OWLOntology ontology, OWLAxiom consequence) throws OWLOntologyCreationException {
        final OWLOntology justification = ontology.getOWLOntologyManager().createOntology(ontology.getAxioms());
        final ElkReasoner justificationReasoner = new ElkReasonerFactory().createReasoner(justification);
        if (!justificationReasoner.isEntailed(consequence)) {
            justificationReasoner.dispose();
            return Optional.empty();
        }
        for (OWLAxiom axiom : ontology.getAxioms()) {
            justification.remove(axiom);
            justificationReasoner.flush();
            if (!justificationReasoner.isEntailed(consequence))
                justification.add(axiom);
        }
        justificationReasoner.dispose();
        return Optional.of(justification);
    }

}
