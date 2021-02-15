package de.tu_dresden.lat.abox_repairs.experiments;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.RepairRequest;
import de.tu_dresden.lat.abox_repairs.ontology_tools.OntologyPreparations;
import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a repair request by randomly selecting an entailed concept assertion.
 */
public class RunExperiment2 {


    public static void main(String[] args) throws OWLOntologyCreationException, SaturationException {
        if(args.length<2) {
            System.out.println("Usage: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+ " ONTOLOGY_FILE IQ|CQ [SEED]");
            System.out.println();
            System.out.println("Generates a repair of ONTOLOGY_FILE with a randomly generated repair request that");
            System.out.println("randomly selects an entailed concept assertion. You may optionally provide");
            System.out.println("a seed value for the random number generator used.");
            System.out.println();
            System.out.println("Example: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+" ore_ont_3453.owl IQ 0.1 0.2");
            System.exit(0);
        }

        String ontologyFileName = args[0];
        Main.RepairVariant repairVariant = pickVariant(args[1]);


        RunExperiment2 experiment = new RunExperiment2();

        if(args.length>2){
            long seed = Long.parseLong(args[2]);
            experiment.setSeed(seed);
        }
        try {
            experiment.startExperiment(ontologyFileName, repairVariant);
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.out.println("Used seed: "+experiment.getSeed());
    }

    private final static Main.RepairVariant pickVariant(String string) {
        switch (string) {
            case "IQ":
                return Main.RepairVariant.IQ;
            case "CQ":
                return Main.RepairVariant.CQ;
            default:
                System.out.println("Unexpected repair variant: " + string);
                System.out.println("Call without parameters to get help information");
                System.exit(1);
                return Main.RepairVariant.CQ;
        }
    }


    private final Random random;
    private long seed;

    private RunExperiment2(){
        random = new Random();
        seed = random.nextLong();
        random.setSeed(seed);
    }

    private long getSeed(){
        return seed;
    }

    private void setSeed(long seed) {
        this.seed=seed;
        random.setSeed(seed);
    }

    private void startExperiment(String ontologyFileName, Main.RepairVariant repairVariant)
            throws OWLOntologyCreationException, SaturationException {

        OWLOntology ontology =
                OWLManager.createOWLOntologyManager()
                        .loadOntologyFromOntologyDocument(new File(ontologyFileName));

        OntologyPreparations.prepare(ontology);

        RepairRequest repairRequest = generateRepairRequest(ontology);

        Main main = new Main();
        main.performRepair(ontology, repairRequest, repairVariant);
    }

    private RepairRequest generateRepairRequest(
            OWLOntology ontology) {

        OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(ontology);

        List<OWLNamedIndividual> individuals = ontology.individualsInSignature().collect(Collectors.toList());


        while(!individuals.isEmpty()) {
            OWLNamedIndividual individual = individuals.get(random.nextInt(individuals.size()));

            individuals.remove(individual);


            List<OWLClass> classes = reasoner.types(individual, false).collect(Collectors.toList());

            while(!classes.isEmpty()) {
                OWLClass clazz = classes.get(random.nextInt(classes.size()));
                classes.remove(clazz);
                if(!reasoner.getTopClassNode().contains(clazz)){
                RepairRequest result = new RepairRequest();
                result.put(individual, Collections.singleton(clazz));
                System.out.println("Repair: " + clazz + "(" + individual + ")");
                return result;
                }
            }
        }

        System.out.println("No non-tautological entailments of concept assertions!");
        System.exit(0);

        return null;
    }
}
