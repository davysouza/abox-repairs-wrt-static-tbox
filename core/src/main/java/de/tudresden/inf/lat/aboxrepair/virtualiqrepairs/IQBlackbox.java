package de.tudresden.inf.lat.aboxrepair.virtualiqrepairs;

import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.Collection;

public interface IQBlackbox {
    boolean isEntailed(OWLClassAssertionAxiom ce);
    Collection<OWLNamedIndividual> query(OWLClassExpression ce);
}
