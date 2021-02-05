package de.tu_dresden.lat.abox_repairs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairTypeHandler;

public class RepairGenerator {
	
	private OWLOntology ontology;
	private OWLDataFactory factory;
	private IRI iri;
	private Map<OWLNamedIndividual, RepairType> seedFunction;
	private Map<OWLNamedIndividual, Integer> individualCounter;
	private Map<OWLNamedIndividual, OWLNamedIndividual> copyToOriginal;
	private Map<OWLNamedIndividual, Set<OWLNamedIndividual>> originalToCopy;
	
	private Set<OWLNamedIndividual> setOfOriginalIndividuals;
	
	private Queue<OWLNamedIndividual> orderedIndividuals;
	
	private ReasonerFacade reasonerWithTBox;
	private ReasonerFacade reasonerWithoutTBox;
	
	private RepairTypeHandler typeHandler;
	
	private CopiedIndividualsHandler copyHandler;
	
	public RepairGenerator(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		this.ontology = inputOntology;
		this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		this.seedFunction = inputSeedFunction;
		this.setOfOriginalIndividuals  = ontology.getIndividualsInSignature();
		this.copyToOriginal = new HashMap<>();
		this.originalToCopy = new HashMap<>();
		this.individualCounter = new HashMap<>();
		// Initializing originalToCopy 
		for(OWLNamedIndividual oriIndividual : setOfOriginalIndividuals) {
			Set<OWLNamedIndividual> initSet = new HashSet<OWLNamedIndividual>();
			initSet.add(oriIndividual);
			individualCounter.put(oriIndividual, 0);
			originalToCopy.put(oriIndividual, initSet);
		}

		Optional<IRI> opt = ontology.getOntologyID().getOntologyIRI();
		this.iri = opt.get();
	}
	
	public void setReasoner(ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox) {
		this.reasonerWithTBox = reasonerWithTBox;
		this.reasonerWithoutTBox = reasonerWithoutTBox;
		this.typeHandler = new RepairTypeHandler(reasonerWithTBox, reasonerWithoutTBox);
	}
	
	public void repair() {
		
		
		
		// Renaming and Copying Individuals
		Set<OWLNamedIndividual> setOfIndividuals = seedFunction.keySet();
		
		copyHandler = new CopiedIndividualsHandler(ontology);
		for(OWLNamedIndividual individual : setOfIndividuals) {
			individualCounter.put(individual, individualCounter.get(individual) + 1);
			OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(
					iri + "#" + individual.getIRI().getFragment() + (individualCounter.get(individual)));
			
			copyToOriginal.put(freshIndividual, individual);
			Set<OWLNamedIndividual> tempSet = originalToCopy.get(individual);
			tempSet.add(freshIndividual);
			originalToCopy.put(individual, tempSet);
			copyHandler.copyIndividual(individual,freshIndividual);
		}
		ontology = copyHandler.getOntology();
		
		this.orderedIndividuals = new PriorityQueue<>(ontology.getIndividualsInSignature());
		
		System.out.println("Before Repairing:");
		ontology.aboxAxioms(Imports.INCLUDED).forEach(
				ax -> {System.out.println(ax);});
		
		
		// Applying the repair rules exhaustively until no rule is applicable to each individual
//		ontology.individualsInSignature()
//		.forEach(i -> process(i,ontology));
		
		while(!orderedIndividuals.isEmpty()) {
			OWLNamedIndividual i = orderedIndividuals.poll();
//			System.out.println(i);
			process(i);
		}
		System.out.println();
		System.out.println("After Repairing:");
		ontology.aboxAxioms(Imports.INCLUDED).forEach(
				ax -> {System.out.println(ax);});
		
	}
	
	private void process(OWLNamedIndividual inputIndividual) {
//		System.out.println("processing " + inputIndividual);
		
		if(seedFunction.get(inputIndividual) != null) {
			for(OWLClassExpression exp : seedFunction.get(inputIndividual).getClassExpressions()) {
				if(exp instanceof OWLClass) {
					OWLAxiom ax = factory.getOWLClassAssertionAxiom(exp, inputIndividual);
					if(ontology.aboxAxioms(Imports.INCLUDED).anyMatch(axiom -> axiom.equals(ax))) {
						ontology.remove(ax);
					}
				}
				else if (exp instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) exp;
					OWLClassExpression filler = some.getFiller();
					OWLObjectProperty roleName = (OWLObjectProperty) some.getProperty();
					for(OWLObjectPropertyAssertionAxiom roleAssertion: ontology.getObjectPropertyAssertionAxioms(inputIndividual)) {
						if(roleAssertion.getProperty().equals(roleName)) {
							OWLNamedIndividual object = (OWLNamedIndividual) roleAssertion.getObject();
							OWLNamedIndividual originalObject = setOfOriginalIndividuals.contains((object))?
									object :copyToOriginal.get(object);
							if(reasonerWithTBox.instanceOf(originalObject, filler)) {
								RepairType type = seedFunction.get(object);
								boolean check = false;
								if(type != null) {
									for(OWLClassExpression atom: type.getClassExpressions()) {
										if(reasonerWithoutTBox.subsumedBy(filler, atom)) {
											check = true;
											break;
										}
									}
								}
								
								if(type == null || check == false) {
									for(OWLClassExpression atom : filler.asConjunctSet()) {
										Set<OWLClassExpression> tempType = (type != null)? 
												new HashSet<>(type.getClassExpressions()) : new HashSet<>();
										tempType.add(atom);
//										System.out.println("hai " + originalObject + " " + tempType);
										RepairType newType = typeHandler.newMinimisedRepairType(tempType);
										boolean objectAlreadyExists = false;
										for(OWLNamedIndividual ind : originalToCopy.get(originalObject)) {
//											System.out.println("Who " + ind + " are you?");
											if(seedFunction.get(ind) != null && seedFunction.get(ind).equals(newType)) {
												objectAlreadyExists = true;
//												System.out.println("hola " + ind + " " + newType.getClassExpressions());
												break;
											}
										}
//										System.out.println(objectAlreadyExists);
										if(!objectAlreadyExists) {
											individualCounter.put(originalObject, individualCounter.get(originalObject) + 1);
											OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(
													iri + "#" + originalObject.getIRI().getFragment() + 
													individualCounter.get(originalObject));
//											System.out.println("Test " + freshIndividual + " " + newType.getClassExpressions());
											seedFunction.put(freshIndividual, newType);
											copyToOriginal.put(freshIndividual, originalObject);
											Set<OWLNamedIndividual> tempSet = originalToCopy.get(originalObject);
											tempSet.add(freshIndividual);
											originalToCopy.put(originalObject, tempSet);
											copyHandler.setOntology(ontology);
											copyHandler.copyIndividual(object,freshIndividual);
											ontology = copyHandler.getOntology();
											orderedIndividuals.add(freshIndividual);
										}
									}
									ontology.remove(roleAssertion);
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
}
