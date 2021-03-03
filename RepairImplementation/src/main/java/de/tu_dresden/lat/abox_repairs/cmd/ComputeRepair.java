package de.tu_dresden.lat.abox_repairs.cmd;

import de.tu_dresden.lat.abox_repairs.repairManager.RepairManager;
import de.tu_dresden.lat.abox_repairs.repairManager.RepairManagerBuilder;
import de.tu_dresden.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.lat.abox_repairs.repair_request.RepairRequestParser;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.io.IOException;

/**
 * TODO explain what this class does
 */
public final class ComputeRepair {

    private ComputeRepair() {
    }

    public static void main(String args[]) throws IOException, OWLOntologyCreationException, SaturationException {
        int i = 0;
        while (i < args.length) {
            // Initialize ontology
            // m.ontologyInitialisation(args, i);

            OWLOntology ontology =
                    OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(args[i]));

            //logger.debug("after loading ontology: ");

            //System.out.println(m.ontology.getNestedClassExpressions());

            File file = new File(args[i + 1]);
            RepairRequestParser rrParser = new RepairRequestParser(ontology);
            RepairRequest repairRequest = rrParser.repairRequestScanning(file);

            RepairManagerBuilder.RepairVariant variant;

            switch (args[i + 2]) {
                case "IQ":
                    variant = RepairManagerBuilder.RepairVariant.IQ;
                    break;
                case "CQ":
                    variant = RepairManagerBuilder.RepairVariant.CQ;
                    break;
                case "CANONICAL_IQ":
                    variant = RepairManagerBuilder.RepairVariant.CANONICAL_IQ;
                    break;
                case "CANONICAL_CQ":
                    variant = RepairManagerBuilder.RepairVariant.CANONICAL_CQ;
                    break;
                default:
                    System.out.println("Unknown repairVariant: " + args[i + 2]);
                    System.exit(1);
                    variant = RepairManagerBuilder.RepairVariant.CQ;
            }

            boolean saturate = args[i + 3].equals("true");

            RepairManager m =
                    new RepairManagerBuilder()
                    .setOntology(ontology)
                    .setRepairRequest(repairRequest)
                    .setVariant(variant)
                    .setNeedsSaturation(saturate)
                    .build();

            m.performRepair();


            i += 4;
            if (i < args.length)
                System.out.println("\n" + "=================================================");
//			}
        }
    }
}
