package de.tu_dresden.lat.abox_repairs.generator;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import java.util.Objects;

public final class CopiedOWLObjectPropertyAssertionAxiom {

    private final CopiedOWLIndividual subject, object;
    private final OWLObjectPropertyExpression property;

    public CopiedOWLObjectPropertyAssertionAxiom(
            CopiedOWLIndividual subject,
            OWLObjectPropertyExpression property,
            CopiedOWLIndividual object
    ) {
        this.subject = subject;
        this.property = property;
        this.object = object;
    }

    public CopiedOWLIndividual getSubject() {
        return subject;
    }

    public OWLObjectPropertyExpression getProperty() {
        return property;
    }

    public CopiedOWLIndividual getObject() {
        return object;
    }

    public OWLObjectPropertyAssertionAxiom toAxiomInTheRepair() {
        return OWLManager.getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(
                property, subject.getIndividualInTheRepair(), object.getIndividualInTheRepair());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CopiedOWLObjectPropertyAssertionAxiom that = (CopiedOWLObjectPropertyAssertionAxiom) o;
        return subject.equals(that.subject) && object.equals(that.object) && property.equals(that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, object, property);
    }

}
