package de.tu_dresden.lat.abox_repairs;

import java.io.*;
import java.util.*;

import de.tu_dresden.lat.abox_repairs.ontology_tools.*;
import de.tu_dresden.lat.abox_repairs.saturation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import de.tu_dresden.lat.abox_repairs.generator.CQRepairGenerator;
import de.tu_dresden.lat.abox_repairs.generator.CanonicalRepairGenerator;
import de.tu_dresden.lat.abox_repairs.generator.IQRepairGenerator;
import de.tu_dresden.lat.abox_repairs.generator.RepairGenerator;
import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;

/**
 * TODO: be able to handle the case where the given ontology is already saturated.
 */
public class Main {

    private static Logger logger = LogManager.getLogger(Main.class);

    /**
     * Check: usually, static fields (variables) should be avoided where possible.
     * In this case: check whether they are really needed to be outside the main method,
     * and otherwise, add them.
     */
    private OWLOntology ontology;

    private Map<OWLNamedIndividual, RepairType> seedFunction;
    private RepairRequest repairRequest;

    private ReasonerFacade reasonerWithTBox, reasonerWithoutTBox;

    public static enum RepairVariant {IQ, CQ, CANONICAL_IQ, CANONICAL_CQ};
    
    private RepairVariant variant;

    public static EnumSet<RepairVariant> IQ_ANY = EnumSet.of(RepairVariant.IQ, RepairVariant.CANONICAL_IQ);
    public static EnumSet<RepairVariant> CQ_ANY = EnumSet.of(RepairVariant.CQ, RepairVariant.CANONICAL_CQ);
    public static EnumSet<RepairVariant> CANONICAL_ANY = EnumSet.of(RepairVariant.CANONICAL_IQ, RepairVariant.CANONICAL_CQ);

    private ABoxSaturator saturator;

    private RepairGenerator repairGenerator;

    private final Random random;

