package de.tu_dresden.lat.abox_repairs;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.HashMap;
import java.util.Set;

public class RepairRequest extends HashMap<OWLNamedIndividual, Set<OWLClassExpression>> {
    public RepairRequest(){
        super();
    }

    public Set<OWLNamedIndividual> individuals() {
        return super.keySet();
    }

    /**
     * checks whether the given repair request is valid, which means that it contains no class names that are equivalent
     * to owl:thing.
     */
    public static boolean checkValid(RepairRequest rr, ReasonerFacade reasonerWithTBox) {
        for(OWLNamedIndividual ind: rr.individuals()){
            for(OWLClassExpression exp: rr.get(ind)){
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
