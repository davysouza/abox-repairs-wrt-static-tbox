package de.tu_dresden.lat.abox_repairs.generator;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.*;

public class CQRepairGenerator extends RepairGenerator {

    private static Logger logger = LogManager.getLogger(CQRepairGenerator.class);
    private final Set<CopiedOWLIndividual> individualsInTheRepair = new HashSet<>();
    private final CopiedOWLIndividual.Factory copiedOWLIndividualFactory = new CopiedOWLIndividual.Factory();
    private final Map<OWLNamedIndividual, RepairType> seedFunction;
    private final Queue<CopiedOWLObjectPropertyAssertionAxiom> queue = new LinkedList<>();
    private final OWLOntologyWithFurtherIndexes ontologyWithFurtherIndexes;

    public CQRepairGenerator(OWLOntology inputOntology,
                             Map<OWLNamedIndividual, RepairType> inputSeedFunction) {

        super(inputOntology, inputSeedFunction);
        this.seedFunction = inputSeedFunction;
        this.ontologyWithFurtherIndexes = new OWLOntologyWithFurtherIndexes(inputOntology);
    }

    @Override
    public int getNumberOfCollectedIndividuals() {
        return individualsInTheRepair.size();
    }

    @Override
    protected void initialise() {
        reasonerWithTBox.cleanOntology(); // TODO: bad code design
        try {
            repair = ontology.getOWLOntologyManager().createOntology();
            repair.add(ontology.getTBoxAxioms(Imports.INCLUDED));
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
        anonymousDetector = AnonymousVariableDetector.newInstance(true, Main.RepairVariant.CQ);
    }

    @Override
    protected void generateVariables() {

        /**
         * The set of collected individuals for the repair will contain every named individual
         */
        inputObjectNames.stream()
                .filter(anonymousDetector::isNamed)
                .map(individualName ->
                        copiedOWLIndividualFactory.newNamedIndividual(individualName, seedFunction.get(individualName))
                ).forEach(this::addNewIndividualToTheRepair);

        /**
         * plus one copy of every variable, and every named individual with nothing assigned by the initial seed function,
         * which is assigned an empty repair type.
         */
        inputObjectNames.stream()
                .filter(objectName -> // If the test fails, then we have already created a copy above.
                        !(anonymousDetector.isNamed(objectName) && seedFunction.get(objectName).isEmpty()))
                .map(objectName ->
                        copiedOWLIndividualFactory.newAnonymousIndividual(objectName, RepairType.empty())
                ).forEach(this::addNewIndividualToTheRepair);

        while (!queue.isEmpty()) {
            final CopiedOWLObjectPropertyAssertionAxiom copiedAxiom = queue.poll();
            final CopiedOWLIndividual subject = copiedAxiom.getSubject();
            final OWLObjectPropertyExpression property = copiedAxiom.getProperty();
            final CopiedOWLIndividual object = copiedAxiom.getObject();
            final Set<OWLClassExpression> successorSet = computeSuccessorSet(
                    subject.getRepairType(),
                    property.getNamedProperty(),
                    object.getIndividualInTheSaturation().asOWLNamedIndividual());
            if (reasonerWithoutTBox.isCovered(successorSet, object.getRepairType().getClassExpressions())) {
                repair.add(copiedAxiom.toAxiomInTheRepair());
            } else {
                typeHandler.findCoveringRepairTypes(object.getRepairType(), successorSet, object.getIndividualInTheSaturation().asOWLNamedIndividual())
                        .forEach(repairType -> {
                            final CopiedOWLIndividual newIndividual =
                                    copiedOWLIndividualFactory.newAnonymousIndividual(object.getIndividualInTheSaturation(), repairType);
                            addNewIndividualToTheRepair(newIndividual);
                        });
            }
        }
    }

    private boolean addNewIndividualToTheRepair(CopiedOWLIndividual newIndividual) {
        if (individualsInTheRepair.add(newIndividual)) {
            ontology.classAssertionAxioms(newIndividual.getIndividualInTheSaturation())
                    .map(OWLClassAssertionAxiom::getClassExpression)
                    .filter(owlClassExpression ->
                            !newIndividual.getRepairType().getClassExpressions().contains(owlClassExpression))
//                    .filter(owlClassExpression -> !FreshNameProducer.isFreshOWLClass(owlClassExpression))
                    .map(owlClassExpression ->
                            OWLManager.getOWLDataFactory().getOWLClassAssertionAxiom(owlClassExpression, newIndividual.getIndividualInTheRepair()))
                    .forEach(repair::add);
            ontologyWithFurtherIndexes.objectPropertyAssertionAxiomsWithSubject(newIndividual.getIndividualInTheSaturation())
                    .flatMap(axiom ->
                            copiedOWLIndividualFactory.getCopiesOf(axiom.getObject()).stream()
                                    .map(individual ->
                                            new CopiedOWLObjectPropertyAssertionAxiom(newIndividual, axiom.getProperty(), individual))
                    )
                    .forEach(queue::offer);
            ontologyWithFurtherIndexes.objectPropertyAssertionAxiomsWithObject(newIndividual.getIndividualInTheSaturation())
                    .flatMap(axiom ->
                            copiedOWLIndividualFactory.getCopiesOf(axiom.getSubject()).stream()
                                    .filter(individual ->
                                            !individual.equals(newIndividual))
                                    .map(individual ->
                                            new CopiedOWLObjectPropertyAssertionAxiom(individual, axiom.getProperty(), newIndividual))
                    )
                    .forEach(queue::offer);
            return true;
        }
        return false;
    }

    @Override
    @Deprecated
    protected void generateMatrix() throws OWLOntologyCreationException {
        /* The matrix is already populated during the above enumeration of all object names. */
    }

}
