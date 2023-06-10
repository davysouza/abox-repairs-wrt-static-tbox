package de.tudresden.inf.lat.aboxrepair.repairtype;

import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Implements a RepairType based on the Definition 5 of the paper.
 *
 * @see <a href="https://lat.inf.tu-dresden.de/research/papers/2021/BaKoKrNu-CADE2021.pdf">Computing Optimal Repairs of
 * Quantified ABoxes w.r.t. Static EL TBoxes</a>
 *
 * @author Patrick Koopmann
 */
public class RepairType {
    /**
     * Set of atoms that compose the repair type.
     */
    private final Set<OWLClassExpression> atoms;

    /**
     * RepairType constructor. Package-visible only.
     * @param atoms Set of atoms
     */
    RepairType(Set<OWLClassExpression> atoms) {
        this.atoms = atoms;
    }

    /**
     * Returns a repair type with an empty set of atoms.
     * @return Returns a repair type with an empty set of atoms.
     */
    public static final RepairType empty() {
        return new RepairType(Collections.emptySet());
    }

    /**
     * Get the set of atoms of the repair type
     * @return Return a Set<OWLClassExpression> object representing the set of atoms
     */
    public Set<OWLClassExpression> getClassExpressions() {
        return Collections.unmodifiableSet(atoms);
    }

    /**
     * Check if the repair type is empty
     * @return Returns true if is empty, false otherwise
     */
    public final boolean isEmpty() {
        return atoms.isEmpty();
    }

    // region Override

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof RepairType))
            return false;

        RepairType repairType = (RepairType) o;
        return Objects.equals(atoms, repairType.atoms);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(atoms);
    }

    @Override
    public String toString() {
        return "RepairType { " + atoms + " }";
    }

    // endregion
}
