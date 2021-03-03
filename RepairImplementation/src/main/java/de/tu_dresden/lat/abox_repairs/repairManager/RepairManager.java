package de.tu_dresden.lat.abox_repairs.repairManager;

import de.tu_dresden.lat.abox_repairs.RepairRequest;
import de.tu_dresden.lat.abox_repairs.SeedFunction;
import de.tu_dresden.lat.abox_repairs.SeedFunctionHandler;
import de.tu_dresden.lat.abox_repairs.ontology_tools.*;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.saturation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import de.tu_dresden.lat.abox_repairs.generator.CanonicalRepairGenerator;
import de.tu_dresden.lat.abox_repairs.generator.RepairGenerator;
import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;

import java.util.Iterator;
import java.util.Set;

public class RepairManager {

    private static Logger logger = LogManager.getLogger(RepairManager.class);

    /**
     * Check: usually, static fields (variables) should be avoided where possible.
     * In this case: check whether they are really needed to be outside the main method,
     * and otherwise, add them.
     */
    private OWLOntology ontology;

    private SeedFunction seedFunction;

    private RepairRequest repairRequest;

    private ReasonerFacade reasonerWithTBox, reasonerWithoutTBox;

    private ABoxSaturator saturator;

    private RepairGenerator repairGenerator;

   // private final Random random; // TODO do we really need it?

    /**
     * please leave package visibility - this class should only be initialised by the builder
     */
    RepairManager(OWLOntology ontology,
                          ReasonerFacade reasonerWithoutTBox,
                          ReasonerFacade reasonerWithTBox,
                          RepairGenerator generator,
                          ABoxSaturator saturator,
                          RepairRequest repairRequest) {
        this.ontology=ontology;
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

    public void performRepair(//OWLOntology inputOntology,
                              //RepairRequest repairRequest
                              //RepairVariant repairVariant,
                              //boolean saturationRequired
    ) throws OWLOntologyCreationException, SaturationException {

        long startTime = System.nanoTime();

        int oldIndividuals = ontology.getIndividualsInSignature().size();
        long oldAssertions = ontology.aboxAxioms(Imports.INCLUDED).count();

        reasonerWithTBox.update();

        saturator.saturate(ontology);

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
            ontology = repairGenerator.getRepair();

           // reasonerWithTBox = ReasonerFacade.newReasonerFacadeWithTBox(ontology, additionalExpressions);
           // reasonerWithTBox.update();

            reasonerWithTBox.cleanOntology(); // <-- this should not be done if the reasoner facades is still used!

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
                        ontology.getIndividualsInSignature().size());
                ;
                System.out.print(" #Assertions (orig/sat/repair): "
                        + oldAssertions + "/"
                        + (oldAssertions + saturator.addedAssertions()) + "/" +
//                        ontology.aboxAxioms(Imports.EXCLUDED).count());
                        ontology.getABoxAxioms(Imports.EXCLUDED).size());
            }
            System.out.println(" Duration (sat/repair sec.): " + saturator.getDuration() + "/" + timeRepairing);

            ReasonerFacade forRepair =
                    ReasonerFacade.newReasonerFacadeWithTBox(ontology, reasonerWithTBox.getClassExpressions());


            if (isCompliant(repairRequest, forRepair)) {
                System.out.println("The ontology is now compliant");
            } else {
                System.out.println("The ontology is still not compliant");
            }
        }
    }


    private void generateRandomSeedFunction(){
        long time = System.nanoTime();
        AnonymousVariableDetector detector = AnonymousVariableDetector.newInstance(saturator);
        SeedFunctionHandler seedFunctionHandler =
                new SeedFunctionHandler(ontology, reasonerWithTBox, reasonerWithoutTBox, detector);
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
                        logger.info(ontology.getClassAssertionAxioms(individual));
                        try {
                            logger.info(Prover.getJustification(ontology,OWLManager.getOWLDataFactory().getOWLClassAssertionAxiom(concept, individual)));
                        } catch (OWLOntologyCreationException e) {
                            throw new RuntimeException(e);
                        }
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
