package de.tu_dresden.lat.abox_repairs.reasoning;

import java.awt.desktop.UserSessionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.ontology_tools.FreshNameProducer;
import de.tu_dresden.lat.abox_repairs.tools.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

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

    private final BiMap<OWLClassExpression, OWLClass> expression2Name;

    private final OWLOntology ontology;
    private final Set<OWLAxiom> addedAxioms;
    private final OWLDataFactory factory;

    private final OWLReasoner reasoner;

    private final FreshNameProducer freshNameProducer;

    //private Timer timer;

    public static ReasonerFacade newReasonerFacadeWithoutTBox(OWLOntology ontology)
            throws OWLOntologyCreationException {
        return newReasonerFacadeWithoutTBox(ontology, Collections.emptyList());
    }

    public static ReasonerFacade newReasonerFacadeWithoutTBox(
            Collection<OWLClassExpression> expressions, OWLOntologyManager manager) throws OWLOntologyCreationException {
        OWLOntology emptyTBox = manager.createOntology();

        return newReasonerFacadeWithTBox(emptyTBox, expressions);
    }

    public static ReasonerFacade newReasonerFacadeWithoutTBox(OWLOntology ontology,
            Collection<OWLClassExpression> additionalExpressions) throws OWLOntologyCreationException 
        {
        Set<OWLClassExpression> expressions = ontology.getNestedClassExpressions();
        expressions.addAll(additionalExpressions); 
        
        OWLOntology emptyTBox = ontology.getOWLOntologyManager().createOntology();

        return newReasonerFacadeWithTBox(emptyTBox, expressions);
    }

    public static ReasonerFacade newReasonerFacadeWithTBox(OWLOntology ontology) {
        return newReasonerFacadeWithTBox(ontology, Collections.emptyList());
    }



    public static ReasonerFacade newReasonerFacadeWithTBox(
            OWLOntology ontology, Collection<OWLClassExpression> additionalExpressions) {
        Set<OWLClassExpression> expressions = ontology.getNestedClassExpressions();
//        System.out.println("additionalExpressions " + additionalExpressions);
        expressions.addAll(additionalExpressions);

        logger.info("used expressions: "+additionalExpressions.size());

//        System.out.println("expressions " + expressions);
        return new ReasonerFacade(ontology, expressions);
    }


    private ReasonerFacade(OWLOntology ontology, Set<OWLClassExpression> expressions) {
        expression2Name = HashBiMap.create();
        addedAxioms = new HashSet<>();
        this.ontology = ontology;
        this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();

        freshNameProducer = FreshNameProducer.newFromClassExpressions(factory,expressions);
        addExpressions(expressions);

        reasoner = new ElkReasonerFactory().createReasoner(ontology);

        long start = System.nanoTime();
        reasoner.precomputeInferences();
        logger.info("classification took "+(((double)System.nanoTime()-start)/1_000_000_000));
    }


    private void addExpressions(Set<OWLClassExpression> exps) {
        for(OWLClassExpression exp:exps) {
            OWLClass name;
            if(exp instanceof OWLClass) {
                name = (OWLClass) exp;
            } else {
                name = freshNameProducer.freshName();
                OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(exp,name);
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

        //timer.continueTimer();

        boolean result = reasoner.getTypes(ind).containsEntity(expression2Name.get(exp));

        //timer.pause();

        return result;
    }

    public boolean subsumedBy(OWLClassExpression subsumee, OWLClassExpression subsumer) {
        verifyKnows(subsumee);
        verifyKnows(subsumer);

        //timer.continueTimer();

        OWLClass subsumeeName = expression2Name.get(subsumee);
        OWLClass subsumerName = expression2Name.get(subsumer);

        boolean result =  reasoner.getEquivalentClasses(subsumeeName).contains(subsumerName)
        || reasoner.getSuperClasses(subsumeeName).containsEntity(subsumerName);

        //timer.pause();

        return result;
    }

    public Set<OWLClassExpression> instanceOf(OWLNamedIndividual ind) {

        //timer.continueTimer();

        Set<OWLClassExpression> result = reasoner.types(ind)
            .filter(c -> !c.isOWLThing())
            .map(name -> expression2Name.inverse().get(name))
                .collect(Collectors.toSet());

        //timer.pause();

        return result;
    }

    public Set<OWLClassExpression> equivalentClasses(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        //timer.continueTimer();

        Set<OWLClassExpression> result = reasoner.equivalentClasses(expression2Name.get(exp))
            .map(name -> expression2Name.inverse().get(name))
            .collect(Collectors.toSet());

        //timer.pause();

        return result;
    }

    public boolean equivalentToOWLThing(OWLClassExpression exp) {
        verifyKnows(exp);

       // timer.continueTimer();

        return reasoner.topClassNode().anyMatch(cl -> expression2Name.get(exp).equals(cl));
    }

    public Set<OWLClassExpression> directSubsumers(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        return reasoner.superClasses(expression2Name.get(exp), true)
            .filter(c -> !c.isOWLThing())
            .map(name -> expression2Name.inverse().get(name)).collect(Collectors.toSet());
    }

    public Set<OWLClassExpression> subsumers(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        return reasoner.superClasses(expression2Name.get(exp), false)
            .filter(c -> !c.isOWLThing())
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
            .filter(c -> !c.isOWLNothing())
            .map(name -> expression2Name.inverse().get(name)).collect(Collectors.toSet());
    }

    public Set<OWLClassExpression> subsumees(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

//        logger.info("TBox size: "+ontology.tboxAxioms(Imports.INCLUDED).count());

        Set<OWLClassExpression> result = reasoner.subClasses(expression2Name.get(exp), false)
            .filter(c -> (!c.isOWLThing() && !c.isOWLNothing()))
            .map(name -> expression2Name.inverse().get(name))
            .collect(Collectors.toSet());

       // logger.info("Subsumees of "+exp+":");
       // result.stream().forEach(logger::info);

        if(result.contains(null)){
            System.out.println("Unexpected null caused by:");
            reasoner.subClasses(expression2Name.get(exp), false)
                    .filter(c -> !c.isOWLThing()).forEach(x -> System.out.println(x));
            System.exit(1);
        }
        return result;
    }


    public Set<OWLClassExpression> equivalentOrSubsumedBy(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        Set<OWLClassExpression> result = subsumees(exp);
        result.addAll(equivalentClasses(exp));
        return result; 
    }


    private void verifyKnows(OWLClassExpression exp) throws IllegalArgumentException {
        if(!expression2Name.containsKey(exp))
            throw new IllegalArgumentException("ClassExpression unknown: "+exp);
    }


    public boolean isCovered(Set<OWLClassExpression> set1, Set<OWLClassExpression> set2) {

        // cheap test first
        if(set2.containsAll(set1))
            return true;

    	return set1.parallelStream()
    			.allMatch(atom1 -> set2.parallelStream()
    					.anyMatch(atom2 -> subsumedBy(atom1, atom2)));
    }
    
    public Optional<OWLClassExpression> findCoveringConcept(Set<OWLClassExpression> set1, Set<OWLClassExpression> set2) {

    	for (OWLClassExpression atom1 : set1) {
    		for(OWLClassExpression atom2 : set2) {
    			if(subsumedBy(atom1, atom2)) {
    				return Optional.of(atom2);
    			}
    		}
    	}
        return Optional.empty();
    }

}
