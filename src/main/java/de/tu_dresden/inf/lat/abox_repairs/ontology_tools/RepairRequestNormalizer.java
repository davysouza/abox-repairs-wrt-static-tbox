package de.tu_dresden.inf.lat.abox_repairs.ontology_tools;

import de.tu_dresden.inf.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.inf.lat.abox_repairs.tools.Util;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class RepairRequestNormalizer {

    public static RepairRequest normalizeRepairRequest(OWLOntologyManager ontologyManager, RepairRequest repairRequest) throws OWLOntologyCreationException {
        final ClassExpressionNormalizer normalizer = new ClassExpressionNormalizer(ontologyManager);
        final RepairRequest normalizedRepairRequest = new RepairRequest();
        for (OWLNamedIndividual individual : repairRequest.individuals()) {
            normalizedRepairRequest.put(individual,
                    Util.getMinimalTransformedElements(repairRequest.get(individual), normalizer::normalize, normalizer::isSubsumedBy));
        }
        return normalizedRepairRequest;
    }

}
