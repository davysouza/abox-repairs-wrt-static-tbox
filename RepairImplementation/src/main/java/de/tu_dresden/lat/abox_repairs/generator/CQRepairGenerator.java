package de.tu_dresden.lat.abox_repairs.generator;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.collect.HashMultimap;
import org.semanticweb.owlapi.model.*;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;

public class CQRepairGenerator extends RepairGenerator {

	private static Logger logger = LogManager.getLogger(CQRepairGenerator.class);

	//private Queue<Pair<OWLNamedIndividual, OWLNamedIndividual>> queueOfPairs;
	
	public CQRepairGenerator(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		
		super(inputOntology, inputSeedFunction);
	}
	
	@Override
	protected void initialise() {
		// Variables Intitialisation
		
		anonymousDetector = AnonymousVariableDetector.newInstance(true, Main.RepairVariant.CQ);

		/**
		 * The set of collected individuals for the repair will contain every named individual
		 */
		setOfCollectedIndividuals = inputObjectNames.stream()
				.filter(ind -> anonymousDetector.isNamed(ind))
				.collect(Collectors.toSet());

		/**
		 * plus one copy of every variable, and every named individual with nothing assigned by the initial seed function,
		 * which is assigned an empty repair type.
		 */
		for(OWLNamedIndividual objectName : inputObjectNames) {
			if(!objectToRepairType.containsKey(objectName) ||
//					anonymousDetector.isAnonymous(objectName) ||
				!objectToRepairType.get(objectName).getClassExpressions().isEmpty()) {
				createCopy(objectName,typeHandler.newMinimisedRepairType(new HashSet<>()));
				//individualCounter.put(objectName, individualCounter.get(objectName) + 1);
			}
		}
	}

	/**
	 * Assumption: all names in the triple occur in the input ontology.
	 * Returns all ways of replacing an individual name A in the triple by the corresponding copy
	 * (both individual names are replaced)
	 */
	private Set<RATriple> expandNamesToCopies(RATriple triple) {
		Set<RATriple> result = new HashSet<>();

		for (OWLNamedIndividual ind1 : objectToCopies.get(triple.getIndividual1()))

			// TODO here (and later): why is this check necessary?
			if (setOfCollectedIndividuals.contains(ind1))
				for (OWLNamedIndividual ind2 : objectToCopies.get(triple.getIndividual2()))
					if (setOfCollectedIndividuals.contains(ind2)) {
						assert objectToRepairType.containsKey(ind1) && objectToRepairType.containsKey(ind2);
						result.add(new RATriple(ind1, ind2, triple.getProperty()));
					}
		return result;
	}

	/* The below code is a bit difficult to understand, but I believe that the current version now does the job
	 *  correctly. */
	@Override
	protected void generateVariables() {

		// TODO the following queue contains pairs of individuals that may stand in a role relation
		Queue<RATriple> triplesToProcess = new LinkedList<RATriple>();

		Multimap<OWLIndividual, OWLObjectPropertyAssertionAxiom> asSubject = HashMultimap.create();
		Multimap<OWLIndividual, OWLObjectPropertyAssertionAxiom> asObject = HashMultimap.create();

		ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)
				.forEach(ra -> {
					RATriple triple = RATriple.fromAxiom(ra);
					Set<RATriple> expanded = expandNamesToCopies(triple);
					triplesToProcess.addAll(expanded);
					asSubject.put(ra.getSubject(),ra);
					asObject.put(ra.getObject(),ra);
				});

		/**
		 * Optimisation: iterate over role assertions, rather than all pairs.
		 * store also the role name
		 */
	/*	for(OWLNamedIndividual ind1 : setOfCollectedIndividuals) {
			for (OWLNamedIndividual ind2 : setOfCollectedIndividuals) {
				Pair<OWLNamedIndividual, OWLNamedIndividual> pair = Pair.of(ind1, ind2);
				queueOfPairs.add(pair);
			}
		}
	*/
		while(!triplesToProcess.isEmpty()) {
			RATriple p = triplesToProcess.poll();

			OWLNamedIndividual ind1 = p.getIndividual1();
			OWLNamedIndividual ind2 = p.getIndividual2();
			OWLObjectProperty role = p.getProperty();

			OWLNamedIndividual originalInd1 = inputObjectNames.contains(ind1)? ind1 : copyToObject.get(ind1);
			OWLNamedIndividual originalInd2 = inputObjectNames.contains(ind2)? ind2 : copyToObject.get(ind2);
			
			//for(OWLObjectProperty role: ontology.getObjectPropertiesInSignature()) {
				
			//	OWLObjectPropertyAssertionAxiom roleAssertion = factory
			//			.getOWLObjectPropertyAssertionAxiom(role, originalInd1, originalInd2);
				
			//	if(ontology.containsAxiom(roleAssertion)) {
					RepairType type1 = objectToRepairType.get(ind1);
					RepairType type2 = objectToRepairType.get(ind2);
					
					Set<OWLClassExpression> successorSet = computeSuccessorSet(
							type1,role, originalInd2);
					
					Set<OWLClassExpression> coveringSet = type2.getClassExpressions();
					
					if(!reasonerWithTBox.isCovered(successorSet, coveringSet)) {
						Set<RepairType> setOfRepairTypes = 
								typeHandler.findCoveringRepairTypes(type2, successorSet, originalInd2);
						
						for(RepairType newType : setOfRepairTypes) {
							// the following could/should be simplified by using a hashmap
							if(!objectToTypesWithCopies.get(originalInd2).contains(newType)){
								//objectToCopies.get(originalInd2).stream().anyMatch(copy -> newType.equals(objectToRepairType.get(copy)))) {
								OWLNamedIndividual copy = createCopy(originalInd2, newType);

								// add new triples to process with the copy as subject
								for(OWLObjectPropertyAssertionAxiom x:asSubject.get(originalInd2)) {
									for(OWLNamedIndividual objectCopy:objectToCopies.get(x.getObject())) {
										if(setOfCollectedIndividuals.contains(objectCopy))
											triplesToProcess.add(new RATriple(copy, objectCopy, (OWLObjectProperty) x.getProperty()));
									}
								}
								// add new triples to process with the copy as object
								for(OWLObjectPropertyAssertionAxiom x:asObject.get(originalInd2)) {
									for(OWLNamedIndividual subjectCopy:objectToCopies.get(x.getSubject())) {
										if(setOfCollectedIndividuals.contains(subjectCopy))
											triplesToProcess.add(new RATriple(subjectCopy, copy, (OWLObjectProperty) x.getProperty()));
									}
								}

								/*for(OWLNamedIndividual individual : setOfCollectedIndividuals) {
										Pair<OWLNamedIndividual, OWLNamedIndividual> pair1 = Pair.of(individual, copy);
										queueOfPairs.add(pair1);

										if(!individual.equals(copy)) {// otherwise pair2 would be identical to pair1
											Pair<OWLNamedIndividual, OWLNamedIndividual> pair2 = Pair.of(copy, individual);
											queueOfPairs.add(pair2);
										}
								}*/
							}
							

					}
				}
			//}
		}
	}
	
	
}
