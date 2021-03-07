package de.tu_dresden.inf.lat.abox_repairs.ontology_tools;

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
//    private final FreshOWLEntityFactory freshNameProducer;

    public ABoxFlattener(OWLDataFactory factory) {
        this.factory = factory;
//        this.freshNameProducer = new FreshOWLEntityFactory(factory);
    }

    /**
     * Flattens the given ontology.
     * <p>
     * Careful: changes the content of the ontology, rather than producing a new ontology.
     */
    public void flatten(OWLOntology ontology) {
        final FreshOWLEntityFactory.FreshOWLClassFactory freshNameProducer = FreshOWLEntityFactory.FreshOWLClassFactory.of(ontology);
//        freshNameProducer.addKnownExpressions(ontology.getClassesInSignature());

        Set<OWLLogicalAxiom> toAdd = new HashSet<>();
        Set<OWLClassAssertionAxiom> toRemove = new HashSet<>();

//        Map<OWLClassExpression,OWLClass> knownClasses = new HashMap<>();

        ontology.aboxAxioms(Imports.INCLUDED).forEach(axiom -> {
            if (axiom instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom assertion = (OWLClassAssertionAxiom) axiom;
//                if (!(assertion.getClassExpression() instanceof OWLClass)) {
                if (!(assertion.getClassExpression().isOWLClass())) {
                    toRemove.add(assertion);
                    OWLClassExpression expression = assertion.getClassExpression();

//                    OWLClass name;
//                    if(knownClasses.containsKey(expression))
//                        name = knownClasses.get(expression);
//                    else {
//                        name = freshNameProducer.getEntity();
//                        knownClasses.put(expression,name);
//                    }
                    System.out.println("Complex assertion: " + assertion);
                    OWLClass name = freshNameProducer.getEntity(expression);
                    toAdd.add(factory.getOWLClassAssertionAxiom(name, assertion.getIndividual()));
                    //toAdd.add(factory.getOWLSubClassOfAxiom(name, expression));
                    toAdd.add(factory.getOWLEquivalentClassesAxiom(name, expression)); // produces names more useful for repairs
                }
            }
        });

        ontology.addAxioms(toAdd);
        ontology.removeAxioms(toRemove);
    }

}
