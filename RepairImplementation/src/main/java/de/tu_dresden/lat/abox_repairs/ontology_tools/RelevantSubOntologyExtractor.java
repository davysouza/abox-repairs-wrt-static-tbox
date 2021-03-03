package de.tu_dresden.lat.abox_repairs.ontology_tools;

import de.tu_dresden.lat.abox_repairs.repair_request.RepairRequest;

import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

import java.util.HashSet;
import java.util.Set;

/**
 * @author koopmann
 */
public class RelevantSubOntologyExtractor {

    private final OWLOntology ontology;

    public RelevantSubOntologyExtractor(OWLOntology ontology){
        this.ontology=ontology;
    }

    private static long counter = 0;

    public OWLOntology relevantSubOntology(RepairRequest repairRequest) throws OWLOntologyCreationException {
        Set<OWLEntity> signature = signatureOf(repairRequest);

        // TODO bot might not contain all we need
        // do manual: compute set of individual names connected, and module of instantiations' signature
        SyntacticLocalityModuleExtractor moduleExtractor =
                new SyntacticLocalityModuleExtractor(ontology.getOWLOntologyManager(), ontology, ModuleType.BOT);

        counter++;
        return moduleExtractor.extractAsOntology(signature, IRI.create("mod"+counter));
    }

    private Set<OWLEntity> signatureOf(RepairRequest repairRequest) {
        Set<OWLEntity> result = new HashSet<>();
        for(OWLNamedIndividual ind:repairRequest.individuals()){
            result.add(ind);
            for(OWLClassExpression cl:repairRequest.get(ind)){
                result.addAll(cl.getSignature());
            }
        }
        return result;
    }
}
