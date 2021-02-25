package de.tu_dresden.lat.abox_repairs.generator;

import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLIndividual;

import java.util.Objects;

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

    public static CopiedOWLIndividual newNamedIndividual(
            OWLIndividual individualInTheSaturation,
            RepairType repairType) {
        return new CopiedOWLIndividual(individualInTheSaturation, repairType, individualInTheSaturation);
    }

    public static CopiedOWLIndividual newAnonymousIndividual(
            OWLIndividual individualInTheSaturation,
            RepairType repairType) {
        return new CopiedOWLIndividual(individualInTheSaturation, repairType, OWLManager.getOWLDataFactory().getOWLAnonymousIndividual());
    }

    public OWLIndividual getIndividualInTheSaturation() {
        return individualInTheSaturation;
    }

    public RepairType getRepairType() {
        return repairType;
    }

    public OWLIndividual getIndividualInTheRepair() {
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

}
