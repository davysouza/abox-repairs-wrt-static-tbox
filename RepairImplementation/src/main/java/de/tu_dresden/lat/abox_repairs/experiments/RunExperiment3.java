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
            if(args.length<2) {
                System.out.println("Usage: ");
                System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+ " ONTOLOGY_FILE IQ|CQ ");
                System.out.println();
                System.out.println();
                System.out.println("Example: ");
                System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+" ore_ont_3453.owl IQ");
                System.exit(0);
            }

            String ontologyFileName = args[0];
            Main.RepairVariant repairVariant = pickVariant(args[1]);


            RunExperiment3 experiment = new RunExperiment3();

            try {
                experiment.startExperiment(ontologyFileName, repairVariant);
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



        private void startExperiment(String ontologyFileName, Main.RepairVariant repairVariant)
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

                        Main main = new Main();
                        try {
                            main.performRepair(ontology, request, repairVariant);
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
