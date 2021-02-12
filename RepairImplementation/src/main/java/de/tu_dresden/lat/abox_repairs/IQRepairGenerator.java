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
import org.checkerframework.checker.units.qual.m;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
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

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairTypeHandler;

public class IQRepairGenerator {
	private OWLOntology ontology;
	private OWLDataFactory factory;
	private IRI iri;
	private Map<OWLNamedIndividual, RepairType> seedFunction;
	private Map<OWLNamedIndividual, Integer> individualCounter;
	private Map<OWLNamedIndividual, OWLNamedIndividual> copyToOriginal;
	private Map<OWLNamedIndividual, Set<OWLNamedIndividual>> originalToCopy;
	
	private Set<OWLNamedIndividual> setOfOriginalIndividuals;
	
	private Set<OWLNamedIndividual> setOfCollectedIndividuals;
	
	private Queue<OWLNamedIndividual> queueOfIndividuals;
	
	private ReasonerFacade reasonerWithTBox;
	private ReasonerFacade reasonerWithoutTBox;
	
	private RepairTypeHandler typeHandler;
	

	
	private OWLOntology newOntology;
	
	public IQRepairGenerator(OWLOntology inputOntology,
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
	
	public void IQRepair() throws OWLOntologyCreationException {
		
		iqVariablesGenerator();
//		System.out.println("After generating necessary variables");
//		for(OWLNamedIndividual ind : setOfCollectedIndividuals) {
//			System.out.println("- " + ind);
//			if(seedFunction.get(ind)!= null) {
//				System.out.println(seedFunction.get(ind).getClassExpressions());
//				System.out.println();
//			}
//			
//		}
		
		iqMatrixGenerator();
//		System.out.println("\nAfter building the matrix");
//		newOntology.axioms().forEach(ax -> System.out.println("- " + ax.toString()));
		
		
//		ontology = newOntology;
		
	}
	
	
	private void iqVariablesGenerator() {
		queueOfIndividuals = new PriorityQueue<>(setOfCollectedIndividuals);

		while(!queueOfIndividuals.isEmpty()) {
			OWLNamedIndividual individual = queueOfIndividuals.poll();
			
			OWLNamedIndividual originalIndividual = setOfOriginalIndividuals.contains(individual) ?
					individual : copyToOriginal.get(individual);
			Set<OWLObjectPropertyAssertionAxiom> setOfRoleAssertions = ontology
					.getObjectPropertyAssertionAxioms(originalIndividual);
			for(OWLObjectPropertyAssertionAxiom roleAssertion : setOfRoleAssertions) {
				OWLNamedIndividual originalObject = (OWLNamedIndividual) roleAssertion.getObject();
				RepairType type = seedFunction.get(individual);
				if(type != null) {
					Set<OWLClassExpression> successorSet = computeSuccessorSet(
							type,(OWLObjectProperty) roleAssertion.getProperty(), 
							originalObject);
			
					RepairType emptyType = typeHandler.newMinimisedRepairType(new HashSet<>());
					if(successorSet.isEmpty()) {
						boolean individualAlreadyExists = false;
						for(OWLNamedIndividual copy : originalToCopy.get(originalObject)) {
							if (seedFunction.get(copy) == null || 
								(seedFunction.get(copy) != null && seedFunction.get(copy).equals(emptyType))) {
									individualAlreadyExists = true;
									break;
							}
						}
						
						if(!individualAlreadyExists) {
							makeCopy(originalObject, emptyType);
						}
					}
					else {
						Set<RepairType> setOfRepairTypes = typeHandler.findCoveringRepairTypes(emptyType, successorSet);
						if(!setOfRepairTypes.isEmpty()) {
							
							for(RepairType newType : setOfRepairTypes) {
								
								boolean individualAlreadyExists = false;
								for(OWLNamedIndividual copy : originalToCopy.get(originalObject)) {
									if(seedFunction.get(copy) != null) {
										if(seedFunction.get(copy).equals(newType)) {
											individualAlreadyExists = true;
											break;
										}
									}
									
								}
								if(!individualAlreadyExists) {
									makeCopy(originalObject, newType);
								}
								
							}
						}
					}
				}
			}
			
		}
		
	}
	
	private void iqMatrixGenerator() throws OWLOntologyCreationException {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		IRI 	newIRI =  IRI.create(new File("TestOntologies/Repair.owl"));
		
		newOntology = man.loadOntology(newIRI);
//		System.out.println("When building the matrix of IQ repair");
		newOntology.add(ontology.getTBoxAxioms(Imports.INCLUDED));
		for(OWLNamedIndividual ind : setOfCollectedIndividuals) {
			OWLNamedIndividual originalInd = copyToOriginal.get(ind);
			for(OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(originalInd)) {
				if(seedFunction.get(ind) == null || !seedFunction.get(ind).getClassExpressions().contains(ax.getClassExpression())) {
					OWLClassAssertionAxiom newAxiom = factory.getOWLClassAssertionAxiom(ax.getClassExpression(), ind);
					newOntology.add(newAxiom);
//					System.out.println("New " + newAxiom);
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
	
	public OWLOntology getRepair() {
		return newOntology;
	}
	
	private Set<OWLClassExpression> computeSuccessorSet(RepairType inputType, OWLObjectProperty inputRole, OWLNamedIndividual ind) {
		Set<OWLClassExpression> set = new HashSet<>();
		for(OWLClassExpression concept : inputType.getClassExpressions()) {
			if(concept instanceof OWLObjectSomeValuesFrom &&
				((OWLObjectSomeValuesFrom) concept).getProperty().equals(inputRole) && 
				reasonerWithTBox.instanceOf(ind, ((OWLObjectSomeValuesFrom) concept).getFiller())) {
					set.add(((OWLObjectSomeValuesFrom) concept).getFiller());
			}
		}
		
		return set;
	}
	
	private void makeCopy(OWLNamedIndividual ind, RepairType typ) {
		individualCounter.put(ind, individualCounter.get(ind) + 1);
		OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(
				 ind.getIRI().getFragment() + 
				individualCounter.get(ind));
		seedFunction.put(freshIndividual, typ);
		copyToOriginal.put(freshIndividual, ind);
		
		Set<OWLNamedIndividual> setOfCopies = originalToCopy.get(ind);
		setOfCopies.add(freshIndividual);
		originalToCopy.put(ind, setOfCopies);
		
		queueOfIndividuals.add(freshIndividual);
		
		setOfCollectedIndividuals.add(freshIndividual);
	}
}
