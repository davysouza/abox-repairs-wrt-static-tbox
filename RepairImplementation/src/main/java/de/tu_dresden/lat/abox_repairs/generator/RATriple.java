package de.tu_dresden.lat.abox_repairs.generator;

import org.semanticweb.owlapi.model.*;

@Deprecated
public class RATriple {
    private final OWLNamedIndividual individual1;
    private final OWLNamedIndividual individual2;
    private final OWLObjectProperty property;

    public RATriple(OWLNamedIndividual individual1, OWLNamedIndividual individual2, OWLObjectProperty property){
        this.individual1=individual1;
        this.individual2=individual2;
        this.property=property;
    }

    public static RATriple fromAxiom(OWLObjectPropertyAssertionAxiom ax) {
        // TODO unsafe
        return new RATriple(
                (OWLNamedIndividual)ax.getSubject(),
                (OWLNamedIndividual) ax.getObject(),
                (OWLObjectProperty)ax.getProperty());
    }

    public OWLNamedIndividual getIndividual1() {
        return individual1;
    }
    public OWLNamedIndividual getIndividual2() {
        return individual2;
    }

    public OWLObjectProperty getProperty() {
        return property;
    }
}
