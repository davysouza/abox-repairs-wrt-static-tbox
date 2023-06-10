package de.tudresden.inf.lat.aboxrepair.repairrequest;

import de.tudresden.inf.lat.aboxrepair.reasoner.ReasonerFacade;
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

    /**
     * Retrieves the set of the named individuals of the repair request
     * @return A Set<OWLNamedIndividual> object
     */
    public Set<OWLNamedIndividual> individuals() {
        return super.keySet();
    }

    /**
     * Retrieves a set of all the nested class expressions of the given repair request
     * @return A Set<OWLNamedIndividual> object containing the nested OWLClassExpression
     */
    public Set<OWLClassExpression> getNestedClassExpressions() {
        final Set<OWLClassExpression> nestedClassExpressions = new HashSet<>();

        for (Collection<OWLClassExpression> expressions : values()) {
            for (OWLClassExpression expression : expressions) {
                nestedClassExpressions.add(expression);
                nestedClassExpressions.addAll(expression.getNestedClassExpressions());
            }
        }
        return nestedClassExpressions;
    }

    /**
     * Checks whether the given repair request is valid, which means that it contains no class names that are equivalent
     * to owl:thing.
     *
     * @param repairRequest The repair request to be checked
     * @param reasonerWithTBox Reasoner to be used for checking the equivalence
     * @return Returns true if the given repair request is valid, false otherwise.
     */
    public static boolean checkValid(RepairRequest repairRequest, ReasonerFacade reasonerWithTBox) {
        for(OWLNamedIndividual individual : repairRequest.individuals()) {
            for(OWLClassExpression expression: repairRequest.get(individual)) {
            	// if(expression.isOWLThing()) {
            	//      System.out.println("Top!! for " + individual );
            	//  	return false;
            	// }

                if(reasonerWithTBox.equivalentToOWLThing(expression)) {
                    System.out.println("Repair request contains " + expression + " for " + individual);
                    System.out.println(expression + " is equivalent to owl:Thing");
                    return false;
                }
            }
        }
        return true;
    }
}
