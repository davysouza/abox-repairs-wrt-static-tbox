package de.tu_dresden.inf.lat.abox_repairs.repair_manager;

import de.tu_dresden.inf.lat.abox_repairs.generator.CanonicalRepairGenerator;
import de.tu_dresden.inf.lat.abox_repairs.generator.RepairGenerator;
import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.inf.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.inf.lat.abox_repairs.saturation.ABoxSaturator;
import de.tu_dresden.inf.lat.abox_repairs.saturation.AnonymousVariableDetector;
import de.tu_dresden.inf.lat.abox_repairs.saturation.SaturationException;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunctionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.Iterator;
import java.util.Set;

public class RepairManager {

    private static Logger logger = LogManager.getLogger(RepairManager.class);

    /**
     * TODO: some of the following fields are not needed here, and should be made local to the only method were they are used
     */
    private OWLOntology ontology, workingCopy;

    private SeedFunction seedFunction;

    private RepairRequest repairRequest;

    private ReasonerFacade reasonerWithTBox, reasonerWithoutTBox;

    private ABoxSaturator saturator;

    private RepairGenerator repairGenerator;

    /**
     * please leave package visibility - this class should only be initialised by the builder
     */
    RepairManager(OWLOntology ontology,
                          OWLOntology workingCopy,
                          ReasonerFacade reasonerWithoutTBox,
                          ReasonerFacade reasonerWithTBox,
                          RepairGenerator generator,
                          ABoxSaturator saturator,
                          RepairRequest repairRequest) {
        this.ontology=ontology;
        this.workingCopy = workingCopy;
        this.reasonerWithoutTBox=reasonerWithoutTBox;
        this.reasonerWithTBox=reasonerWithTBox;
        this.repairGenerator=generator;
        this.saturator=saturator;
        this.repairRequest=repairRequest;
    }


    /*public RepairManager() {
        this.random = new Random();
    }

    public RepairManager(Random random) {
        this.random = random;
    }*/

    public OWLOntology performRepair(//OWLOntology inputOntology,
                              //RepairRequest repairRequest
                              //RepairVariant repairVariant,
                              //boolean saturationRequired
    ) throws OWLOntologyCreationException, SaturationException {

        long startTime = System.nanoTime();

        int oldIndividuals = workingCopy.getIndividualsInSignature().size() + workingCopy.getAnonymousIndividuals().size();
//        long oldAssertions = workingCopy.aboxAxioms(Imports.INCLUDED).count();
        long oldAssertions = workingCopy.getABoxAxioms(Imports.INCLUDED).size();

        reasonerWithTBox.update();

        System.out.println("Before saturation:");
        System.out.println(workingCopy.getIndividualsInSignature().size() + " individuals");
        System.out.println(workingCopy.getAnonymousIndividuals().size() + " variables");

        saturator.saturate(workingCopy);

        System.out.println("After saturation:");
        System.out.println(workingCopy.getIndividualsInSignature().size() + " individuals");
        System.out.println(workingCopy.getAnonymousIndividuals().size() + " variables");

        logger.debug("after initializing reasoners: ");


        reasonerWithTBox.update(); // update again to add concepts to fresh variables

        generateRandomSeedFunction();

        if (isCompliant(repairRequest,reasonerWithTBox)) {
            System.out.println("\nThe ontology is compliant!");
        } else {
            System.out.println("\nThe ontology is not compliant!");

            
           /* if(!CANONICAL_ANY.contains(repairVariant)) {
                    seedFunctionConstruction();
            */
                Set<OWLNamedIndividual> setIndividuals = seedFunction.keySet();
                Iterator<OWLNamedIndividual> iteSetIndividuals = setIndividuals.iterator();
                logger.debug("\nSeed Function");
                while (iteSetIndividuals.hasNext()) {
                    OWLNamedIndividual oni = iteSetIndividuals.next();
                    logger.debug("- " + oni);
                    RepairType type = seedFunction.get(oni);
                    logger.debug(type.getClassExpressions());
                    logger.debug("");
                }
            /*}
            */

            assert seedFunction!=null;

            repairGenerator.setSeedFunction(seedFunction);
            repairGenerator.repair();
            workingCopy = repairGenerator.getRepair();
            workingCopy.addAxioms(ontology.tboxAxioms(Imports.INCLUDED));

           // reasonerWithTBox = ReasonerFacade.newReasonerFacadeWithTBox(ontology, additionalExpressions);
           // reasonerWithTBox.update();

//            reasonerWithTBox.cleanOntology(); // <-- this should not be done if the reasoner facades is still used!
            reasonerWithTBox.dispose();

            double timeRepairing = (double) (System.nanoTime() - startTime) / 1_000_000_000;



            /**
             * Please do not remove! This is not debugging output, but used for the evaluation of the experiments.
             */
            if (repairGenerator instanceof CanonicalRepairGenerator){
				System.out.print("#Individuals (orig/sat/repair): "
						+ oldIndividuals + "/"
						+ (oldIndividuals + saturator.addedIndividuals()) + "/" +
						repairGenerator.getNumberOfCollectedIndividuals());
				System.out.print(" #Assertions (orig/sat/repair): "
						+ oldAssertions + "/"
						+ (oldAssertions + saturator.addedAssertions()) + "/-");
            } else {
                System.out.print("#Individuals (orig/sat/repair): "
                        + oldIndividuals + "/"
                        + (oldIndividuals + saturator.addedIndividuals()) + "/"+
                        repairGenerator.getNumberOfCollectedIndividuals() + "/"+
                        (workingCopy.getIndividualsInSignature().size() + workingCopy.getAnonymousIndividuals().size()));
                ;
                System.out.print(" #Assertions (orig/sat/repair): "
                        + oldAssertions + "/"
                        + (oldAssertions + saturator.addedAssertions()) + "/" +
//                        ontology.aboxAxioms(Imports.EXCLUDED).count());
                        workingCopy.getABoxAxioms(Imports.EXCLUDED).size());
            }
            System.out.println(" Duration (sat/repair sec.): " + saturator.getDuration() + "/" + timeRepairing);

            ReasonerFacade forRepair =
                    ReasonerFacade.newReasonerFacadeWithTBox(workingCopy, reasonerWithTBox.getSupportedClassExpressions());


            if (isCompliant(repairRequest, forRepair)) {
                System.out.println("The ontology is now compliant");
            } else {
                System.out.println("The ontology is still not compliant");
            }
        }

        return workingCopy;
    }


