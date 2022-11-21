package de.tu_dresden.inf.lat.abox_repairs.virtual_iq_repairs;

import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Collection;
import java.util.stream.Collectors;

public class VirtualIQRepair implements IQBlackbox {
    private final OWLOntology ontology;
    private final SeedFunction seedFunction;

    private final OWLDataFactory factory;
    private final OWLReasoner reasoner;

    public VirtualIQRepair(OWLOntology ontology, SeedFunction seedFunction) {
        this.ontology=ontology;
        this.seedFunction=seedFunction;
        this.factory=ontology.getOWLOntologyManager().getOWLDataFactory();
        this.reasoner = new ElkReasonerFactory().createReasoner(ontology);
    }

    public boolean isEntailed(OWLClassAssertionAxiom axiom) {
        return reasoner.isEntailed(axiom) && canReveal(axiom);
    }

    @Override
    public Collection<OWLNamedIndividual> query(OWLClassExpression ce) {
        return reasoner.getInstances(ce)
                .entities()
                .filter(ind -> canReveal(ind, ce))
                .collect(Collectors.toSet());
    }

    private boolean canReveal(OWLClassAssertionAxiom axiom) {
        OWLClassExpression clazz = axiom.getClassExpression();
        OWLIndividual ind = axiom.getIndividual();
        return canReveal(ind, clazz);
    }

    private boolean canReveal(OWLIndividual ind, OWLClassExpression clazz) {
        if(!seedFunction.containsKey(ind))
            return true;
        else {
            RepairType rt = seedFunction.get(ind);
            return !rt.getClassExpressions()
                    .stream()
                    .anyMatch(ce ->
                            reasoner.isEntailed(
                                    factory.getOWLSubClassOfAxiom(clazz, ce)));
        }
    }
}
