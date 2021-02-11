package de.tu_dresden.lat.abox_repairs.repair_types;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
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
        this.reasonerWithoutTBox=reasonerWithoutTBox;
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
        
        for(OWLClassExpression exp:type.getClassExpressions()){
            if(newClasses.contains(exp)){
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
    
    public RepairType convertToRepairType(Set<OWLClassExpression> expSet) {
    	Random rand = new Random();
    	for(OWLClassExpression exp : expSet) {
    			Set<OWLClassExpression> setOfSubsumees = new HashSet<>(reasonerWithTBox.subsumees(exp));
    			System.out.println("set of subsumees wrt TBox of " + exp + " is " + reasonerWithTBox.subsumees(exp));
    			System.out.println("set of subsumees not wrt TBox of " + exp + " is " + reasonerWithoutTBox.subsumees(exp));
    			setOfSubsumees.addAll(reasonerWithoutTBox.subsumees(exp));
    			System.out.println("set of subsumees of " + exp + " is " + setOfSubsumees);
    			for (OWLClassExpression subConcept : setOfSubsumees) {
//    				System.out.println("subsumess " + subConcept);
    				if(subConcept != null) {
    					if (!expSet.stream().anyMatch(otherExp -> reasonerWithoutTBox.subsumedBy(subConcept, otherExp))) {
            				List<OWLClassExpression> listOfConcept = new LinkedList<>(subConcept.asConjunctSet());
            				int index = rand.nextInt(listOfConcept.size());
            				expSet.add(listOfConcept.get(index));
            			}
    				}
        		}
    	}
    	
    	return newMinimisedRepairType(expSet);
    	
    }
    
    
    public Set<RepairType> findCoveringRepairTypes(RepairType type, Set<OWLClassExpression> expSet) {
    	Set<Set<OWLClassExpression>> setOfCandidates = repairTypeCandidates(new HashSet<>(type.getClassExpressions()), expSet);
    	Set<RepairType> resultingSet = new HashSet<>();
    	
    	LinkedList<Set<OWLClassExpression>> queueOfCandidates = new LinkedList<>(setOfCandidates);
//    	System.out.println("Queue " + queueOfCandidates);
    	while(!queueOfCandidates.isEmpty()) {
    		Set<OWLClassExpression> candidate = queueOfCandidates.poll();
//    		System.out.println("candidate " + candidate);
    		boolean alreadySaturated = true;
    		for(OWLClassExpression concept : candidate) {
    			for(OWLClassExpression subsumee : reasonerWithTBox.subsumees(concept)) {
//    				System.out.println("subsumee for " + concept + " is " + subsumee);
    				if(subsumee != null) {
    					if(!candidate.stream().anyMatch(otherConcept -> 
    						reasonerWithoutTBox.subsumedBy(subsumee, otherConcept))) {
    						alreadySaturated = false;
    						Set<OWLClassExpression> topLevelConjunct = subsumee.asConjunctSet();
    						for(OWLClassExpression conjunct : topLevelConjunct) {
    							Set<OWLClassExpression> tempCandidate = new HashSet<>(candidate);
    							tempCandidate.add(conjunct);
    							queueOfCandidates.add(tempCandidate);
    						}
    						
    					}
    				}
    			}	
    		}
    		if(alreadySaturated) {
    			resultingSet.add(newMinimisedRepairType(candidate));
    		}
    	}   
//    	resultingSet.stream().forEach(res -> System.out.println("res " + res.getClassExpressions()));
    	return resultingSet;
  	
    }
    
    public Set<RepairType> computeManyRepairTypes(RepairType candidate) {
    	
    	return null;
    }

    /**
     * Return all types that are obtained from the given repair type by applying 
     * premise saturation with respect to the given class expression.
     * 
     * Assumption: the individual assigned the repair type is an instance of exp, and 
     * the repair type contains some class expression D s.t. the ontology entails 
     * exp SubClassOf D
     */
    private Set<Set<OWLClassExpression>> repairTypeCandidates(Set<OWLClassExpression> type, OWLClassExpression exp) {

    	Set<Set<OWLClassExpression>> result = new HashSet<>();

        for(OWLClassExpression subsumer: reasonerWithoutTBox.subsumers(exp)){
            if(!(subsumer instanceof OWLObjectIntersectionOf)){
                Set<OWLClassExpression> newType = new HashSet<>();
                newType.add(subsumer);
                result.add(newType);
            }
        }

        return result;
    }
    
    public Set<Set<OWLClassExpression>> repairTypeCandidates(Set<OWLClassExpression> type, Set<OWLClassExpression> expSet) {
    	
    	assert expSet.size()>0;
    	
    	Set<Set<OWLClassExpression>> candidates = new HashSet<>();
    	
    	if(expSet.size() == 1) {
    		Iterator<OWLClassExpression> ite = expSet.iterator();
    		return repairTypeCandidates(type, ite.next());
    	} else {
    		Iterator<OWLClassExpression> ite = expSet.iterator();
    		OWLClassExpression concept = ite.next();
    		Set<Set<OWLClassExpression>> resultSet = repairTypeCandidates(type, concept);
    		expSet.remove(concept);
    	
    		for(Set<OWLClassExpression> currentType : resultSet) {
    		
    			candidates.addAll(repairTypeCandidates(currentType, expSet));
    		}
    	}
    	return candidates;
    }
}
