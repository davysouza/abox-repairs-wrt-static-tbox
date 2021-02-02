package de.tu_dresden.lat.abox_repairs.cmd;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.saturation.ABoxSaturator;
import de.tu_dresden.lat.abox_repairs.saturation.CanonicalModelGenerator;
import de.tu_dresden.lat.abox_repairs.saturation.ChaseGenerator;
import de.tu_dresden.lat.abox_repairs.saturation.CycleChecker;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;

/**
 * @author Patrick Koopmann
 */
public class SaturateOntology {

    public static void main(String[] args) throws SaturationException, OWLOntologyCreationException {
        String filename = args[0];
        String method = args[1];

        System.out.println("Saturate "+filename+" using method "+method);
        System.out.println("(Methods available: CHASE and CANONICAL)");


		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(filename));

        ReasonerFacade reasoner = new ReasonerFacade(ontology);

		CycleChecker cycleChecker = new CycleChecker(reasoner);
		if(method.equals("CHASE") && cycleChecker.cyclic()){
			System.out.println("Found cycle - skipping.");
			System.exit(0);
		}

        ABoxSaturator generator = null; 
        switch(method) {
            case "CHASE": 
                reasoner.cleanOntology();
                generator = new ChaseGenerator(); 
                break;
            case "CANONICAL": generator = new CanonicalModelGenerator(reasoner); break;
            default: 
                System.out.println("Unsupported method: "+method);
                System.exit(1);
        }

		int axiomsBefore = ontology.getAxiomCount();

        generator.saturate(ontology);

        reasoner.cleanOntology();

        System.out.println("BEFORE: "+axiomsBefore+" AFTER: "+ontology.getAxiomCount());
		//System.out.println("Axioms before: "+axiomsBefore);
		//System.out.println("Axioms after: "+ontology.getAxiomCount());
    }
}
