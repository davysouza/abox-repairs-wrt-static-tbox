package de.tu_dresden.lat.abox_repairs;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.HashMap;
import java.util.Set;

public class RepairRequest extends HashMap<OWLNamedIndividual, Set<OWLClassExpression>> {
    public RepairRequest(){
        super();
    }
}
