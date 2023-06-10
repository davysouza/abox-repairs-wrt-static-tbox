package de.tudresden.inf.lat.aboxrepair.ontologytools;

import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.util.Optional;

public class Prover {

    /**
     * Gets the justification of a consequence based on an {@link ElkReasoner}. It goes
     * through all {@link OWLOntology}, removing those axioms that are not needed to entail
     * the given {@link OWLAxiom}. The remaining set is the justification.
     *
     * @param ontology An {@link OWLOntology} object
     * @param consequence The {@link OWLAxiom} that we want to get the justification
     * @return Returns an {@link OWLOntology} object if the consequence has a justification at
     * the given ontology or empty if there is no justification.
     * @throws OWLOntologyCreationException
     */
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
