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
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;

abstract public class RepairGenerator {


	private static Logger logger = LogManager.getLogger(IQRepairGenerator.class);

	protected OWLOntology ontology;
	protected OWLDataFactory factory;
	protected IRI iri;
	protected Map<OWLNamedIndividual, RepairType> seedFunction;
	protected Map<OWLNamedIndividual, Integer> individualCounter;
	protected Map<OWLNamedIndividual, OWLNamedIndividual> copyToObject;
	protected Map<OWLNamedIndividual, Set<OWLNamedIndividual>> objectToCopies;
	
	// the set of all individuals contained in the saturation
	protected Set<OWLNamedIndividual> setOfSaturatedIndividuals;
	
	// the set of all individuals that are needed for constructing the repair
	protected Set<OWLNamedIndividual> setOfCollectedIndividuals;

	
	protected ReasonerFacade reasonerWithTBox;
	protected ReasonerFacade reasonerWithoutTBox;
	
	protected RepairTypeHandler typeHandler;
	
	protected OWLOntology newOntology;
	
	protected AnonymousVariableDetector anonymousDetector;

	/* Methods usually have action verbs or action phrases as names, cf.
	 *  https://docs.oracle.com/javase/tutorial/java/javaOO/methods.html */
	protected abstract void generatingVariables();

	/* Methods usually have action verbs or action phrases as names, cf.
	 *  https://docs.oracle.com/javase/tutorial/java/javaOO/methods.html */
	protected abstract void initialisation();
	
	


	

	public RepairGenerator(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		
		this.ontology = inputOntology;
		this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		this.seedFunction = inputSeedFunction;
		
		this.setOfSaturatedIndividuals  = ontology.getIndividualsInSignature();

		this.copyToObject = new HashMap<>();
		this.objectToCopies = new HashMap<>();
		this.individualCounter = new HashMap<>();
		
		// Initializing originalToCopy, and vice versa
		for(OWLNamedIndividual originalIndividual : setOfSaturatedIndividuals) {
			Set<OWLNamedIndividual> initSet = new HashSet<OWLNamedIndividual>();
			initSet.add(originalIndividual);
			individualCounter.put(originalIndividual, 0);
			objectToCopies.put(originalIndividual, initSet);
			copyToObject.put(originalIndividual, originalIndividual);
		}

		Optional<IRI> opt = ontology.getOntologyID().getOntologyIRI();
		this.iri = opt.get();
	}
	
	public void setReasoner(ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox) {
		this.reasonerWithTBox = reasonerWithTBox;
		this.reasonerWithoutTBox = reasonerWithoutTBox;
		this.typeHandler = new RepairTypeHandler(reasonerWithTBox, reasonerWithoutTBox);
	}
	
	/**
	 * Performs a stepwise rule to compute a repair
	 * @throws OWLOntologyCreationException
	 */
	public void repair() throws OWLOntologyCreationException {

		initialisation();

		long startTimeVariables = System.nanoTime();

		generatingVariables();

		double timeVariables = (double)(System.nanoTime() - startTimeVariables)/1_000_000_000;

		logger.info("Time for generating variables: " + timeVariables);
		logger.info("Variables introduced: "+setOfCollectedIndividuals.size());

		logger.debug("\nAfter generating necessary variables");
		for(OWLNamedIndividual ind : setOfCollectedIndividuals) {
			logger.debug("- " + ind);
			if(seedFunction.get(ind)!= null) {
				logger.debug(seedFunction.get(ind).getClassExpressions());
				logger.debug("");
			}

		}

		long startTimeMatrix = System.nanoTime();

		generatingMatrix();

		double timeMatrix = (double)(System.nanoTime() - startTimeMatrix)/1_000_000_000;

		logger.info("Time for generating Matrix: " + timeMatrix);

		logger.debug("\nAfter generating matrix");
		newOntology.axioms().forEach(ax -> logger.debug(ax));
	}
	
	/**
	 * Generating the matrix of the repair
	 * @throws OWLOntologyCreationException
	 */

