package de.tu_dresden.lat.abox_repairs.experiments;

import de.tu_dresden.lat.abox_repairs.generator.CanonicalRepairGenerator;
import de.tu_dresden.lat.abox_repairs.generator.RepairGenerator;
import de.tu_dresden.lat.abox_repairs.ontology_tools.CycleChecker;
import de.tu_dresden.lat.abox_repairs.ontology_tools.OntologyPreparations;
import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.saturation.ABoxSaturator;
import de.tu_dresden.lat.abox_repairs.saturation.ChaseGenerator;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class RunExperimentCanCQ {
    private static Logger logger = LogManager.getLogger(RunExperimentCanCQ.class);

    private RunExperimentCanCQ() {
    }

    public static void main(String[] args) throws OWLOntologyCreationException, SaturationException {
        final String ontologyFileName = args[0];
        final OWLOntology ontology =
                OWLManager.createOWLOntologyManager()
                        .loadOntologyFromOntologyDocument(new File(ontologyFileName));
        OntologyPreparations.prepare(ontology);

        long startTime = System.nanoTime();

        int oldIndividuals = ontology.getIndividualsInSignature().size();
        long oldAssertions = ontology.aboxAxioms(Imports.INCLUDED).count();

        long start = System.nanoTime();
        logger.info("Init reasoner facade without TBox");
        final List<OWLClassExpression> additionalExpressions = new LinkedList<>();
        additionalExpressions.addAll(ontology.getNestedClassExpressions());
        final ReasonerFacade reasonerWithoutTBox = ReasonerFacade.newReasonerFacadeWithoutTBox(additionalExpressions, ontology.getOWLOntologyManager());
        logger.info("Init reasoner facade with TBox");
        final ReasonerFacade reasonerWithTBox = ReasonerFacade.newReasonerFacadeWithTBox(ontology, additionalExpressions);
        logger.info("Initialising reasoner facades took " + ((double) System.nanoTime() - start) / 1_000_000_000);

        CycleChecker cycleChecker = new CycleChecker(reasonerWithTBox);
        if (cycleChecker.cyclic()) {
            System.out.println("Cyclic!");
            System.exit(0);
        }

        System.out.println("\nCQ-saturation");
        final ABoxSaturator saturator = new ChaseGenerator();
        saturator.saturate(ontology);

        reasonerWithTBox.update();

        System.out.println("\nCQ-repair");
        final RepairGenerator repairGenerator = new CanonicalRepairGenerator(ontology);
        repairGenerator.setReasoner(reasonerWithTBox, reasonerWithoutTBox);
        repairGenerator.repair();
        /*ontology = repairGenerator.getRepair();*/

        double timeRepairing = (double) (System.nanoTime() - startTime) / 1_000_000_000;

        /**
         * Please do not remove! This is not debugging output, but used for the evaluation of the experiments.
         */
        System.out.print("#Individuals (orig/sat/repair): "
                + oldIndividuals + "/"
                + (oldIndividuals + saturator.addedIndividuals()) + "/" +
                repairGenerator.getNumberOfCollectedIndividuals());
        System.out.print(" #Assertions (orig/sat/repair): "
                + oldAssertions + "/"
                + (oldAssertions + saturator.addedAssertions()) + "/-");
        System.out.println(" Duration (sat/repair sec.): " + saturator.getDuration() + "/" + timeRepairing);
    }
}
