package de.tudresden.inf.lat.aboxrepair.generator;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import java.util.Objects;

/**
 * @author Francesco Kriegel
 */
public class CopiedOWLObjectPropertyAssertionAxiom {
    private final CopiedOWLIndividual subject, object;
    private final OWLObjectPropertyExpression property;

    CopiedOWLObjectPropertyAssertionAxiom (CopiedOWLIndividual subject,
                                           OWLObjectPropertyExpression property,
                                           CopiedOWLIndividual object) {
        this.subject = subject;
        this.property = property;
        this.object = object;
    }

    CopiedOWLIndividual getSubject() {
        return subject;
    }

    OWLObjectPropertyExpression getProperty() {
        return property;
    }

    CopiedOWLIndividual getObject() {
        return object;
    }

    OWLObjectPropertyAssertionAxiom toAxiomInTheRepair() {
        return OWLManager.getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(
                property, subject.getIndividualInTheRepair(), object.getIndividualInTheRepair());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        CopiedOWLObjectPropertyAssertionAxiom that = (CopiedOWLObjectPropertyAssertionAxiom) o;
        return subject.equals(that.subject) && object.equals(that.object) && property.equals(that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, object, property);
    }
}
