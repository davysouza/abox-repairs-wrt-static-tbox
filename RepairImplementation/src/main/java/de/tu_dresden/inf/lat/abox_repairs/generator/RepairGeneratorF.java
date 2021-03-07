package de.tu_dresden.inf.lat.abox_repairs.generator;

import com.google.common.collect.Sets;
import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.inf.lat.abox_repairs.repair_manager.RepairManagerBuilder;
import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairTypeGeneratorF;
import de.tu_dresden.inf.lat.abox_repairs.saturation.AnonymousVariableDetector;
import de.tu_dresden.inf.lat.abox_repairs.tools.UtilF;
import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

@Deprecated
public class RepairGeneratorF {

    private OWLOntology saturation, repair;
    private RepairManagerBuilder.RepairVariant repairVariant;
    private Map<OWLNamedIndividual, RepairType> repairSeedFunction;
    private Set<Pair<OWLNamedIndividual, RepairType>> objectNamesInTheRepair, individualNamesInTheRepair;
    private AnonymousVariableDetector anonymousVariableDetector;
    private ReasonerFacade reasonerWithoutTBox, reasonerWithTBox;
    private RepairTypeGeneratorF repairTypeGenerator;
    private boolean repairIsAlreadyComputed = false;

    public RepairGeneratorF(OWLOntology saturation, RepairManagerBuilder.RepairVariant repairVariant, Map<OWLNamedIndividual, RepairType> repairSeedFunction, ReasonerFacade reasonerWithoutTBox, ReasonerFacade reasonerWithTBox) {
        this.saturation = saturation;
        this.repairVariant = repairVariant;
        this.repairSeedFunction = repairSeedFunction;
        this.anonymousVariableDetector = AnonymousVariableDetector.newInstance(true, repairVariant);
        this.reasonerWithoutTBox = reasonerWithoutTBox;
        this.reasonerWithTBox = reasonerWithTBox;
        this.repairTypeGenerator = new RepairTypeGeneratorF(reasonerWithTBox, reasonerWithoutTBox);
    }

    private void enumerateIndividualNamesInTheRepair() {
        this.individualNamesInTheRepair = new HashSet<>();
        saturation.individualsInSignature()
                .filter(anonymousVariableDetector::isNamed)
                .map(individualName ->
                        Pair.of(
                                individualName,
                                repairSeedFunction.getOrDefault(individualName, RepairTypeGeneratorF.emptyRepairType())
                        )
                ).forEach(individualNamesInTheRepair::add);
    }

    private void enumerateObjectNamesInTheRepair() {
        this.objectNamesInTheRepair = new HashSet<>();
        switch (repairVariant) {
            case IQ:
                enumerateObjectNamesInTheIQRepair();
                break;
            case CQ:
                enumerateObjectNamesInTheCQRepair();
                break;
            case CANONICAL_IQ:
            case CANONICAL_CQ:
                enumerateObjectNamesInTheCanonicalRepair();
                break;
        }
    }

    private void enumerateObjectNamesInTheIQRepair() {
        objectNamesInTheRepair.addAll(individualNamesInTheRepair);
        final Queue<Pair<OWLNamedIndividual, RepairType>> queue = new LinkedList<>(objectNamesInTheRepair);
        while (!queue.isEmpty()) {
            final Pair<OWLNamedIndividual, RepairType> y_tK = queue.poll();
            final OWLNamedIndividual t = y_tK.getLeft();
            final RepairType K = y_tK.getRight();
            saturation.objectPropertyAssertionAxioms(t).forEach(roleAssertion -> {
                final OWLObjectProperty r = roleAssertion.getProperty().getNamedProperty();
                final OWLNamedIndividual u = roleAssertion.getObject().asOWLNamedIndividual();
                repairTypeGenerator.getMinimalRepairTypesCovering(Succ(K, r, u), u).forEach(L -> {
                    final Pair<OWLNamedIndividual, RepairType> y_uL = Pair.of(u, L);
                    if (objectNamesInTheRepair.add(y_uL))
                        queue.offer(y_uL);
                });
            });
        }
    }

    private void enumerateObjectNamesInTheCQRepair() {
        objectNamesInTheRepair.addAll(individualNamesInTheRepair);
        saturation.individualsInSignature()
                .map(objectName ->
                        Pair.of(
                                objectName,
                                RepairTypeGeneratorF.emptyRepairType()
                        )
                ).forEach(objectNamesInTheRepair::add);
        final Queue<Pair<Pair<OWLNamedIndividual, RepairType>, Pair<OWLNamedIndividual, RepairType>>> queue = new LinkedList<>();
        objectNamesInTheRepair.forEach(y_tK -> objectNamesInTheRepair.forEach(y_uL -> queue.offer(Pair.of(y_tK, y_uL))));
        while (!queue.isEmpty()) {
            final Pair<Pair<OWLNamedIndividual, RepairType>, Pair<OWLNamedIndividual, RepairType>> pair = queue.poll();
            final Pair<OWLNamedIndividual, RepairType> y_tK = pair.getLeft();
            final Pair<OWLNamedIndividual, RepairType> y_uL = pair.getRight();
            final OWLNamedIndividual t = y_tK.getLeft();
            final OWLNamedIndividual u = y_uL.getLeft();
            final RepairType K = y_tK.getRight();
            final RepairType L = y_uL.getRight();
            saturation.objectPropertyAssertionAxioms(t)
                    .filter(roleAssertion -> roleAssertion.getObject().asOWLNamedIndividual().equals(u))
                    .forEach(roleAssertion -> {
                        final OWLObjectProperty r = roleAssertion.getProperty().getNamedProperty();
                        repairTypeGenerator.getMinimalRepairTypesCovering(L, Succ(K, r, u), u).forEach(M -> {
                            final Pair<OWLNamedIndividual, RepairType> y_uM = Pair.of(u, M);
                            if (objectNamesInTheRepair.add(y_uM)) {
                                objectNamesInTheRepair.forEach(y_vN -> {
                                    if (!y_uM.equals(y_vN)) {
                                        queue.offer(Pair.of(y_uM, y_vN));
                                        queue.offer(Pair.of(y_vN, y_uM));
                                    }
                                });
                                queue.offer(Pair.of(y_uM, y_uM));
                            }
                        });
                    });
        }
    }

