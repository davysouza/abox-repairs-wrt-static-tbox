package de.tudresden.inf.lat.aboxrepair.repairrequest;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class RepairRequestParser {
    private final OWLOntology ontology;
    private final OWLOntologyManager manager;
    private ManchesterOWLSyntaxParser parser;

    /**
     * Class constructor. Responsible to initialize the repair request parser
     * @param ontology Ontology to be parsed
     */
    public RepairRequestParser(OWLOntology ontology) {
        this.ontology = ontology;
        this.manager = ontology.getOWLOntologyManager();

        initParser();
    }

    private void initParser() {
        Set<OWLOntology> importsClosure = ontology.getImportsClosure();

        OWLEntityChecker entityChecker = new ShortFormEntityChecker(
                new BidirectionalShortFormProviderAdapter(manager, importsClosure, new SimpleShortFormProvider()));

        parser = OWLManager.createManchesterParser();
        parser.setDefaultOntology(ontology);
        parser.setOWLEntityChecker(entityChecker);
    }

    /**
     * Scans a repair request file parsing it to a RepairRequest object
     * @param repairRequestFile File containing the repair request assertions
     * @return Returns the RepairRequest object of the parsed ontology
     * @throws FileNotFoundException Couldn't scan the ontology file
     */
    public RepairRequest parse(File repairRequestFile) throws FileNotFoundException {
        RepairRequest repairRequest = new RepairRequest();

        Scanner reader = new Scanner(repairRequestFile);
        while(reader.hasNextLine()) {
            String policy = reader.nextLine();
            parser.setStringToParse(policy.trim());

            OWLClassAssertionAxiom axiom = (OWLClassAssertionAxiom) parser.parseAxiom();
            OWLNamedIndividual assertedIndividual = (OWLNamedIndividual) axiom.getIndividual();

            Set<OWLClassExpression> setOfClasses = repairRequest.containsKey(assertedIndividual)
                    ? repairRequest.get(assertedIndividual)
                    : new HashSet<OWLClassExpression>();

            setOfClasses.add(axiom.getClassExpression());
            repairRequest.put(assertedIndividual, setOfClasses);
        }
        return repairRequest;
    }
}
