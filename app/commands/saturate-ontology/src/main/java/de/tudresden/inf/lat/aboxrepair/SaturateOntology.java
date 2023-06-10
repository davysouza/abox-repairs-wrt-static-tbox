package de.tudresden.inf.lat.aboxrepair;

import de.tudresden.inf.lat.aboxrepair.ontologytools.CycleChecker;
import de.tudresden.inf.lat.aboxrepair.ontologytools.OntologyPreparations;
import de.tudresden.inf.lat.aboxrepair.reasoner.ReasonerFacade;
import de.tudresden.inf.lat.aboxrepair.saturation.ABoxSaturator;
import de.tudresden.inf.lat.aboxrepair.saturation.CanonicalModelGenerator;
import de.tudresden.inf.lat.aboxrepair.saturation.ChaseGenerator;
import de.tudresden.inf.lat.aboxrepair.saturation.SaturationException;
import de.tudresden.inf.lat.aboxrepair.utils.PrettyMessage;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * @author Patrick Koopmann
 */
public class SaturateOntology {
    private enum Method { CHASE, CANONICAL };
    private static File ontologyFile;
    private static Method saturationMethod;
    private static ABoxSaturator saturator;

    public static void main(String[] args) throws SaturationException, OWLOntologyCreationException, FileNotFoundException, OWLOntologyStorageException {
        if(!parseArguments(args)) {
            System.exit(1);
        }

        System.out.println("Loading ontology...");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);

        System.out.println("  TBox: " + ontology.getTBoxAxioms(Imports.INCLUDED).size() + " axioms\n" +
                           "  ABox: " + ontology.getABoxAxioms(Imports.INCLUDED).size() + " axioms");

        System.out.println("Restricting to pure EL and flattening ABox...");
        OntologyPreparations.prepare(ontology, null);

        System.out.println("  TBox: " + ontology.getTBoxAxioms(Imports.INCLUDED).size() + " axioms\n" +
                           "  ABox: " + ontology.getABoxAxioms(Imports.INCLUDED).size() + " axioms");

        int axiomsBefore = ontology.getAxiomCount();

        System.out.println("Classifying...");
        ReasonerFacade reasoner = ReasonerFacade.newReasonerFacadeWithTBox(ontology);

        System.out.println("Checking for cycles...");
        CycleChecker cycleChecker = new CycleChecker(reasoner);
        if(saturationMethod == Method.CHASE && cycleChecker.cyclic()) {
            PrettyMessage.printError("A cycle was found. The chase saturation cannot be executed into cyclic ontologies.");
            System.exit(0);
        }

        System.out.println("Saturating...");
        switch(saturationMethod) {
            case CHASE:
                reasoner.cleanOntology();
                saturator = new ChaseGenerator();
                break;
            case CANONICAL:
                saturator = new CanonicalModelGenerator(reasoner);
                break;
            default:
                PrettyMessage.printError("Unexpected behavior: An unsupported saturation method was chosen.");
                System.exit(1);
        }

        saturator.saturate(ontology);
        reasoner.cleanOntology();

        System.out.println("Saturation completed.");
        System.out.println("BEFORE SATURATION: " + axiomsBefore + "axioms.\n" +
                           "AFTER SATURATION: " + ontology.getAxiomCount() + "axioms.");

        String filename = ontologyFile.getName();
        String baseName = filename.substring(0, filename.lastIndexOf("."));
        String outputName = baseName + "-" + saturationMethod + ".owl";

        System.out.println("Saving...");
        ontology.saveOntology(new FileOutputStream(new File(outputName)));
    }

    private static boolean parseArguments(String args[]) {
        if(args == null || args.length != 2) {
            PrettyMessage.printError("ERROR: Invalid or missing arguments\n");
            return false;
        }

        try {
            ontologyFile = new File(args[0]);

            switch (args[1]) {
                case "CHASE":
                    saturationMethod = Method.CHASE;
                    break;
                case "CANONICAL":
                    saturationMethod = Method.CANONICAL;
                    break;
                default:
                    PrettyMessage.printError("ERROR: Unsupported saturation method." + "\n" +
                                             "  - (Methods available: CHASE and CANONICAL)");
                    return false;
            }
        } catch (Exception e) {
            PrettyMessage.printError("ERROR: Failed to parse arguments\n");
            System.out.println("Exception: " + e.getMessage());
            return false;
        }

        PrettyMessage.printInfo("Saturation of " + args[0] + " will be executed using " + args[1] + " method.");
        return true;
    }
}
