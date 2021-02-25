package de.tu_dresden.lat.abox_repairs.generator;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.parameters.Imports;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;

public class IQRepairGeneratorA extends RepairGenerator{
	private static Logger logger = LogManager.getLogger(IQRepairGenerator.class);
	
	private final Set<CopiedOWLIndividual> individualsInTheRepair = new HashSet<>();
	private Queue<CopiedOWLIndividual> queue = new LinkedList<>();
	private final Map<OWLNamedIndividual, RepairType> seedFunction;
	
	public IQRepairGeneratorA(OWLOntology inputOntology,
			Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
		
		super(inputOntology, inputSeedFunction);
		this.seedFunction = inputSeedFunction;
	}

	@Override
	protected void generateVariables() {
		// TODO Auto-generated method stub
		individualsInTheRepair.forEach(this::addToQueue);
		
		while(!queue.isEmpty()) {
			CopiedOWLIndividual copy = queue.poll();
			ontology.getObjectPropertyAssertionAxioms(copy.getIndividualInTheSaturation()).forEach(axiom -> {
				final OWLObjectProperty role = axiom.getProperty().getNamedProperty();
				final OWLNamedIndividual object = axiom.getObject().asOWLNamedIndividual();
				final Set<OWLClassExpression> successorSet = computeSuccessorSet(
						copy.getRepairType(), role, object);
				
				typeHandler.findCoveringRepairTypes(
					RepairType.empty(), successorSet, object).forEach(newType -> {
						final CopiedOWLIndividual newCopy = CopiedOWLIndividual.newAnonymousIndividual(object, newType);
						if(addNewIndividualToTheRepair(newCopy)) {
							repair.add(new CopiedOWLObjectPropertyAssertionAxiom(
									copy, role, newCopy).toAxiomInTheRepair());
							addToQueue(newCopy);
						}
					});
			});
				
			
		}
		
	}

	@Override
	protected void initialise() {
		
		
		try {
            repair = ontology.getOWLOntologyManager().createOntology();
            repair.add(ontology.getTBoxAxioms(Imports.INCLUDED));
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
		
		anonymousDetector = AnonymousVariableDetector.newInstance(true, Main.RepairVariant.IQ);
		inputObjectNames.stream()
				.filter(anonymousDetector::isNamed)
				.map(ind -> CopiedOWLIndividual.newNamedIndividual(ind,seedFunction.get(ind)))
				.forEach(this::addNewIndividualToTheRepair);
		
	}
	
	private boolean addNewIndividualToTheRepair(CopiedOWLIndividual copy) {
		if(individualsInTheRepair.add(copy)) {
			ontology.classAssertionAxioms(copy.getIndividualInTheSaturation())
					.map(axiom -> axiom.getClassExpression())
					.filter(concept -> !copy.getRepairType().getClassExpressions().contains(concept))
					.map(concept -> OWLManager.getOWLDataFactory().getOWLClassAssertionAxiom(concept, copy.getIndividualInTheRepair()))
					.forEach(repair::add);
			
			return true;
		}
		return false;
	}
	
	private void addToQueue(CopiedOWLIndividual copy) {
		if(!ontology.getObjectPropertyAssertionAxioms(copy.getIndividualInTheSaturation()).isEmpty()) {
			queue.offer(copy);
		}
	}
	
	@Override
	protected void generateMatrix() throws OWLOntologyCreationException {
		
	}
	
	@Override
    public int getNumberOfCollectedIndividuals() {
        return individualsInTheRepair.size();
    }
	
}
