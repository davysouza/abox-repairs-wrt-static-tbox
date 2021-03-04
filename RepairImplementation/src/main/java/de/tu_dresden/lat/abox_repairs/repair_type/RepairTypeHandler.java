package de.tu_dresden.lat.abox_repairs.repair_type;

import java.util.*;
import java.util.stream.Collectors;

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
//    	this.ontology = inputOntology;
    	this.reasonerWithTBox = reasonerWithTBox;
        this.reasonerWithoutTBox = reasonerWithoutTBox;
//        this.setOfSubconcepts = ontology.getNestedClassExpressions();
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
     * A repair pre-type for an individual u only satisfies Properties 1.) and 2.) of Definition 5 in the paper
     * 
     * Check whether the given repair pre-type is already premise-saturated
     *
     * @param repairPreType a repair pre-type
     * @return true if the given repair pre-type is already premise-saturated
     */

    public boolean isPremiseSaturated(Set<OWLClassExpression> repairPreType, OWLNamedIndividual ind) {

        /* You should replace the condition reasonerWithoutTBox.subsumedByAny(subsumee, repairPreType) &&
		             			reasonerWithTBox.instanceOf(ind, subsumee)
           by
            !reasonerWithTBox.instanceOf(ind, subsumee) || reasonerWithoutTBox.subsumedByAny(subsumee, repairPreType)
           to match Condition 3 of the definition.

           That comment applies to the version before your last two commits, which is:

    public boolean isPremiseSaturated(Set<OWLClassExpression> repairPreType, OWLNamedIndividual ind) {

    	return repairPreType.stream().allMatch(atom -> reasonerWithTBox.equivalentOrSubsumedBy(atom)
				.stream().allMatch(subsumee ->
								reasonerWithoutTBox.subsumedByAny(subsumee, repairPreType) &&
		             			reasonerWithTBox.instanceOf(ind, subsumee) ) );

    }
*/

        /* Please do not introduce new bugs, Adrian.  Before your commit "fixed the method isPremiseSaturated" four days
        *  ago, the method was as follows and returned correct results.  Please code carefully!
        *
        *
        *  public boolean isPremiseSaturated(Set<OWLClassExpression> repairPreType, OWLNamedIndividual ind) {
        */
        for (OWLClassExpression atom : repairPreType) {
            Set<OWLClassExpression> setOfSubsumees = new HashSet<>(reasonerWithTBox.equivalentOrSubsumedBy(atom));
            for (OWLClassExpression subsumee : setOfSubsumees) {
                if (!reasonerWithoutTBox.subsumedByAny(subsumee, repairPreType) &&
                		reasonerWithTBox.instanceOf(ind, subsumee)) {
                    return false;
                }
            }
        }

        return true;
    }
