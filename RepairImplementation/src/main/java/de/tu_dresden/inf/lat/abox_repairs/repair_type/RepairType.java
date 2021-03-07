package de.tu_dresden.inf.lat.abox_repairs.repair_type;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;


import org.semanticweb.owlapi.model.OWLClassExpression;

/**
 * @author Patrick Koopmann
 */
public class RepairType {

    public static final RepairType empty() {
        return new RepairType(Collections.emptySet());
    }

    private final Set<OWLClassExpression> toRepair;

    // package visibility since it should only be used by the RepairTypeHandler
    RepairType(Set<OWLClassExpression> toRepair){
        this.toRepair=toRepair;
    }

    public Set<OWLClassExpression> getClassExpressions(){
        return Collections.unmodifiableSet(toRepair);
    }

    public final boolean isEmpty() {
        return toRepair.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RepairType)) {
            return false;
        }
        RepairType repairType = (RepairType) o;
        return Objects.equals(toRepair, repairType.toRepair);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(toRepair);
    }

    @Override
    public String toString() {
        return "RepairType{" + toRepair + '}';
    }
}