    private Set<OWLClassExpression> Succ(RepairType K, OWLObjectProperty r, OWLNamedIndividual u) {
        return K.getClassExpressions().stream()
                .filter(owlClassExpression -> owlClassExpression instanceof OWLObjectSomeValuesFrom)
                .map(owlClassExpression -> (OWLObjectSomeValuesFrom) owlClassExpression)
                .filter(owlObjectSomeValuesFrom -> owlObjectSomeValuesFrom.getProperty().getNamedProperty().equals(r))
                .map(OWLObjectSomeValuesFrom::getFiller)
                .filter(filler -> reasonerWithTBox.instanceOf(u, filler))
                .collect(Collectors.toSet());
    }

    private void enumerateObjectNamesInTheCanonicalRepair() {
        saturation.individualsInSignature().forEach(objectName -> {
            final Set<OWLClassExpression> instantiatedAtoms =
                    reasonerWithTBox.instanceOf(objectName).stream()
                            .filter(UtilF::isAtom)
                            .map(UtilF::toAtom)
                            .collect(Collectors.toSet());
            Sets.powerSet(instantiatedAtoms).stream()
                    .filter(this::notContainsComparableOWLClassExpressions)
                    .filter(setOfAtoms -> isPremiseSaturated(setOfAtoms, objectName))
                    .forEach(setOfAtoms -> objectNamesInTheRepair.add(Pair.of(objectName, RepairTypeGeneratorF.newRepairType(setOfAtoms))));
        });
    }

    private boolean notContainsComparableOWLClassExpressions(Set<OWLClassExpression> owlClassExpressions) {
        return owlClassExpressions.stream()
                .noneMatch(owlClassExpression1 -> owlClassExpressions.stream()
                        .filter(owlClassExpression2 -> !owlClassExpression1.equals(owlClassExpression2))
                        .anyMatch(owlClassExpression2 -> reasonerWithoutTBox.subsumedBy(owlClassExpression1, owlClassExpression2))
                );
    }

    private boolean isPremiseSaturated(Set<OWLClassExpression> atoms, OWLNamedIndividual owlIndividual) {
        return atoms.stream()
                .allMatch(atom ->
                        reasonerWithTBox.equivalentOrSubsumedBy(atom).stream()
                                .filter(subsumee -> reasonerWithTBox.instanceOf(owlIndividual, subsumee))
                                .allMatch(subsumee -> atoms.stream().anyMatch(btom -> reasonerWithoutTBox.subsumedBy(subsumee, btom)))
                );
    }

    private void constructOptimizedRepair() throws OWLOntologyCreationException {
        final OWLOntologyManager owlOntologyManager = saturation.getOWLOntologyManager();
        final OWLDataFactory owlDataFactory = owlOntologyManager.getOWLDataFactory();
        repair = owlOntologyManager.createOntology();
        final Map<Pair<OWLNamedIndividual, RepairType>, OWLIndividual> owlIndividuals = new HashMap<>();
        objectNamesInTheRepair.stream().forEach(y_tK -> {
            if (individualNamesInTheRepair.contains(y_tK))
                owlIndividuals.put(y_tK, y_tK.getLeft());
            else {
                owlIndividuals.put(y_tK, owlDataFactory.getOWLAnonymousIndividual());
            }
        });
        objectNamesInTheRepair.stream().forEach(y_tK -> {
            final OWLNamedIndividual t = y_tK.getLeft();
            final RepairType K = y_tK.getRight();
            saturation.getClassAssertionAxioms(t).stream()
                    .map(conceptAssertion -> conceptAssertion.getClassExpression().asOWLClass())
                    .filter(A -> !K.getClassExpressions().contains(A))
                    .map(A -> owlDataFactory.getOWLClassAssertionAxiom(A, owlIndividuals.get(y_tK)))
                    .forEach(repair::add);
            objectNamesInTheRepair.stream().forEach(y_uL -> {
                final OWLNamedIndividual u = y_uL.getLeft();
                final RepairType L = y_uL.getRight();
                saturation.getObjectPropertyAssertionAxioms(t).stream()
                        .filter(roleAssertion -> roleAssertion.getObject().equals(u))
                        .map(roleAssertion -> roleAssertion.getProperty().getNamedProperty())
                        .filter(r -> reasonerWithoutTBox.isCovered(Succ(K, r, u), L.getClassExpressions()))
                        .map(r -> owlDataFactory.getOWLObjectPropertyAssertionAxiom(r, owlIndividuals.get(y_tK), owlIndividuals.get(y_uL)))
                        .forEach(repair::add);
            });
        });
    }

    public OWLOntology constructAndGetOptimizedRepair() {
        if (!repairIsAlreadyComputed) {
            enumerateIndividualNamesInTheRepair();
            enumerateObjectNamesInTheRepair();
            try {
                constructOptimizedRepair();
            } catch (OWLOntologyCreationException e) {
                throw new RuntimeException(e);
            }
        }
        return repair;
    }

}