    public static void main(String args[]) throws IOException, OWLOntologyCreationException, SaturationException {

        Main m = new Main();

        int i = 0;
        while (i < args.length) {
            // Initialize ontology
            // m.ontologyInitialisation(args, i);

            m.ontology =
                    OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(args[i]));

            logger.debug("after loading ontology: ");
            
            System.out.println(m.ontology.getNestedClassExpressions());

            File file = new File(args[i + 1]);
            RepairRequestParser rrParser = new RepairRequestParser(m.ontology);
            m.repairRequest = rrParser.repairRequestScanning(file);


            


            switch (args[i + 2]) {
                case "IQ":
                    m.variant = RepairVariant.IQ;
                    break;
                case "CQ":
                    m.variant = RepairVariant.CQ;
                    break;
                case "CANONICAL_IQ":
                    m.variant = RepairVariant.CANONICAL_IQ;
                    break;
                case "CANONICAL_CQ":
                    m.variant = RepairVariant.CANONICAL_CQ;
                    break;
                default:
                    System.out.println("Unknown repairVariant: " + args[i + 2]);
                    System.exit(1);
                    m.variant = RepairVariant.CQ;
            }

            if (args[i + 3].equals("true"))
                m.performRepair(m.ontology, m.repairRequest, m.variant, true);
            else
                m.performRepair(m.ontology, m.repairRequest, m.variant, false);

            i += 4;
            if (i < args.length) System.out.println("\n" + "=================================================");
//			}
        }
    }

    public Main() {
        this.random = new Random();
    }

    public Main(Random random) {
        this.random = random;
    }

    public void performRepair(OWLOntology inputOntology,
                              RepairRequest repairRequest,
                              RepairVariant repairVariant,
                              boolean saturationRequired
    ) throws OWLOntologyCreationException, SaturationException {


        //OWLOntology module = new RelevantSubOntologyExtractor(inputOntology)
        //		.relevantSubOntology(repairRequest);

        //setOntology(module);
        setOntology(inputOntology);
        this.repairRequest = repairRequest;
        this.variant = repairVariant;

        long startTime = System.nanoTime();

        int oldIndividuals = ontology.getIndividualsInSignature().size();
        long oldAssertions = ontology.aboxAxioms(Imports.INCLUDED).count();

        //boolean tboxExists = true;
        if (ontology.getTBoxAxioms(Imports.INCLUDED).isEmpty()) {
            //tboxExists = false;
            saturationRequired = false; // TODO bad style - should not reassign parameter variables!
        }
        //saturationRequirednoSaturation = !saturationRequired||!tboxExists;

        if (!saturationRequired)
            saturator = new DummySaturator();


        // Initialize reasoner
        initReasonerFacade();

        CycleChecker cycleChecker = new CycleChecker(reasonerWithTBox);
        if (CQ_ANY.contains(repairVariant) && cycleChecker.cyclic()) {
            System.out.println("Cyclic!");
            System.exit(0);
        }

        if (CQ_ANY.contains(repairVariant) && saturationRequired) {
            cqSaturate();
        }


        logger.debug("after initializing reasoners: ");


        if (!RepairRequest.checkValid(repairRequest, reasonerWithTBox)) {
            throw new IllegalArgumentException("Invalid repair request.");
        }

        // Saturate the ontology
        if (IQ_ANY.contains(repairVariant) && saturationRequired) {
            iqSaturate();
        }


        reasonerWithTBox.update();

        if (isCompliant()) {
            System.out.println("\nThe ontology is compliant!");
        } else {
            System.out.println("\nThe ontology is not compliant!");

            
            if(!CANONICAL_ANY.contains(repairVariant)) {
            	seedFunctionConstruction();

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
            }
            

            if (repairVariant.equals(RepairVariant.IQ)) {
                repairIQ();

            } else if (repairVariant.equals(RepairVariant.CQ)) {
                repairCQ();
            } else {
                assert CANONICAL_ANY.contains(repairVariant);
                CanonicalRepair();
            }

            reasonerWithTBox = ReasonerFacade.newReasonerFacadeWithTBox(ontology, additionalExpressions);
            reasonerWithTBox.update();

            cleanOntology(); // <-- this should not be done if the reasoner facades is still used!

            double timeRepairing = (double) (System.nanoTime() - startTime) / 1_000_000_000;


            /**
             * Please do not remove! This is not debugging output, but used for the evaluation of the experiments.
             */
            if (CANONICAL_ANY.contains(repairVariant)) {
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
            initReasonerFacade();

            if (isCompliant()) {
                System.out.println("The ontology is now compliant");
            } else {
                System.out.println("The ontology is still not compliant");
            }
        }
    }


    public void setOntology(OWLOntology ontology) {
        this.ontology = ontology;
        OntologyPreparations.prepare(ontology);

        try {
            ontology.getOWLOntologyManager().saveOntology(ontology, new FileOutputStream(new File("el-fragment.owl")));
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

//		logger.debug("Ontology:");
//		ontology.axioms().forEach(ax -> logger.debug(ax));

    }

    private List<OWLClassExpression> additionalExpressions = new LinkedList<>();

    private void initReasonerFacade() throws OWLOntologyCreationException {
        long start = System.nanoTime();

        for (Collection<OWLClassExpression> exps : repairRequest.values()) {
            for (OWLClassExpression exp : exps) {
                additionalExpressions.add(exp);
                additionalExpressions.addAll(exp.getNestedClassExpressions());
            }
        }

        logger.info("Init reasoner facade without TBox");
        additionalExpressions.addAll(ontology.getNestedClassExpressions());
        reasonerWithoutTBox = ReasonerFacade.newReasonerFacadeWithoutTBox(additionalExpressions, ontology.getOWLOntologyManager());


        logger.info("Init reasoner facade with TBox");
        reasonerWithTBox = ReasonerFacade.newReasonerFacadeWithTBox(ontology, additionalExpressions);


        logger.info("Initialising reasoner facades took " + ((double) System.nanoTime() - start) / 1_000_000_000);

    }


    private void cqSaturate() throws SaturationException {
        System.out.println("\nCQ-saturation");

        saturator = new ChaseGenerator();
        saturator.saturate(ontology);
    }

    private void iqSaturate() throws SaturationException {
        System.out.println("\nIQ-saturation");
        saturator = new CanonicalModelGenerator(reasonerWithTBox);
        saturator.saturate(ontology);
    }

    private void seedFunctionConstruction() {
        long time = System.nanoTime();
        
        SeedFunctionHandler seedFunctionHandler = new SeedFunctionHandler(ontology, repairRequest, variant,
        		reasonerWithTBox, reasonerWithoutTBox);
        seedFunction = seedFunctionHandler.computeRandomSeedFunction();

        logger.info("Seed function construction took: " + (((double) System.nanoTime() - time) / 1_000_000_000));
    }

    private boolean isCompliant() {
        boolean compliant = true;

        for (OWLNamedIndividual individual : repairRequest.keySet()) {

            for (OWLClassExpression concept : repairRequest.get(individual)) {
                if (reasonerWithTBox.instanceOf(individual, concept)) {
                    logger.info("The ontology is not compliant, since " + individual + " is an instance of " + concept + ".");
                    if (seedFunction != null) {
//                    	logger.info("check if the seed function is " + seedFunction);
//                    	logger.info("The individual " + individual + "is now being handled.");
//                        logger.info("The seed function maps " + individual + " to " + seedFunction.get(individual).getClassExpressions());
                        logger.info(ontology.getClassAssertionAxioms(individual));
                        try {
                            logger.info(Prover.getJustification(ontology,OWLManager.getOWLDataFactory().getOWLClassAssertionAxiom(concept, individual)));
                        } catch (OWLOntologyCreationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return false;
                }
            }

        }

        return compliant;
    }

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
}
