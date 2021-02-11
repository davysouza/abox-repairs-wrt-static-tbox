package de.tu_dresden.lat.abox_repairs;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;

import com.google.common.base.Objects;
//import com.sun.tools.javac.util.Pair;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairTypeHandler;

public class CQRepairGenerator {
	private OWLOntology ontology;
	private OWLDataFactory factory;
	private IRI iri;
	private Map<OWLNamedIndividual, RepairType> seedFunction;
	private Map<OWLNamedIndividual, Integer> individualCounter;
	private Map<OWLNamedIndividual, OWLNamedIndividual> copyToOriginal;
	private Map<OWLNamedIndividual, Set<OWLNamedIndividual>> originalToCopy;
	
	private Set<OWLNamedIndividual> setOfOriginalIndividuals;
	
	private Set<OWLNamedIndividual> setOfCollectedIndividuals;
	
	
	private ReasonerFacade reasonerWithTBox;
	private ReasonerFacade reasonerWithoutTBox;
	
	private RepairTypeHandler typeHandler;
	
	private OWLOntology newOntology;
	
	
	public CQRepairGenerator(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		this.ontology = inputOntology;
		this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		this.seedFunction = inputSeedFunction;
		this.setOfOriginalIndividuals  = ontology.getIndividualsInSignature();
		this.setOfCollectedIndividuals = new HashSet<>(setOfOriginalIndividuals);
		this.copyToOriginal = new HashMap<>();
		this.originalToCopy = new HashMap<>();
		this.individualCounter = new HashMap<>();
		// Initializing originalToCopy 
		for(OWLNamedIndividual originalIndividual : setOfOriginalIndividuals) {
			Set<OWLNamedIndividual> initSet = new HashSet<OWLNamedIndividual>();
			initSet.add(originalIndividual);
			individualCounter.put(originalIndividual, 0);
			originalToCopy.put(originalIndividual, initSet);
			copyToOriginal.put(originalIndividual, originalIndividual);
		}

		Optional<IRI> opt = ontology.getOntologyID().getOntologyIRI();
		this.iri = opt.get();
	}
	
	public void setReasoner(ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox) {
		this.reasonerWithTBox = reasonerWithTBox;
		this.reasonerWithoutTBox = reasonerWithoutTBox;
		this.typeHandler = new RepairTypeHandler(reasonerWithTBox, reasonerWithoutTBox);
	}
	
	public void CQRepair() throws OWLOntologyCreationException {
		
		initialisation();
		
		cqVariablesGenerator();
		
		System.out.println("After generating necessary variables");
		for(OWLNamedIndividual ind : setOfCollectedIndividuals) {
			System.out.println("- " + ind);
			if(seedFunction.get(ind)!= null) {
				System.out.println(seedFunction.get(ind).getClassExpressions());
				System.out.println();
			}
			
		}
		
		
		cqMatrixGenerator();
		System.out.println("\nAfter building the matrix");
		newOntology.axioms().forEach(ax -> System.out.println("- " + ax.toString()));
		
		ontology = newOntology;
	}
	
	private void initialisation() {
//		Set<OWLNamedIndividual> setOfIndividuals = seedFunction.keySet();
	
		for(OWLNamedIndividual individual : setOfOriginalIndividuals) {
			
			if(seedFunction.get(individual) != null) {
				individualCounter.put(individual, individualCounter.get(individual) + 1);
				OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(
						individual.getIRI().getFragment() + (individualCounter.get(individual)));
				
				RepairType type = typeHandler.newMinimisedRepairType(new HashSet<>());
				seedFunction.put(freshIndividual, type);
				Set<OWLNamedIndividual> setOfCopies = originalToCopy.get(individual);
				setOfCopies.add(freshIndividual);
				originalToCopy.put(individual, setOfCopies);
				copyToOriginal.put(freshIndividual, individual);
				setOfCollectedIndividuals.add(freshIndividual);
			}
			
			else {
				RepairType type = typeHandler.newMinimisedRepairType(new HashSet<>());
				seedFunction.put(individual, type);
			}
			
		}
	}
	
