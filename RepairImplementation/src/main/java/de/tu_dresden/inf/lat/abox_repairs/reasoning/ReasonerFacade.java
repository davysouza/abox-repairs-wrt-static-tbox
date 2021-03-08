package de.tu_dresden.inf.lat.abox_repairs.reasoning;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import de.tu_dresden.inf.lat.abox_repairs.ontology_tools.FreshOWLEntityFactory.FreshOWLClassFactory;
import de.tu_dresden.inf.lat.abox_repairs.ontology_tools.FreshOWLEntityFactory.FreshOWLNamedIndividualFactory;
import de.tu_dresden.inf.lat.abox_repairs.tools.DisposableFinalVariable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;

/**
 * Facade to encapsulate common reasoning tasks.
 *
 * Mainly used to determine subsumption relationships between a given set of
 * class expressions. Usually, we use the class expressions occurring in the
 * ontology for this.
 *
 * @author Patrick Koopmann
 */
public class ReasonerFacade {

    private static Logger logger = LogManager.getLogger(ReasonerFacade.class);

//    private final BiMap<OWLClassExpression, OWLClass> expression2Name;

    private final DisposableFinalVariable<OWLOntology> ontology, ontologyCopy;
//    private final Set<OWLAxiom> addedAxioms;
    private final OWLDataFactory factory;

    private final OWLReasoner reasoner;

    private final FreshOWLClassFactory freshOWLClassFactory;
    private final FreshOWLNamedIndividualFactory freshOWLNamedIndividualFactory;

//    private final Set<OWLClassExpression> expressions; // TODO only for testing - remove afterwards!

    //private Timer timer;

    /**
     * Uses no TBox, and only supports the expressions provided. Hence, the manager is required to create the ontology used internally.
     */
    public static ReasonerFacade newReasonerFacadeWithoutTBox(
            Set<OWLClassExpression> expressions, OWLOntologyManager manager) throws OWLOntologyCreationException {
        OWLOntology emptyTBox = manager.createOntology();

        return newReasonerFacadeWithTBox(emptyTBox, expressions);
    }

