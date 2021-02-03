package de.tu_dresden.lat.abox_repairs;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;


public class SeedFunctionHandler {
	// CHECK: check whether for which variables you may be able to minimise the skope where they are visible
	// If a variable is only used within one method, it should only be initialised there.
	// If a variable should not change its value once initialised, use the keyword "final"
	

	private Map<OWLNamedIndividual, Set<OWLClassExpression>> seedFunction;
	

	public Map<OWLNamedIndividual, Set<OWLClassExpression>> getSeedFunction(){
		return Collections.unmodifiableMap(seedFunction);
	}

	public Map<OWLNamedIndividual, Set<OWLClassExpression>> constructSeedFunction(
			Map<OWLNamedIndividual, Set<OWLClassExpression>> repairRequest) {
		Random rand = new Random();

		seedFunction = new HashMap<OWLNamedIndividual, Set<OWLClassExpression>>();
		Set<OWLNamedIndividual> setOfIndividuals = repairRequest.keySet();
		Iterator<OWLNamedIndividual> iteratorOfIndividuals = setOfIndividuals.iterator();
		while(iteratorOfIndividuals.hasNext()) {
			OWLNamedIndividual individual = iteratorOfIndividuals.next();
			Set<OWLClassExpression> setOfConcepts = repairRequest.get(individual);
			for(OWLClassExpression concept: setOfConcepts) {
				if(concept instanceof OWLObjectIntersectionOf) {
					if(seedFunction.containsKey(individual)) {
						Set<OWLClassExpression> topLevelConjuncts = concept.asConjunctSet();
						if(Collections.disjoint(seedFunction.get(individual), topLevelConjuncts)) {
							List<OWLClassExpression> topLevelConjunctList = new ArrayList<OWLClassExpression>(topLevelConjuncts);
							
							int randomIndex = rand.nextInt(topLevelConjunctList.size());
							OWLClassExpression conceptTemp = topLevelConjunctList.get(randomIndex);
							seedFunction.get(individual).add(conceptTemp);
							seedFunction.put(individual, seedFunction.get(individual));
						}
					}
					else {
						List<OWLClassExpression> topLevelConjunctList = new ArrayList<OWLClassExpression>(concept.asConjunctSet());
						
						int randomIndex = rand.nextInt(topLevelConjunctList.size());
						OWLClassExpression conceptTemp = topLevelConjunctList.get(randomIndex);
						Set<OWLClassExpression> setOfConceptsTemp = new HashSet<OWLClassExpression>();
						setOfConceptsTemp.add(conceptTemp);
						seedFunction.put(individual, setOfConceptsTemp);
					}
				}
				else {
					if(seedFunction.containsKey(individual)) {
						if(!seedFunction.get(individual).contains(concept)) {
							Set<OWLClassExpression> setOfConceptsTemp = seedFunction.get(individual);
							setOfConceptsTemp.add(concept);
							seedFunction.put(individual, setOfConceptsTemp);
						}
					}
					else {
						Set<OWLClassExpression> setOfConceptsTemp = new HashSet<OWLClassExpression>();
						setOfConceptsTemp.add(concept);
						seedFunction.put(individual, setOfConceptsTemp);
						
					}
				}
			}
		}
		return seedFunction;
		
		
	}
}
