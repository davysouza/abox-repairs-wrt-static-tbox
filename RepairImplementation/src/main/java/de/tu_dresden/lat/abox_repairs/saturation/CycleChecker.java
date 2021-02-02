package de.tu_dresden.lat.abox_repairs.saturation;

import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * Checks whether an EL ontology is cycle-restricted.
 * 
 * O is cycle-restricted if for no concept C and word w of role names,
 * O entails C subumedBy exists w.C
 * 
 * To detect this, we use the canonical model as implicitlly given in the classification result.
 * 
 * 
 * @author Patrick Koopmann
 */
public class CycleChecker extends {

    private final ElkReasoner reasoner;

    public CycleChecker(ElkReasoner reasoner) {


        this.reasoner = reasoner;
    }

    public boolean cyclic(OWLOntology ontology) {
        MultiMap<OWLClassEx, OWLClass> dependencies = new HashMultimap<>();

        ontology.tboxAxioms(Imports.INCLUDED).forEach( axiom ->
            Collection<OWLClass> lhs = lhsClasses(axiom);
            Collection<OWLClass> dependencies = 
        );
    }
}
