package de.tu_dresden.inf.lat.abox_repairs.saturation;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.*;

import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * @author Patrick Koopmann
 */
public class CanonicalModelGenerator implements ABoxSaturator {
	private static Logger logger = LogManager.getLogger(CanonicalModelGenerator.class);

	private final ReasonerFacade reasoner;

	private Map<OWLClassExpression, OWLAnonymousIndividual> class2ind;

	private OWLDataFactory factory;
	
	private OWLOntology ontology;

	private int addedAssertions = 0;
	private int addedIndividuals = 0;
	private double duration = 0.0;

	public CanonicalModelGenerator(ReasonerFacade reasoner) {
		this.reasoner = reasoner;
	}

	public void saturate(OWLOntology ontology) {

		long start = System.nanoTime();

		this.ontology = ontology;
		factory = ontology.getOWLOntologyManager().getOWLDataFactory();

		int sizeOld = ontology.getABoxAxioms(Imports.INCLUDED).size();
		int indsOld = ontology.getIndividualsInSignature().size() + ontology.getAnonymousIndividuals().size();

		class2ind = new HashMap<>();

		//System.out.println("Anonymous individuals: "+ontology.getAnonymousIndividuals());

		ontology.individualsInSignature()
			.forEach(i -> process(i,ontology));
		ontology.anonymousIndividuals()
				.forEach(i -> process(i, ontology));

		int sizeNew = ontology.getABoxAxioms(Imports.INCLUDED).size();
		int indsNew = ontology.getIndividualsInSignature().size() + ontology.getAnonymousIndividuals().size();

		addedAssertions = sizeNew-sizeOld;
		addedIndividuals = indsNew-indsOld;

		logger.info("Saturation added "+addedAssertions+" axioms.");

		duration = ((double)(System.nanoTime() - start)/1_000_000_000);

		logger.info("Saturation took "+duration);
	}

	@Override
	public int addedAssertions() {
		return addedAssertions;
	}

	@Override
	public int addedIndividuals() {
		return addedIndividuals;
	}

	@Override
	public double getDuration() {
		return duration;
	}

	private void process(OWLIndividual ind, OWLOntology ontology) {
		//System.out.println("Processing "+ind);
		for(OWLClassExpression exp:reasoner.instanceOfExcludingOWLThing(ind)){
			if(exp instanceof OWLClass){
				OWLAxiom assertion = factory.getOWLClassAssertionAxiom(exp, ind);
				logger.debug("Newly added 0: "+assertion);
				if(!ontology.containsAxiom(assertion))
					ontology.addAxiom(assertion);
			}else if(exp instanceof OWLObjectSomeValuesFrom){
				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) exp;

				if(!satisfied(ind,some,ontology)){
					OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(
						some.getProperty(), 
						ind, 
						getIndividual(some.getFiller(), ontology));
						logger.debug("Newly added 1: "+assertion);
					if(!ontology.containsAxiom(assertion))
						ontology.addAxiom(assertion);
				} 
			}
		}
	}

	private boolean satisfied(OWLIndividual ind, OWLObjectSomeValuesFrom some, OWLOntology ontology) {
		OWLObjectPropertyExpression property = some.getProperty();
		OWLClassExpression filler = some.getFiller();

		//System.out.println("does "+ind+" has some "+property+"-successors with "+filler+"?");

		return ontology.objectPropertyAssertionAxioms(ind).anyMatch(ass -> {
			//System.out.println("checking "+ass);
			//System.out.println(" "+ass.getObject()+" satisfies "+reasoner.instanceOf((OWLNamedIndividual)ass.getObject()));
			//System.out.println(" contains: "+reasoner.instanceOf((OWLNamedIndividual)ass.getObject())
			//	.contains(filler));
			return ass.getProperty().equals(property)  && (filler.isOWLThing() ||
			 reasoner.instanceOfExcludingOWLThing(ass.getObject())
					.contains(filler));
		});
	}

	/* Please rather return an instance of OWLAnonymousIndividual here.
	*  A benefit is that then there is a clear distinction between named individuals (individual names in the paper)
	*  and anonymous individuals (variables in the paper), i.e., the class AnonymousVariableDetector is then not
	*  required anymore.
	*  To make these instances of OWLAnonymousIndividual accessible, the reasoner facade can add fresh instances of
	*  OWLNamedIndividual plus a corresponding instance of OWLSameIndividualAxiom---just like it was done to make the
	*  complex, anonymous instances of OWLClassExpression accessible. */
	private OWLAnonymousIndividual getIndividual(OWLClassExpression exp, OWLOntology ontology) {
		//System.out.println("For "+exp);

		OWLAnonymousIndividual individual;
		if(!class2ind.containsKey(exp)){
//			IRI name = nameFor(exp);
//			individual = factory.getOWLNamedIndividual(name);
			individual = factory.getOWLAnonymousIndividual();
			class2ind.put(exp, individual);

			//System.out.println("We take fresh individual "+name);


			for(OWLClassExpression subsumer:reasoner.equivalentIncludingOWLThingOrStrictlySubsumingExcludingOWLThing(exp)){
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
					logger.debug("Newly added 2: "+assertion);
					ontology.addAxiom(assertion);
				}
			}
		} else
			individual = class2ind.get(exp);
			
		return individual;
	}

	@Deprecated
	int individualNameCounter = 0;
	@Deprecated
	private IRI nameFor(OWLClassExpression exp) {
		individualNameCounter++;
		return IRI.create("__i__"+individualNameCounter);
	}
	
	public OWLOntology getOntology() {
		return ontology;
	}


}
