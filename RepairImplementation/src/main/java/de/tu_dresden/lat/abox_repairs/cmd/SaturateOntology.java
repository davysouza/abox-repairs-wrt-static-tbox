package de.tu_dresden.lat.abox_repairs.cmd;

import java.io.File;

import de.tu_dresden.lat.abox_repairs.ontology_tools.OntologyPreparations;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
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

    public static void main(String[] args) throws SaturationException, OWLOntologyCreationException {
        String filename = args[0];
        String method = args[1];

        System.out.println("Saturate "+filename+" using method "+method);
        System.out.println("(Methods available: CHASE and CANONICAL)");


        System.out.println("Loading ontology...");
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(filename));
        
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

        System.out.println("Saturating...");
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


        generator.saturate(ontology);

        reasoner.cleanOntology();

        System.out.println("BEFORE: "+axiomsBefore+" AFTER: "+ontology.getAxiomCount());
		//System.out.println("Axioms before: "+axiomsBefore);
		//System.out.println("Axioms after: "+ontology.getAxiomCount());
    }
}
