package de.tu_dresden.inf.lat.abox_repairs.generator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairType;
import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLIndividual;

import java.util.*;

final class CopiedOWLIndividual {

    private final OWLIndividual individualInTheSaturation; // t
    private final RepairType repairType;                   // 𝒦
    private final OWLIndividual individualInTheRepair;     // y_{t,𝒦}

    private CopiedOWLIndividual(
            OWLIndividual individualInTheSaturation,
            RepairType repairType,
            OWLIndividual individualInTheRepair
    ) {
        this.individualInTheSaturation = individualInTheSaturation;
        this.repairType = repairType;
        this.individualInTheRepair = individualInTheRepair;
    }

    OWLIndividual getIndividualInTheSaturation() {
        return individualInTheSaturation;
    }

    RepairType getRepairType() {
        return repairType;
    }

    OWLIndividual getIndividualInTheRepair() {
        return individualInTheRepair;
    }

    /* The field 'individualInTheRepair' is intentionally not compared. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CopiedOWLIndividual that = (CopiedOWLIndividual) o;
        return individualInTheSaturation.equals(that.individualInTheSaturation) && repairType.equals(that.repairType);
    }

    /* The field 'individualInTheRepair' is intentionally not hashed. */
    @Override
    public int hashCode() {
        return Objects.hash(individualInTheSaturation, repairType);
    }

    static class FactoryIQ {

        private final Map<Pair<OWLIndividual, RepairType>, CopiedOWLIndividual> lookupTableIQ = new HashMap<>();
//        private int nextAnonymousIndividual = 0;

        FactoryIQ() {
            super();
        }

        CopiedOWLIndividual newNamedIndividual(
                OWLIndividual individualInTheSaturation,
                RepairType repairType) {
            final CopiedOWLIndividual copiedOWLIndividual = new CopiedOWLIndividual(individualInTheSaturation, repairType, individualInTheSaturation);
            lookupTableIQ.put(Pair.of(individualInTheSaturation, repairType), copiedOWLIndividual);
            return copiedOWLIndividual;
        }

        CopiedOWLIndividual newAnonymousIndividual(
                OWLIndividual individualInTheSaturation,
                RepairType repairType) {
            final CopiedOWLIndividual copiedOWLIndividual = new CopiedOWLIndividual(individualInTheSaturation, repairType, OWLManager.getOWLDataFactory().getOWLAnonymousIndividual());
//            final CopiedOWLIndividual copiedOWLIndividual = new CopiedOWLIndividual(individualInTheSaturation, repairType, OWLManager.getOWLDataFactory().getOWLNamedIndividual("anonymous_individual_" + nextAnonymousIndividual++));
            lookupTableIQ.put(Pair.of(individualInTheSaturation, repairType), copiedOWLIndividual);
            return copiedOWLIndividual;
        }

        Optional<CopiedOWLIndividual> getCopy(OWLIndividual individualInTheSaturation,
                                              RepairType repairType) {
            if (lookupTableIQ.containsKey(Pair.of(individualInTheSaturation, repairType))) {
                return Optional.of(lookupTableIQ.get(Pair.of(individualInTheSaturation, repairType)));
            } else {
                return Optional.empty();
            }
        }

        boolean containsCopy(OWLIndividual individualInTheSaturation,
                             RepairType repairType) {
            return lookupTableIQ.containsKey(Pair.of(individualInTheSaturation, repairType));
        }

        int size() {
            return lookupTableIQ.size();
        }

    }

    static final class FactoryCQ extends FactoryIQ {

        private final Multimap<OWLIndividual, CopiedOWLIndividual> lookupTableCQ = HashMultimap.create();

        FactoryCQ() {
            super();
        }

        CopiedOWLIndividual newNamedIndividual(
                OWLIndividual individualInTheSaturation,
                RepairType repairType) {
            final CopiedOWLIndividual copiedOWLIndividual = super.newNamedIndividual(individualInTheSaturation, repairType);
            lookupTableCQ.put(individualInTheSaturation, copiedOWLIndividual);
            return copiedOWLIndividual;
        }

        CopiedOWLIndividual newAnonymousIndividual(
                OWLIndividual individualInTheSaturation,
                RepairType repairType) {
            final CopiedOWLIndividual copiedOWLIndividual = super.newAnonymousIndividual(individualInTheSaturation, repairType);
            lookupTableCQ.put(individualInTheSaturation, copiedOWLIndividual);
            return copiedOWLIndividual;
        }

        Collection<CopiedOWLIndividual> getCopiesOf(OWLIndividual individualInTheSaturation) {
            return Collections.unmodifiableCollection(lookupTableCQ.get(individualInTheSaturation));
        }

    }

}
