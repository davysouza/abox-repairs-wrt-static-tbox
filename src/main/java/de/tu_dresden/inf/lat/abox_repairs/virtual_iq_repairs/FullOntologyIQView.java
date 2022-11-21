package de.tu_dresden.inf.lat.abox_repairs.virtual_iq_repairs;

import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Collection;
import java.util.stream.Collectors;

public class FullOntologyIQView implements IQBlackbox {

    private final OWLOntology ontology;

    private final OWLDataFactory factory;
    private final OWLReasoner reasoner;

    public FullOntologyIQView(OWLOntology ontology) {
        this.ontology=ontology;
        this.factory=ontology.getOWLOntologyManager().getOWLDataFactory();
        this.reasoner = new ElkReasonerFactory().createReasoner(ontology);
    }



    @Override
    public boolean isEntailed(OWLClassAssertionAxiom ce) {
        return reasoner.isEntailed(ce);
    }

    @Override
    public Collection<OWLNamedIndividual> query(OWLClassExpression ce) {
        return reasoner.getInstances(ce)
                .entities()
                .collect(Collectors.toSet());
    }
}
