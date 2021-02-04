package de.tu_dresden.lat.abox_repairs;

import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;

public class RepairGenerator {
	
	private OWLOntology ontology;
	private OWLDataFactory factory;
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> seedFunction;
	
	private ReasonerFacade reasoner;
		
	public RepairGenerator(ReasonerFacade inputReasoner) {
		this.reasoner = inputReasoner;
	}
	
	public void repair(OWLOntology inputOntology,
			Map<OWLNamedIndividual, Set<OWLClassExpression>> inputSeedFunction) {
		this.ontology = inputOntology;
		this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		this.seedFunction = inputSeedFunction;
		
		ontology.individualsInSignature()
		.forEach(i -> process(i,ontology));
	}
	
	private void process(OWLNamedIndividual inputIndividual, OWLOntology inputOntology) {
		System.out.println("processing " + inputIndividual);
		for(OWLClassExpression exp:reasoner.instanceOf(inputIndividual)) {
			System.out.println("class 1 " + exp);
			if(exp instanceof OWLClass) {
				System.out.println("class 2 " + exp);
				if(seedFunction.get(inputIndividual) != null) {
					if(seedFunction.get(inputIndividual).contains(exp)) {
						OWLAxiom assertion = factory.getOWLClassAssertionAxiom(exp, inputIndividual);
						System.out.println("Assertion " + assertion);
						ontology.remove(assertion);
					}
				}
			}
			else if (exp instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) exp;
			}
		}
	}
	
	public OWLOntology getOntology() {
		return ontology;
	}
}
