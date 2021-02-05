package de.tu_dresden.lat.abox_repairs;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;


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
	
	public void setOntology(OWLOntology inputOntology) {
		this.ontology = inputOntology;
		this.factory = inputOntology.getOWLOntologyManager().getOWLDataFactory();
	}
	
	public void copyIndividual(OWLNamedIndividual ind1, OWLNamedIndividual ind2) {
		
		Stream<OWLAxiom> setOfAxioms = ontology.aboxAxioms(Imports.INCLUDED);
		setOfAxioms.forEach(
			ax -> {
				// Copy Class Assertions containing ind1
				if(ax instanceof OWLClassAssertionAxiom) {
					OWLClassAssertionAxiom ax2 = (OWLClassAssertionAxiom) ax;
					if(ax2.getIndividual().equals(ind1)) {
						OWLClassExpression concept = ax2.getClassExpression();
						OWLClassAssertionAxiom freshConceptAssertion = factory.getOWLClassAssertionAxiom(concept, ind2);
						ontology.addAxiom(freshConceptAssertion);
					}
				}
				// Copy Role Assertions containing ind1 
				else if (ax instanceof OWLObjectPropertyAssertionAxiom) {
					OWLObjectPropertyAssertionAxiom ax2 = (OWLObjectPropertyAssertionAxiom) ax;
					if(ax2.getSubject().equals(ind1) && !ax2.getObject().equals(ind1)) {
						OWLObjectPropertyAssertionAxiom freshRoleAssertion = factory.getOWLObjectPropertyAssertionAxiom(
								ax2.getProperty(), ind2, ax2.getObject());
						ontology.addAxiom(freshRoleAssertion);
					}
					
					if(ax2.getSubject().equals(ind1) && ax2.getObject().equals(ind1)) {
						OWLObjectPropertyAssertionAxiom freshRoleAssertion1 = factory.getOWLObjectPropertyAssertionAxiom(
								ax2.getProperty(), ind2, ind2);
						ontology.addAxiom(freshRoleAssertion1);
						
						OWLObjectPropertyAssertionAxiom freshRoleAssertion2 = factory.getOWLObjectPropertyAssertionAxiom(
								ax2.getProperty(), ind1, ind2);
						ontology.addAxiom(freshRoleAssertion2);
						
						OWLObjectPropertyAssertionAxiom freshRoleAssertion3 = factory.getOWLObjectPropertyAssertionAxiom(
								ax2.getProperty(), ind2, ind1);
						ontology.addAxiom(freshRoleAssertion3);
					}
					
					if(!ax2.getSubject().equals(ind1) && ax2.getObject().equals(ind1)) {
						OWLObjectPropertyAssertionAxiom freshRoleAssertion = factory.getOWLObjectPropertyAssertionAxiom(
								ax2.getProperty(), ax2.getSubject(), ind2);
						ontology.addAxiom(freshRoleAssertion);
					}
				}
			});
		

	}
	
	public OWLOntology getOntology() {
		return ontology;
	}
	
	
}
