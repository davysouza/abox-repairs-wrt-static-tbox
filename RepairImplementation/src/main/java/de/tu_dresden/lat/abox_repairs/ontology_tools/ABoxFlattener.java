package de.tu_dresden.lat.abox_repairs.ontology_tools;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.HashSet;
import java.util.Set;

/**
 * Class to flatten ABoxes, as this is the assumed normal form in the CADE-21 paper.
 *
 * @author koopmann
 */
public class ABoxFlattener {

    private final OWLDataFactory factory;
    private final FreshNameProducer freshNameProducer;

    public ABoxFlattener(OWLDataFactory factory) {
        this.factory=factory;
        this.freshNameProducer = new FreshNameProducer(factory);
    }

    /**
     * Flattens the given ontology.
     *
     * Careful: changes the content of the ontology, rather than producing a new ontology.
     */
    public void flatten(OWLOntology ontology) {

        freshNameProducer.addKnownExpressions(ontology.getClassesInSignature());

        Set<OWLLogicalAxiom> toAdd = new HashSet<>();
        Set<OWLClassAssertionAxiom> toRemove = new HashSet<>();

        ontology.aboxAxioms(Imports.INCLUDED).forEach(axiom -> {
            if(axiom instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom assertion = (OWLClassAssertionAxiom) axiom;
                if (!(assertion.getClassExpression() instanceof OWLClass)) {
                    toRemove.add(assertion);
                    OWLClassExpression expression = assertion.getClassExpression();
                    OWLClass name = freshNameProducer.freshName();
                    toAdd.add(factory.getOWLClassAssertionAxiom(name, assertion.getIndividual()));
                    toAdd.add(factory.getOWLSubClassOfAxiom(name, expression));
                }
            }
        });

        ontology.addAxioms(toAdd);
        ontology.removeAxioms(toRemove);
    }

}
