package de.tu_dresden.lat.abox_repairs;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairTypeHandler;

public class RepairGenerator {
	protected OWLOntology ontology;
	protected OWLDataFactory factory;
	protected IRI iri;
	protected Map<OWLNamedIndividual, RepairType> seedFunction;
	protected Map<OWLNamedIndividual, Integer> individualCounter;
	protected Map<OWLNamedIndividual, OWLNamedIndividual> copyToOriginal;
	protected Map<OWLNamedIndividual, Set<OWLNamedIndividual>> originalToCopy;
	
	protected Set<OWLNamedIndividual> setOfOriginalIndividuals;
	
	protected Set<OWLNamedIndividual> setOfCollectedIndividuals;
	
//	private Queue<OWLNamedIndividual> orderedIndividuals;
	
	protected Queue<OWLNamedIndividual> queueOfIndividuals;
	
	protected ReasonerFacade reasonerWithTBox;
	protected ReasonerFacade reasonerWithoutTBox;
	
	protected RepairTypeHandler typeHandler;
	
	
	
	
	protected void matrixGenerator() throws OWLOntologyCreationException {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		IRI 	newIRI = iri;
		OWLOntology newOntology = man.loadOntology(newIRI);
		for(OWLNamedIndividual ind : setOfCollectedIndividuals) {
			OWLNamedIndividual originalInd = copyToOriginal.get(ind);
			
		}
	}
	
	public OWLOntology getOntology() {
		return ontology;
	}
}
