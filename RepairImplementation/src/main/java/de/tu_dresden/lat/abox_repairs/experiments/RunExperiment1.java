package de.tu_dresden.lat.abox_repairs.experiments;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.RepairRequest;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class RunExperiment1 {

    public static void main(String[] args) throws OWLOntologyCreationException, SaturationException {
        if(args.length!=3) {
            System.out.println("Usage: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+ " ONTOLOGY_FILE [IQ|CQ] PROPORTION");
            System.out.println();
            System.out.println("Generates a repair of ONTOLOGY_FILE with a randomly generated repair request that");
            System.out.println("randomly assigns concept names to each individual name so that a proportion of ");
            System.out.println("PROPORTION of the entire set of concept names is selected");
            System.out.println();
            System.out.println("Example: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+" ore_ont_3453.owl IQ 0.2");
            System.exit(0);
        }

        String ontologyFileName = args[0];
        Main.RepairVariant repairVariant;
        switch(args[1]) {
            case "IQ": repairVariant = Main.RepairVariant.IQ; break;
            case "CQ": repairVariant = Main.RepairVariant.CQ; break;
            default:
                System.out.println("Unexpected repair variant: "+args[1]);
                System.out.println("Call without parameters to get help information");
                repairVariant=Main.RepairVariant.CQ;
                System.exit(1);
        }

        double proportion = Double.parseDouble(args[2]);

        startExperiment(ontologyFileName, repairVariant, proportion);
    }

    private static void startExperiment(String ontologyFileName, Main.RepairVariant repairVariant, double proportion)
            throws OWLOntologyCreationException, SaturationException {

        OWLOntology ontology =
                OWLManager.createOWLOntologyManager()
                        .loadOntologyFromOntologyDocument(new File(ontologyFileName));

        RepairRequest repairRequest = generateRepairRequest(ontology, proportion);

        Main main = new Main();
        main.performRepair(ontology, repairRequest, repairVariant);
    }

    private static RepairRequest generateRepairRequest(OWLOntology ontology, double proportion) {
        RepairRequest request = new RepairRequest();

        List<OWLClass> classList = ontology.classesInSignature().collect(Collectors.toList());

        ontology.individualsInSignature().forEach(individual ->
            request.put(individual, randomClasses(classList, proportion))
        );

        return request;
    }

    private static Set<OWLClassExpression> randomClasses(List<OWLClass> classList, double proportion) {
        Set<OWLClassExpression> result = new HashSet<>();
        Random random = new Random();

        for(int i=0; i<proportion*classList.size(); i++) {
            OWLClass cl = classList.get(random.nextInt(classList.size()));
            while(result.contains(cl)){
                cl = classList.get(random.nextInt(classList.size()));
            }
            result.add(cl);
        }
        return result;
    }
}