    /**
     * Uses no TBox for reasoning, however, supports the expressions found in the ontology (subsumption relations without TBox).
     * @param ontology
     * @return
     * @throws OWLOntologyCreationException
     */
    public static ReasonerFacade newReasonerFacadeWithoutTBox(OWLOntology ontology)
            throws OWLOntologyCreationException {
        return newReasonerFacadeWithoutTBox(ontology, Collections.emptySet());
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
    public static ReasonerFacade newReasonerFacadeWithoutTBox(OWLOntology ontology,
            Set<OWLClassExpression> additionalExpressions) throws OWLOntologyCreationException
        {

        Set<OWLClassExpression> expressions = ontology.getNestedClassExpressions();
        expressions.addAll(additionalExpressions);

        OWLOntology emptyTBox = ontology.getOWLOntologyManager().createOntology();

        return newReasonerFacadeWithTBox(emptyTBox, expressions);
    }

    /**
     * Uses TBox for inferences, and supports only those expressions found in the ontology.
     * @param ontology
     * @return
     */
    public static ReasonerFacade newReasonerFacadeWithTBox(OWLOntology ontology) {
        return newReasonerFacadeWithTBox(ontology, Collections.emptySet());
    }


    /**
     * Uses the TBox for inferences, and supports those expressions found in the ontology, together with the ones supplied.
     * @param ontology
     * @param additionalExpressions
     * @return
     */
    public static ReasonerFacade newReasonerFacadeWithTBox(
            OWLOntology ontology, Set<OWLClassExpression> additionalExpressions) {
//        Set<OWLClassExpression> expressions = ontology.getNestedClassExpressions();
////        System.out.println("additionalExpressions " + additionalExpressions);
//        expressions.addAll(additionalExpressions);
//
//        logger.info("used expressions: "+additionalExpressions.size());
//
////        System.out.println("expressions " + expressions);
//        return new ReasonerFacade(ontology, expressions);
        return new ReasonerFacade(ontology, additionalExpressions);
    }

    private ReasonerFacade(OWLOntology ontology, Set<OWLClassExpression> expressions) {
//        expression2Name = HashBiMap.create();
//        addedAxioms = new HashSet<>();
        this.ontology = new DisposableFinalVariable<>(ontology);
        try {
            this.ontologyCopy = new DisposableFinalVariable<>(ontology.getOWLOntologyManager().createOntology());
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }

        this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();
        this.freshOWLClassFactory = FreshOWLClassFactory.of(ontologyCopy.get());
        this.freshOWLClassFactory.addAdditionalKnownEntities(expressions);
        this.freshOWLNamedIndividualFactory = FreshOWLNamedIndividualFactory.of(ontologyCopy.get());

        ontology.axioms(Imports.INCLUDED).forEach(axiom -> axiom.accept(addAxiomVisitor));
        ontology.getOWLOntologyManager().addOntologyChangeListener(changes -> {
            for (OWLOntologyChange change : changes) {
                if (change.isAddAxiom()) {
                    change.getAxiom().accept(addAxiomVisitor);
                } else if (change.isRemoveAxiom()) {
                    change.getAxiom().accept(removeAxiomVisitor);
                } else {
                    throw new UnsupportedOperationException("The reasoner facade does not support the change " + change);
                }
            }
        }, new SpecificOntologyChangeBroadcastStrategy(ontology));

        addExpressions(expressions);

//        this.expressions=expressions;

//        System.out.println(ontologyCopy.get().getAxioms(AxiomType.CLASS_ASSERTION).size() + " class assertions");
//        System.out.println(ontologyCopy.get().getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION).size() + " object property assertions");
//        System.out.println(ontologyCopy.get().getAxioms(AxiomType.SUBCLASS_OF).size() + " subclass assertions");
//        System.out.println(ontologyCopy.get().getAxioms(AxiomType.EQUIVALENT_CLASSES).size() + " equivalent class assertions");

        reasoner = new ElkReasonerFactory().createReasoner(ontologyCopy.get());

        long start = System.nanoTime();
        reasoner.precomputeInferences();
        reasoner.flush();
        logger.info("classification took "+(((double)System.nanoTime()-start)/1_000_000_000));
    }

    public void update() {
//        System.out.println("update...");
//        System.out.println(ontologyCopy.get().getAxioms(AxiomType.CLASS_ASSERTION).size() + " class assertions");
//        System.out.println(ontologyCopy.get().getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION).size() + " object property assertions");
//        System.out.println(ontologyCopy.get().getAxioms(AxiomType.SUBCLASS_OF).size() + " subclass assertions");
//        System.out.println(ontologyCopy.get().getAxioms(AxiomType.EQUIVALENT_CLASSES).size() + " equivalent class assertions");
        reasoner.flush();
    }

//    private final Set<OWLClassExpression> alreadyAdded = new HashSet<>();

    public void addExpression(OWLClassExpression ce) {
        addExpressions(Collections.singleton(ce));
    }

    public void addExpressions(Set<OWLClassExpression> exps) {
//        for(OWLClassExpression exp:exps) {
//            OWLClass name;
////            if(exp instanceof OWLClass) {
////            name = (OWLClass) exp;
//            if(exp.isOWLClass()) {
//                name = exp.asOWLClass();
//            } else {
//                name = freshOWLClassFactory.getEntity(exp);
//                OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(exp,name);
////                System.out.println("Halo axiom " + axiom);
//                ontologyCopy.addAxiom(axiom);
////                addedAxioms.add(axiom);
//            }
//
////            expression2Name.put(exp, name);
//        }
        freshOWLClassFactory.addAdditionalKnownEntities(exps);
        for (OWLClassExpression owlClassExpression : exps) {
            if (!freshOWLClassFactory.containsObject(owlClassExpression)) {
                final OWLEquivalentClassesAxiom axiom = factory.getOWLEquivalentClassesAxiom(
                        owlClassExpression, freshOWLClassFactory.getEntity(owlClassExpression));
                ontologyCopy.get().addAxiom(axiom);
//                System.out.println("Adding helper axiom: " + axiom);
            }
        }
    }

    /**
     * Cleans up the ontology by removing all axioms that have been added by this class.
     */
    @Deprecated
    public void cleanOntology() {
//        addedAxioms.forEach(ontologyCopy::remove);
    }

    public void dispose() {
        reasoner.dispose();
//        alreadyAdded.clear();
        ontology.dispose();
        ontologyCopy.dispose();
    }

    public Set<OWLClassExpression> getSupportedClassExpressions() {
        return freshOWLClassFactory.getObjects();
    }

    public boolean instanceOf(OWLIndividual ind, OWLClassExpression exp) {
        verifyKnows(ind);
        verifyKnows(exp);

        //timer.continueTimer();

        boolean result = reasoner.getTypes(freshOWLNamedIndividualFactory.getEntity(ind)).containsEntity(freshOWLClassFactory.getEntity(exp));

        //timer.pause();

        return result;
    }

    public boolean subsumedBy(OWLClassExpression subsumee, OWLClassExpression subsumer) {
        verifyKnows(subsumee);
        verifyKnows(subsumer);

        //timer.continueTimer();

        OWLClass subsumeeName = freshOWLClassFactory.getEntity(subsumee);
        OWLClass subsumerName = freshOWLClassFactory.getEntity(subsumer);

        boolean result =  reasoner.getEquivalentClasses(subsumeeName).contains(subsumerName)
        || reasoner.getSuperClasses(subsumeeName).containsEntity(subsumerName);

        //timer.pause();

        return result;
    }

    @Deprecated
    public Set<OWLClassExpression> instanceOfExcludingOWLThing(OWLIndividual ind) {
        verifyKnows(ind);

        //timer.continueTimer();

        Set<OWLClassExpression> result = reasoner.types(freshOWLNamedIndividualFactory.getEntity(ind))
            .filter(c -> !c.isOWLThing())
            .map(name -> freshOWLClassFactory.getObject(name))
                .collect(Collectors.toSet());

        //timer.pause();

        return result;
    }

    public final Stream<OWLClassExpression> types(OWLIndividual ind) throws IllegalArgumentException {
        verifyKnows(ind);
        return reasoner.types(freshOWLNamedIndividualFactory.getEntity(ind))
                .map(name -> freshOWLClassFactory.getObject(name));
    }

    public final Set<OWLClassExpression> getTypes(OWLIndividual ind) throws IllegalArgumentException {
        return OWLAPIStreamUtils.asSet(types(ind));
    }

    public final Stream<OWLClassExpression> equivalentClasses(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);
        return reasoner.equivalentClasses(freshOWLClassFactory.getEntity(exp))
                .map(name -> freshOWLClassFactory.getObject(name));
    }

