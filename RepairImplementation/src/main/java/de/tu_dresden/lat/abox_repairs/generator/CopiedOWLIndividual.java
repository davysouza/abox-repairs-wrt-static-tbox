package de.tu_dresden.lat.abox_repairs.generator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.tu_dresden.lat.abox_repairs.repair_type.RepairType;
import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLIndividual;

import java.util.*;

public final class CopiedOWLIndividual {

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

    protected OWLIndividual getIndividualInTheSaturation() {
        return individualInTheSaturation;
    }

    protected RepairType getRepairType() {
        return repairType;
    }

    protected OWLIndividual getIndividualInTheRepair() {
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

    protected static final class Factory {

        private final Multimap<OWLIndividual, CopiedOWLIndividual> lookupTableCQ = HashMultimap.create();
        private final Map<Pair<OWLIndividual, RepairType>, CopiedOWLIndividual> lookupTableIQ = new HashMap<>();
        private int nextAnonymousIndividual = 0;

        protected Factory() {
            super();
        }

        protected CopiedOWLIndividual newNamedIndividual(
                OWLIndividual individualInTheSaturation,
                RepairType repairType) {
            final CopiedOWLIndividual copiedOWLIndividual = new CopiedOWLIndividual(individualInTheSaturation, repairType, individualInTheSaturation);
            lookupTableCQ.put(individualInTheSaturation, copiedOWLIndividual);
            lookupTableIQ.put(Pair.of(individualInTheSaturation, repairType), copiedOWLIndividual);
            return copiedOWLIndividual;
        }

        protected CopiedOWLIndividual newAnonymousIndividual(
                OWLIndividual individualInTheSaturation,
                RepairType repairType) {
            // final CopiedOWLIndividual copiedOWLIndividual = new CopiedOWLIndividual(individualInTheSaturation, repairType, OWLManager.getOWLDataFactory().getOWLAnonymousIndividual());
            final CopiedOWLIndividual copiedOWLIndividual = new CopiedOWLIndividual(individualInTheSaturation, repairType, OWLManager.getOWLDataFactory().getOWLNamedIndividual("anonymous_individual_" + nextAnonymousIndividual++));
            lookupTableCQ.put(individualInTheSaturation, copiedOWLIndividual);
            lookupTableIQ.put(Pair.of(individualInTheSaturation, repairType), copiedOWLIndividual);
            return copiedOWLIndividual;
        }

        protected Collection<CopiedOWLIndividual> getCopiesOf(OWLIndividual individualInTheSaturation) {
            return Collections.unmodifiableCollection(lookupTableCQ.get(individualInTheSaturation));
        }

        protected Optional<CopiedOWLIndividual> getCopy(OWLIndividual individualInTheSaturation,
                                                        RepairType repairType) {
            if (lookupTableIQ.containsKey(Pair.of(individualInTheSaturation, repairType))) {
                return Optional.of(lookupTableIQ.get(Pair.of(individualInTheSaturation, repairType)));
            } else {
                return Optional.empty();
            }
        }

        protected boolean containsCopy(OWLIndividual individualInTheSaturation,
                                       RepairType repairType) {
            return lookupTableIQ.containsKey(Pair.of(individualInTheSaturation, repairType));
        }

        protected int size() {
            return lookupTableIQ.size();
        }

    }

}
