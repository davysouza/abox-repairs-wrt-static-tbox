package de.tu_dresden.lat.abox_repairs.reasoning;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

/**
 * Facade to encapsulate common reasoning tasks.
 * 
 * Mainly used to determine subsumption relationships between a given set of class expressions.
 * Usually, we use the class expressions occurring in the ontology for this.
 * 
 * @author Patrick Koopmann
 */
public class ReasonerFacade {
    private final BiMap<OWLClassExpression,OWLClass> expression2Name;

    private final OWLOntology ontology;
    private final Set<OWLAxiom> addedAxioms;
    private final OWLDataFactory factory;
    
    private final OWLReasoner reasoner;
    private int freshNameCounter = 0;

    public ReasonerFacade(OWLOntology ontology) {
        this(ontology, ontology.getNestedClassExpressions());
    }

    public ReasonerFacade(OWLOntology ontology, Set<OWLClassExpression> expressions) {
        expression2Name = HashBiMap.create();
        addedAxioms = new HashSet<>();
        this.ontology = ontology;
        this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();

        addExpressions(expressions);

        reasoner = new ElkReasonerFactory().createReasoner(ontology);
    }


    private void addExpressions(Set<OWLClassExpression> exps) {
        for(OWLClassExpression exp:exps) {
            OWLClass name;
            if(exp instanceof OWLClass) {
                name = (OWLClass) exp;
            } else {
                name = freshName(exps);
                OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(exp,name);
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

    private void verifyKnows(OWLClassExpression exp) throws IllegalArgumentException {
        if(!expression2Name.containsKey(exp))
            throw new IllegalArgumentException("ClassExpression unknown: "+exp);
    }


    private OWLClass freshName(Set<OWLClassExpression> expressions) {
        OWLClass name = factory.getOWLClass(IRI.create(""+freshNameCounter));

        freshNameCounter++;

        if(!expressions.contains(name))
            return name;
        else
            return freshName(expressions);
    }
}