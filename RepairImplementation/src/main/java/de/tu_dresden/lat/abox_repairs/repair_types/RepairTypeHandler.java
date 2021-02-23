package de.tu_dresden.lat.abox_repairs.repair_types;

import java.util.*;
import java.util.stream.Collectors;

import de.tu_dresden.lat.abox_repairs.saturation.ChaseGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;


import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;

/**
 * Factory for repair types, including also functionality to modify repair types.
 *
 * @author Patrick Koopmann & Adrian Nuradiansyah
 */
public class RepairTypeHandler {

    private static Logger logger = LogManager.getLogger(RepairTypeHandler.class);


    private final ReasonerFacade reasonerWithTBox, reasonerWithoutTBox;

    public RepairTypeHandler(ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox) {
        this.reasonerWithTBox = reasonerWithTBox;
        this.reasonerWithoutTBox = reasonerWithoutTBox;
    }

    public RepairType newMinimisedRepairType(Set<OWLClassExpression> classExpressions) {
        return minimise(new RepairType(classExpressions));
    }

    /**
     * Keeps only the subsumption-maximal classes in the repair type, that is: there is no class
     * in the resulting repair type that is subsumed by another one.
     */
    public RepairType minimise(RepairType type) {
        Set<OWLClassExpression> newClasses = new HashSet<>(type.getClassExpressions());
        for(OWLClassExpression exp:type.getClassExpressions()){
        	assert(!exp.isOWLThing());
            if(newClasses.contains(exp)) {
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
     */
    public boolean premiseSaturationApplies(RepairType type, OWLClassExpression exp) {
        for (OWLClassExpression toRepair : type.getClassExpressions()) {
            if (reasonerWithTBox.subsumees(toRepair).contains(exp))
                return true;
        }

        return false;
    }

    /**
     * Check whether the given repair pre-type is already premise-saturated
     *
     * @param repairPreType a repair pre-type
     * @return true if the given repair pre-type is already premise-saturated
     */

    public boolean isPremiseSaturated(Set<OWLClassExpression> repairPreType, OWLNamedIndividual ind) {

    	return repairPreType.stream().allMatch(atom -> reasonerWithTBox.equivalentOrSubsumedBy(atom)
				.stream().allMatch(subsumee -> 
								reasonerWithoutTBox.subsumedByAny(subsumee, repairPreType) && 
		             			reasonerWithTBox.instanceOf(ind, subsumee) ) );
    	
    }

    /**
     * Given a repair pre-type and a random object, this method will this repair pre-type
     * to a repair type that has been premise-saturated
     *
     * @param hittingSet a repair pre-type that may not be premise-saturated yet
     * @param random  a random object that help choose a random repair type that will be returned
     * @return a random repair type
     */

   
    public RepairType convertToRandomRepairType(Set<OWLClassExpression> hittingSet, OWLNamedIndividual ind) {
    	
    	boolean isSaturated = false;
        
        Set<OWLClassExpression> resultingSet = new HashSet<>(hittingSet);
        Set<OWLClassExpression> setOfNewAtoms = new HashSet<>(hittingSet);
        while (!isSaturated) {
            isSaturated = true;
            Set<OWLClassExpression> buffer = new HashSet<>();
            for (OWLClassExpression exp : setOfNewAtoms) {
                
                for (OWLClassExpression subsumee : reasonerWithTBox.equivalentOrSubsumedBy(exp)) {

                    
                    if (!reasonerWithoutTBox.subsumedByAny(subsumee, resultingSet) 
                    		&& reasonerWithTBox.instanceOf(ind, subsumee)) {
                    	
                    	OWLClassExpression concept = subsumee.asConjunctSet().stream()
                    			.filter(con -> !reasonerWithTBox.equivalentToOWLThing(con)).findAny().get();
                    	
                    	resultingSet.add(concept);
                    	buffer.add(concept);
                        isSaturated = false;
                    }
                }
            }
            setOfNewAtoms = new HashSet<>(buffer);
        }

        return newMinimisedRepairType(resultingSet);
    	
  
    }

    /**
     * The method receives a repair type that does not cover the given set Succ(K,r,u).
     * It then computes a set of repair types that cover the union of the repair type and the set Succ(K,r,u)
     *
     * @param repairType
     * @param successorSet Succ(K,r,u) in this case.
     * @return the set that contains all minimal repair types that cover the union of
     * the repair type and the set Succ(K,r,u).
     */
    public Set<RepairType> findCoveringRepairTypes(RepairType repairType, 
    		Set<OWLClassExpression> successorSet, OWLNamedIndividual ind) {

        // repairType should never be null
    	
    	// each candidate in the setOfCandidates cannot contain any atom that is equivalent to owl:Thing

        Set<Set<OWLClassExpression>> setOfCandidates =
                        findCoveringPreTypes(new HashSet<>(repairType.getClassExpressions()), successorSet);


        Set<RepairType> resultingSet = new HashSet<>();


        while (setOfCandidates.iterator().hasNext()) {
            Set<OWLClassExpression> candidate = setOfCandidates.iterator().next();
            setOfCandidates.remove(candidate);

            boolean isSaturated = true;
           
            for (OWLClassExpression atom : candidate) {

                for (OWLClassExpression subsumee : reasonerWithTBox.equivalentOrSubsumedBy(atom)) {
                    if (! reasonerWithoutTBox.subsumedByAny(subsumee, candidate) && 
                    		reasonerWithTBox.instanceOf(ind, subsumee)) {

                        isSaturated = false;

                        Set<OWLClassExpression> topLevelConjunct = subsumee.asConjunctSet();

                        for (OWLClassExpression conjunct : topLevelConjunct) {
                        	if(!reasonerWithTBox.equivalentToOWLThing(conjunct)) {
                        		Set<OWLClassExpression> tempCandidate = new HashSet<>(candidate);
                                tempCandidate.add(conjunct);
                                setOfCandidates.add(tempCandidate);
                        	}
                            
                        }
                    }
                }
            }
            if (isSaturated) {
                RepairType newType = newMinimisedRepairType(candidate);
                if (!resultingSet.contains(newType)) resultingSet.add(newType);
            }


        }

        final Set<RepairType> minimalRepairTypes = new HashSet<>();
        
        for (RepairType type : resultingSet) {
            if (minimalRepairTypes.stream().noneMatch(otherType ->
                    reasonerWithoutTBox.isCovered(otherType.getClassExpressions(), type.getClassExpressions()))) {
                minimalRepairTypes.removeIf(otherType ->
                        reasonerWithoutTBox.isCovered(type.getClassExpressions(), otherType.getClassExpressions()));
                minimalRepairTypes.add(type);
            }
        }

        return minimalRepairTypes;
        
    }


//	/**
//	 * Find covering pretypes. There is one such for each (non-TBox) subsumee atom of exp, which is then added to the type.
//     *
//	 * @param type	a repair type
//	 * @param exp 	a class expression
//	 * 
//	 * @return the set that contains minimal repair pre-types that cover the union of
//	 *         the given repair type and the class expression
//	 */
//    private Set<Set<OWLClassExpression>> findCoveringPreTypes(Set<OWLClassExpression> type, OWLClassExpression exp) {
//    	
//    	if(reasonerWithTBox.equivalentToOWLThing(exp)) {
//    		return Collections.emptySet();
//    	}
//    	
//        Set<Set<OWLClassExpression>> result = new HashSet<>();
//
//        for (OWLClassExpression subsumer : reasonerWithoutTBox.equivalentOrSubsuming(exp)) {
//            if (!(subsumer instanceof OWLObjectIntersectionOf)) {
//                Set<OWLClassExpression> newType = new HashSet<>(type);
//                newType.add(subsumer);
//                result.add(newType);
//            }
//        }
//
//        return result;
//    }

    /**
     * compute pre-types for the union of two sets.
     * Returns the result of iterating over the findCoveringPreTypes method that takes a single concept as second
     * argument, each case using the newly extended set of pretypes as argument.
     *
     * @param type   a repair type
     * @param successorSet a set of class expressions
     * @return the set that contains minimal repair pre-types that cover the union of the two input sets
     */
    private Set<Set<OWLClassExpression>> findCoveringPreTypes(Set<OWLClassExpression> type, Set<OWLClassExpression> successorSet) {

        Set<Set<OWLClassExpression>> queueSet = new HashSet<>();
        queueSet.add(type);

        for (OWLClassExpression exp : successorSet.stream()
        								.filter(concept -> !reasonerWithTBox.equivalentToOWLThing(concept))
        								.collect(Collectors.toSet())) {
        	
        	Set<Set<OWLClassExpression>> newSet = new HashSet<>();
    		Set<OWLClassExpression> setOfAtoms = reasonerWithoutTBox.equivalentOrSubsuming(exp).stream()
    								.filter(atom -> !(atom instanceof OWLObjectIntersectionOf &&  
    												!reasonerWithTBox.equivalentToOWLThing(atom)))
    								.collect(Collectors.toSet());
    		
    		
    		for(OWLClassExpression atom : setOfAtoms) {
    			for (Set<OWLClassExpression> currentSet : queueSet) {
    				Set<OWLClassExpression> unionSet = new HashSet<>(currentSet);
    				unionSet.add(atom);
    				newSet.add(unionSet);
    			}
    		}
            queueSet.removeAll(queueSet);
            queueSet.addAll(newSet);
        }

        return queueSet;
    }
}
