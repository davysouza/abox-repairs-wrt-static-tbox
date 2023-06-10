package de.tudresden.inf.lat.aboxrepair.ontologytools;

import de.tudresden.inf.lat.aboxrepair.reasoner.ReasonerFacade;
import org.semanticweb.owlapi.model.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ClassExpressionNormalizer {
    private final OWLDataFactory dataFactory;
    private final ReasonerFacade reasoner;

    public ClassExpressionNormalizer(OWLOntologyManager ontologyManager) throws OWLOntologyCreationException {
        this.dataFactory = ontologyManager.getOWLDataFactory();
        this.reasoner = ReasonerFacade.newReasonerFacadeWithTBox(ontologyManager.createOntology());
    }

    public OWLClassExpression normalize(OWLClassExpression classExpression) throws IllegalArgumentException {
        final Optional<OWLClassExpression> result = tryNormalize(classExpression);

        if (result.isPresent())
            return result.get();

        throw new IllegalArgumentException("The expressivity of " + classExpression + " is not supported.");
    }

    final Optional<OWLClassExpression> tryNormalize(OWLClassExpression classExpression) {
        return classExpression.accept(classExpressionNormalizer);
    }

    final boolean isSubsumedBy(OWLClassExpression ce1, OWLClassExpression ce2) {
        reasoner.addExpressions(ce1, ce2);
        reasoner.update();
        return reasoner.subsumedBy(ce1, ce2);
    }

    private final OWLClassExpressionVisitorEx<Optional<OWLClassExpression>> classExpressionNormalizer = new OWLClassExpressionVisitorEx<Optional<OWLClassExpression>>() {
        @Override
        public Optional<OWLClassExpression> doDefault(Object object) {
            // setException();
            return Optional.empty();
        }

        @Override
        public Optional<OWLClassExpression> visit(OWLClass ce) {
            if (ce.isOWLNothing()) {
                // setException();
                return Optional.empty();
            }
            return Optional.of(ce);
        }

        @Override
        public Optional<OWLClassExpression> visit(OWLObjectIntersectionOf ce) {
            final Set<OWLClassExpression> minimalNormalizedConjuncts = new HashSet<>();
            for (OWLClassExpression conjunct : ce.asConjunctSet()) {
                final Optional<OWLClassExpression> normalizedConjunct = tryNormalize(conjunct);

                if (!normalizedConjunct.isPresent())
                    return Optional.empty();

                if (minimalNormalizedConjuncts.stream().noneMatch(otherNormalizedConjunct -> isSubsumedBy(otherNormalizedConjunct, normalizedConjunct.get()))) {
                    minimalNormalizedConjuncts.removeIf(otherNormalizedConjunct -> isSubsumedBy(normalizedConjunct.get(), otherNormalizedConjunct));
                    minimalNormalizedConjuncts.add(normalizedConjunct.get());
                }
            }

            assert !minimalNormalizedConjuncts.isEmpty();
            return (minimalNormalizedConjuncts.size() == 1)
                    ? Optional.of(minimalNormalizedConjuncts.iterator().next())
                    : Optional.of(dataFactory.getOWLObjectIntersectionOf(minimalNormalizedConjuncts));
        }

        @Override
        public Optional<OWLClassExpression> visit(OWLObjectSomeValuesFrom ce) {
            return tryNormalize(ce.getFiller()).map(normalizedFiller ->
                    dataFactory.getOWLObjectSomeValuesFrom(ce.getProperty(), normalizedFiller));
        }

        @Override
        public Optional<OWLClassExpression> visit(OWLObjectMinCardinality ce) {
            if (ce.getCardinality() == 0)
                return Optional.of(dataFactory.getOWLThing());

            if (ce.getCardinality() == 1)
                return tryNormalize(ce.getFiller()).map(normalizedFiller ->
                        dataFactory.getOWLObjectSomeValuesFrom(ce.getProperty(), normalizedFiller));

            // setException();
            return Optional.empty();
        }
    };
}
