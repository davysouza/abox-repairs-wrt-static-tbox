package de.tu_dresden.inf.lat.abox_repairs.ontology_tools;

import java.util.HashSet;
import java.util.Set;

import com.github.jsonldjava.shaded.com.google.common.base.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * Restrict ontologies to EL by replacing domain restrictions into the corresponding EL CIs,  
 * and removing all other axioms that are not in EL. Here, n-ary equivalence axioms are allowed, but 
 * equality or inequality assertions are not.
 * 
 * @author Patrick Koopmann
 */
public class ELRestrictor {
    private static Logger logger = LogManager.getLogger(ELRestrictor.class);


    private final OWLDataFactory dataFactory;

    public ELRestrictor(OWLDataFactory dataFactory) {
        this.dataFactory=dataFactory;
    }

    public void restrict(OWLOntology ontology) {
        Set<OWLAxiom> toRemove = new HashSet<>();
        Set<OWLAxiom> toAdd = new HashSet<>();

        logger.debug("Restricting to pure EL...");

        ontology.axioms(Imports.INCLUDED).forEach(axiom -> {
            if(!axiomTypeAllowed(axiom)) {
                toRemove.add(axiom);
            }
            else if(!axiom.nestedClassExpressions().allMatch(this::classExpressionAllowed)){
                toRemove.add(axiom);
            } else {
                Optional<OWLAxiom> optConverted = convertIfNeeded(axiom);
                if(optConverted.isPresent()) {
                    toRemove.add(axiom);
                    toAdd.add(optConverted.get());
                }
            }
        });

        toRemove.stream().forEach(a -> logger.debug("Removing (not in EL): "+a));
        toAdd.stream().forEach(a -> logger.debug("Adding (converted): "+a));
        
        
        //System.out.println("Removed "+(toRemove.size()-toAdd.size())+" axioms and converted "+toAdd.size()+" axioms.");

        toRemove.stream().forEach(ontology::removeAxiom);
        toAdd.stream().forEach(ontology::addAxiom);
    }

    public boolean axiomTypeAllowed(OWLAxiom axiom) {
        return (axiom instanceof OWLClassAssertionAxiom) ||
               (axiom instanceof OWLObjectPropertyAssertionAxiom) ||
               (axiom instanceof OWLSubClassOfAxiom) ||
               (axiom instanceof OWLEquivalentClassesAxiom) ||
               (axiom instanceof OWLObjectPropertyDomainAxiom) ||
               (axiom instanceof OWLDeclarationAxiom);
    }

    public boolean classExpressionAllowed(OWLClassExpression exp) {
        if(exp.isOWLNothing())
            return false;
        return (exp.isOWLThing()) ||
               (exp instanceof OWLClass) ||
               (exp instanceof OWLObjectIntersectionOf) ||
               (exp instanceof OWLObjectSomeValuesFrom);
    }

    public Optional<OWLAxiom> convertIfNeeded(OWLAxiom axiom) {
        if(axiom instanceof OWLObjectPropertyDomainAxiom) {
            OWLObjectPropertyDomainAxiom domainAxiom = (OWLObjectPropertyDomainAxiom) axiom;
            return Optional.of(dataFactory.getOWLSubClassOfAxiom(
              dataFactory.getOWLObjectSomeValuesFrom(
                  domainAxiom.getProperty(),
                  dataFactory.getOWLThing()), 
              domainAxiom.getDomain()));
        } else
            return Optional.absent();
    }
}
