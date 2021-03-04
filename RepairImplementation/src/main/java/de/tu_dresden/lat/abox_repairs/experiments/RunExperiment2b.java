package de.tu_dresden.lat.abox_repairs.experiments;

import de.tu_dresden.lat.abox_repairs.repairManager.RepairManager;
import de.tu_dresden.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.lat.abox_repairs.ontology_tools.OntologyPreparations;
import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repairManager.RepairManagerBuilder;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;
import de.tu_dresden.lat.abox_repairs.tools.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.*;

/**
 * Difference to RunExperiment2: we only use complex concepts
 */
public class RunExperiment2b {

    private static Logger logger = LogManager.getLogger(RunExperiment2.class);

    public static void main(String[] args) throws OWLOntologyCreationException, SaturationException {
        if(args.length<3) {
            System.out.println("Usage: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+ " ONTOLOGY_FILE SATURATED|NOT_SATURATED IQ|CQ|CANONICAL_IQ|CANONICAL_CQ [SEED]");
            System.out.println();
            System.out.println("Generates a repair of ONTOLOGY_FILE with a randomly generated repair request that");
            System.out.println("randomly selects an entailed concept assertion. You may optionally provide");
            System.out.println("a seed value for the random number generator used.");
            System.out.println("SATURATED should be used if the ontology is already saturated in the appropriate way");
            System.out.println("otherwise, specify NOT_SATURATED");
            System.out.println();
            System.out.println("Example: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+" ore_ont_3453.owl NOT_SATURATED IQ ");
            System.exit(0);
        }

        String ontologyFileName = args[0];
        RepairManagerBuilder.RepairVariant repairVariant = pickVariant(args[2]);


        boolean saturationRequired = false;
        switch(args[1]){
            case "SATURATED": saturationRequired=false; break;
            case "NOT_SATURATED": saturationRequired=true; break;
            default:
                System.out.println("Please specify whether the given ontology is already saturated.");
                System.out.println("(Use no parameters to get help)");
                System.exit(1);
        }

        RunExperiment2b experiment = new RunExperiment2b();

        if(args.length>3){
            long seed = Long.parseLong(args[3]);
            experiment.setSeed(seed);
        }
        try {
            experiment.startExperiment(ontologyFileName, repairVariant, saturationRequired);
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.out.println("Used seed: "+experiment.getSeed());
    }

    private final static RepairManagerBuilder.RepairVariant pickVariant(String string) {
        switch (string) {
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
                System.out.println("Unexpected repair variant: " + string);
                System.out.println("Call without parameters to get help information");
                System.exit(1);
                return RepairManagerBuilder.RepairVariant.CQ;
        }
    }


    private final Random random;
    private long seed;

    private RunExperiment2b(){
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

    private AnonymousVariableDetector anonymousVariableDetector;

    private void startExperiment(String ontologyFileName, RepairManagerBuilder.RepairVariant repairVariant, boolean saturationRequired)
            throws OWLOntologyCreationException, SaturationException {

        OWLOntology ontology =
                OWLManager.createOWLOntologyManager()
                        .loadOntologyFromOntologyDocument(new File(ontologyFileName));

        anonymousVariableDetector= AnonymousVariableDetector.newInstance(!saturationRequired,repairVariant);

        OntologyPreparations.prepare(ontology);

        Timer timer = Timer.newTimer();
        timer.continueTimer();
        RepairRequest repairRequest = generateRepairRequest(ontology);
        timer.pause();
        logger.info("Generating repair request took "+timer.getTime()+" seconds.");
        RepairManager repairManager =
                new RepairManagerBuilder()
                        .setOntology(ontology)
                        .setRepairRequest(repairRequest)
                        .setVariant(repairVariant)
                        .setNeedsSaturation(saturationRequired)
                        .build();
        repairManager.performRepair();
    }

    private RepairRequest generateRepairRequest(
            OWLOntology ontology) {

        ReasonerFacade facade = ReasonerFacade.newReasonerFacadeWithTBox(ontology);

        List<OWLNamedIndividual> individuals = anonymousVariableDetector.getNamedIndividuals(ontology);

        individuals.sort(Comparator.comparing(a -> a.getIRI().toString()));

        while(!individuals.isEmpty()) {
            OWLNamedIndividual individual = individuals.get(random.nextInt(individuals.size()));

            individuals.remove(individual);


            List<OWLClassExpression> classes = new ArrayList<>(facade.instanceOf(individual));

            classes.sort(Comparator.comparing(a -> a.toString()));

            while(!classes.isEmpty()) {
                OWLClassExpression clazz = classes.get(random.nextInt(classes.size()));
                classes.remove(clazz);
                long sigSize = clazz.signature().filter(c -> !c.isTopEntity()).count();
                if(sigSize>1 && !facade.equivalentToOWLThing(clazz)){
                    RepairRequest result = new RepairRequest();
                    result.put(individual, Collections.singleton(clazz));
                    System.out.println("Repair: " + clazz + "(" + individual + ")");
                    facade.cleanOntology();
                    return result;
                }
            }
        }

        System.out.println("No non-tautological entailments of complex concept assertions!");
        System.exit(0);

        return null;
    }
}
