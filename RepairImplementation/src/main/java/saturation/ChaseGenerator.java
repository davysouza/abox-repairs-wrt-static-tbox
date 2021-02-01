package saturation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.rulewerk.core.model.api.Fact;
import org.semanticweb.rulewerk.core.model.api.PositiveLiteral;
import org.semanticweb.rulewerk.core.model.api.Predicate;
import org.semanticweb.rulewerk.core.model.api.Rule;
import org.semanticweb.rulewerk.core.model.api.Term;
import org.semanticweb.rulewerk.core.reasoner.Algorithm;
import org.semanticweb.rulewerk.core.reasoner.KnowledgeBase;
import org.semanticweb.rulewerk.owlapi.OwlToRulesConverter;
import org.semanticweb.rulewerk.reasoner.vlog.VLogReasoner;

public class ChaseGenerator implements ABoxSaturator {

	public static void main(String[] args) throws OWLOntologyCreationException, SaturationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		//OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File("ore_ont_5760.owl"));
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(
			new File("/home/patrick/Documents/Ontologies/pool_sample/files/ore_ont_12150.owl"));

		int axiomsBefore = ontology.getAxiomCount();

		ChaseGenerator generator = new ChaseGenerator();
		generator.saturate(ontology);


		System.out.println("Axioms before: "+axiomsBefore);
		System.out.println("Axioms after: "+ontology.getAxiomCount());
	}

	private OWLDataFactory factory;

	public void saturate(OWLOntology ontology) throws SaturationException {

		factory = ontology.getOWLOntologyManager().getOWLDataFactory();

		final OwlToRulesConverter owlToRulesConverter = new OwlToRulesConverter();
		owlToRulesConverter.addOntology(ontology);

		/*System.out.println("GCIs as rules:");
		final Set<Rule> rules = owlToRulesConverter.getRules();
		for (final Rule rule : rules) {
			System.out.println(" - rule: " + rule);
		}
		System.out.println();
		System.out.println("Facts:");
		final Set<Fact> facts = owlToRulesConverter.getFacts();
		for (final PositiveLiteral fact : facts) {
			System.out.println(" - fact: " + fact);
		}
		System.out.println();
		*/
		final KnowledgeBase kb = new KnowledgeBase();
		kb.addStatements(new ArrayList<>(owlToRulesConverter.getRules()));
		kb.addStatements(owlToRulesConverter.getFacts());

		try (VLogReasoner reasoner = new VLogReasoner(kb)) {

			reasoner.setAlgorithm(Algorithm.SKOLEM_CHASE);

			System.out.println("Reasoning default algorithm: " + reasoner.getAlgorithm());
			try {
				reasoner.reason();
			} catch (IOException e) {
				throw new SaturationException("exception during vlog reasoning", e);
			}

			reasoner.getInferences().forEach(fact -> {
			//	System.out.println("Fact: "+fact);
			//	System.out.println("Abox: "+fact2Axiom(fact));
				ontology.add(fact2Axiom(fact));
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

}
