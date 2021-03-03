package de.tu_dresden.lat.abox_repairs.experiments;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.lat.abox_repairs.ontology_tools.OntologyPreparations;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Runs experiment where a given proportion of individuals gets assigned the same concept name.
 */
public class RunExperiment4 {


    public static void main(String[] args) throws OWLOntologyCreationException, SaturationException {
        if(args.length<5) {
            System.out.println("Usage: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+ " ONTOLOGY_FILE SATURATED|NOT_SATURATED IQ|CQ PROPORTION NUMBER [SEED]");
            System.out.println();
            System.out.println();
            System.out.println("Example: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+" ore_ont_3453.owl NOT_SATURATED IQ 0.1 ");
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

        double proportion = Double.parseDouble(args[3]);

        int number = Integer.parseInt(args[4]);

        RunExperiment4 experiment = new RunExperiment4();

        if(args.length>5){
            long seed = Long.parseLong(args[5]);
            experiment.setSeed(seed);
        }
        try {
            experiment.startExperiment(ontologyFileName, repairVariant, saturationRequired, proportion,number);
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

    private AnonymousVariableDetector anonymousVariableDetector;

    private RunExperiment4(){
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

    private void startExperiment(String ontologyFileName, Main.RepairVariant repairVariant, boolean saturationRequired, double proportion, int number)
            throws OWLOntologyCreationException, SaturationException {

        OWLOntology ontology =
                OWLManager.createOWLOntologyManager()
                        .loadOntologyFromOntologyDocument(new File(ontologyFileName));

        anonymousVariableDetector = AnonymousVariableDetector.newInstance(!saturationRequired,repairVariant);

        OntologyPreparations.prepare(ontology);

        RepairRequest repairRequest = generateRepairRequest(ontology,proportion, number);

        Main main = new Main(random);
        main.performRepair(ontology, repairRequest, repairVariant,saturationRequired);
    }

    private RepairRequest generateRepairRequest(
            OWLOntology ontology, double proportion, int number) {

        OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(ontology);

        Set<OWLClassExpression> classes = new HashSet<>();

        for(int i=0; i<number; i++)
            classes.add(pickClass(ontology));

        List<OWLNamedIndividual> individuals = anonymousVariableDetector.getNamedIndividuals(ontology);

        individuals.sort(Comparator.comparing(a -> a.getIRI().toString()));

        Collections.shuffle(individuals, random);

        RepairRequest request = new RepairRequest();

        for(int i =0; i<=proportion*individuals.size(); i++){

            request.put(individuals.get(i), classes);

        }

        return request;
    }

    /**
     * Find some class satisfied by some individual name.
     */
    private OWLClass pickClass(OWLOntology ontology){

        OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(ontology);

        List<OWLNamedIndividual> individuals = ontology.individualsInSignature().collect(Collectors.toList());
        individuals.sort(Comparator.comparing(a -> a.getIRI().toString()));

        while(!individuals.isEmpty()) {
            OWLNamedIndividual individual = individuals.get(random.nextInt(individuals.size()));

            individuals.remove(individual);


            List<OWLClass> classes = reasoner.types(individual, false).collect(Collectors.toList());
            classes.sort(Comparator.comparing(a -> a.getIRI().toString()));

            while(!classes.isEmpty()) {
                OWLClass clazz = classes.get(random.nextInt(classes.size()));
                classes.remove(clazz);
                if(!reasoner.getTopClassNode().contains(clazz)){
                    return clazz;
                }
            }
        }

        System.out.println("No non-tautological entailments of concept assertions!");
        System.exit(0);

        return null;
    }
}
