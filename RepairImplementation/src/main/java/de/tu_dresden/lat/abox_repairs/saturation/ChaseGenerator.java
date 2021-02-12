package de.tu_dresden.lat.abox_repairs.saturation;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.rulewerk.core.model.api.Fact;
import org.semanticweb.rulewerk.core.model.api.PositiveLiteral;
import org.semanticweb.rulewerk.core.model.api.Predicate;
import org.semanticweb.rulewerk.core.model.api.Rule;
import org.semanticweb.rulewerk.core.model.api.Term;
import org.semanticweb.rulewerk.core.reasoner.Algorithm;
import org.semanticweb.rulewerk.core.reasoner.KnowledgeBase;
import org.semanticweb.rulewerk.owlapi.OwlToRulesConverter;
import org.semanticweb.rulewerk.reasoner.vlog.VLogReasoner;

/**
 * Computes the chase for a given ontology, adding all derived ABox assertions to the given ontology.
 * 
 * @author Patrick Koopmann
 */
public class ChaseGenerator implements ABoxSaturator {

	private OWLDataFactory factory;
	private OWLOntology ontology;

	public void saturate(OWLOntology ontology) throws SaturationException {
		
		this.ontology = ontology;
		factory = ontology.getOWLOntologyManager().getOWLDataFactory();

		final OwlToRulesConverter owlToRulesConverter = new OwlToRulesConverter();
		owlToRulesConverter.addOntology(ontology);

		  
//		System.out.println("TBox axioms as rules:");
		final Set<Rule> rules = owlToRulesConverter.getRules();
//		for (final Rule rule : rules) {
//			System.out.println(" - rule: " + rule);
//		}
//		System.out.println();
//		System.out.println("Facts:");
		final Set<Fact> facts = owlToRulesConverter.getFacts();
//		for (final PositiveLiteral fact : facts) {
//			System.out.println(" - fact: " + fact);
//		}
//		System.out.println();
		
		final KnowledgeBase kb = new KnowledgeBase();
		kb.addStatements(new ArrayList<>(owlToRulesConverter.getRules()));
		kb.addStatements(owlToRulesConverter.getFacts());

		try (VLogReasoner reasoner = new VLogReasoner(kb)) {

			reasoner.setAlgorithm(Algorithm.RESTRICTED_CHASE);

//			System.out.println("Reasoning default algorithm: " + reasoner.getAlgorithm());
			try {
				reasoner.reason();
			} catch (IOException e) {
				throw new SaturationException("exception during vlog reasoning", e);
			}

			reasoner.getInferences().forEach(fact -> {
			//	System.out.println("Fact: "+fact);
			//	System.out.println("Abox: "+fact2Axiom(fact));
				OWLAxiom axiom = fact2Axiom(fact);
				if(!false){//ontology.containsAxiom(axiom)){
					//System.out.println("Newly derived: "+axiom);
					ontology.add(axiom);
				}
			});
		}
	}

	private OWLAxiom fact2Axiom(Fact fact) {
		List<Term> arguments = fact.getArguments();
		Predicate predicate = fact.getPredicate();
		if(arguments.size()==1){
			return factory.getOWLClassAssertionAxiom(
				factory.getOWLClass(toIRI(predicate)), 
				factory.getOWLNamedIndividual(toIRI(arguments.get(0)))
			);
		} else {
			assert arguments.size()==2;

			return factory.getOWLObjectPropertyAssertionAxiom(
				factory.getOWLObjectProperty(toIRI(predicate)), 
				factory.getOWLNamedIndividual(toIRI(arguments.get(0))), 
				factory.getOWLNamedIndividual(toIRI(arguments.get(1))));
		}
	}
	
	

	private static IRI toIRI(Predicate predicate) {
		return IRI.create(predicate.getName());
	}

	private static IRI toIRI(Term term) {
		return IRI.create(term.getName());
	}
	
	public OWLOntology getOntology() {
		return ontology;
	}
}
