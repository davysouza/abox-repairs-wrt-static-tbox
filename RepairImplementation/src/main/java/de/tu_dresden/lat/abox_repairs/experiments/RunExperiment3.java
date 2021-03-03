package de.tu_dresden.lat.abox_repairs.experiments;

import de.tu_dresden.lat.abox_repairs.repairManager.RepairManager;
import de.tu_dresden.lat.abox_repairs.RepairRequest;
import de.tu_dresden.lat.abox_repairs.ontology_tools.OntologyPreparations;
import de.tu_dresden.lat.abox_repairs.repairManager.RepairManagerBuilder;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;
import java.util.Collections;
import java.util.Random;

/**
 * Compute all single concept assertion repairs one after the other.
 */
public class RunExperiment3 {


    public static void main(String[] args) throws OWLOntologyCreationException, SaturationException {
        if (args.length < 3) {
            System.out.println("Usage: ");
            System.out.println("java -cp ... " + RunExperiment1.class.getCanonicalName() + " ONTOLOGY_FILE SATURATED|NOT_SATURATED IQ|CQ");
            System.out.println();
            System.out.println();
            System.out.println("Example: ");
            System.out.println("java -cp ... " + RunExperiment1.class.getCanonicalName() + " ore_ont_3453.owl NOT_SATURATED IQ");
            System.exit(0);
        }

        String ontologyFileName = args[0];


        boolean saturationRequired = false;
        switch (args[1]) {
            case "SATURATED":
                saturationRequired = false;
                break;
            case "NOT_SATURATED":
                saturationRequired = true;
                break;
            default:
                System.out.println("Please specify whether the given ontology is already saturated.");
                System.out.println("(Use no parameters to get help)");
                System.exit(1);
        }

        RepairManagerBuilder.RepairVariant repairVariant = pickVariant(args[2]);


        RunExperiment3 experiment = new RunExperiment3();

        try {
            experiment.startExperiment(ontologyFileName, repairVariant, saturationRequired);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final static RepairManagerBuilder.RepairVariant pickVariant(String string) {
        switch (string) {
            case "IQ":
                return RepairManagerBuilder.RepairVariant.IQ;
            case "CQ":
                return RepairManagerBuilder.RepairVariant.CQ;
            default:
                System.out.println("Unexpected repair variant: " + string);
                System.out.println("Call without parameters to get help information");
                System.exit(1);
                return RepairManagerBuilder.RepairVariant.CQ;
        }
    }


    private final Random random;
    private long seed;

    private RunExperiment3() {
        random = new Random();
        seed = random.nextLong();
        random.setSeed(seed);
    }

    private long getSeed() {
        return seed;
    }

    private AnonymousVariableDetector anonymousVariableDetector;

    private void setSeed(long seed) {
        this.seed = seed;
        random.setSeed(seed);
    }

    private void startExperiment(String ontologyFileName, RepairManagerBuilder.RepairVariant repairVariant, boolean saturationRequired)
            throws OWLOntologyCreationException, SaturationException {

        OWLOntology ontology =
                OWLManager.createOWLOntologyManager()
                        .loadOntologyFromOntologyDocument(new File(ontologyFileName));

        OntologyPreparations.prepare(ontology);

        anonymousVariableDetector = AnonymousVariableDetector.newInstance(!saturationRequired,repairVariant);

        OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(ontology);
        ontology.individualsInSignature().forEach(ind -> {
            reasoner.types(ind, false).forEach(cl -> {
                if (!reasoner.getTopClassNode().contains(cl)) {
                    RepairRequest request = new RepairRequest();
                    request.put(ind, Collections.singleton(cl));
                    System.out.println("Repair: " + cl + "(" + ind + ")");

                    try {
                        RepairManager repairManager =
                                new RepairManagerBuilder()
                                        .setOntology(ontology)
                                        .setRepairRequest(request)
                                        .setVariant(repairVariant)
                                        .setNeedsSaturation(saturationRequired)
                                        .build();
                        repairManager.performRepair();
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
