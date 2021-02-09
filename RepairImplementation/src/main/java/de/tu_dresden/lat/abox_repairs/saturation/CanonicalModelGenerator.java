package de.tu_dresden.lat.abox_repairs.saturation;

import java.util.HashMap;
import java.util.Map;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;

/**
 * @author Patrick Koopmann
 */
public class CanonicalModelGenerator implements ABoxSaturator {

	private final ReasonerFacade reasoner;

	private Map<OWLClassExpression, OWLNamedIndividual> class2ind;

	private OWLDataFactory factory;
	
	private OWLOntology ontology;


	public CanonicalModelGenerator(ReasonerFacade reasoner) {
		this.reasoner = reasoner;
	}

	public void saturate(OWLOntology ontology) {
		
		this.ontology = ontology;
		factory = ontology.getOWLOntologyManager().getOWLDataFactory();

		class2ind = new HashMap<>();

		System.out.println("Anonymus individuals: "+ontology.getAnonymousIndividuals());

		ontology.individualsInSignature()
			.forEach(i -> process(i,ontology));
	}

	private void process(OWLNamedIndividual ind, OWLOntology ontology) {
		//System.out.println("Processing "+ind);
		for(OWLClassExpression exp:reasoner.instanceOf(ind)){
			if(exp instanceof OWLClass){
				OWLAxiom assertion = factory.getOWLClassAssertionAxiom(exp, ind);
				//System.out.println("Newly added: "+assertion);
				if(!ontology.containsAxiom(assertion))
					ontology.addAxiom(assertion);
			}else if(exp instanceof OWLObjectSomeValuesFrom){
				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) exp;

				if(!satisfied(ind,some,ontology)){
					OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(
						some.getProperty(), 
						ind, 
						getIndividual(some.getFiller(), ontology));
						//System.out.println("Newly added: "+assertion);
					if(!ontology.containsAxiom(assertion))
						ontology.addAxiom(assertion);
				} 
			}
		}
	}

	private boolean satisfied(OWLNamedIndividual ind, OWLObjectSomeValuesFrom some, OWLOntology ontology) {
		OWLObjectPropertyExpression property = some.getProperty();
		OWLClassExpression filler = some.getFiller();

		//System.out.println("does "+ind+" has some "+property+"-successors with "+filler+"?");

		return ontology.objectPropertyAssertionAxioms(ind).anyMatch(ass -> {
			//System.out.println("checking "+ass);
			//System.out.println(" "+ass.getObject()+" satisfies "+reasoner.instanceOf((OWLNamedIndividual)ass.getObject()));
			//System.out.println(" contains: "+reasoner.instanceOf((OWLNamedIndividual)ass.getObject())
			//	.contains(filler));
			return ass.getProperty().equals(property) 
			&& reasoner.instanceOf((OWLNamedIndividual)ass.getObject())
					.contains(filler);
		});
	}

	private OWLNamedIndividual getIndividual(OWLClassExpression exp, OWLOntology ontology) {
		//System.out.println("For "+exp);

		OWLNamedIndividual individual;
		if(!class2ind.containsKey(exp)){
			IRI name = nameFor(exp);
			individual = factory.getOWLNamedIndividual(name);
			class2ind.put(exp, individual);

			//System.out.println("We take fresh individual "+name);


			for(OWLClassExpression subsumer:reasoner.equivalentOrSubsuming(exp)){
				//System.out.println("Subsumer "+subsumer);
				if(subsumer instanceof OWLClass){
					OWLAxiom assertion = 
						factory.getOWLClassAssertionAxiom(subsumer, individual);
					//System.out.println("Newly added: "+assertion);
					ontology.addAxiom(assertion);
				} else if(subsumer instanceof OWLObjectSomeValuesFrom){
					OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) subsumer;
					OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(
						some.getProperty(), 
						individual, 
						getIndividual(some.getFiller(), ontology));
					//System.out.println("Newly added: "+assertion);
					ontology.addAxiom(assertion);
				}
			}
		} else
			individual = class2ind.get(exp);
			
		return individual;
	}

	int individualNameCounter = 0;
	private IRI nameFor(OWLClassExpression exp) {
		individualNameCounter++;
		return IRI.create("__i__"+individualNameCounter);
	}
	
	public OWLOntology getOntology() {
		return ontology;
	}


}
