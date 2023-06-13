package de.tudresden.inf.lat.aboxrepair;

import de.tudresden.inf.lat.aboxrepair.ontologytools.OntologyPreparations;
import de.tudresden.inf.lat.aboxrepair.reasoner.ReasonerFacade;
import de.tudresden.inf.lat.aboxrepair.repairmanager.RepairManagerBuilder;
import de.tudresden.inf.lat.aboxrepair.repairrequest.RepairRequest;
import de.tudresden.inf.lat.aboxrepair.saturation.AnonymousVariableDetector;
import de.tudresden.inf.lat.aboxrepair.saturation.SaturationException;
import de.tudresden.inf.lat.aboxrepair.utils.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.util.*;

// Old RunExperiment0

public class GenerateRepairRequestExperiment {

    // region attributes
    private static Logger logger = LogManager.getLogger(GenerateRepairRequestExperiment.class);

    private static File ontologyFile = null;

    private static boolean saturationRequired;

    private static RepairManagerBuilder.RepairVariant repairVariant = null;

    private static Random random;

    private static long seed;
    // endregion

    // region public methods
    public static void main(String[] args) {
        if(args.length == 0) {
            help();
            return;
        }

        // parsing arguments
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
        if(args.length < 3) {
            return false;
        }

        try {
            ontologyFile = new File(args[0]);
            saturationRequired = needSaturation(args[1]);
            repairVariant = pickRepairVariant(args[2]);

            random = new Random();
            seed = (args.length > 3)
                    ? Long.parseLong(args[3])
                    : random.nextLong();
            random.setSeed(seed);
        } catch (Exception ex) {
            System.out.println("Failed parsing arguments. Exception: " + ex.getMessage());
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
            case "CANONICAL_IQ":
                return RepairManagerBuilder.RepairVariant.CANONICAL_IQ;
            case "CANONICAL_CQ":
                return RepairManagerBuilder.RepairVariant.CANONICAL_CQ;
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
        System.out.println("java -cp ... " + GenerateRepairRequestExperiment.class.getCanonicalName() + " ONTOLOGY_FILE SATURATED|NOT_SATURATED IQ|CQ|CANONICAL_IQ|CANONICAL_CQ [SEED]");
        System.out.println();
        System.out.println("Generates a repair of ONTOLOGY_FILE with a randomly generated repair request that");
        System.out.println("randomly selects an entailed concept assertion. You may optionally provide");
        System.out.println("a seed value for the random number generator used.");
        System.out.println("SATURATED should be used if the ontology is already saturated in the appropriate way");
        System.out.println("otherwise, specify NOT_SATURATED");
        System.out.println();
        System.out.println("Example: ");
        System.out.println("java -cp ... " + GenerateRepairRequestExperiment.class.getCanonicalName() + " ore_ont_3453.owl NOT_SATURATED IQ ");
    }

    private static void startExperiment() throws OWLOntologyCreationException {
        // load ontology from file
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontologyFile);


        OntologyPreparations.prepare(ontology, repairVariant, true);

        Timer timer = new Timer();
        timer.start();
        RepairRequest repairRequest = generateRepairRequest(ontology);
        timer.pause();

        logger.info("Generating repair request took " + timer.getTime() + " seconds.");
        System.out.println("Generating repair request took " + timer.getTime() + " seconds.");
    }

    private static RepairRequest generateRepairRequest(OWLOntology ontology) {
        // new reasoner facade
        ReasonerFacade facade = ReasonerFacade.newReasonerFacadeWithTBox(ontology);

        // anonymous variable detector
        AnonymousVariableDetector anonymousVariableDetector = AnonymousVariableDetector.newInstance(!saturationRequired, repairVariant);

        // sort individuals and get a random
        List<OWLNamedIndividual> individuals = anonymousVariableDetector.getNamedIndividuals(ontology);
        individuals.sort(Comparator.comparing(a -> a.getIRI().toString()));
        OWLNamedIndividual individual = individuals.get(randomUnsignedInt(individuals.size()));

        // sort classes and get a random
        List<OWLClassExpression> classes = new ArrayList<>(facade.instanceOfExcludingOWLThing(individual));
        if(classes.size() == 0) {
            System.out.println("No OWLClassExpression objects at the list.");
            return null;
        }

        classes.sort(Comparator.comparing(a -> a.toString()));
        OWLClassExpression clazz = classes.get(randomUnsignedInt(classes.size()));
        classes.remove(clazz);

        long sigSize = clazz.signature().filter(c -> !c.isTopEntity()).count();
        RepairRequest result = new RepairRequest();
        result.put(individual, Collections.singleton(clazz));

        System.out.println("Repair: " + clazz + "(" + individual + ")");
        facade.cleanOntology();
        return result;
    }

    private static int randomUnsignedInt(int size) {
        int number = random.nextInt(size);
        return number >= 0 ? number : number * -1;
    }

    // endregion
}