package de.tu_dresden.lat.abox_repairs.reasoning;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.tu_dresden.lat.abox_repairs.ontology_tools.FreshNameProducer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Facade to encapsulate common reasoning tasks.
 * <p>
 * Mainly used to determine subsumption relationships between a given set of
 * class expressions. Usually, we use the class expressions occurring in the
 * ontology for this.
 *
 * @author Patrick Koopmann
 */
@Deprecated
public class ReasonerFacadeF {

    private static Logger logger = LogManager.getLogger(ReasonerFacadeF.class);

    private final BiMap<OWLClassExpression, OWLClass> expression2Name;

    private final OWLOntology ontology;
    private final Set<OWLAxiom> addedAxioms;
    private final OWLDataFactory factory;

    private final OWLReasoner reasoner;

    private final FreshNameProducer freshNameProducer;

    private final Set<OWLClassExpression> expressions; // TODO only for testing - remove afterwards!

    //private Timer timer;

    private ReasonerFacadeF(OWLOntology ontology, Set<OWLClassExpression> expressions) {
        expression2Name = HashBiMap.create();
        addedAxioms = new HashSet<>();
        this.ontology = ontology;
        this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();

        freshNameProducer = FreshNameProducer.newFromClassExpressions(factory, expressions);
        addExpressions(expressions);

        this.expressions = expressions;

        reasoner = new ElkReasonerFactory().createReasoner(ontology);

        long start = System.nanoTime();
        reasoner.precomputeInferences();
        reasoner.flush();
        logger.info("classification took " + (((double) System.nanoTime() - start) / 1_000_000_000));
    }

    /**
     * Uses no TBox for reasoning, however, supports the expressions found in the ontology (subsumption relations without TBox).
     *
     * @param ontology
     * @return
     * @throws OWLOntologyCreationException
     */
    public static ReasonerFacadeF newReasonerFacadeWithoutTBox(OWLOntology ontology)
            throws OWLOntologyCreationException {
        return newReasonerFacadeWithoutTBox(ontology, Collections.emptyList());
    }

    /**
     * Uses no TBox, and only supports the expressions provided. Hence, the manager is required to create the ontology used internally.
     */
    public static ReasonerFacadeF newReasonerFacadeWithoutTBox(
            Collection<OWLClassExpression> expressions, OWLOntologyManager manager) throws OWLOntologyCreationException {
        OWLOntology emptyTBox = manager.createOntology();

        return newReasonerFacadeWithTBox(emptyTBox, expressions);
    }

    /**
     * Uses no TBox for inferences, but supports all names found in the provided in the ontology, together with the
     * provided ones.
     *
     * @param ontology
     * @param additionalExpressions
     * @return
     * @throws OWLOntologyCreationException
     */
    public static ReasonerFacadeF newReasonerFacadeWithoutTBox(OWLOntology ontology,
                                                               Collection<OWLClassExpression> additionalExpressions) throws OWLOntologyCreationException {

        Set<OWLClassExpression> expressions = ontology.getNestedClassExpressions();
        expressions.addAll(additionalExpressions);

        OWLOntology emptyTBox = ontology.getOWLOntologyManager().createOntology();

        return newReasonerFacadeWithTBox(emptyTBox, expressions);
    }

    /**
     * Uses TBox for inferences, and supports only those expressions found in the ontology.
     *
     * @param ontology
     * @return
     */
    public static ReasonerFacadeF newReasonerFacadeWithTBox(OWLOntology ontology) {
        return newReasonerFacadeWithTBox(ontology, Collections.emptyList());
    }

    /**
     * Uses the TBox for inferences, and supports those expressions found in the ontology, together with the ones supplied.
     *
     * @param ontology
     * @param additionalExpressions
     * @return
     */
    public static ReasonerFacadeF newReasonerFacadeWithTBox(
            OWLOntology ontology, Collection<OWLClassExpression> additionalExpressions) {
        Set<OWLClassExpression> expressions = ontology.getNestedClassExpressions();
//        System.out.println("additionalExpressions " + additionalExpressions);
        expressions.addAll(additionalExpressions);

        logger.info("used expressions: " + additionalExpressions.size());

//        System.out.println("expressions " + expressions);
        return new ReasonerFacadeF(ontology, expressions);
    }

    public void update() {
        reasoner.flush();
    }

    private void addExpressions(Set<OWLClassExpression> exps) {
        for (OWLClassExpression exp : exps) {
            OWLClass name;
            if (exp instanceof OWLClass) {
                name = (OWLClass) exp;
            } else {
                name = freshNameProducer.freshName();
                OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(exp, name);
//                System.out.println("Halo axiom " + axiom);
                ontology.addAxiom(axiom);
                addedAxioms.add(axiom);
            }

            expression2Name.put(exp, name);
        }
    }

    /**
     * Cleans up the ontology by removing all axioms that have been added by this class.
     */
    public void cleanOntology() {
        addedAxioms.forEach(ontology::remove);
    }

