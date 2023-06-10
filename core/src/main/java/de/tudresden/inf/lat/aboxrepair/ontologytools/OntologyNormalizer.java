package de.tudresden.inf.lat.aboxrepair.ontologytools;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OntologyNormalizer {
    public enum Entailment { IQ, CQ }
    private final OWLOntology ontology;
    private final OWLOntology normalization;
    private final OWLDataFactory dataFactory;
    // private final FreshOWLClassFactory freshOWLClassFactory;
    private final ClassExpressionNormalizer classExpressionNormalizer;
    private final boolean iq;
    private boolean isEvaluated = false;
    private Optional<IllegalArgumentException> exception = Optional.empty();

    public OntologyNormalizer(OWLOntology ontology, Entailment entailment) throws OWLOntologyCreationException {
        this.ontology = ontology;
        this.normalization = ontology.getOWLOntologyManager().createOntology();
        this.dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
        // this.freshOWLClassFactory = FreshOWLClassFactory.of(normalization);
        this.classExpressionNormalizer = new ClassExpressionNormalizer(ontology.getOWLOntologyManager());
        this.iq = entailment.equals(Entailment.IQ);
    }

    public final OWLOntology getNormalization(boolean failSilently) throws IllegalArgumentException {
        evaluate();
        if (!failSilently)
            throwExceptionIfPresent();
        return normalization;
    }

    private final void evaluate() {
        if (!isEvaluated) {
            ontology.axioms(Imports.INCLUDED).forEach(axiom -> axiom.accept(objectVisitor));
            ontology.annotations().forEach(annotation -> annotation.accept(objectVisitor));
            isEvaluated = true;
        }
    }

    public final void throwExceptionIfPresent() throws IllegalArgumentException {
        if (exception.isPresent())
            throw exception.get();
    }

    private void setException(Object object) {
        exception = Optional.of(new IllegalArgumentException("The expressivity of " + ontology.getOntologyID() + " is not supported.  Specifically, the following OWL object is not supported: " + object));
    }

    private final Map<OWLClassExpression, OWLIndividual> individualsIQ = new HashMap<>();

    private final void unfold(OWLClassExpression classExpression, OWLIndividual individual) {
        final OWLClassExpressionVisitor conjunctVisitor = new OWLClassExpressionVisitor() {

            @Override
            public void visit(OWLClass ce) {
                normalization.addAxiom(dataFactory.getOWLClassAssertionAxiom(ce, individual));
            }

            @Override
            public void visit(OWLObjectSomeValuesFrom ce) {
                final OWLIndividual successor;
                if (iq)
                    successor = individualsIQ.computeIfAbsent(ce.getFiller(), __ -> dataFactory.getOWLAnonymousIndividual());
                else
                    successor = dataFactory.getOWLAnonymousIndividual();
                normalization.addAxiom(dataFactory.getOWLObjectPropertyAssertionAxiom(ce.getProperty(), individual, successor));
                unfold(ce.getFiller(), successor);
            }
        };
        classExpression.conjunctSet().forEach(conjunct -> conjunct.accept(conjunctVisitor));
    }

    private final OWLObjectVisitor objectVisitor = new OWLObjectVisitor() {

        @Override
        public void doDefault(Object object) {
            setException(object);
        }

        @Override
        public void visit(OWLDeclarationAxiom axiom) {
            axiom.getEntity().accept(this);
        }

        @Override
        public void visit(OWLClassAssertionAxiom axiom) {
            axiom.annotations().forEach(annotation -> annotation.accept(this));
            final Optional<OWLClassExpression> normalizedClassExpression = classExpressionNormalizer.tryNormalize(axiom.getClassExpression());
            if (normalizedClassExpression.isPresent()) {
//            normalization.addAxiom(dataFactory.getOWLClassAssertionAxiom(
//                    freshOWLClassFactory.getEntity(normalizedClassExpression.get()), axiom.getIndividual()));
//            normalization.addAxiom(dataFactory.getOWLEquivalentClassesAxiom(
//                    freshOWLClassFactory.getEntity(normalizedClassExpression.get()), normalizedClassExpression.get()));
                unfold(normalizedClassExpression.get(), axiom.getIndividual());
            } else
                setException(axiom);
        }

        @Override
        public void visit(OWLObjectPropertyAssertionAxiom axiom) {
            axiom.getProperty().accept(this);
            axiom.annotations().forEach(annotation -> annotation.accept(this));
            normalization.addAxiom(axiom);
        }

        @Override
        public void visit(OWLSubClassOfAxiom axiom) {
            axiom.annotations().forEach(annotation -> annotation.accept(this));
            final Optional<OWLClassExpression> normalizedSubClass = classExpressionNormalizer.tryNormalize(axiom.getSubClass());
            final Optional<OWLClassExpression> normalizedSuperClass = classExpressionNormalizer.tryNormalize(axiom.getSuperClass());
            if (normalizedSubClass.isPresent() && normalizedSuperClass.isPresent())
                normalization.addAxiom(dataFactory.getOWLSubClassOfAxiom(normalizedSubClass.get(), normalizedSuperClass.get()));
            else
                setException(axiom);
        }

        @Override
        public void visit(OWLEquivalentClassesAxiom axiom) {
            axiom.annotations().forEach(annotation -> annotation.accept(this));
            final OWLEquivalentClassesAxiom normalizedAxiom = dataFactory.getOWLEquivalentClassesAxiom(
                    axiom.classExpressions().map(classExpressionNormalizer::tryNormalize).filter(opt -> {
                        if (opt.isPresent())
                            return true;
                        else {
                            setException(axiom);
                            return false;
                        }
                    }).map(Optional::get));
            if (normalizedAxiom.getClassExpressions().size() > 1)
                normalization.addAxiom(normalizedAxiom);
        }

        @Override
        public void visit(OWLObjectPropertyDomainAxiom axiom) {
            axiom.getProperty().accept(this);
            axiom.annotations().forEach(annotation -> annotation.accept(this));
            final Optional<OWLClassExpression> normalizedDomain = classExpressionNormalizer.tryNormalize(axiom.getDomain());
            if (normalizedDomain.isPresent())
                normalization.addAxiom(dataFactory.getOWLSubClassOfAxiom(
                        dataFactory.getOWLObjectSomeValuesFrom(axiom.getProperty(), dataFactory.getOWLThing()),
                        normalizedDomain.get()));
        }

        @Override
        public void visit(OWLNamedIndividual individual) {
            // do nothing
        }

        @Override
        public void visit(OWLClass ce) {
            // do nothing
        }

        @Override
        public void visit(OWLObjectProperty property) {
            if (property.isOWLTopObjectProperty() || property.isOWLBottomObjectProperty())
                setException(property);
        }

    };

//    private final OWLIndividualVisitorEx<OWLNamedIndividual> individualNormalizer = new OWLIndividualVisitorEx<OWLNamedIndividual>() {
//
//        @Override
//        public OWLNamedIndividual visit(OWLNamedIndividual individual) {
//            return individual;
//        }
//
//        @Override
//        public OWLNamedIndividual visit(OWLAnonymousIndividual individual) {
//            return FreshOWLEntityFactory.FreshOWLNamedIndividualFactory.of(normalization).getEntity(individual);
//        }
//
//    };
}
