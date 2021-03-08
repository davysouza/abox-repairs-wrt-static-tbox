package de.tu_dresden.inf.lat.abox_repairs.repair_type;

import com.google.common.collect.Sets;
import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.inf.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.inf.lat.abox_repairs.tools.UtilF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO work in progress
 */
@Deprecated
public class RepairTypeGeneratorF {

    private static final Logger logger = LogManager.getLogger(RepairTypeGeneratorF.class);
    private final ReasonerFacade reasonerWithTBox, reasonerWithoutTBox;

    public RepairTypeGeneratorF(ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox) {
        this.reasonerWithTBox = reasonerWithTBox;
        this.reasonerWithoutTBox = reasonerWithoutTBox;
    }

    public static RepairType emptyRepairType() {
        return newRepairType(Collections.emptySet());
    }

    public static RepairType newRepairType(Set<OWLClassExpression> owlClassExpressions) {
        return new RepairType(owlClassExpressions);
    }

    public Set<Map<OWLNamedIndividual, RepairType>> getMinimalRepairSeedFunctions(RepairRequest repairRequest) {
        final Set<Map<OWLNamedIndividual, RepairType>> minimalRepairSeedFunctions = new HashSet<>();
        minimalRepairSeedFunctions.add(new HashMap<>());
        for (OWLNamedIndividual owlNamedIndividual : repairRequest.individuals()) {
            final Set<Map<OWLNamedIndividual, RepairType>> currentMinimalRepairSeedFunctions = new HashSet<>(minimalRepairSeedFunctions);
            minimalRepairSeedFunctions.clear();
            for (RepairType repairType : getMinimalRepairTypesCovering(repairRequest.get(owlNamedIndividual), owlNamedIndividual))
                for (Map<OWLNamedIndividual, RepairType> repairSeedFunction : currentMinimalRepairSeedFunctions)
                    minimalRepairSeedFunctions.add(UtilF.newHashMap(repairSeedFunction, owlNamedIndividual, repairType));
        }
        return minimalRepairSeedFunctions;
    }

    public Set<RepairType> getMinimalRepairTypesCovering(RepairType repairType, Set<OWLClassExpression> owlClassExpressions, OWLNamedIndividual owlIndividual) {
        return getMinimalRepairTypesCovering(Sets.union(repairType.getClassExpressions(), owlClassExpressions), owlIndividual);
    }

    public Set<RepairType> getMinimalRepairTypesCovering(Set<OWLClassExpression> owlClassExpressions, OWLNamedIndividual owlIndividual) {
        final Set<Set<OWLClassExpression>> candidates = new HashSet<>(new HashSet<>());
        final Set<Set<OWLClassExpression>> repairTypes = new HashSet<>();
        boolean firstIteration = true;
        while (!candidates.isEmpty()) {
            final Set<Set<OWLClassExpression>> currentCandidates = new HashSet<>(candidates);
            candidates.clear();
            for (Set<OWLClassExpression> candidate : currentCandidates) {
                final Set<OWLClassExpression> nonCoveredOWLClassExpressions;
                if (firstIteration) {
                    nonCoveredOWLClassExpressions =
                            owlClassExpressions.stream()
                                    .filter(owlClassExpression -> reasonerWithTBox.instanceOf(owlIndividual, owlClassExpression))
                                    .collect(Collectors.toSet());
                    firstIteration = false;
                } else {
                    nonCoveredOWLClassExpressions =
                            candidate.stream().flatMap(atom ->
                                    reasonerWithTBox.equivalentIncludingOWLThingAndOWLNothingOrStrictlySubsumedByExcludingOWLThingAndOWLNothing(atom).stream()
                                            .filter(subsumee -> reasonerWithTBox.instanceOf(owlIndividual, subsumee))
                                            .filter(subsumee -> candidate.stream().noneMatch(btom -> reasonerWithoutTBox.subsumedBy(subsumee, btom)))
                            ).collect(Collectors.toSet());
                }
                if (nonCoveredOWLClassExpressions.isEmpty()) {
                    repairTypes.add(getSubsumptionMaximalOWLClassExpressions(candidate));
                } else {
                    final Set<Set<OWLClassExpression>> newCandidates = new HashSet<>();
                    newCandidates.add(candidate);
                    for (OWLClassExpression nonCoveredOWLClassExpression : nonCoveredOWLClassExpressions) {
                        final Set<Set<OWLClassExpression>> currentNewCandidates = new HashSet<>(newCandidates);
                        newCandidates.clear();
                        reasonerWithoutTBox.equivalentIncludingOWLThingOrStrictlySubsumingExcludingOWLThing(nonCoveredOWLClassExpression).stream()
                                .filter(UtilF::isAtom)
                                .map(UtilF::toAtom)
                                .forEach(atom -> {
                                    for (Set<OWLClassExpression> currentNewCandidate : currentNewCandidates)
                                        newCandidates.add(UtilF.newHashSet(currentNewCandidate, atom));
                                });
                    }
                    candidates.addAll(newCandidates);
                }
            }
        }
        return getCoverMinimalOWLClassExpressionSets(repairTypes).stream().map(RepairType::new).collect(Collectors.toSet());
    }

    private Set<OWLClassExpression> getSubsumptionMaximalOWLClassExpressions(Set<OWLClassExpression> owlClassExpressions) {
        return UtilF.getMaximalElements(owlClassExpressions, x -> x, reasonerWithoutTBox::subsumedBy);
    }

    private Set<Set<OWLClassExpression>> getCoverMinimalOWLClassExpressionSets(Set<Set<OWLClassExpression>> owlClassExpressionSets) {
        return UtilF.getMaximalElements(owlClassExpressionSets, x -> x, (x, y) -> reasonerWithoutTBox.isCovered(y, x));
    }

}
