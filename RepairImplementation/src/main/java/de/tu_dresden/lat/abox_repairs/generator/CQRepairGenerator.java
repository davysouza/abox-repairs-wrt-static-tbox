package de.tu_dresden.lat.abox_repairs.generator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sun.istack.internal.NotNull;
import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import uk.ac.manchester.cs.owl.owlapi.InitVisitorFactory;
import uk.ac.manchester.cs.owl.owlapi.Internals;
import uk.ac.manchester.cs.owl.owlapi.MapPointer;
import uk.ac.manchester.cs.owl.owlapi.OWLAxiomIndexImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyImpl;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static org.semanticweb.owlapi.model.AxiomType.OBJECT_PROPERTY_ASSERTION;

public class CQRepairGenerator extends RepairGenerator {

    // Creating a new visitor that is not defined in uk.ac.manchester.cs.owl.owlapi.InitVisitorFactory
    private static final InitVisitorFactory.InitVisitor<OWLIndividual> INDIVIDUALSUPERNAMED =
            new ConfigurableInitIndividualVisitor<>(false, true);
    private static Logger logger = LogManager.getLogger(CQRepairGenerator.class);
    private final Set<CopiedOWLIndividual> individualsInTheRepair = new HashSet<>();
    private final CopiedOWLIndividual.Factory copiedOWLIndividualFactory = new CopiedOWLIndividual.Factory();
    private final Map<OWLNamedIndividual, RepairType> seedFunction;
    private final Queue<CopiedOWLObjectPropertyAssertionAxiom> queue = new LinkedList<>();
    private transient MapPointer<OWLIndividual, OWLObjectPropertyAssertionAxiom> objectPropertyAssertionsByObjectIndividual;