/*
    	
//    	Set<OWLClassExpression> setOfFilteredSubconcepts =  
//    			setOfSubconcepts.stream().filter(subconcept -> reasonerWithTBox.instanceOf(ind, subconcept) && 
//    														   reasonerWithTBox.subsumedByAny(subconcept, repairPreType))
//    									.collect(Collectors.toSet());
//    	
//    	return repairPreType.stream().allMatch(atom -> !reasonerWithTBox.equivalentToOWLThing(atom)) &&
//    			reasonerWithoutTBox.isCovered(setOfFilteredSubconcepts, repairPreType);

    	return repairPreType.stream().allMatch(atom -> reasonerWithTBox.equivalentOrSubsumedBy(atom)
							.stream().allMatch(subsumee -> !reasonerWithTBox.equivalentToOWLThing(atom) &&
															reasonerWithoutTBox.subsumedByAny(subsumee, repairPreType) && 
															reasonerWithTBox.instanceOf(ind, subsumee)));
    	
    }*/

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
                    	
                    	/*
                    	 * I think the below code can be replaced with the following code:
                    	 * OWLClassExpression subsumer = reasonerWithTBox.equivalentOrSubsuming(subsumee).stream()
                    						.filter(con -> !reasonerWithTBox.equivalentToOWLThing(con) && 
                    										!(con instanceof OWLObjectIntersectionOf)).findAny().get();
                    	 * The one below only considers the top-level conjuncts of the subsumee, but not the whole subsumers of it.
                    	 * The code above should enumerate more possible atoms that can be added to the repair type than the one below.
                    	 */
                    	
                    	
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
    /* If I see it correctly, then this method returns the set of all minimal repair types for 'ind' that cover both
    *  'repairType' and 'successorSet'.  Why not call it 'getMinimalRepairTypes' or 'getMinimalRepairTypesCovering'
    *  then? */
    /* If 'successorSet' contains a concept equivalent to owl:thing with respect to the TBox, then this method must
    *  return the empty set, as then there does not exist any repair types for 'ind' that cover both 'repairType' and
    *  'successorSet' whatsoever.  See also my comment to the below method 'findCoveringPreTypes'. */
    public Set<RepairType> findCoveringRepairTypes(RepairType repairType, 
    		Set<OWLClassExpression> successorSet, OWLNamedIndividual ind) {

        // repairType should never be null
    	
    	// each candidate in the setOfCandidates cannot contain any atom that is equivalent to owl:Thing

        Set<Set<OWLClassExpression>> setOfCandidates =
                        findCoveringPreTypes(new HashSet<>(repairType.getClassExpressions()), successorSet);


        Set<RepairType> resultingSet = new HashSet<>();

        // premise satuation
        /* The below three lines are a bit confusing at first sight. */
        while (setOfCandidates.iterator().hasNext()) {
            Set<OWLClassExpression> candidate = setOfCandidates.iterator().next();
            setOfCandidates.remove(candidate);

            boolean isSaturated = true;

            // ignore candidates that contain atoms equivalent to TOP

            /* Why don't you process several atoms in one go?  Essentially, you always enlarge 'candidate' by exactly one
            * atom.   */
            for (OWLClassExpression atom : candidate) {

                /* It can happen that, in some branch of computation, the current 'candidate' contains an atom being
                *  equivalent to owl:Thing w.r.t. the TBox.  Then, the 'candidate' cannot be saturated to a repair type!
                *  However, due to the unusual implementation of the reasoner facade, these cases cannot be detected
                *  here.  As a quick and dirty fix, you must always explicitly test whether 'candidate' contains an atom
                *  that is equivalent to owl:Thing w.r.t. the TBox. */
                for (OWLClassExpression subsumee : reasonerWithTBox.equivalentOrSubsumedBy(atom)) {
                    if (! reasonerWithoutTBox.subsumedByAny(subsumee, candidate) && 
                    		reasonerWithTBox.instanceOf(ind, subsumee)) {

                        isSaturated = false;

                        /* The below variable should rather be named 'topLevelConjuncts' as it can contain several top
                        * level conjuncts. */
                        // TODO this might have made the wrong loop
                        Set<OWLClassExpression> subsumers = reasonerWithoutTBox.equivalentOrSubsuming(subsumee);

                        for (OWLClassExpression subsumer : subsumers) {
                        	if(!(subsumer instanceof OWLObjectIntersectionOf)
                        	        && !reasonerWithTBox.equivalentToOWLThing(subsumer)) {
                        		Set<OWLClassExpression> tempCandidate = new HashSet<>(candidate);
                                tempCandidate.add(subsumer);
                                setOfCandidates.add(tempCandidate);
                        	}
                        }
                    }
                }
            }
            if (isSaturated) {
                RepairType newType = newMinimisedRepairType(candidate);
                /* The test is superfluous. */
                if (!resultingSet.contains(newType)) resultingSet.add(newType);
            }
        }

        // make sure set is minimal
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

        if(successorSet.stream().anyMatch(reasonerWithTBox::equivalentToOWLThing))
            return Collections.emptySet();

        /* If 'successorSet' contains a concept that is equivalent to owl:thing w.r.t. the TBox, then there does not
        * exist any repair type that covers the union of 'type' and 'successorSet', i.e., this method should then return
        * the empty set, which it actually does not.*/
        /* You do not need to collect all elements in a set over which you then iterate.  You could do the same in a
        * cheaper way by replacing the for loop by
        * successorSet.stream()
        *       .filter(concept -> !reasonerWithTBox.equivalentToOWLThing(concept))
        *       .forEach(exp -> { <code in the body of the for loop> });
        * A similar comment applies to the for loop over 'setOfAtoms' below.*/
        for (OWLClassExpression exp : successorSet) {
        	
        	Set<Set<OWLClassExpression>> newSet = new HashSet<>();
        	/* Could it happen that you encounter an instance of OWLClassExpression that consists of several nested
        	* instances of OWLObjectIntersectionOf but that is an atom after flattening?*/
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
            queueSet.clear();
            queueSet.addAll(newSet);
        }

        return queueSet;
    }
}
