package de.tudresden.inf.lat.aboxrepair.ontologytools;

import de.tudresden.inf.lat.aboxrepair.repairrequest.RepairRequest;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

import java.util.HashSet;
import java.util.Set;

public class RelevantSubOntologyExtractor {
    private final OWLOntology ontology;

    public RelevantSubOntologyExtractor(OWLOntology ontology){
        this.ontology = ontology;
    }

    private static long counter = 0;

    public OWLOntology relevantSubOntology(RepairRequest repairRequest) throws OWLOntologyCreationException {
        Set<OWLEntity> signature = signatureOf(repairRequest);

        // TODO bot might not contain all we need
        // do manual: compute set of individual names connected, and module of instantiations' signature
        SyntacticLocalityModuleExtractor moduleExtractor =
                new SyntacticLocalityModuleExtractor(ontology.getOWLOntologyManager(), ontology, ModuleType.BOT);

        counter++;
        return moduleExtractor.extractAsOntology(signature, IRI.create("mod" + counter));
    }

    private Set<OWLEntity> signatureOf(RepairRequest repairRequest) {
        Set<OWLEntity> result = new HashSet<>();
        for(OWLNamedIndividual individual : repairRequest.individuals()) {
            result.add(individual);
            for(OWLClassExpression cl : repairRequest.get(individual)) {
                result.addAll(cl.getSignature());
            }
        }
        return result;
    }
}