	private void cqVariablesGenerator() {
		// Applying the repair rules exhaustively until no rule is applicable to each individual		
		Queue<Pair<OWLNamedIndividual, OWLNamedIndividual>> queueOfPairs = 
				new PriorityQueue<Pair<OWLNamedIndividual, OWLNamedIndividual>>();
		
		for(OWLNamedIndividual ind1 : setOfCollectedIndividuals) {
			for (OWLNamedIndividual ind2 : setOfCollectedIndividuals) {
				Pair<OWLNamedIndividual, OWLNamedIndividual> pair = Pair.of(ind1, ind2);
				queueOfPairs.add(pair);
			}
		}
		
		while(!queueOfPairs.isEmpty()) {
			Pair<OWLNamedIndividual, OWLNamedIndividual> p = queueOfPairs.poll();
			OWLNamedIndividual ind1 = p.getLeft();
			OWLNamedIndividual ind2 = p.getRight();
			
			OWLNamedIndividual originalInd1 = setOfOriginalIndividuals.contains(ind1)? ind1 : copyToOriginal.get(ind1);
			OWLNamedIndividual originalInd2 = setOfOriginalIndividuals.contains(ind2)? ind2 : copyToOriginal.get(ind2);
			
//			System.out.println("copy " + ind1 + " " + ind2);
//			System.out.println("original " + originalInd1 + " " + originalInd2);
			
			for(OWLObjectProperty role: ontology.getObjectPropertiesInSignature()) {
				OWLObjectPropertyAssertionAxiom roleAssertion = factory
						.getOWLObjectPropertyAssertionAxiom(role, originalInd1, originalInd2);
				if(ontology.axioms().anyMatch(ax -> Objects.equal(ax, roleAssertion) )) {
//					System.out.println("Role " + roleAssertion);
					RepairType type1 = seedFunction.get(ind1);
					RepairType type2 = seedFunction.get(ind2);
					Set<OWLClassExpression> successorSet = computeSuccessorSet(
							type1,role, originalInd2);
					if(type2 != null & !reasonerWithTBox.isCovered(successorSet, type2.getClassExpressions())) {
//						System.out.println("Successor " + successorSet);
//						Set<RepairType> setOfRepairTypes = typeHandler.premiseSaturate(type2, successorSet);
						Set<RepairType> setOfRepairTypes = typeHandler.findCoveringRepairTypes(type2, successorSet);
						if(!setOfRepairTypes.isEmpty()) {
//							System.out.println("SetRepairType " +setOfRepairTypes.size());
							
							boolean individualAlreadyExists = false;
							for(RepairType newType : setOfRepairTypes) {
								for(OWLNamedIndividual copy : originalToCopy.get(originalInd2)) {
									if(seedFunction.get(copy).equals(newType)) {
										individualAlreadyExists = true;
										break;
									}
								}
								if(!individualAlreadyExists) {
									individualCounter.put(originalInd2, individualCounter.get(originalInd2) + 1);
									OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(
											originalInd2.getIRI().getFragment() + 
											individualCounter.get(originalInd2));
									seedFunction.put(freshIndividual, newType);
									copyToOriginal.put(freshIndividual, originalInd2);
									
									Set<OWLNamedIndividual> setOfCopies = originalToCopy.get(originalInd2);
									setOfCopies.add(freshIndividual);
									originalToCopy.put(originalInd2, setOfCopies);
									
									for(OWLNamedIndividual ind : setOfCollectedIndividuals) {
										Pair<OWLNamedIndividual, OWLNamedIndividual> pair = Pair.of(ind, freshIndividual);
										queueOfPairs.add(pair);
									}
									
									setOfCollectedIndividuals.add(freshIndividual);
									
								}
							}
		
						}
					}
				}
			}
		}
	}
	
	private void cqMatrixGenerator() throws OWLOntologyCreationException {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		IRI 	newIRI =  IRI.create(new File("TestOntologies/Repair.owl"));
		
		newOntology = man.loadOntology(newIRI);
		for(OWLAxiom ax : ontology.getTBoxAxioms(Imports.INCLUDED)) {
			System.out.println("axiom " + ax);
		}
		
		newOntology.add(ontology.getTBoxAxioms(Imports.INCLUDED));
		System.out.println("\nWhen building the matrix of CQ repair");
		for(OWLNamedIndividual ind : setOfCollectedIndividuals) {
			OWLNamedIndividual originalInd = copyToOriginal.get(ind);
			for(OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(originalInd)) {
				if(seedFunction.get(ind) == null || !seedFunction.get(ind).getClassExpressions().contains(ax.getClassExpression())) {
					
					OWLClassAssertionAxiom newAxiom = factory.getOWLClassAssertionAxiom(ax.getClassExpression(), ind);
					newOntology.add(newAxiom);
					System.out.println("New " + newAxiom);
				}
			}
		}
		
		for(OWLNamedIndividual ind1 : setOfCollectedIndividuals) {
			OWLNamedIndividual originalInd1 = copyToOriginal.get(ind1);
			
			for(OWLNamedIndividual ind2 : setOfCollectedIndividuals) {
				OWLNamedIndividual originalInd2 = copyToOriginal.get(ind2);
				
				for(OWLObjectProperty role : ontology.getObjectPropertiesInSignature()) {
					OWLObjectPropertyAssertionAxiom roleAssertion = factory
							.getOWLObjectPropertyAssertionAxiom(role, originalInd1, originalInd2);
					if(ontology.axioms().anyMatch(ax -> Objects.equal(ax, roleAssertion) )) {
						if(seedFunction.get(ind1) == null || seedFunction.get(ind1).getClassExpressions().isEmpty()) {
							OWLObjectPropertyAssertionAxiom newAxiom = factory.getOWLObjectPropertyAssertionAxiom(role, ind1, ind2);
							newOntology.add(newAxiom);
						}
						else {
							RepairType type1 = seedFunction.get(ind1);
							Set<OWLClassExpression> successorSet = computeSuccessorSet(
									type1,role,originalInd2);
							if(successorSet.isEmpty()) {
								OWLObjectPropertyAssertionAxiom newAxiom = factory.getOWLObjectPropertyAssertionAxiom(role, ind1, ind2);
								newOntology.add(newAxiom);
							} 
							else {
								RepairType type2 = seedFunction.get(ind2);
								if(type2 != null && reasonerWithoutTBox.isCovered(successorSet, type2.getClassExpressions())) {
									OWLObjectPropertyAssertionAxiom newAxiom = factory.getOWLObjectPropertyAssertionAxiom(role, ind1, ind2);
									newOntology.add(newAxiom);
								}
							}
							
						}
					}
				}
			}
		}
		
	}
	
	public OWLOntology getOntology() {
		return ontology;
	}
	
	private Set<OWLClassExpression> computeSuccessorSet(RepairType inputType, OWLObjectProperty inputRole, OWLNamedIndividual ind) {
		Set<OWLClassExpression> set = new HashSet<>();
		for(OWLClassExpression concept : inputType.getClassExpressions()) {
			if(concept instanceof OWLObjectSomeValuesFrom) {
				if(((OWLObjectSomeValuesFrom) concept).getProperty().equals(inputRole) && 
						reasonerWithTBox.instanceOf(ind, ((OWLObjectSomeValuesFrom) concept).getFiller())) {
					set.add(((OWLObjectSomeValuesFrom) concept).getFiller());
				}
			}
		}
		
		return set;
	}
}
