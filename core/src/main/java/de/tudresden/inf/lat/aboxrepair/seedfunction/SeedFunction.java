package de.tudresden.inf.lat.aboxrepair.seedfunction;

import org.semanticweb.owlapi.model.OWLNamedIndividual;
import de.tudresden.inf.lat.aboxrepair.repairtype.RepairType;

import java.util.HashMap;

/**
 * Implements a SeedFunction based on the Definition 6 of the paper.
 * <p>
 * A seed function is a function s that maps each individual name b to a repair type s(b)
 *
 * @see <a href="https://lat.inf.tu-dresden.de/research/papers/2021/BaKoKrNu-CADE2021.pdf">Computing Optimal Repairs of
 * Quantified ABoxes w.r.t. Static EL TBoxes</a>
 *
 */
public class SeedFunction extends HashMap<OWLNamedIndividual, RepairType> {
    public SeedFunction() {
        super();
    }

    @Override
    public RepairType get(Object key) {
        return super.getOrDefault(key, RepairType.empty());
    }
}
