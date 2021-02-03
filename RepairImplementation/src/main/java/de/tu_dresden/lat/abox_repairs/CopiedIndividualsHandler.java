package de.tu_dresden.lat.abox_repairs;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;


/**
 * 
 * 
 * @author Adrian Nuradiansyah
 *
 */

public class CopiedIndividualsHandler {
	
	private OWLOntology ontology;
	private OWLDataFactory factory;
	
//	public CopiedIndividualsHandler(OWLOntologyManager inputManager, 
//			OWLOntology inputOntology, Map<OWLNamedIndividual, Set<OWLClassExpression>> inputSeedFunction) {
//		
//		this.ontology = inputOntology;
//		this.seedFunction=inputSeedFunction;
//		this.individualCounter = new HashMap<>();
//		this.manager = inputManager;
//		
//		Optional<IRI> option = ontology.getOntologyID().getOntologyIRI();
//		this.iri = option.get();
//		this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();
//		Set<OWLNamedIndividual> setOfIndividuals = ontology.getIndividualsInSignature();
//		for(OWLNamedIndividual individual: setOfIndividuals) {
//			individualCounter.put(individual, 1);
//			OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(iri + "#" + individual.getIRI().getFragment() + 
//					individualCounter.get(individual));
//			copyObject(individual,freshIndividual);
//		}
//		
//		Set<OWLNamedIndividual> setOfIndividuals2 = ontology.getIndividualsInSignature();
//		for(OWLNamedIndividual individual: setOfIndividuals2) {
//			System.out.println("Individual " + individual);
//			Set<OWLClassAssertionAxiom> ocaa = ontology.getClassAssertionAxioms(individual);
//			System.out.println(ocaa);
//			Set<OWLObjectPropertyAssertionAxiom> oopaa = ontology.getObjectPropertyAssertionAxioms(individual);
//			System.out.println(oopaa);
//		}
//		
//	}
	
	public CopiedIndividualsHandler(OWLOntology inputOntology, OWLDataFactory inputFactory) {
		this.ontology = inputOntology;
		this.factory = inputFactory;
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
	
	public OWLOntology getOntology() {
		return ontology;
	}
	
	
}
