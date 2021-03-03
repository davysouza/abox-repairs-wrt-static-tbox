package de.tu_dresden.lat.abox_repairs.experiments;

import de.tu_dresden.lat.abox_repairs.repairManager.RepairManager;
import de.tu_dresden.lat.abox_repairs.repairManager.RepairManagerBuilder;
import de.tu_dresden.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates a random instances of the repair problem by randomly selecting a fixed proportion of individuals and randomly
 * assigning a fixed proportion of the concept names in the signature.
 */
public class RunExperiment1 {


    public static void main(String[] args) throws OWLOntologyCreationException, SaturationException {
        if(args.length<5) {
            System.out.println("Usage: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+ " ONTOLOGY_FILE SATURATED|NOT_SATURATED IQ|CQ PROPORTION1 PROPORTION2 [SEED]");
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
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+" ore_ont_3453.owl NOT_SATURATED IQ 0.1 0.2");
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

        RepairManagerBuilder.RepairVariant repairVariant;
        switch(args[2]){
            case "IQ": repairVariant = RepairManagerBuilder.RepairVariant.IQ; break;
            case "CQ": repairVariant = RepairManagerBuilder.RepairVariant.CQ; break;
            default:
                System.out.println("Unexpected repair variant: "+args[1]);
                System.out.println("Call without parameters to get help information");
                repairVariant= RepairManagerBuilder.RepairVariant.CQ;
                System.exit(1);
        }

        double proportionIndividuals = Double.parseDouble(args[3]);
        double proportionClassNames = Double.parseDouble(args[4]);

        RunExperiment1 experiment = new RunExperiment1();

        if(args.length>5){
            long seed = Long.parseLong(args[5]);
            experiment.setSeed(seed);
        }
        try {
            experiment.startExperiment(ontologyFileName, repairVariant, proportionIndividuals, proportionClassNames, saturationRequired);
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.out.println("Used seed: "+experiment.getSeed());
    }

    private final Random random;
    private long seed;

    private RunExperiment1(){
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

    private AnonymousVariableDetector anonymousVariableDetector=null;

    private void startExperiment(String ontologyFileName, RepairManagerBuilder.RepairVariant repairVariant, double proportionIndividuals, double proportionClassNames, boolean saturationRequired)
            throws OWLOntologyCreationException, SaturationException {

        OWLOntology ontology =
                OWLManager.createOWLOntologyManager()
                        .loadOntologyFromOntologyDocument(new File(ontologyFileName));

        anonymousVariableDetector=AnonymousVariableDetector.newInstance(!saturationRequired,repairVariant);

        RepairRequest repairRequest = generateRepairRequest(ontology, proportionIndividuals, proportionClassNames);

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
            OWLOntology ontology, double proportionIndividuals, double proportionClassNames) {
        RepairRequest request = new RepairRequest();

        List<OWLClass> classList = ontology.classesInSignature().collect(Collectors.toList());

        Set<OWLNamedIndividual> individuals = randomIndividuals(ontology, proportionIndividuals);

        for(OWLNamedIndividual individual: individuals) {
            request.put(individual, randomClasses(classList, proportionClassNames));
        }

        return request;
    }

    private Set<OWLNamedIndividual> randomIndividuals(OWLOntology ontology, double proportion) {
        Set<OWLNamedIndividual> result = new HashSet<>();

        List<OWLNamedIndividual> individuals = anonymousVariableDetector.getNamedIndividuals(ontology);

        System.out.println("Requests for "+((int)(proportion*individuals.size()))+" individual names.");

        for(int i=0; i<proportion*individuals.size(); i++) {
            OWLNamedIndividual ind = individuals.get(random.nextInt(individuals.size()));
            while(result.contains(ind)){
                ind = individuals.get(random.nextInt(individuals.size()));
            }
            result.add(ind);
        }
        return result;
    }

    private Set<OWLClassExpression> randomClasses(List<OWLClass> classList, double proportion) {
        Set<OWLClassExpression> result = new HashSet<>();

        for(int i=0; i<proportion*classList.size(); i++) {
            OWLClass cl = classList.get(random.nextInt(classList.size()));
          
            while(cl.isOWLThing() || result.contains(cl)){
                cl = classList.get(random.nextInt(classList.size()));
            }
            result.add(cl);
        }
        return result;
    }
}