    public Collection<OWLClassExpression> getClassExpressions() {
        return expression2Name.keySet();
    }

    public boolean instanceOf(OWLNamedIndividual ind, OWLClassExpression exp) {
        verifyKnows(exp);

        boolean result = reasoner.getTypes(ind).containsEntity(expression2Name.get(exp));

        return result;
    }

    public boolean subsumedBy(OWLClassExpression subsumee, OWLClassExpression subsumer) {
        verifyKnows(subsumee);
        verifyKnows(subsumer);

        OWLClass subsumeeName = expression2Name.get(subsumee);
        OWLClass subsumerName = expression2Name.get(subsumer);

        boolean result = reasoner.getEquivalentClasses(subsumeeName).contains(subsumerName)
                || reasoner.getSuperClasses(subsumeeName).containsEntity(subsumerName);

        return result;
    }

    public Set<OWLClassExpression> instanceOf(OWLNamedIndividual ind) {

        Set<OWLClassExpression> result = reasoner.types(ind)
                .map(name -> expression2Name.inverse().get(name))
                .collect(Collectors.toSet());

        return result;
    }

    public Set<OWLClassExpression> equivalentClasses(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        Set<OWLClassExpression> result = reasoner.equivalentClasses(expression2Name.get(exp))
                .map(name -> expression2Name.inverse().get(name))
                .collect(Collectors.toSet());

        return result;
    }

    public boolean equivalentToOWLThing(OWLClassExpression exp) {
        verifyKnows(exp);

        return reasoner.topClassNode().anyMatch(cl -> expression2Name.get(exp).equals(cl));
    }

    public Set<OWLClassExpression> directSubsumers(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        return reasoner.superClasses(expression2Name.get(exp), true)
                .map(name -> expression2Name.inverse().get(name)).collect(Collectors.toSet());
    }

    public Set<OWLClassExpression> subsumers(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        return reasoner.superClasses(expression2Name.get(exp), false)
                .map(name -> expression2Name.inverse().get(name))
                .collect(Collectors.toSet());
    }


    public Set<OWLClassExpression> equivalentOrSubsuming(OWLClassExpression exp) throws IllegalAccessError {
        verifyKnows(exp);

        Set<OWLClassExpression> result = subsumers(exp);
        result.addAll(equivalentClasses(exp));
        return result;
    }

    public Set<OWLClassExpression> directSubsumees(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        return reasoner.subClasses(expression2Name.get(exp), true)
                .map(name -> expression2Name.inverse().get(name)).collect(Collectors.toSet());
    }

    public Set<OWLClassExpression> subsumees(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        Set<OWLClassExpression> result = reasoner.subClasses(expression2Name.get(exp), false)
                .map(name -> expression2Name.inverse().get(name))
                .collect(Collectors.toSet());

        return result;
    }


    /**
     * Return or class expressions that are equivalent to exp, or subsumed by it. That is: the set of all subsummes,
     * including the equivalent ones.
     */
    public Set<OWLClassExpression> equivalentOrSubsumedBy(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        Set<OWLClassExpression> result = subsumees(exp);
        result.addAll(equivalentClasses(exp));
        return result;
    }


    private void verifyKnows(OWLClassExpression exp) throws IllegalArgumentException {
        if (!expression2Name.containsKey(exp))
            throw new IllegalArgumentException("ClassExpression unknown: " + exp);
    }


    /**
     * Determine whether set1 is covered by set2, according to the Definition in the CADE-21 paper.
     * <p>
     * Here, "covered by" means that for every concept C in set1, we can find a concept D in set2
     * s.t. C is subsumed by D.
     *
     * @param set1
     * @param set2
     * @return
     */
    public boolean isCovered(Set<OWLClassExpression> set1, Set<OWLClassExpression> set2) {

        // cheap test first
        if (set2.containsAll(set1))
            return true;

        return set1.parallelStream()
                .allMatch(atom1 -> set2.parallelStream()
                        .anyMatch(atom2 -> subsumedBy(atom1, atom2)));
    }

    /**
     * Find a concept in set2 that is subsumed by some concept in set1
     *
     * @param set1
     * @param set2
     * @return
     */
    public Optional<OWLClassExpression> findCoveringConcept(Set<OWLClassExpression> set1, Set<OWLClassExpression> set2) {

        for (OWLClassExpression atom1 : set1) {
            for (OWLClassExpression atom2 : set2) {
                if (subsumedBy(atom1, atom2)) {
                    return Optional.of(atom2);
                }
            }
        }
        return Optional.empty();
    }


    /**
     * Checks whether exp is subsumed by some element in the set.
     */
    public boolean subsumedByAny(OWLClassExpression exp, Set<OWLClassExpression> set2) {
        if (set2.contains(exp)) // cheap test first
            return true;
        else
            return set2.stream()
                    .anyMatch(otherExp -> subsumedBy(exp, otherExp));
    }

}