	/* Methods usually have action verbs or action phrases as names, cf.
	*  https://docs.oracle.com/javase/tutorial/java/javaOO/methods.html */
	protected void generatingMatrix() throws OWLOntologyCreationException {
		
		reasonerWithTBox.cleanOntology();
	// TODO: bad code design
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		
		newOntology = man.createOntology();

		newOntology.add(ontology.getTBoxAxioms(Imports.INCLUDED));


		for(OWLAxiom ax: ontology.getABoxAxioms(Imports.INCLUDED)) {
			
			if(ax instanceof OWLClassAssertionAxiom) {
				OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom) ax;
				OWLNamedIndividual originalInd = (OWLNamedIndividual) classAssertion.getIndividual();
				
				for(OWLNamedIndividual copyInd : objectToCopies.get(originalInd)) {
					if(seedFunction.get(copyInd) == null || !seedFunction.get(copyInd).getClassExpressions()
							.contains(classAssertion.getClassExpression())) {
						OWLClassAssertionAxiom newAxiom = factory
									.getOWLClassAssertionAxiom(classAssertion.getClassExpression(), copyInd);
						newOntology.add(newAxiom);
						logger.debug("New Class Assertion " + newAxiom);
					}
				}
			}
			
			else if(ax instanceof OWLObjectPropertyAssertionAxiom) {
				OWLObjectPropertyAssertionAxiom roleAssertion = (OWLObjectPropertyAssertionAxiom) ax;
				OWLObjectProperty role = (OWLObjectProperty) roleAssertion.getProperty();
				OWLNamedIndividual originalSubject = (OWLNamedIndividual) roleAssertion.getSubject();
				OWLNamedIndividual originalObject = (OWLNamedIndividual) roleAssertion.getObject();
				
				for(OWLNamedIndividual copySubject : objectToCopies.get(originalSubject)) {
					for(OWLNamedIndividual copyObject : objectToCopies.get(originalObject)) {
						
						if(seedFunction.get(copySubject) == null || seedFunction.get(copySubject).getClassExpressions().isEmpty()) {
							OWLObjectPropertyAssertionAxiom newAxiom = factory
									.getOWLObjectPropertyAssertionAxiom(role, copySubject, copyObject);
							newOntology.add(newAxiom);
							
							logger.debug("New Role Assertion" + newAxiom);
						}
						else {
							RepairType type1 = seedFunction.get(copySubject);
							Set<OWLClassExpression> successorSet = computeSuccessorSet(
									type1, role, originalObject);
							if(successorSet.isEmpty()) {
								OWLObjectPropertyAssertionAxiom newAxiom = factory
										.getOWLObjectPropertyAssertionAxiom(role, copySubject, copyObject);
								newOntology.add(newAxiom);
								
								logger.debug("New Role Assertion " + newAxiom);
							} 
							else  {
								RepairType type2 = seedFunction.get(copyObject);
								if(type2 != null && reasonerWithoutTBox.isCovered(successorSet, type2.getClassExpressions())) {
									OWLObjectPropertyAssertionAxiom newAxiom = factory
											.getOWLObjectPropertyAssertionAxiom(role, copySubject, copyObject);
									newOntology.add(newAxiom);
									
									logger.debug("New Role Assertion " + newAxiom);
								}
							}
						}
					}
				}
			}
		}
		

	}
	
	/**
	 * Take the set of all fillers of existential restrictions with the role name r 
	 * that occur in the repair type K such that 
	 * the individual u is an instance of each filler w.r.t. the saturation
	 * 
	 * @param Repair type k
	 * @param Role name r
	 * @param individual u
	 * @return
	 */
	protected Set<OWLClassExpression> computeSuccessorSet(RepairType k, OWLObjectProperty r, OWLNamedIndividual u) {
		Set<OWLClassExpression> set = new HashSet<>();
		for(OWLClassExpression concept : k.getClassExpressions()) {
			if(concept instanceof OWLObjectSomeValuesFrom &&
				((OWLObjectSomeValuesFrom) concept).getProperty().equals(r) && 
				reasonerWithTBox.instanceOf(u, ((OWLObjectSomeValuesFrom) concept).getFiller())) {
					set.add(((OWLObjectSomeValuesFrom) concept).getFiller());
			}
		}
		
		return set;
	}
	
	/**
	 * Make a copy of an individual with repair type K
	 * @param ind
	 * @param k
	 * @return
	 */
	protected OWLNamedIndividual createCopy(OWLNamedIndividual ind, RepairType k) {
		individualCounter.put(ind, individualCounter.get(ind) + 1);
		OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(
				 ind.getIRI().getFragment() + 
				individualCounter.get(ind));
		
		seedFunction.put(freshIndividual, k);
		copyToObject.put(freshIndividual, ind);
		objectToCopies.get(ind).add(freshIndividual);
		
		setOfCollectedIndividuals.add(freshIndividual);
		
		return freshIndividual;
	}
	
	
	
	public OWLOntology getRepair() {
		
		return newOntology;
	}
	
	public int getNumberOfCollectedIndividuals() {
		return setOfCollectedIndividuals.size();
	}
}
