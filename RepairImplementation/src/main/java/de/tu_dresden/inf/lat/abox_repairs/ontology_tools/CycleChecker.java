package de.tu_dresden.inf.lat.abox_repairs.ontology_tools;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Checks whether an EL ontology is cycle-restricted.
 * 
 * O is cycle-restricted if for no concept C and word w of role names, O entails
 * C subumedBy exists w.C
 * 
 * To detect this, we use the canonical model as implicitlly given in the
 * classification result.
 * 
 * 
 * @author Patrick Koopmann
 */
public class CycleChecker {

    private final ReasonerFacade reasoner;

    public CycleChecker(OWLOntology ontology) {
        this(ReasonerFacade.newReasonerFacadeWithTBox(ontology));
    }

    public CycleChecker(ReasonerFacade reasoner) {
        this.reasoner = reasoner;
    }

    /**
     * Idea: collect all known subsumption-ships of the form C \sqsubseteq \exists r.D
     * and check whether the resulting graph contains a cycle. 
     * 
     * Note that our implementation of ReasonerFacade makes sure that we do not have to consider 
     * entailments where the existential restriction is under a conjunction.
     */
    public boolean cyclic() {
        Multimap<OWLClassExpression, OWLClassExpression> dependencies = HashMultimap.create();

        for(OWLClassExpression expression: reasoner.getSupportedClassExpressions()){
            for(OWLClassExpression subsumer: reasoner.subsumersExcludingOWLThing(expression)){
                if(subsumer instanceof OWLObjectSomeValuesFrom) {
                    OWLClassExpression filler = ((OWLObjectSomeValuesFrom)subsumer).getFiller();
                    dependencies.put(expression, filler);
                    dependencies.putAll(expression, dependencies.get(filler)); // make transitive
                }
            }
        }

        for(OWLClassExpression exp:dependencies.keySet()){
            if(dependencies.get(exp).contains(exp)){
                System.out.println("Cyclic: "+exp);
                return true;
            }
        }

        return false;
    }

}
