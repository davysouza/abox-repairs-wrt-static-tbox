package de.tu_dresden.lat.abox_repairs;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class RepairRequestParser {

    private final OWLOntology ontology;
    private final OWLOntologyManager manager;

    private ManchesterOWLSyntaxParser parser;

    public RepairRequestParser(OWLOntology ontology) {
        this.ontology=ontology;
        this.manager=ontology.getOWLOntologyManager();

        initParser();
    }


    private void initParser() {
        Set<OWLOntology> importsClosure = ontology.getImportsClosure();
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(
                new BidirectionalShortFormProviderAdapter(manager, importsClosure,
                        new SimpleShortFormProvider()));

        parser = OWLManager.createManchesterParser();
        parser.setDefaultOntology(ontology);
        parser.setOWLEntityChecker(entityChecker);
    }


    public RepairRequest repairRequestScanning(File file) throws FileNotFoundException {

        Scanner reader = new Scanner(file);
        RepairRequest repairRequest = new RepairRequest();
        while(reader.hasNextLine()) {
            String policy = reader.nextLine();
            parser.setStringToParse(policy.trim());

            OWLClassAssertionAxiom axiom = (OWLClassAssertionAxiom) parser.parseAxiom();

            OWLNamedIndividual assertedIndividual = (OWLNamedIndividual) axiom.getIndividual();

            if(repairRequest.containsKey(assertedIndividual)) {
                Set<OWLClassExpression> setOfClasses = repairRequest.get(assertedIndividual);
                setOfClasses.add(axiom.getClassExpression());
                repairRequest.put(assertedIndividual, setOfClasses);
            }
            else {
                Set<OWLClassExpression> setOfClasses = new HashSet<OWLClassExpression>();
                setOfClasses.add(axiom.getClassExpression());
                repairRequest.put(assertedIndividual, setOfClasses);
            }
        }

        return repairRequest;
    }

}
