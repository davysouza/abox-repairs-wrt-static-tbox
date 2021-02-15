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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
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

    public static ReasonerFacade newReasonerFacadeWithoutTBox(OWLOntology ontology)
            throws OWLOntologyCreationException {
        return newReasonerFacadeWithoutTBox(ontology, Collections.emptyList());
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

        return reasoner.getTypes(ind).containsEntity(expression2Name.get(exp));
    }

    public boolean subsumedBy(OWLClassExpression subsumee, OWLClassExpression subsumer) {
        verifyKnows(subsumee);
        verifyKnows(subsumer);

        OWLClass subsumeeName = expression2Name.get(subsumee);
        OWLClass subsumerName = expression2Name.get(subsumer);

        return reasoner.getEquivalentClasses(subsumeeName).contains(subsumerName)
        || reasoner.getSuperClasses(subsumeeName).containsEntity(subsumerName);
    }

    public Set<OWLClassExpression> instanceOf(OWLNamedIndividual ind) {
        return reasoner.types(ind)
            .filter(c -> !c.isOWLThing())
            .map(name -> expression2Name.inverse().get(name))
                .collect(Collectors.toSet());
    }

    public Set<OWLClassExpression> equivalentClasses(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);

        return reasoner.equivalentClasses(expression2Name.get(exp))
            .map(name -> expression2Name.inverse().get(name))
            .collect(Collectors.toSet());
    }

    public boolean equivalentToOWLThing(OWLClassExpression exp) {
        verifyKnows(exp);
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
            .filter(c -> !c.isOWLThing())
            .map(name -> expression2Name.inverse().get(name)).collect(Collectors.toSet());
    }

    public Set<OWLClassExpression> subsumees(OWLClassExpression exp) throws IllegalArgumentException {
        verifyKnows(exp);
        return reasoner.subClasses(expression2Name.get(exp), false)
            .filter(c -> !c.isOWLThing())
            .map(name -> expression2Name.inverse().get(name))
            .collect(Collectors.toSet());
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

    
    	return set1.parallelStream()
    			.allMatch(atom1 -> set2.parallelStream()
    					.anyMatch(atom2 -> subsumedBy(atom1, atom2)));
    	
//    	for(OWLClassExpression atom1 : set1) {
//    		Set<OWLClassExpression> singleton = new HashSet<>();
//    		singleton.add(atom1);
//    		OWLClassExpression tempConcept = atLeastOneCovered(singleton, set2);
//    		if(tempConcept == null) {
//    			return false;
//    		}
//    		
//    	}
//    	return true;
    }
    
    public Optional<OWLClassExpression> atLeastOneCovered(Set<OWLClassExpression> set1, Set<OWLClassExpression> set2) {
		
    	for(OWLClassExpression atom1 : set1) {
    		Set<OWLClassExpression> tempSet = new HashSet<>();
    		tempSet.add(atom1);
    		if(isCovered(tempSet, set2)) return Optional.of(atom1);
    	}
    	return Optional.empty();
    	
//    	
//    	
//		for(OWLClassExpression atom1 : set1) {
//			for(OWLClassExpression atom2 : set2) {
//				if(subsumedBy(atom1, atom2)) {
//					return atom2;
//				}
//			}
//		}
//		
//		return null; // TODO: null should never be returned by a method. Have you considered using an Optional
//                     // TODO: or throwing an Exception?
	}
}