    public CQRepairGenerator(OWLOntology inputOntology,
                             Map<OWLNamedIndividual, RepairType> inputSeedFunction) {

        super(inputOntology, inputSeedFunction);
        this.seedFunction = inputSeedFunction;

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

    @Override
    public int getNumberOfCollectedIndividuals() {
        return individualsInTheRepair.size();
    }

    @Override
    protected void initialise() {
        /* All named individuals are returned by ontology.getIndividualsInSignature();
         *  All anonymous individuals are returned by ontology.getAnonymousIndividuals(); */

        try {
            repair = ontology.getOWLOntologyManager().createOntology();
            repair.add(ontology.getTBoxAxioms(Imports.INCLUDED));
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }

        // Variables Intitialisation

        anonymousDetector = AnonymousVariableDetector.newInstance(true, Main.RepairVariant.CQ);

        /**
         * The set of collected individuals for the repair will contain every named individual
         */
//		setOfCollectedIndividuals = inputObjectNames.stream()
//				.filter(ind -> anonymousDetector.isNamed(ind))
//				.collect(Collectors.toSet());
        inputObjectNames.stream()
                .filter(anonymousDetector::isNamed)
                .map(individualName ->
                        copiedOWLIndividualFactory.newNamedIndividual(individualName, seedFunction.get(individualName))
                ).forEach(this::addNewIndividualToTheRepair);

        /**
         * plus one copy of every variable, and every named individual with nothing assigned by the initial seed function,
         * which is assigned an empty repair type.
         */
//		for(OWLNamedIndividual objectName : inputObjectNames) {
//			if(!objectToRepairType.containsKey(objectName) ||
////					anonymousDetector.isAnonymous(objectName) ||
//				!objectToRepairType.get(objectName).getClassExpressions().isEmpty()) {
//				createCopy(objectName,typeHandler.newMinimisedRepairType(new HashSet<>()));
//				//individualCounter.put(objectName, individualCounter.get(objectName) + 1);
//			}
//		}
        inputObjectNames.stream()
                .filter(objectName -> // If the test fails, then we have already created a copy above.
                        !(anonymousDetector.isNamed(objectName) && seedFunction.get(objectName).isEmpty()))
                .map(objectName ->
                        copiedOWLIndividualFactory.newAnonymousIndividual(objectName, RepairType.empty())
                ).forEach(this::addNewIndividualToTheRepair);
    }

    private boolean addNewIndividualToTheRepair(CopiedOWLIndividual newIndividual) {
        if (individualsInTheRepair.add(newIndividual)) {
            ontology.classAssertionAxioms(newIndividual.getIndividualInTheSaturation())
                    .map(OWLClassAssertionAxiom::getClassExpression)
                    .filter(owlClassExpression ->
                            !newIndividual.getRepairType().getClassExpressions().contains(owlClassExpression))
//                    .filter(owlClassExpression -> !FreshNameProducer.isFreshOWLClass(owlClassExpression))
                    .map(owlClassExpression ->
                            OWLManager.getOWLDataFactory().getOWLClassAssertionAxiom(owlClassExpression, newIndividual.getIndividualInTheRepair()))
                    .forEach(repair::add);
            objectPropertyAssertionAxiomsWithSubject(newIndividual.getIndividualInTheSaturation())
                    .flatMap(axiom ->
                            copiedOWLIndividualFactory.getCopiesOf(axiom.getObject()).stream()
                                    .map(individual ->
                                            new CopiedOWLObjectPropertyAssertionAxiom(newIndividual, axiom.getProperty(), individual))
                    )
                    .forEach(queue::offer);
            objectPropertyAssertionAxiomsWithObject(newIndividual.getIndividualInTheSaturation())
                    .flatMap(axiom ->
                            copiedOWLIndividualFactory.getCopiesOf(axiom.getSubject()).stream()
                                    .filter(individual ->
                                            !individual.equals(newIndividual))
                                    .map(individual ->
                                            new CopiedOWLObjectPropertyAssertionAxiom(individual, axiom.getProperty(), newIndividual))
                    )
                    .forEach(queue::offer);
            return true;
        }
        return false;
    }

    @Override
    protected void generateVariables() {
//        individualsInTheRepair.forEach(individual1 ->
//                individualsInTheRepair.forEach(individual2 ->
//                        addToQueue(individual1, individual2)
//                )
//        );
        while (!queue.isEmpty()) {
            final CopiedOWLObjectPropertyAssertionAxiom copiedAxiom = queue.poll();
            final CopiedOWLIndividual subject = copiedAxiom.getSubject();
            final OWLObjectPropertyExpression property = copiedAxiom.getProperty();
            final CopiedOWLIndividual object = copiedAxiom.getObject();
            final Set<OWLClassExpression> successorSet = computeSuccessorSet(
                    subject.getRepairType(),
                    property.getNamedProperty(),
                    object.getIndividualInTheSaturation().asOWLNamedIndividual());
            if (reasonerWithoutTBox.isCovered(successorSet, object.getRepairType().getClassExpressions())) {
                repair.add(copiedAxiom.toAxiomInTheRepair());
            } else {
                typeHandler.findCoveringRepairTypes(object.getRepairType(), successorSet, object.getIndividualInTheSaturation().asOWLNamedIndividual())
                        .forEach(repairType -> {
                            //
                            final CopiedOWLIndividual newIndividual =
                                    copiedOWLIndividualFactory.newAnonymousIndividual(object.getIndividualInTheSaturation(), repairType);
                            if (addNewIndividualToTheRepair(newIndividual)) {
//                                individualsInTheRepair.forEach(individual -> {
//                                    addToQueue(newIndividual, individual);
//                                    if (!newIndividual.equals(individual))
//                                        addToQueue(individual, newIndividual);
//                                });
                            }
                        });
            }
        }
    }

    @Deprecated
    private void addToQueue(CopiedOWLIndividual individual1, CopiedOWLIndividual individual2) {
        ontology.objectPropertyAssertionAxioms(individual1.getIndividualInTheSaturation())
                .filter(axiom -> axiom.getObject().equals(individual2.getIndividualInTheSaturation()))
                .map(axiom -> new CopiedOWLObjectPropertyAssertionAxiom(individual1, axiom.getProperty(), individual2))
                .forEach(queue::offer);
    }

    private Stream<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxiomsWithSubject(OWLIndividual individual) {
//      The OWLAPI already manages an index for the desired results, so there is no need to create a second one.
//
        return ontology.objectPropertyAssertionAxioms(individual);
    }

    private Stream<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxiomsWithObject(OWLIndividual individual) {
//        The below instruction would produce the desired results, but in an unoptimized manner as the OWLAPI does not
//        manage an according index.  Specifically, all axioms would be visited once, which is expensive.
//
//        return ontology.axioms(OWLObjectPropertyAssertionAxiom.class, OWLIndividual.class, individual,
//                EXCLUDED, IN_SUPER_POSITION);

        return objectPropertyAssertionsByObjectIndividual.getValues(individual);
    }

    @Override
    @Deprecated
    protected void generateMatrix() throws OWLOntologyCreationException {

//        reasonerWithTBox.cleanOntology(); // TODO: bad code design
//
//        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
//
//        newOntology = man.createOntology();
//
//        newOntology.add(ontology.getTBoxAxioms(Imports.INCLUDED));
//
//        for (OWLAxiom ax : ontology.getABoxAxioms(Imports.INCLUDED)) {
//
//        }

    }

    /**
     * Assumption: all names in the triple occur in the input ontology.
     * Returns all ways of replacing an individual name A in the triple by the corresponding copy
     * (both individual names are replaced)
     */
    @Deprecated
    private Set<RATriple> expandNamesToCopies(RATriple triple) {
        Set<RATriple> result = new HashSet<>();

        for (OWLNamedIndividual ind1 : objectToCopies.get(triple.getIndividual1()))

            // TODO here (and later): why is this check necessary?
            if (setOfCollectedIndividuals.contains(ind1))
                for (OWLNamedIndividual ind2 : objectToCopies.get(triple.getIndividual2()))
                    if (setOfCollectedIndividuals.contains(ind2)) {
                        assert objectToRepairType.containsKey(ind1) && objectToRepairType.containsKey(ind2);
                        result.add(new RATriple(ind1, ind2, triple.getProperty()));
                    }
        return result;
    }

    /* The below code is a bit difficult to understand, but I believe that the current version now does the job
     *  correctly. */
//	@Override
    @Deprecated
    protected void generateVariablesOld() {

        // TODO the following queue contains pairs of individuals that may stand in a role relation
        Queue<RATriple> triplesToProcess = new LinkedList<RATriple>();

        Multimap<OWLIndividual, OWLObjectPropertyAssertionAxiom> asSubject = HashMultimap.create();
        Multimap<OWLIndividual, OWLObjectPropertyAssertionAxiom> asObject = HashMultimap.create();

        ontology.getAxioms(OBJECT_PROPERTY_ASSERTION)
                .forEach(ra -> {
                    RATriple triple = RATriple.fromAxiom(ra);
                    Set<RATriple> expanded = expandNamesToCopies(triple);
                    triplesToProcess.addAll(expanded);
                    asSubject.put(ra.getSubject(), ra);
                    asObject.put(ra.getObject(), ra);
                });

        /**
         * Optimisation: iterate over role assertions, rather than all pairs.
         * store also the role name
         */
	/*	for(OWLNamedIndividual ind1 : setOfCollectedIndividuals) {
			for (OWLNamedIndividual ind2 : setOfCollectedIndividuals) {
				Pair<OWLNamedIndividual, OWLNamedIndividual> pair = Pair.of(ind1, ind2);
				queueOfPairs.add(pair);
			}
		}
	*/
        while (!triplesToProcess.isEmpty()) {
            RATriple p = triplesToProcess.poll();

            OWLNamedIndividual ind1 = p.getIndividual1();
            OWLNamedIndividual ind2 = p.getIndividual2();
            OWLObjectProperty role = p.getProperty();

            OWLNamedIndividual originalInd1 = inputObjectNames.contains(ind1) ? ind1 : copyToObject.get(ind1);
            OWLNamedIndividual originalInd2 = inputObjectNames.contains(ind2) ? ind2 : copyToObject.get(ind2);

            //for(OWLObjectProperty role: ontology.getObjectPropertiesInSignature()) {

            //	OWLObjectPropertyAssertionAxiom roleAssertion = factory
            //			.getOWLObjectPropertyAssertionAxiom(role, originalInd1, originalInd2);

            //	if(ontology.containsAxiom(roleAssertion)) {
            RepairType type1 = objectToRepairType.get(ind1);
            RepairType type2 = objectToRepairType.get(ind2);

            Set<OWLClassExpression> successorSet = computeSuccessorSet(
                    type1, role, originalInd2);

            Set<OWLClassExpression> coveringSet = type2.getClassExpressions();

            if (!reasonerWithTBox.isCovered(successorSet, coveringSet)) {
                Set<RepairType> setOfRepairTypes =
                        typeHandler.findCoveringRepairTypes(type2, successorSet, originalInd2);

                for (RepairType newType : setOfRepairTypes) {
                    // the following could/should be simplified by using a hashmap
                    if (!objectToTypesWithCopies.get(originalInd2).contains(newType)) {
                        //objectToCopies.get(originalInd2).stream().anyMatch(copy -> newType.equals(objectToRepairType.get(copy)))) {
                        OWLNamedIndividual copy = createCopy(originalInd2, newType);

                        // add new triples to process with the copy as subject
                        for (OWLObjectPropertyAssertionAxiom x : asSubject.get(originalInd2)) {
                            for (OWLNamedIndividual objectCopy : objectToCopies.get(x.getObject())) {
                                if (setOfCollectedIndividuals.contains(objectCopy))
                                    triplesToProcess.add(new RATriple(copy, objectCopy, (OWLObjectProperty) x.getProperty()));
                            }
                        }
                        // add new triples to process with the copy as object
                        for (OWLObjectPropertyAssertionAxiom x : asObject.get(originalInd2)) {
                            for (OWLNamedIndividual subjectCopy : objectToCopies.get(x.getSubject())) {
                                if (setOfCollectedIndividuals.contains(subjectCopy))
                                    triplesToProcess.add(new RATriple(subjectCopy, copy, (OWLObjectProperty) x.getProperty()));
                            }
                        }

								/*for(OWLNamedIndividual individual : setOfCollectedIndividuals) {
										Pair<OWLNamedIndividual, OWLNamedIndividual> pair1 = Pair.of(individual, copy);
										queueOfPairs.add(pair1);

										if(!individual.equals(copy)) {// otherwise pair2 would be identical to pair1
											Pair<OWLNamedIndividual, OWLNamedIndividual> pair2 = Pair.of(copy, individual);
											queueOfPairs.add(pair2);
										}
								}*/
                    }


                }
            }
            //}
        }
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
        public K visit( OWLDataPropertyAssertionAxiom axiom) {
            if (sub) {
                return (K) axiom.getSubject();
            } else {
                return (K) axiom.getObject();
            }
        }

    }


}
