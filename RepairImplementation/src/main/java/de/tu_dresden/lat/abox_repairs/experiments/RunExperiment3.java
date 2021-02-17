package de.tu_dresden.lat.abox_repairs.experiments;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.RepairRequest;
import de.tu_dresden.lat.abox_repairs.ontology_tools.OntologyPreparations;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RunExperiment3 {


        public static void main(String[] args) throws OWLOntologyCreationException, SaturationException {
            if(args.length<3) {
                System.out.println("Usage: ");
                System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+ " ONTOLOGY_FILE SATURATED|NOT_SATURATED IQ|CQ ");
                System.out.println();
                System.out.println();
                System.out.println("Example: ");
                System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+" ore_ont_3453.owl IQ");
                System.exit(0);
            }

            String ontologyFileName = args[0];


            boolean saturationRequired = false;
            switch(args[1]){
                case "SATURATED": saturationRequired=false; break;
                case "NOT_SATURATED": saturationRequired=true; break;
                default:
                    System.out.println("Please specify whether the given ontology is already saturated.");
                    System.out.println("(Use no parameters to get help)");
                    System.exit(1);
            }

            Main.RepairVariant repairVariant = pickVariant(args[2]);


            RunExperiment3 experiment = new RunExperiment3();

            try {
                experiment.startExperiment(ontologyFileName, repairVariant,saturationRequired);
            } catch(Exception e) {
                e.printStackTrace();
            }
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

    private RunExperiment3(){
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

        private void startExperiment(String ontologyFileName, Main.RepairVariant repairVariant, boolean saturationRequired)
                throws OWLOntologyCreationException, SaturationException {

            OWLOntology ontology =
                    OWLManager.createOWLOntologyManager()
                            .loadOntologyFromOntologyDocument(new File(ontologyFileName));

            OntologyPreparations.prepare(ontology);

            OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(ontology);
            ontology.individualsInSignature().forEach(ind -> {
                reasoner.types(ind, false).forEach(cl -> {
                    if(!reasoner.getTopClassNode().contains(cl)){
                        RepairRequest request = new RepairRequest();
                        request.put(ind, Collections.singleton(cl));
                        System.out.println("Repair: " + cl + "(" + ind + ")");

                        Main main = new Main(random);
                        try {
                            main.performRepair(ontology, request, repairVariant,saturationRequired);
                        } catch (OWLOntologyCreationException e) {
                            e.printStackTrace();
                        } catch (SaturationException e) {
                            e.printStackTrace();
                        }
                    }
                });
            });


        }

}
