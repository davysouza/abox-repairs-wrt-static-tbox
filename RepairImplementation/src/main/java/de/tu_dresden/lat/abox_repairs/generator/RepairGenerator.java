package de.tu_dresden.lat.abox_repairs.generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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


import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairTypeHandler;

abstract public class RepairGenerator {


	private static Logger logger = LogManager.getLogger(IQRepairGenerator.class);

	protected OWLOntology ontology;
	protected OWLDataFactory factory;
	protected IRI iri;
	protected Map<OWLNamedIndividual, RepairType> seedFunction;
	protected Map<OWLNamedIndividual, Integer> individualCounter;
	protected Map<OWLNamedIndividual, OWLNamedIndividual> copyToOriginal;
	protected Map<OWLNamedIndividual, Set<OWLNamedIndividual>> originalToCopy;
	
	protected Set<OWLNamedIndividual> setOfOriginalIndividuals;
	
	protected Set<OWLNamedIndividual> setOfCollectedIndividuals;

	
	protected ReasonerFacade reasonerWithTBox;
	protected ReasonerFacade reasonerWithoutTBox;
	
	protected RepairTypeHandler typeHandler;
	
	protected OWLOntology newOntology;
	
	protected abstract void generatingVariables();
	
	public abstract void repair() throws OWLOntologyCreationException;
	
	public RepairGenerator(OWLOntology inputOntology,
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
	
	
	
	protected void generatingMatrix() throws OWLOntologyCreationException {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		
		newOntology = man.createOntology();
//		for(OWLAxiom ax : ontology.getTBoxAxioms(Imports.INCLUDED)) {
//			logger.debug("axiom " + ax);
//		}
		
		newOntology.add(ontology.getTBoxAxioms(Imports.INCLUDED));
//		logger.debug("\nWhen building the matrix of the repair");

		for(OWLAxiom ax: ontology.getABoxAxioms(Imports.INCLUDED)) {
			
			if(ax instanceof OWLClassAssertionAxiom) {
				OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom) ax;
				OWLNamedIndividual originalInd = (OWLNamedIndividual) classAssertion.getIndividual();
				
				for(OWLNamedIndividual copyInd : originalToCopy.get(originalInd)) {
					if(seedFunction.get(copyInd) == null || !seedFunction.get(copyInd).getClassExpressions()
							.contains(classAssertion.getClassExpression())) {
						OWLClassAssertionAxiom newAxiom = factory
									.getOWLClassAssertionAxiom(classAssertion.getClassExpression(), copyInd);
						newOntology.add(newAxiom);
//						logger.debug("New Class Assertion " + newAxiom);
					}
				}
			}
			
			else if(ax instanceof OWLObjectPropertyAssertionAxiom) {
				OWLObjectPropertyAssertionAxiom roleAssertion = (OWLObjectPropertyAssertionAxiom) ax;
				OWLObjectProperty role = (OWLObjectProperty) roleAssertion.getProperty();
				OWLNamedIndividual originalSubject = (OWLNamedIndividual) roleAssertion.getSubject();
				OWLNamedIndividual originalObject = (OWLNamedIndividual) roleAssertion.getObject();
				
				for(OWLNamedIndividual copySubject : originalToCopy.get(originalSubject)) {
					for(OWLNamedIndividual copyObject : originalToCopy.get(originalObject)) {
						
						if(seedFunction.get(copySubject) == null || seedFunction.get(copySubject).getClassExpressions().isEmpty()) {
							OWLObjectPropertyAssertionAxiom newAxiom = factory
									.getOWLObjectPropertyAssertionAxiom(role, copySubject, copyObject);
							newOntology.add(newAxiom);
							
//							logger.debug("New Role Assertion" + newAxiom);
						}
						else {
							RepairType type1 = seedFunction.get(copySubject);
							Set<OWLClassExpression> successorSet = computeSuccessorSet(
									type1, role, originalObject);
							if(successorSet.isEmpty()) {
								OWLObjectPropertyAssertionAxiom newAxiom = factory
										.getOWLObjectPropertyAssertionAxiom(role, copySubject, copyObject);
								newOntology.add(newAxiom);
								
//								logger.debug("New Role Assertion " + newAxiom);
							} 
							else {
								RepairType type2 = seedFunction.get(copyObject);
								if(type2 != null && reasonerWithoutTBox.isCovered(successorSet, type2.getClassExpressions())) {
									OWLObjectPropertyAssertionAxiom newAxiom = factory
											.getOWLObjectPropertyAssertionAxiom(role, copySubject, copyObject);
									newOntology.add(newAxiom);
									
//									logger.debug("New Role Assertion " + newAxiom);
								}
							}
						}
					}
				}
			}
		}
		

	}
	
	protected Set<OWLClassExpression> computeSuccessorSet(RepairType inputType, OWLObjectProperty inputRole, OWLNamedIndividual ind) {
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
	
	
	protected abstract void makeCopy(OWLNamedIndividual ind, RepairType typ);
	
	
	public OWLOntology getRepair() {
		
		return newOntology;
	}
}
