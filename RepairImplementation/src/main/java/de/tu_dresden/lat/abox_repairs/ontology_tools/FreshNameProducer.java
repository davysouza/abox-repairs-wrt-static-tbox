package de.tu_dresden.lat.abox_repairs.ontology_tools;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Producing fresh owl classes.
 *
 * @author koopmann
 */
public class FreshNameProducer {

    private static final String PREFIX = "Fresh_Class_";
    private final OWLDataFactory factory;
    private final Set<OWLClass> knownClasses;

    private int freshNameCounter = 0;


    public static FreshNameProducer
            newFromClassExpressions(OWLDataFactory factory, Set<OWLClassExpression> knownExpressions) {
        Set<OWLClass> knownClasses= knownExpressions
                        .stream()
                        .filter(c -> c instanceof  OWLClass)
                        .map(c -> (OWLClass) c)
                        .collect(Collectors.toSet());

        return new FreshNameProducer(factory,knownClasses);
    }

    public FreshNameProducer(OWLDataFactory factory) {
        this(factory, new HashSet<>());
    }

    public FreshNameProducer(OWLDataFactory factory, Set<OWLClass> knownClasses) {
        this.factory=factory;
        this.knownClasses=knownClasses;
    }

    public void addKnownExpressions(Set<? extends OWLClassExpression> expressions) {
        expressions.stream().forEach(c -> {
            if(c instanceof OWLClass) knownClasses.add((OWLClass) c);
        });
    }

    public OWLClass freshName() {
        OWLClass name = factory.getOWLClass(IRI.create(PREFIX + freshNameCounter));

        freshNameCounter++;

        while(knownClasses.contains(name)) {
            name = factory.getOWLClass(IRI.create(PREFIX + freshNameCounter));
            freshNameCounter++;
        }

        return name;
    }

    public static boolean isFreshOWLClass(OWLClassExpression owlClassExpression) {
        return !owlClassExpression.isOWLThing()
                && !owlClassExpression.isOWLNothing()
                && owlClassExpression instanceof OWLClass
                && isFreshOWLClassName(owlClassExpression.asOWLClass().getIRI().toString());
    }

    public static boolean isFreshOWLClassName(String name) {
        return name.startsWith(PREFIX);
    }

}
