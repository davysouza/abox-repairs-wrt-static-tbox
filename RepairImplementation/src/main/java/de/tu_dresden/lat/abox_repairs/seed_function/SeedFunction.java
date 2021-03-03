package de.tu_dresden.lat.abox_repairs.seed_function;

import de.tu_dresden.lat.abox_repairs.repair_type.RepairType;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.HashMap;

public class SeedFunction extends HashMap<OWLNamedIndividual, RepairType> {

    public SeedFunction() {
        super();
    }

    @Override
    public RepairType get(Object key) {
        return super.getOrDefault(key, RepairType.empty());
    }

}
