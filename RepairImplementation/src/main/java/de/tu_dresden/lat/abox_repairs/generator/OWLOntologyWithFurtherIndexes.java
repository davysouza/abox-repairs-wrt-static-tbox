package de.tu_dresden.lat.abox_repairs.generator;

import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.InitVisitorFactory;
import uk.ac.manchester.cs.owl.owlapi.Internals;
import uk.ac.manchester.cs.owl.owlapi.MapPointer;
import uk.ac.manchester.cs.owl.owlapi.OWLAxiomIndexImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyImpl;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.semanticweb.owlapi.model.AxiomType.OBJECT_PROPERTY_ASSERTION;

public class OWLOntologyWithFurtherIndexes {

    /* Creating a new visitor that is not defined in uk.ac.manchester.cs.owl.owlapi.InitVisitorFactory */
    private static final InitVisitorFactory.InitVisitor<OWLIndividual> INDIVIDUALSUPERNAMED =
            new ConfigurableInitIndividualVisitor<>(false, true);

    private final OWLOntology ontology;
    private transient MapPointer<OWLIndividual, OWLObjectPropertyAssertionAxiom> objectPropertyAssertionsByObjectIndividual;

    public OWLOntologyWithFurtherIndexes(OWLOntology ontology) {
        super();
        this.ontology = ontology;
        initializeLazyObjectPropertyAssertionsMap();
    }

    @SuppressWarnings("unchecked")
    private void initializeLazyObjectPropertyAssertionsMap() {
        try {
            final Field intsField = OWLAxiomIndexImpl.class.getDeclaredField("ints");
            intsField.setAccessible(true);
            final Internals ints;
            if (ontology instanceof ConcurrentOWLOntologyImpl) {
                final Field delegateField = ConcurrentOWLOntologyImpl.class.getDeclaredField("delegate");
                delegateField.setAccessible(true);
                ints = (Internals) intsField.get((OWLOntology) delegateField.get(ontology));
            } else {
                ints = (Internals) intsField.get(ontology);
            }
            final Method buildLazyMethod =
                    Arrays.stream(Internals.class.getDeclaredMethods())
                            .filter(method -> method.getName().equals("buildLazy"))
                            .findAny().get();
            buildLazyMethod.setAccessible(true);
            objectPropertyAssertionsByObjectIndividual =
                    (MapPointer<OWLIndividual, OWLObjectPropertyAssertionAxiom>)
                            buildLazyMethod.invoke(ints, OBJECT_PROPERTY_ASSERTION, INDIVIDUALSUPERNAMED, OWLObjectPropertyAssertionAxiom.class);
        } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public final OWLOntology getOntology() {
        return ontology;
    }

    public final Stream<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxiomsWithSubject(OWLIndividual individual) {
        /* The OWLAPI already manages an index for the desired results, so there is no need to create a second one. */
        return ontology.objectPropertyAssertionAxioms(individual);
    }

    public final Stream<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxiomsWithObject(OWLIndividual individual) {
        /* The below instruction would produce the desired results, but in an unoptimized manner as the OWLAPI does not
         * manage an according index.  Specifically, all axioms would be visited once, which is expensive.
         *
         * return ontology.axioms(OWLObjectPropertyAssertionAxiom.class, OWLIndividual.class, individual, EXCLUDED, IN_SUPER_POSITION); */
        return objectPropertyAssertionsByObjectIndividual.getValues(individual);
    }

    private static class ConfigurableInitIndividualVisitor<K extends OWLObject> extends InitVisitorFactory.InitIndividualVisitor<K> {

        private final boolean sub;

        public ConfigurableInitIndividualVisitor(boolean sub, boolean named) {
            super(sub, named);
            this.sub = sub;
        }

        @Override
        @SuppressWarnings("unchecked")
        @ParametersAreNonnullByDefault
        public K visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
            if (sub) {
                return (K) axiom.getSubject();
            } else {
                return (K) axiom.getObject();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        @ParametersAreNonnullByDefault
        public K visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
            if (sub) {
                return (K) axiom.getSubject();
            } else {
                return (K) axiom.getObject();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        @ParametersAreNonnullByDefault
        public K visit(OWLObjectPropertyAssertionAxiom axiom) {
            if (sub) {
                return (K) axiom.getSubject();
            } else {
                return (K) axiom.getObject();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        @ParametersAreNonnullByDefault
        public K visit(OWLDataPropertyAssertionAxiom axiom) {
            if (sub) {
                return (K) axiom.getSubject();
            } else {
                return (K) axiom.getObject();
            }
        }

    }

}
