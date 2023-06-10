package de.tudresden.inf.lat.aboxrepair.virtualiqrepairs;

import de.tudresden.inf.lat.aboxrepair.repairtype.RepairType;
import de.tudresden.inf.lat.aboxrepair.seedfunction.SeedFunction;
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
        this.ontology = ontology;
        this.seedFunction = seedFunction;
        this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();
        this.reasoner = new ElkReasonerFactory().createReasoner(ontology);
    }

    public boolean isEntailed(OWLClassAssertionAxiom axiom) {
        return reasoner.isEntailed(axiom) && canReveal(axiom);
    }

    @Override
    public Collection<OWLNamedIndividual> query(OWLClassExpression ce) {
        return reasoner.getInstances(ce)
                .entities()
                .filter(individual -> canReveal(individual, ce))
                .collect(Collectors.toSet());
    }

    private boolean canReveal(OWLClassAssertionAxiom axiom) {
        OWLClassExpression clazz = axiom.getClassExpression();
        OWLIndividual individual = axiom.getIndividual();
        return canReveal(individual, clazz);
    }

    private boolean canReveal(OWLIndividual individual, OWLClassExpression clazz) {
        if(!seedFunction.containsKey(individual))
            return true;

        RepairType rt = seedFunction.get(individual);
        return !rt.getClassExpressions()
                .stream()
                .anyMatch(ce -> reasoner.isEntailed(factory.getOWLSubClassOfAxiom(clazz, ce)));
    }
}
