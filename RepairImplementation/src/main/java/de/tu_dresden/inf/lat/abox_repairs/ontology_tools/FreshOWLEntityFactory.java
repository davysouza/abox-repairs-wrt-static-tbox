package de.tu_dresden.inf.lat.abox_repairs.ontology_tools;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.semanticweb.owlapi.model.*;

import java.util.*;

/**
 * Producing fresh owl classes.
 *
 * @author koopmann
 */
public abstract class FreshOWLEntityFactory<O extends OWLObject, E extends OWLEntity> {

    protected final OWLDataFactory factory;
    protected final Set<E> knownEntities;
    private final String prefix;
    private final OWLObjectVisitor additionalKnownOWLEntityVisitor;
    private final BiMap<O, E> freshEntities;
    @Deprecated
    private final Set<String> namesOfFreshEntities;
    private int freshEntityCounter = 0;

    private FreshOWLEntityFactory(OWLDataFactory factory, String prefix, Set<E> knownEntities) {
        this.factory = factory;
        this.prefix = prefix;
        this.knownEntities = new HashSet<>(knownEntities);
        this.freshEntities = HashBiMap.create();
        this.namesOfFreshEntities = new HashSet<>();
        this.additionalKnownOWLEntityVisitor = initializeAdditionalKnownOWLEntityVisitor();
    }

    public final boolean isFreshEntity(Object object) {
        return freshEntities.containsValue(object);
    }

    @Deprecated
    public final boolean isNameOfFreshEntity(String name) {
//        return freshEntities.stream().anyMatch(entity -> entity.getIRI().toString().equals(name));
        return namesOfFreshEntities.contains(name);
    }

    protected abstract Optional<E> asEntity(O owlObject);

    protected abstract O asObject(E owlEntity);

    protected abstract E newEntity(IRI iri);

    private E newFreshEntity() {
        E freshEntity;
        do {
            freshEntity = newEntity(IRI.create(prefix + freshEntityCounter++));
        } while (knownEntities.contains(freshEntity));
        namesOfFreshEntities.add(freshEntity.getIRI().toString());
        return freshEntity;
    }

    public final E getEntity(O owlObject) {
        return asEntity(owlObject).orElseGet(() -> freshEntities.computeIfAbsent(owlObject, __ -> this.newFreshEntity()));
    }

//    public final boolean containsEntity(E owlEntity) {
//        return freshEntities.containsValue(owlEntity);
//    }
//
//    public final Set<E> getEntities() {
//        return Collections.unmodifiableSet(freshEntities.values());
//    }

    public final O getObject(E owlEntity) {
        return freshEntities.inverse().getOrDefault(owlEntity, asObject(owlEntity));
    }

    public final boolean containsObject(O owlObject) {
        return asEntity(owlObject).isPresent() || freshEntities.containsKey(owlObject);
    }

    public final Set<O> getObjects() {
        return Collections.unmodifiableSet(freshEntities.keySet());
    }

    protected abstract OWLObjectVisitor initializeAdditionalKnownOWLEntityVisitor();

    public void addAdditionalKnownEntity(OWLObject owlObject) throws IllegalArgumentException {
        owlObject.accept(additionalKnownOWLEntityVisitor);
    }

    public void addAdditionalKnownEntities(Iterable<? extends OWLObject> owlObjects) throws IllegalArgumentException {
        owlObjects.forEach(owlObject -> owlObject.accept(additionalKnownOWLEntityVisitor));
    }

    public static final class FreshOWLClassFactory extends FreshOWLEntityFactory<OWLClassExpression, OWLClass> {

        private static final String FRESH_CLASS_PREFIX = "Fresh_Class_";
        private static final Map<OWLOntology, FreshOWLClassFactory> freshOWLClassFactories = new HashMap<>();

        private FreshOWLClassFactory(OWLDataFactory factory, Set<OWLClass> knownClasses) {
            super(factory, FRESH_CLASS_PREFIX, knownClasses);
        }

        private FreshOWLClassFactory(OWLOntology ontology) {
            this(ontology.getOWLOntologyManager().getOWLDataFactory(), ontology.getClassesInSignature());
        }

        public static final FreshOWLClassFactory of(OWLOntology ontology) {
            return freshOWLClassFactories.computeIfAbsent(ontology, FreshOWLClassFactory::new);
        }

        @Override
        public Optional<OWLClass> asEntity(OWLClassExpression owlClassExpression) {
            if (owlClassExpression.isOWLClass()) {
                return Optional.of(owlClassExpression.asOWLClass());
            } else {
                return Optional.empty();
            }
        }

        @Override
        protected OWLClassExpression asObject(OWLClass owlEntity) {
            return owlEntity;
        }

        @Override
        protected final OWLClass newEntity(IRI iri) {
            return factory.getOWLClass(iri);
        }

        @Override
        protected OWLObjectVisitor initializeAdditionalKnownOWLEntityVisitor() {
            return new OWLObjectVisitor() {
                @Override
                public void visit(OWLClass ce) {
                    if (isFreshEntity(ce)) {
                        throw new IllegalArgumentException("Cannot add " + ce + " to the set of known entities, " +
                                "since a fresh entity with the same name has already been produced.");
                    } else {
                        knownEntities.add(ce);
                    }
                }
            };
        }
    }

    public static final class FreshOWLNamedIndividualFactory extends FreshOWLEntityFactory<OWLIndividual, OWLNamedIndividual> {

        private static final String FRESH_INDIVIDUAL_PREFIX = "Fresh_Individual_";
        private static final Map<OWLOntology, FreshOWLNamedIndividualFactory> freshOWLNamedIndividualFactories = new HashMap<>();

        private FreshOWLNamedIndividualFactory(OWLDataFactory factory, Set<OWLNamedIndividual> knownNamedIndividuals) {
            super(factory, FRESH_INDIVIDUAL_PREFIX, knownNamedIndividuals);
        }

        private FreshOWLNamedIndividualFactory(OWLOntology ontology) {
            this(ontology.getOWLOntologyManager().getOWLDataFactory(), ontology.getIndividualsInSignature());
        }

        public static final FreshOWLNamedIndividualFactory of(OWLOntology ontology) {
            return freshOWLNamedIndividualFactories.computeIfAbsent(ontology, FreshOWLNamedIndividualFactory::new);
        }

        @Override
        public Optional<OWLNamedIndividual> asEntity(OWLIndividual owlIndividual) {
            if (owlIndividual.isOWLNamedIndividual()) {
                return Optional.of(owlIndividual.asOWLNamedIndividual());
            } else {
                return Optional.empty();
            }
        }

        @Override
        protected OWLIndividual asObject(OWLNamedIndividual owlEntity) {
            return owlEntity;
        }

        @Override
        protected final OWLNamedIndividual newEntity(IRI iri) {
            return factory.getOWLNamedIndividual(iri);
        }

        @Override
        protected OWLObjectVisitor initializeAdditionalKnownOWLEntityVisitor() {
            return new OWLObjectVisitor() {
                @Override
                public void visit(OWLNamedIndividual individual) {
                    if (isFreshEntity(individual)) {
                        throw new IllegalArgumentException("Cannot add " + individual + " to the set of known entities, " +
                                "since a fresh entity with the same name has already been produced.");
                    } else {
                        knownEntities.add(individual);
                    }
                }
            };
        }

    }

}
