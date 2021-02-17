package de.tu_dresden.lat.abox_repairs.cmd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import de.tu_dresden.lat.abox_repairs.ontology_tools.OntologyPreparations;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import de.tu_dresden.lat.abox_repairs.ontology_tools.CycleChecker;
import de.tu_dresden.lat.abox_repairs.ontology_tools.ELRestrictor;
import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.saturation.ABoxSaturator;
import de.tu_dresden.lat.abox_repairs.saturation.CanonicalModelGenerator;
import de.tu_dresden.lat.abox_repairs.saturation.ChaseGenerator;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;

/**
 * @author Patrick Koopmann
 */
public class SaturateOntology {

    private enum Method {CHASE, CANONICAL};

    public static void main(String[] args) throws SaturationException, OWLOntologyCreationException, FileNotFoundException, OWLOntologyStorageException {
        String pathString = args[0];
        String method = args[1];

        System.out.println("Saturate "+pathString+" using method "+method);
        System.out.println("(Methods available: CHASE and CANONICAL)");


        File inputFile = new File(pathString);

        System.out.println("Loading ontology...");
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(inputFile);
        
        System.out.println("TBox: "+ontology.getTBoxAxioms(Imports.INCLUDED).size()
            +" ABox: "+ontology.getABoxAxioms(Imports.INCLUDED).size());

        System.out.println("Restricting to pure EL and flattening ABox...");
        OntologyPreparations.prepare(ontology);

        System.out.println("TBox: "+ontology.getTBoxAxioms(Imports.INCLUDED).size()
            +" ABox: "+ontology.getABoxAxioms(Imports.INCLUDED).size());
            
        int axiomsBefore = ontology.getAxiomCount();
        
        System.out.println("Classifying...");
        ReasonerFacade reasoner = ReasonerFacade.newReasonerFacadeWithTBox(ontology);

        System.out.println("Checking for cycles...");
		CycleChecker cycleChecker = new CycleChecker(reasoner);
		if(method.equals("CHASE") && cycleChecker.cyclic()){
			System.out.println("Found cycle - skipping.");
			System.exit(0);
		}

		Method methodUsed = Method.CANONICAL;

        System.out.println("Saturating...");
        ABoxSaturator generator = null; 
        switch(method) {
            case "CHASE": 
                reasoner.cleanOntology();
                generator = new ChaseGenerator();
                methodUsed=Method.CHASE;
                break;
            case "CANONICAL":
                generator = new CanonicalModelGenerator(reasoner);
                methodUsed=Method.CANONICAL;
                break;
            default: 
                System.out.println("Unsupported method: "+method);
                System.exit(1);
        }


        generator.saturate(ontology);

        reasoner.cleanOntology();

        System.out.println("BEFORE: "+axiomsBefore+" AFTER: "+ontology.getAxiomCount());

        String filename = inputFile.getName();

        String baseName = filename.substring(0, filename.lastIndexOf("."));

        String outputName = baseName+"-"+method+".owl";

        System.out.println("saving...");

        ontology.saveOntology(new FileOutputStream(new File(outputName)));

    }
}