    public final Set<OWLClassExpression> getEquivalentClasses(OWLClassExpression exp) throws IllegalArgumentException {
//        verifyKnows(exp);
//
//        //timer.continueTimer();
//
//        Set<OWLClassExpression> result = reasoner.equivalentClasses(freshOWLClassFactory.getEntity(exp))
//            .map(name -> freshOWLClassFactory.getObject(name))
//            .collect(Collectors.toSet());
//
//        //timer.pause();
//
//        return result;
        return OWLAPIStreamUtils.asSet(equivalentClasses(exp));
    }

    public boolean equivalentToOWLThing(OWLClassExpression exp) {
        verifyKnows(exp);

       // timer.continueTimer();

        return reasoner.topClassNode().anyMatch(cl -> freshOWLClassFactory.getEntity(exp).equals(cl));
    }

    @Deprecated
    public Set<OWLClassExpression> directSubsumersExcludingOWLThing(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        return reasoner.superClasses(freshOWLClassFactory.getEntity(exp), true)
            .filter(c -> !c.isOWLThing())
            .map(name -> freshOWLClassFactory.getObject(name)).collect(Collectors.toSet());
    }

    public final Stream<OWLClassExpression> directSubsumers(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);
        return reasoner.superClasses(freshOWLClassFactory.getEntity(exp), true)
                .map(name -> freshOWLClassFactory.getObject(name));
    }

    public final Set<OWLClassExpression> getDirectSubsumers(OWLClassExpression exp) throws IllegalArgumentException {
        return OWLAPIStreamUtils.asSet(directSubsumers(exp));
    }

    @Deprecated
    public Set<OWLClassExpression> strictSubsumersExcludingOWLThing(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        return reasoner.superClasses(freshOWLClassFactory.getEntity(exp), false)
            .filter(c -> !c.isOWLThing())
            .map(name -> freshOWLClassFactory.getObject(name))
            .collect(Collectors.toSet());
    }

    public final Stream<OWLClassExpression> strictSubsumers(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);
        return reasoner.superClasses(freshOWLClassFactory.getEntity(exp), false)
                .map(name -> freshOWLClassFactory.getObject(name));
    }

    public final Set<OWLClassExpression> getStrictSubsumers(OWLClassExpression exp) throws IllegalArgumentException {
        return OWLAPIStreamUtils.asSet(strictSubsumers(exp));
    }

    @Deprecated
    public Set<OWLClassExpression> equivalentIncludingOWLThingOrStrictlySubsumingExcludingOWLThing(OWLClassExpression exp) throws IllegalAccessError {
        verifyKnows(exp);

        Set<OWLClassExpression> result = strictSubsumersExcludingOWLThing(exp);
        result.addAll(getEquivalentClasses(exp));
        return result;
    }

    public final Stream<OWLClassExpression> subsumers(OWLClassExpression exp) throws IllegalArgumentException {
        return Streams.concat(equivalentClasses(exp), strictSubsumers(exp));
    }

    public final Set<OWLClassExpression> getSubsumers(OWLClassExpression exp) throws IllegalArgumentException {
        return OWLAPIStreamUtils.asSet(subsumers(exp));
    }

    @Deprecated
    public Set<OWLClassExpression> directSubsumeesExcludingOWLNothing(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        return reasoner.subClasses(freshOWLClassFactory.getEntity(exp), true)
            .filter(c -> !c.isOWLNothing())
            .map(name -> freshOWLClassFactory.getObject(name)).collect(Collectors.toSet());
    }

    public final Stream<OWLClassExpression> directSubsumees(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);
        return reasoner.subClasses(freshOWLClassFactory.getEntity(exp), true)
                .map(name -> freshOWLClassFactory.getObject(name));
    }

    public final Set<OWLClassExpression> getDirectSubsumees(OWLClassExpression exp) throws IllegalArgumentException {
        return OWLAPIStreamUtils.asSet(directSubsumees(exp));
    }

    @Deprecated
    public Set<OWLClassExpression> strictSubsumeesExcludingOWLThingAndOWLNothing(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

//        logger.info("TBox size: "+ontology.tboxAxioms(Imports.INCLUDED).count());

        Set<OWLClassExpression> result = reasoner.subClasses(freshOWLClassFactory.getEntity(exp), false)
            .filter(c -> (!c.isOWLThing() && !c.isOWLNothing()))
            .map(name -> freshOWLClassFactory.getObject(name))
            .collect(Collectors.toSet());

       // logger.info("Subsumees of "+exp+":");
       // result.stream().forEach(logger::info);

//        if(result.contains(null)){
//            System.out.println("Unexpected null caused by:");
//
//
//            reasoner.subClasses(freshOWLClassFactory.getEntity(exp), false)
//                    .filter(c -> !c.isOWLThing() && !c.isOWLNothing()).forEach(x -> {
//                        System.out.println(x);
//                        if(freshOWLClassFactory.getObject(x)==null)
//                            System.out.println(" -- that one I did not know.");
//                        if(!expressions.contains(x))
//                            System.out.println(" -- and indeed, it was not provided.");
//            });
//            System.exit(1);
//        }
        return result;
    }

    public final Stream<OWLClassExpression> strictSubsumees(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);
        return reasoner.subClasses(freshOWLClassFactory.getEntity(exp), false)
                .map(name -> freshOWLClassFactory.getObject(name));
    }

    public final Set<OWLClassExpression> getStrictSubsumees(OWLClassExpression exp) throws IllegalArgumentException {
        return OWLAPIStreamUtils.asSet(strictSubsumees(exp));
    }

    /**
     * Return or class expressions that are equivalent to exp, or subsumed by it. That is: the set of all subsummes,
     * including the equivalent ones.
     */
    @Deprecated
    public Set<OWLClassExpression> equivalentIncludingOWLThingAndOWLNothingOrStrictlySubsumedByExcludingOWLThingAndOWLNothing(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        Set<OWLClassExpression> result = strictSubsumeesExcludingOWLThingAndOWLNothing(exp);
        result.addAll(getEquivalentClasses(exp));
        return result;
    }

    public final Stream<OWLClassExpression> subsumees(OWLClassExpression exp) throws IllegalArgumentException {
        return Streams.concat(equivalentClasses(exp), strictSubsumees(exp));
    }

    public final Set<OWLClassExpression> getSubsumees(OWLClassExpression exp) throws IllegalArgumentException {
        return OWLAPIStreamUtils.asSet(subsumees(exp));
    }

    private void verifyKnows(OWLClassExpression exp) throws IllegalArgumentException {
        if(!freshOWLClassFactory.containsObject(exp))
            throw new IllegalArgumentException("ClassExpression unknown: "+exp);
    }


    private void verifyKnows(OWLIndividual ind) throws IllegalArgumentException {
        if (!freshOWLNamedIndividualFactory.containsObject(ind))
            throw new IllegalArgumentException("Individual unknown: " + ind);
    }


    /**
     * Determine whether set1 is covered by set2, according to the Definition in the CADE-21 paper.
     *
     * Here, "covered by" means that for every concept C in set1, we can find a concept D in set2
     * s.t. C is subsumed by D.
     *
     * @param set1
     * @param set2
     * @return
     */
    public boolean isCovered(Set<OWLClassExpression> set1, Set<OWLClassExpression> set2) {

        // cheap test first
        if(set2.containsAll(set1))
            return true;

    	return set1.parallelStream()
    			.allMatch(atom1 -> set2.parallelStream()
    					.anyMatch(atom2 -> subsumedBy(atom1, atom2)));
    }




    /**
     * Checks whether exp is subsumed by some element in the set.
     */
    public boolean subsumedByAny(OWLClassExpression exp, Set<OWLClassExpression> set2) {
        if(set2.contains(exp)) // cheap test first
            return true;
        else
            return set2.stream()
                    .anyMatch(otherExp -> subsumedBy(exp, otherExp));
    }

    private final OWLAxiomVisitor addAxiomVisitor = new OWLAxiomVisitor() {

        @Override
        public void visit(OWLDeclarationAxiom axiom) {
            ontologyCopy.get().add(axiom);
            freshOWLNamedIndividualFactory.addAdditionalKnownEntity(axiom.getEntity());
            freshOWLClassFactory.addAdditionalKnownEntity(axiom.getEntity());
        }

        @Override
        public void visit(OWLClassAssertionAxiom axiom) {
            if (!axiom.getClassExpression().isOWLClass()) {
                doDefault(axiom);
            } else if (axiom.getIndividual().isAnonymous()) {
                ontologyCopy.get().addAxiom(factory.getOWLClassAssertionAxiom(
                        axiom.getClassExpression(),
                        freshOWLNamedIndividualFactory.getEntity(axiom.getIndividual())));
            } else {
                ontologyCopy.get().add(axiom);
                freshOWLNamedIndividualFactory.addAdditionalKnownEntity(axiom.getIndividual());
            }
        }

        @Override
        public void visit(OWLObjectPropertyAssertionAxiom axiom) {
            if (axiom.getSubject().isAnonymous() || axiom.getObject().isAnonymous()) {
                ontologyCopy.get().addAxiom(factory.getOWLObjectPropertyAssertionAxiom(
                        axiom.getProperty(),
                        freshOWLNamedIndividualFactory.getEntity(axiom.getSubject()),
                        freshOWLNamedIndividualFactory.getEntity(axiom.getObject())));
            } else {
                ontologyCopy.get().add(axiom);
            }
            if (axiom.getSubject().isOWLNamedIndividual())
                freshOWLNamedIndividualFactory.addAdditionalKnownEntity(axiom.getSubject());
            if (axiom.getObject().isOWLNamedIndividual())
                freshOWLNamedIndividualFactory.addAdditionalKnownEntity(axiom.getObject());
        }

        @Override
        public void visit(OWLSubClassOfAxiom axiom) {
            ontologyCopy.get().add(axiom);
            addExpressions(axiom.getNestedClassExpressions());
        }

        @Override
        public void visit(OWLEquivalentClassesAxiom axiom) {
            ontologyCopy.get().add(axiom);
            addExpressions(axiom.getNestedClassExpressions());
        }

        @Override
        public void doDefault(Object object) {
            throw new IllegalArgumentException("The reasoner facade does not support the axiom " + object + ".");
        }

    };

    private final OWLAxiomVisitor removeAxiomVisitor = new OWLAxiomVisitor() {

        @Override
        public void visit(OWLDeclarationAxiom axiom) {
            ontologyCopy.get().remove(axiom);
        }

        @Override
        public void visit(OWLClassAssertionAxiom axiom) {
            if (!axiom.getClassExpression().isOWLClass()) {
                doDefault(axiom);
            } else if (axiom.getIndividual().isAnonymous()) {
                ontologyCopy.get().removeAxiom(factory.getOWLClassAssertionAxiom(
                        axiom.getClassExpression(),
                        freshOWLNamedIndividualFactory.getEntity(axiom.getIndividual())));
            } else {
                ontologyCopy.get().remove(axiom);
            }
        }

        @Override
        public void visit(OWLObjectPropertyAssertionAxiom axiom) {
            if (axiom.getSubject().isAnonymous() || axiom.getObject().isAnonymous()) {
                ontologyCopy.get().removeAxiom(factory.getOWLObjectPropertyAssertionAxiom(
                        axiom.getProperty(),
                        freshOWLNamedIndividualFactory.getEntity(axiom.getSubject()),
                        freshOWLNamedIndividualFactory.getEntity(axiom.getObject())));
            } else {
                ontologyCopy.get().remove(axiom);
            }
        }

        @Override
        public void visit(OWLSubClassOfAxiom axiom) {
            ontologyCopy.get().remove(axiom);
        }

        @Override
        public void visit(OWLEquivalentClassesAxiom axiom) {
            ontologyCopy.get().remove(axiom);
        }

        @Override
        public void doDefault(Object object) {
            throw new IllegalArgumentException("The reasoner facade does not support the axiom " + object + ".");
        }

    };

}
