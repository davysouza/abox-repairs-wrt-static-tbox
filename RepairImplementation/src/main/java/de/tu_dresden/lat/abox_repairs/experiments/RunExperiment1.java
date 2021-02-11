package de.tu_dresden.lat.abox_repairs.experiments;

import org.semanticweb.owlapi.model.OWLOntologyManager;

public class RunExperiment1 {
    private enum RepairVariant {IQ, CQ};

    public static void main(String[] args) {
        if(args.length!=3) {
            System.out.println("Usage: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+ " ONTOLOGY_FILE [IQ|CQ] PROPORTION");
            System.out.println();
            System.out.println("Generates a repair of ONTOLOGY_FILE with a randomly generated repair request that");
            System.out.println("randomly assigns concept names to each individual name so that a proportion of ");
            System.out.println("PROPORTION of the entire set of concept names is selected");
            System.out.println();
            System.out.println("Example: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getClass().getCanonicalName()+" ore_ont_3453.owl IQ 0.2");
            System.exit(0);
        }

        String ontologyFileName = args[0];
        RepairVariant repairVariant;
        switch(args[1]) {
            case "IQ": repairVariant = RepairVariant.IQ; break;
            case "CQ": repairVariant = RepairVariant.CQ; break;
            default:
                System.out.println("Unexpected repair variant: "+args[1]);
                System.out.println("Call without parameters to get help information");
                repairVariant=RepairVariant.CQ;
                System.exit(1);
        }

        double proportion = Double.parseDouble(args[2]);

        startExperiment(ontologyFileName, repairVariant, proportion);
    }

    private static void startExperiment(String ontologyFileName, RepairVariant repairVariant, double proportion) {
        // TODO implement!
    }
}
