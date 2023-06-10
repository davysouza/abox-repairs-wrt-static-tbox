package de.tudresden.inf.lat.aboxrepair.saturation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import de.tudresden.inf.lat.aboxrepair.reasoner.ReasonerFacade;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Patrick Koopmann
 **/
public class CanonicalModelGenerator implements ABoxSaturator {
    private static Logger logger = LogManager.getLogger(CanonicalModelGenerator.class);
    private final ReasonerFacade reasoner;
    private Map<OWLClassExpression, OWLAnonymousIndividual> class2ind;
    private OWLDataFactory factory;
    private OWLOntology ontology;
    private int addedAssertions = 0;
    private int addedIndividuals = 0;
    private double duration = 0.0;

    public CanonicalModelGenerator(ReasonerFacade reasoner) {
        this.reasoner = reasoner;
    }

    public OWLOntology getOntology() {
        return ontology;
    }

    // region Override
    @Override
    public void saturate(OWLOntology ontology) {
        long start = System.nanoTime();

        this.ontology = ontology;
        factory = ontology.getOWLOntologyManager().getOWLDataFactory();

        int numAxiomsBeforeSaturation = ontology.getABoxAxioms(Imports.INCLUDED).size();
        int numIndividualsBeforeSaturation = ontology.getIndividualsInSignature().size() +
                                             ontology.getAnonymousIndividuals().size();

        class2ind = new HashMap<>();

        // System.out.println("Anonymous individuals: " + ontology.getAnonymousIndividuals());
        ontology.individualsInSignature().forEach(individual -> process(individual, ontology));
        ontology.anonymousIndividuals().forEach(individual -> process(individual, ontology));

        int numAxiomsAfterSaturation = ontology.getABoxAxioms(Imports.INCLUDED).size();
        int numIndividualsAfterSaturation = ontology.getIndividualsInSignature().size() + ontology.getAnonymousIndividuals().size();

        addedAssertions = numAxiomsAfterSaturation - numAxiomsBeforeSaturation;
        addedIndividuals = numIndividualsAfterSaturation - numIndividualsBeforeSaturation;

        logger.info("Saturation added " + addedAssertions + " axioms.");

        duration = TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        logger.info("Saturation took " + duration + " seconds.");
    }

    @Override
    public int addedAssertions() {
        return addedAssertions;
    }

    @Override
    public int addedIndividuals() {
        return addedIndividuals;
    }

    @Override
    public double getDuration() {
        return duration;
    }
    // endregion

    // region Private methods
    private void process(OWLIndividual individual, OWLOntology ontology) {
        // System.out.println("Processing " + individual);
        for (OWLClassExpression expression : reasoner.instanceOfExcludingOWLThing(individual)) {
            if (expression instanceof OWLClass) {
                OWLAxiom assertion = factory.getOWLClassAssertionAxiom(expression, individual);

                logger.debug("Newly added 0: " + assertion);
                if(!ontology.containsAxiom(assertion))
                    ontology.addAxiom(assertion);

            } else if (expression instanceof OWLObjectSomeValuesFrom) {
                OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) expression;

                if(!satisfied(individual, some, ontology)){
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(
                            some.getProperty(),
                            individual,
                            getIndividual(some.getFiller(), ontology));

                    logger.debug("Newly added 1: " + assertion);
                    if(!ontology.containsAxiom(assertion))
                        ontology.addAxiom(assertion);
                }
            }
        }
    }

    private boolean satisfied(OWLIndividual individual, OWLObjectSomeValuesFrom some, OWLOntology ontology) {
        OWLObjectPropertyExpression property = some.getProperty();
        OWLClassExpression filler = some.getFiller();

        // System.out.println("does " + individual + " has some " + property + "-successors with " + filler + "?");
        return ontology.objectPropertyAssertionAxioms(individual).anyMatch(assertion -> {
            // System.out.println("checking " + ass);
            // System.out.println(" " + ass.getObject() + " satisfies " + reasoner.instanceOf((OWLNamedIndividual)ass.getObject()));
            // System.out.println(" contains: " + reasoner.instanceOf((OWLNamedIndividual)ass.getObject()).contains(filler));
            return assertion.getProperty().equals(property) && (filler.isOWLThing() ||
                    reasoner.instanceOfExcludingOWLThing(assertion.getObject()).contains(filler));
        });
    }

    /**
     *  Please rather return an instance of OWLAnonymousIndividual here.
     *  A benefit is that then there is a clear distinction between named individuals (individual names in the paper)
     *  and anonymous individuals (variables in the paper), i.e., the class AnonymousVariableDetector is then not
     *  required anymore.
     *  To make these instances of OWLAnonymousIndividual accessible, the reasoner facade can add fresh instances of
     *  OWLNamedIndividual plus a corresponding instance of OWLSameIndividualAxiom---just like it was done to make the
     *  complex, anonymous instances of OWLClassExpression accessible.
     **/
    private OWLAnonymousIndividual getIndividual(OWLClassExpression expression, OWLOntology ontology) {
        // System.out.println("For " + expression);

        OWLAnonymousIndividual individual;
        if(!class2ind.containsKey(expression)){
            // IRI name = nameFor(exp);
            // individual = factory.getOWLNamedIndividual(name);
            individual = factory.getOWLAnonymousIndividual();
            class2ind.put(expression, individual);

            // System.out.println("We take fresh individual " + name);
            for(OWLClassExpression subsumer:reasoner.equivalentIncludingOWLThingOrStrictlySubsumingExcludingOWLThing(expression)){
                // System.out.println("Subsumer " + subsumer);
                if (subsumer instanceof OWLClass) {
                    OWLAxiom assertion = factory.getOWLClassAssertionAxiom(subsumer, individual);
                    // System.out.println("Newly added: " + assertion);
                    ontology.addAxiom(assertion);
                } else if (subsumer instanceof OWLObjectSomeValuesFrom) {
                    OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) subsumer;
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(
                            some.getProperty(),
                            individual,
                            getIndividual(some.getFiller(), ontology));
                    logger.debug("Newly added 2: " + assertion);
                    ontology.addAxiom(assertion);
                }
            }
        } else {
            individual = class2ind.get(expression);
        }

        return individual;
    }
    // endregion

    // region Deprecated
    @Deprecated
    int individualNameCounter = 0;

    @Deprecated
    private IRI nameFor(OWLClassExpression exp) {
        individualNameCounter++;
        return IRI.create("__i__"+individualNameCounter);
    }
    // endregion
}
