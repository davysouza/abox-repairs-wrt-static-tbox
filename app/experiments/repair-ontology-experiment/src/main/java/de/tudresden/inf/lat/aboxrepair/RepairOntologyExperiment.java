package de.tudresden.inf.lat.aboxrepair;

import de.tudresden.inf.lat.aboxrepair.repairmanager.RepairManager;
import de.tudresden.inf.lat.aboxrepair.repairmanager.RepairManagerBuilder;
import de.tudresden.inf.lat.aboxrepair.repairrequest.RepairRequest;
import de.tudresden.inf.lat.aboxrepair.saturation.AnonymousVariableDetector;
import de.tudresden.inf.lat.aboxrepair.saturation.SaturationException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

// Old RunExperiment1

public class RepairOntologyExperiment {
    // region attributes
    private static String ontologyFileName = null;

    private static boolean saturationRequired;

    private static RepairManagerBuilder.RepairVariant repairVariant = null;

    private static double proportionIndividuals;

    private static double proportionClassNames;

    private static Random random;

    private static long seed;
    // endregion

    // region public methods
    public static void main(String[] args) {
        if(args.length == 0) {
            help();
            return;
        }

        if(!parseArguments(args)) {
            help();
            System.exit(1);
        }

        try {
            startExperiment();
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("Used seed: " + seed);
    }
    // endregion

    // region private methods
    private static boolean parseArguments(String[] args) {
        if(args.length < 5) {
            return false;
        }

        try {
            ontologyFileName = args[0];
            saturationRequired = needSaturation(args[1]);
            repairVariant = pickRepairVariant(args[2]);
            proportionIndividuals = Double.parseDouble(args[3]);
            proportionClassNames = Double.parseDouble(args[4]);

            random = new Random();
            seed = (args.length > 5)
                    ? Long.parseLong(args[5])
                    : random.nextLong();
            random.setSeed(seed);
        } catch (Exception ex) {
            return false;
        }

        return true;
    }

    private static RepairManagerBuilder.RepairVariant pickRepairVariant(String repairVariant) {
        switch (repairVariant) {
            case "IQ":
                return RepairManagerBuilder.RepairVariant.IQ;
            case "IQ2":
                return RepairManagerBuilder.RepairVariant.IQ2;
            case "CQ":
                return RepairManagerBuilder.RepairVariant.CQ;
            default:
                System.out.println("Unexpected repair variant: " + repairVariant);
                System.out.println("Call without parameters to get help information");
                throw new IllegalArgumentException("Unexpected repair variant");
        }
    }

    private static boolean needSaturation(String saturationRequired) {
        switch (saturationRequired) {
            case "SATURATED":
                return false;
            case "NOT_SATURATED":
                return true;
            default:
                System.out.println("Please specify whether the given ontology is already saturated.");
                System.out.println("Call without parameters to get help information");
                throw new IllegalArgumentException("Missing saturation required parameter");
        }
    }

    private static void help() {
        System.out.println("Usage: ");
        System.out.println("java -cp ... " + RepairOntologyExperiment.class.getCanonicalName() + " ONTOLOGY_FILE SATURATED|NOT_SATURATED IQ|CQ PROPORTION1 PROPORTION2 [SEED]");
        System.out.println();
        System.out.println("Generates a repair of ONTOLOGY_FILE with a randomly generated repair request that");
        System.out.println("randomly assigns concept names to some individual name so that a proportion of ");
        System.out.println("PROPORTION2 of the entire set of concept names is selected, and a proportion of ");
        System.out.println("PROPORTION1 of the individuals gets a repair request. You may optionally provide");
        System.out.println("a seed value for the random number generator used.");
        System.out.println("SATURATED should be used if the ontology is already saturated in the appropriate way");
        System.out.println("otherwise, specify NOT_SATURATED");
        System.out.println();
        System.out.println();
        System.out.println("Example: ");
        System.out.println("java -cp ... " + RepairOntologyExperiment.class.getCanonicalName() + " ore_ont_3453.owl NOT_SATURATED IQ 0.1 0.2");
    }

    private static void startExperiment() throws OWLOntologyCreationException, SaturationException {
        OWLOntology ontology =
                OWLManager.createOWLOntologyManager()
                        .loadOntologyFromOntologyDocument(new File(ontologyFileName));

        RepairRequest repairRequest = generateRepairRequest(ontology);

        RepairManager repairManager =
                new RepairManagerBuilder()
                        .setOntology(ontology)
                        .setRepairRequest(repairRequest)
                        .setVariant(repairVariant)
                        .setNeedsSaturation(saturationRequired)
                        .build();
        repairManager.initAndPerformRepair();
    }

    private static RepairRequest generateRepairRequest(OWLOntology ontology) {
        RepairRequest request = new RepairRequest();
        List<OWLClass> classList = ontology.classesInSignature().collect(Collectors.toList());

        Set<OWLNamedIndividual> individuals = randomIndividuals(ontology, proportionIndividuals);
        for(OWLNamedIndividual individual: individuals) {
            request.put(individual, randomClasses(classList, proportionClassNames));
        }

        return request;
    }

    private static Set<OWLNamedIndividual> randomIndividuals(OWLOntology ontology, double proportion) {
        Set<OWLNamedIndividual> result = new HashSet<>();

        AnonymousVariableDetector anonymousVariableDetector = AnonymousVariableDetector.newInstance(!saturationRequired, repairVariant);
        List<OWLNamedIndividual> individuals = anonymousVariableDetector.getNamedIndividuals(ontology);

        System.out.println("Requests for " + ((int)(proportion * individuals.size())) + " individual names.");

        for(int i = 0; i < proportion * individuals.size(); i++) {
            OWLNamedIndividual individual = individuals.get(random.nextInt(individuals.size()));
            while(result.contains(individual)){
                individual = individuals.get(random.nextInt(individuals.size()));
            }
            result.add(individual);
        }
        return result;
    }

    private static Set<OWLClassExpression> randomClasses(List<OWLClass> classList, double proportion) {
        Set<OWLClassExpression> result = new HashSet<>();

        for(int i = 0; i < proportion * classList.size(); i++) {
            OWLClass cl = classList.get(random.nextInt(classList.size()));

            while(cl.isOWLThing() || result.contains(cl)){
                cl = classList.get(random.nextInt(classList.size()));
            }
            result.add(cl);
        }
        return result;
    }
    // endregion
}