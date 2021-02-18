package de.tu_dresden.lat.abox_repairs.repair_types;

import java.util.*;
import java.util.stream.Collectors;

import de.tu_dresden.lat.abox_repairs.saturation.ChaseGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;


import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;

/**
 * Factory for repair types, including also functionality to modify repair types.
 *
 * @author Patrick Koopmann
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

    public boolean isPremiseSaturated(Set<OWLClassExpression> repairPreType) {

        for (OWLClassExpression atom : repairPreType) {
            Set<OWLClassExpression> setOfSubsumees = new HashSet<>(reasonerWithTBox.equivalentOrSubsumedBy(atom));
            for (OWLClassExpression subsumee : setOfSubsumees) {
                if (reasonerWithTBox.subsumedByAny(subsumee, repairPreType)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Given a repair pre-type and a random object, this method will this repair pre-type
     * to a repair type that has been premise-saturated
     *
     * @param hittingSet a repair pre-type that may not be premise-saturated yet
     * @param random  a random object that help choose a random repair type that will be returned
     * @return a random repair type
     */

   
    public RepairType convertToRandomRepairType(Set<OWLClassExpression> hittingSet, Random random) {

        Set<OWLClassExpression> resultingSet = new HashSet<>(hittingSet);

        boolean isSaturated = false;
        while (!isSaturated) {
            Set<OWLClassExpression> tempSet = new HashSet<>(resultingSet);
            isSaturated = true;
            for (OWLClassExpression exp : tempSet) {
                Set<OWLClassExpression> setOfSubsumees = reasonerWithTBox.equivalentOrSubsumedBy(exp);
                for (OWLClassExpression subConcept : setOfSubsumees) {          
                    if (!reasonerWithoutTBox.subsumedByAny(subConcept, tempSet)) {
                    	
                    	Set<OWLClassExpression> topLevelConjuncts = subConcept.asConjunctSet();
                        List<OWLClassExpression> listOfConcept = topLevelConjuncts.stream()
								.filter(con -> !reasonerWithTBox.equivalentToOWLThing(con))
								.collect(Collectors.toList());

                        // bit dirty, but ensures experiment can be reproduced
						// remark: expensive, but only used once when the seed functions are selected
                        listOfConcept.sort(Comparator.comparing(a -> a.toString()));

                        int index = random.nextInt(listOfConcept.size());

                        resultingSet.add(listOfConcept.get(index));

                        isSaturated = false;
                    }
                }
            }
        }

        return newMinimisedRepairType(resultingSet);
    }

    /**
     * The method receives a repair type that does not cover the given set Succ(K,r,u).
     * This
     *
     * @param repairType
     * @param successorSet
     * @return the set that contains all minimal repair types that cover the union of
     * the repair type and the set Succ(K,ru)
     */

    public Set<RepairType> findCoveringRepairTypes(RepairType repairType, Set<OWLClassExpression> successorSet) {

        // if type is null, we are computing IQ repairs, otherwise, we are computing CQ repairs
        // see CQ/IQ construction rule in CADE-21 paper.

        Set<Set<OWLClassExpression>> setOfCandidates =
                repairType != null ?
                        findCoveringPreTypes(new HashSet<>(repairType.getClassExpressions()), successorSet) :
                        findCoveringPreTypes(new HashSet<>(), successorSet);

        Set<RepairType> resultingSet = new HashSet<>();


        while (setOfCandidates.iterator().hasNext()) {
            Set<OWLClassExpression> candidate = setOfCandidates.iterator().next();
            setOfCandidates.remove(candidate);

            boolean alreadySaturated = true;

            outerloop:
            for (OWLClassExpression concept : candidate) {

                for (OWLClassExpression subsumee : reasonerWithTBox.equivalentOrSubsumedBy(concept)) {
                    if (!candidate.stream().anyMatch(otherConcept ->
                            reasonerWithoutTBox.subsumedBy(subsumee, otherConcept))) {

                        alreadySaturated = false;

                        Set<OWLClassExpression> topLevelConjunct = subsumee.asConjunctSet();

                        for (OWLClassExpression conjunct : topLevelConjunct) {
                        	if(!reasonerWithTBox.equivalentToOWLThing(conjunct)) {
                        		Set<OWLClassExpression> tempCandidate = new HashSet<>(candidate);
                                tempCandidate.add(conjunct);
                                setOfCandidates.add(tempCandidate);
                        	}
                            
                        }

                        break outerloop;
                    }
                }
            }
            if (alreadySaturated) {
                RepairType newType = newMinimisedRepairType(candidate);
                if (!resultingSet.contains(newType)) resultingSet.add(newType);
            }


        }

        return resultingSet;


    }


	/**
	 * 
	 * @param type	a repair type
	 * 
	 * @param exp 	a class expression
	 * 
	 * @return
	 * the set that contains minimal repair pre-types that cover the union of 
	 * the given repair type and the class expression
	 */
    private Set<Set<OWLClassExpression>> findCoveringPreTypes(Set<OWLClassExpression> type, OWLClassExpression exp) {
    	
    	if(reasonerWithTBox.equivalentToOWLThing(exp)) {
    		return Collections.emptySet();
    	}
    	
        Set<Set<OWLClassExpression>> result = new HashSet<>();

        for (OWLClassExpression subsumer : reasonerWithoutTBox.equivalentOrSubsuming(exp)) {
            if (!(subsumer instanceof OWLObjectIntersectionOf)) {
                Set<OWLClassExpression> newType = new HashSet<>(type);
                newType.add(subsumer);
                result.add(newType);
            }
        }

        return result;
    }

    /**
     * @param type   a repair type
     * @param successorSet a set of class expressions
     * @return the set that contains minimal repair pre-types that cover the union of the two input sets
     */
    private Set<Set<OWLClassExpression>> findCoveringPreTypes(Set<OWLClassExpression> type, Set<OWLClassExpression> successorSet) {

        assert successorSet.size() > 0;


        Set<Set<OWLClassExpression>> queue = new HashSet<>();
        queue.add(type);

        for (OWLClassExpression exp : successorSet) {
        	if(!reasonerWithTBox.equivalentToOWLThing(exp)) {
        		Set<Set<OWLClassExpression>> tempSet = new HashSet<>();
                for (Set<OWLClassExpression> currentSet : queue) {
                    tempSet.addAll(findCoveringPreTypes(currentSet, exp));
                }
                queue.removeAll(queue);
                queue.addAll(tempSet);
        	}
        }

        return queue;



    	/*
    		REMARK BY PATRICK: I noticed that the commented code fixed the problem we talked about in the wrong way
    		but what I mean did not fix the problem at all. I fixed it in the way I would have solved it. I am currently
    		not sure anymore what this method is supposed to do, and therefore I cannot check whether the above approach
    		does the correct thing.


		Set<Set<OWLClassExpression>> candidates = new HashSet<>();
    	if(expSet.size() == 1) {
    		Iterator<OWLClassExpression> ite = expSet.iterator();
    		return findRepairTypeCandidates(type, ite.next());
    	} else {
    		Iterator<OWLClassExpression> ite = expSet.iterator();
    		OWLClassExpression concept = ite.next();
    		Set<Set<OWLClassExpression>> resultSet = findRepairTypeCandidates(type, concept);
    		Set<OWLClassExpression> expSetCopy = new HashSet<>(expSet);
    		expSetCopy.remove(concept);

    		for(Set<OWLClassExpression> currentType : resultSet) {

    			candidates.addAll(findRepairTypeCandidates(currentType, expSetCopy));
    			expSet.add(concept);
    		}

    	}
    	return candidates;
    	 */
    }
}