    private void generateRandomSeedFunction(){
        long time = System.nanoTime();
        AnonymousVariableDetector detector = AnonymousVariableDetector.newInstance(saturator);
        SeedFunctionHandler seedFunctionHandler =
                new SeedFunctionHandler(workingCopy, reasonerWithTBox, reasonerWithoutTBox, detector);
        seedFunction = seedFunctionHandler.computeRandomSeedFunction(repairRequest);
        logger.info("Seed function construction took: " + (((double) System.nanoTime() - time) / 1_000_000_000));
    }

   // private List<OWLClassExpression> additionalExpressions = new LinkedList<>();


    private boolean isCompliant(RepairRequest repairRequest, ReasonerFacade reasoner) {
        boolean compliant = true;

        for (OWLNamedIndividual individual : repairRequest.keySet()) {

            for (OWLClassExpression concept : repairRequest.get(individual)) {
                if (reasoner.instanceOf(individual, concept)) {
                    logger.info("The ontology is not compliant, since " + individual + " is an instance of " + concept + ".");
//                    if (seedFunction != null) {
//                    	logger.info("check if the seed function is " + seedFunction);
//                    	logger.info("The individual " + individual + "is now being handled.");
//                        logger.info("The seed function maps " + individual + " to " + seedFunction.get(individual).getClassExpressions());
//                        logger.info(ontology.getClassAssertionAxioms(individual));
//                        try {
//                            logger.info(Prover.getJustification(ontology,OWLManager.getOWLDataFactory().getOWLClassAssertionAxiom(concept, individual)));
//                        } catch (OWLOntologyCreationException e) {
//                            throw new RuntimeException(e);
//                        }
  //                  }
                    return false;
                }
            }

        }

        return compliant;
    }

    /*
    private void repairCQ() throws OWLOntologyCreationException {
        repairGenerator = new CQRepairGenerator(ontology, seedFunction);
		repairGenerator.setReasoner(reasonerWithTBox, reasonerWithoutTBox);
		repairGenerator.repair();
        ontology = repairGenerator.getRepair();
    }

    private void repairIQ() throws OWLOntologyCreationException {
        repairGenerator = new IQRepairGenerator(ontology, seedFunction);
		repairGenerator.setReasoner(reasonerWithTBox, reasonerWithoutTBox);
		repairGenerator.repair();
        ontology = repairGenerator.getRepair();
    }

    private void CanonicalRepair() throws OWLOntologyCreationException {
        repairGenerator = new CanonicalRepairGenerator(ontology, seedFunction);
        repairGenerator.setReasoner(reasonerWithTBox, reasonerWithoutTBox);
        repairGenerator.repair();
    }

    private void cleanOntology() {
        reasonerWithTBox.cleanOntology();
    }

     */
}
