package de.tu_dresden.lat.abox_repairs.repair_types;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;


import org.semanticweb.owlapi.model.OWLClassExpression;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;

/**
 * @author Patrick Koopmann
 */
public class RepairType {
    private final Set<OWLClassExpression> toRepair;

    // package visibility since it should only be used by the RepairTypeHandler
    RepairType(Set<OWLClassExpression> toRepair){
        this.toRepair=toRepair;
    }

    public Set<OWLClassExpression> getClassExpressions(){
        return Collections.unmodifiableSet(toRepair);
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
}
