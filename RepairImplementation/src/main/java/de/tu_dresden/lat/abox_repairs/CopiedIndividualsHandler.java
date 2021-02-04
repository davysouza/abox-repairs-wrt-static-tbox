package de.tu_dresden.lat.abox_repairs;

import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;


/**
 * 
 * 
 * @author Adrian Nuradiansyah
 *
 */

public class CopiedIndividualsHandler {
	
	private OWLOntology ontology;
	private OWLDataFactory factory;
//	private Map<OWLNamedIndividual, Set<OWLClassExpression>> seedFunction;
	

	public CopiedIndividualsHandler(OWLOntology inputOntology) {
		this.ontology = inputOntology;
		this.factory = inputOntology.getOWLOntologyManager().getOWLDataFactory();
//		this.seedFunction = inputSeedFunction;
	}
	
	public void copyIndividual(OWLNamedIndividual ind1, OWLNamedIndividual ind2) {
		
		// Copy Class Assertions containing ind1
		Set<OWLClassAssertionAxiom> classAssertion = ontology.getClassAssertionAxioms(ind1);
		for(OWLClassAssertionAxiom axiom: classAssertion) {
			OWLClassExpression concept = axiom.getClassExpression();
			OWLClassAssertionAxiom freshConceptAssertion = factory.getOWLClassAssertionAxiom(concept, ind2);
			ontology.addAxiom(freshConceptAssertion);
			
		}
		
		// Copy Role Assertions containing ind1 as the subject
		Set<OWLObjectPropertyAssertionAxiom> setOfRoleAssertions = ontology.getObjectPropertyAssertionAxioms(ind1);
		for(OWLObjectPropertyAssertionAxiom axiom: setOfRoleAssertions) {
			OWLNamedIndividual subject = (OWLNamedIndividual) axiom.getSubject();
			OWLNamedIndividual object = (OWLNamedIndividual) axiom.getObject();
			
			if(subject.equals(ind1) && !object.equals(ind1)) {
				OWLObjectPropertyAssertionAxiom freshRoleAssertion = factory.getOWLObjectPropertyAssertionAxiom(
						axiom.getProperty(), ind2, object);
				ontology.addAxiom(freshRoleAssertion);
			}
			else if (subject.equals(ind1) && object.equals(ind1)) {
				OWLObjectPropertyAssertionAxiom freshRoleAssertion = factory.getOWLObjectPropertyAssertionAxiom(
						axiom.getProperty(), ind2, ind2);
				ontology.addAxiom(freshRoleAssertion);
			}
		}
		
		// Copy Role Assertions containing ind1 as the object
		Set<OWLNamedIndividual> setOfIndividuals = ontology.getIndividualsInSignature();
		for(OWLNamedIndividual ind: setOfIndividuals) {
			if(!ind.equals(ind1)) {
				Set<OWLObjectPropertyAssertionAxiom> tempSetOfRoleAssertions = ontology.getObjectPropertyAssertionAxioms(ind);
				for(OWLObjectPropertyAssertionAxiom axiom: tempSetOfRoleAssertions) {
					OWLNamedIndividual object = (OWLNamedIndividual) axiom.getObject();
					if(object.equals(ind1)) {
						OWLObjectPropertyAssertionAxiom freshRoleAssertion = factory.getOWLObjectPropertyAssertionAxiom(
								axiom.getProperty(), axiom.getSubject(), ind2);
						ontology.addAxiom(freshRoleAssertion);
					}
				}
			}
			
		}
	}
	
	public void adjustSeedFunction() {
		
	}
	
	public OWLOntology getOntology() {
		return ontology;
	}
	
//	public Map<OWLNamedIndividual, Set<OWLClassExpression>> getSeedFunction() {
//		return seedFunction;
//	}
	
}
