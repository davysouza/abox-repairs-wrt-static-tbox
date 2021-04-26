package de.tu_dresden.inf.lat.abox_repairs.repair_request;

import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class RepairRequest extends HashMap<OWLNamedIndividual, Set<OWLClassExpression>> {
    public RepairRequest(){
        super();
    }

    public Set<OWLNamedIndividual> individuals() {
        return super.keySet();
    }

    public Set<OWLClassExpression> getNestedClassExpressions() {
        final Set<OWLClassExpression> nestedClassExpressions = new HashSet<>();

        for (Collection<OWLClassExpression> exps : values()) {
            for (OWLClassExpression exp : exps) {
                nestedClassExpressions.add(exp);
                nestedClassExpressions.addAll(exp.getNestedClassExpressions());
            }
        }
        return nestedClassExpressions;
    }

    /**
     * checks whether the given repair request is valid, which means that it contains no class names that are equivalent
     * to owl:thing.
     */
    public static boolean checkValid(RepairRequest rr, ReasonerFacade reasonerWithTBox) {
        for(OWLNamedIndividual ind: rr.individuals()){
            for(OWLClassExpression exp: rr.get(ind)){
//            	if(exp.isOWLThing()) {
//            		System.out.println("Top!! for " + ind );
//            		return false;
//            	}
                if(reasonerWithTBox.equivalentToOWLThing(exp)) {
                    System.out.println("Repair request contains "+exp+" for "+ind);
                    System.out.println(exp+" is equivalent to owl:Thing");
                    return false;
                }
            }
        }
        return true;
    }
}
