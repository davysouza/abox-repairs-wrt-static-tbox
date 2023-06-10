package de.tudresden.inf.lat.aboxrepair;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import de.tudresden.inf.lat.aboxrepair.repairmanager.RepairManager;
import de.tudresden.inf.lat.aboxrepair.repairmanager.RepairManagerBuilder;
import de.tudresden.inf.lat.aboxrepair.repairmanager.RepairManagerBuilder.RepairVariant;
import de.tudresden.inf.lat.aboxrepair.repairrequest.RepairRequest;
import de.tudresden.inf.lat.aboxrepair.repairrequest.RepairRequestParser;
import de.tudresden.inf.lat.aboxrepair.utils.PrettyMessage;

import java.io.File;

/**
 * Implements a command line to repair the specified ontology according to the given parameters. <br/><br/>
 * <b>Running:</b><br/>
 *      java -cp compute-repair.jar ONTOLOGY_FILEPATH REPAIR_REQUEST_FILEPATH REPAIR_VARIANT NEEDS_SATURATION
 */
public final class ComputeRepair {
    private static File ontologyFile;
    private static File repairRequestFile;
    private static RepairVariant repairVariant;
    private static boolean needsSaturation;

    /**
     * Main method. Responsible for parsing args and calling the repair for the given ontology
     *
     * @param args String array of the arguments
     * @throws OWLOntologyCreationException
     */
    public static void main(String args[]) {
        if(!parseArguments(args)) {
            System.exit(1);
        }

        try {
            OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontologyFile);

            RepairRequestParser repairRequestParserParser = new RepairRequestParser(ontology);
            RepairRequest repairRequest = repairRequestParserParser.parse(repairRequestFile);

            RepairManager repairManager =
                    new RepairManagerBuilder()
                            .setOntology(ontology)
                            .setRepairRequest(repairRequest)
                            .setVariant(repairVariant)
                            .setNeedsSaturation(needsSaturation)
                            .build();

            repairManager.initAndPerformRepair();

        } catch(Exception e) {
            PrettyMessage.printError("ERROR: Failed to repair ontology\n");
            System.out.println("Exception: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Parse the given arguments that will be used to compute the repair.
     * The method expects to read the arguments at the following order:<br/>
     *    - ontology filepath<br/>
     *    - repair request filepath<br/>
     *    - repair variant<br/>
     *    - needs saturation
     *
     * @param args String array of the arguments
     * @return Returns true if args could be parsed, false otherwise.
     */
    private static boolean parseArguments(String args[]) {
        if(args == null || args.length != 4) {
            PrettyMessage.printError("ERROR: Invalid or missing arguments\n");
            help();
            return false;
        }

        try {
            ontologyFile = new File(args[0]);
            repairRequestFile = new File(args[1]);

            switch (args[2]) {
                case "IQ":
                    repairVariant = RepairVariant.IQ;
                    break;
                case "IQ2":
                    repairVariant = RepairVariant.IQ2;
                    break;
                case "CQ":
                    repairVariant = RepairVariant.CQ;
                    break;
                case "CANONICAL_IQ":
                    repairVariant = RepairVariant.CANONICAL_IQ;
                    break;
                case "CANONICAL_CQ":
                    repairVariant = RepairVariant.CANONICAL_CQ;
                    break;
                default:
                    PrettyMessage.printError("ERROR: Unknown repair variant: " + args[2] + "\n");
                    help();
                    return false;
            }

            needsSaturation = args[3].equals("true");
        } catch (Exception e) {
            PrettyMessage.printError("ERROR: Failed to parse arguments\n");
            System.out.println("Exception: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Prints help
     */
    private static void help() {
        System.out.println("Compute Repair:");
        System.out.println("    Computes a repair of the specified ontology according to the given arguments.");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("    java -cp de.tudresden.inf.lat.aboxrepair.ComputeRepair.jar ONTOLOGY_FILEPATH REPAIR_REQUEST_FILEPATH REPAIR_VARIANT NEEDS_SATURATION");
        System.out.println();
        System.out.println("    Computes a repair of ONTOLOGY_FILEPATH w.r.t. the REPAIR_REQUEST_FILEPATH and the selected");
        System.out.println("    REPAIR_VARIANT. If ontology needs saturation, the NEEDS_SATURATION flag must be set true.");
        System.out.println("    The REPAIR_VARIANT value should be a string with containing one of the following values:");
        System.out.println("    \"IQ\", \"IQ2\", \"CQ\", \"CANONICAL_IQ\" or \"CANONICAL_CQ\"\n");
    }
}
