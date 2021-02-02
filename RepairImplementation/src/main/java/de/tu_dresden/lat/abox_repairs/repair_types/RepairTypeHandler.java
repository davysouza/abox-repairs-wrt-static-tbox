package de.tu_dresden.lat.abox_repairs.repair_types;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;

/**
 * Factory for repair types, including also functionality to modify repair types.
 * 
 * @author Patrick Koopmann
 */
public class RepairTypeHandler {
 
    private final ReasonerFacade reasonerWithTBox, reasonerWithoutTBox;

    public RepairTypeHandler(ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox) {
        this.reasonerWithTBox=reasonerWithTBox;
        this.reasonerWithoutTBox=reasonerWithTBox;
    }

    public RepairType newMinimisedRepairType(Set<OWLClassExpression> classExpressions) {
        return minimise(new RepairType(classExpressions));
    }

    /**
     * Keeps only the subsumption-maximal classes in the repair type, that is: there is no class 
     * in the resulting repair type that is subsumed by another one.
     */
    public RepairType minimise(RepairType type){
        Set<OWLClassExpression> newClasses = new HashSet<>(type.getClassExpressions());
        Set<OWLClassExpression> processed = new HashSet<>();

        for(OWLClassExpression exp:type.getClassExpressions()){
            if(!processed.contains(exp) && newClasses.contains(exp)){
                newClasses.removeAll(reasonerWithoutTBox.equivalentOrSubsumedBy(exp));
                newClasses.add(exp);
            }
        }

        return new RepairType(newClasses);
    }

    /**
     * Check whether the precondition for the premise saturation rule is satisfied from the 
     * perspective of the repair type. Specifically, check whether there exists a class
     * expression D in the repair type s.t. the ontology entails exp SubClassOf D.  
     * 
     */
    public boolean premiseSaturationApplies(RepairType type, OWLClassExpression exp) {
        for(OWLClassExpression toRepair:type.getClassExpressions()){
            if(reasonerWithTBox.subsumees(toRepair).contains(exp))
                return true;
        }

        return false;
    }

    /**
     * Return all types that are obtained from the given repair type by applying 
     * premise saturation with respect to the given class expression.
     * 
     * Assumption: the individual assigned the repair type is an instance of exp, and 
     * the repair type contains some class expression D s.t. the ontology entails 
     * exp SubClassOf D
     */
    public Set<RepairType> premiseSaturate(RepairType type, OWLClassExpression exp) {

        Set<RepairType> result = new HashSet<>();

        for(OWLClassExpression subsumer: reasonerWithoutTBox.subsumers(exp)){
            if(!(subsumer instanceof OWLObjectIntersectionOf)){
                Set<OWLClassExpression> newType = new HashSet<>(type.getClassExpressions());
                newType.add(subsumer);
                result.add(newMinimisedRepairType(newType));
            }
        }

        return result;
    }
}
