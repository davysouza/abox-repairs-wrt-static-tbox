package de.tu_dresden.lat.abox_repairs.repair_types;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

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
    
    public boolean isPremiseSaturated(Set<OWLClassExpression> expSet) {
    	
    	for(OWLClassExpression atom : expSet ) {
    		Set<OWLClassExpression> setOfSubsumees = new HashSet<>(reasonerWithTBox.subsumees(atom));
    		for (OWLClassExpression subsumee : setOfSubsumees) {
    			if(subsumee != null && !expSet.stream().anyMatch(exp -> reasonerWithTBox.subsumedBy(subsumee, exp))) {
    				return false;
    			}
    		}
    	}
    	
    	return true;
    }

	/**
	 * Returns some repair type that contains the given set of class expressions.
	 *
	 * Non-determinism is resolved using a random number generator.
	 */
	public RepairType convertToRandomRepairType(Set<OWLClassExpression> expSet) {
    	logger.debug(expSet);
    	Random rand = new Random();
    	
    	Set<OWLClassExpression> resultingSet = new HashSet<>(expSet);
    		for(OWLClassExpression exp : expSet) {
    			Set<OWLClassExpression> setOfSubsumees = new HashSet<>(reasonerWithTBox.equivalentOrSubsumedBy(exp));

//    			setOfSubsumees.addAll(reasonerWithoutTBox.subsumees(exp));
    			logger.debug("Size " + setOfSubsumees.size());
    			logger.debug("Set of subsumees " + setOfSubsumees);
    			for (OWLClassExpression subConcept : setOfSubsumees) {
    				logger.debug("subconcept " + subConcept);
					if (subConcept != null && !expSet.stream().anyMatch(otherExp -> reasonerWithoutTBox.subsumedBy(subConcept, otherExp))) {
	    				List<OWLClassExpression> listOfConcept = new LinkedList<>(subConcept.asConjunctSet());
	    				int index = rand.nextInt(listOfConcept.size());
	    				logger.debug("chosen Concept " + listOfConcept.get(index));
	    				resultingSet.add(listOfConcept.get(index));
	    			}
    				
        		}
    		}
    
    	
    	
    	return newMinimisedRepairType(resultingSet);
    	
    }
    
    
    public Set<RepairType> findCoveringRepairTypes(RepairType type, Set<OWLClassExpression> expSet) {
    	
    	
    	Set<Set<OWLClassExpression>> setOfCandidates = 
    			type != null? 
    					findRepairTypeCandidates(new HashSet<>(type.getClassExpressions()), expSet) :
    					findRepairTypeCandidates(new HashSet<>(), expSet);
    	
    	Set<RepairType> resultingSet = new HashSet<>();
    	
    	LinkedList<Set<OWLClassExpression>> queueOfCandidates = new LinkedList<>(setOfCandidates);

    	while(!queueOfCandidates.isEmpty()) {
    		Set<OWLClassExpression> candidate = queueOfCandidates.poll();

    		boolean alreadySaturated = true;
    		for(OWLClassExpression concept : candidate) {
    			for(OWLClassExpression subsumee : reasonerWithTBox.equivalentOrSubsumedBy(concept)) {   				
					if(subsumee != null && !candidate.stream().anyMatch(otherConcept -> 
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
    		if(alreadySaturated) {
    			resultingSet.add(newMinimisedRepairType(candidate));
    		}
    	}   

    	return resultingSet;
  	
    }
    
//    public Set<RepairType> computeManyRepairTypes(RepairType candidate) {
//    	
//    	return null;
//    }

    /**
     * Return all types that are obtained from the given repair type by applying 
     * premise saturation with respect to the given class expression.
     * 
     * Assumption: the individual assigned the repair type is an instance of exp, and 
     * the repair type contains some class expression D s.t. the ontology entails 
     * exp SubClassOf D
     */
    private Set<Set<OWLClassExpression>> findRepairTypeCandidates(Set<OWLClassExpression> type, OWLClassExpression exp) {

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
    
    public Set<Set<OWLClassExpression>> findRepairTypeCandidates(Set<OWLClassExpression> type, Set<OWLClassExpression> expSet) {
    	
    	assert expSet.size()>0;
    	
    	Set<Set<OWLClassExpression>> candidates = new HashSet<>();
    	
    	if(expSet.size() == 1) {
    		Iterator<OWLClassExpression> ite = expSet.iterator();
    		return findRepairTypeCandidates(type, ite.next());
    	} else {
    		Iterator<OWLClassExpression> ite = expSet.iterator();
    		OWLClassExpression concept = ite.next();
    		Set<Set<OWLClassExpression>> resultSet = findRepairTypeCandidates(type, concept);
    		expSet.remove(concept);
    	
    		for(Set<OWLClassExpression> currentType : resultSet) {
    		
    			candidates.addAll(findRepairTypeCandidates(currentType, expSet));
    		}
    	}
    	return candidates;
    }
}
