package de.tudresden.inf.lat.aboxrepair.generator;

import de.tudresden.inf.lat.aboxrepair.repairtype.RepairType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class CQRepairGenerator extends RepairGenerator {
    private static Logger logger = LogManager.getLogger(CQRepairGenerator.class);
    private final CopiedOWLIndividual.FactoryCQ copiedOWLIndividualFactory = new CopiedOWLIndividual.FactoryCQ();
    private final Queue<CopiedOWLObjectPropertyAssertionAxiom> queue = new LinkedList<>();
    private final OWLOntologyWithFurtherIndexes ontologyWithFurtherIndexes;

    public CQRepairGenerator(OWLOntology inputOntology) {
        super(inputOntology);
        this.ontologyWithFurtherIndexes = new OWLOntologyWithFurtherIndexes(inputOntology);
    }

    @Override
    public int getNumberOfCollectedIndividuals() {
        return copiedOWLIndividualFactory.size();
    }

    @Override
    protected void initialise() {
        // reasonerWithTBox.cleanOntology(); // TODO: bad code design
        try {
            repair = ontology.getOWLOntologyManager().createOntology();
            // repair.add(ontology.getTBoxAxioms(Imports.INCLUDED));
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
        // anonymousDetector = AnonymousVariableDetector.newInstance(true, RepairManagerBuilder.RepairVariant.CQ);
    }

    @Override
    protected void generateVariables() {
        /**
         * The set of collected individuals for the repair will contain every named individual
         */
//        inputObjectNames.stream()
//                .filter(anonymousDetector::isNamed)
//                .map(individualName ->
//                        copiedOWLIndividualFactory.newNamedIndividual(individualName, inputSeedFunction.get(individualName))
//                ).forEach(this::addNewIndividualToTheRepair);

        /**
         * plus one copy of every variable, and every named individual with nothing assigned by the initial seed function,
         * which is assigned an empty repair type.
         */
//        inputObjectNames.stream()
//                .filter(objectName -> // If the test fails, then we have already created a copy above.
//                        !(anonymousDetector.isNamed(objectName) && inputSeedFunction.get(objectName).isEmpty()))
//                .map(objectName ->
//                        copiedOWLIndividualFactory.newAnonymousIndividual(objectName, RepairType.empty())
//                ).forEach(this::addNewIndividualToTheRepair);

        ontology.getIndividualsInSignature().stream().forEach(objectName -> {
//            if (anonymousDetector.isNamed(objectName)) {
            addNewIndividualToTheRepair(copiedOWLIndividualFactory.newNamedIndividual(objectName, inputSeedFunction.get(objectName)));
            if (!inputSeedFunction.get(objectName).isEmpty()) {
                addNewIndividualToTheRepair(copiedOWLIndividualFactory.newAnonymousIndividual(objectName, RepairType.empty()));
            }
//            } else {
//                addNewIndividualToTheRepair(copiedOWLIndividualFactory.newAnonymousIndividual(objectName, RepairType.empty()));
//            }
        });
        ontology.getAnonymousIndividuals().stream().forEach(objectName -> {
            addNewIndividualToTheRepair(copiedOWLIndividualFactory.newAnonymousIndividual(objectName, RepairType.empty()));
        });

        while (!queue.isEmpty()) {
            final CopiedOWLObjectPropertyAssertionAxiom copiedAxiom = queue.poll();
            final CopiedOWLIndividual subject = copiedAxiom.getSubject();
            final OWLObjectPropertyExpression property = copiedAxiom.getProperty();
            final CopiedOWLIndividual object = copiedAxiom.getObject();
            final Set<OWLClassExpression> successorSet = computeSuccessorSet(
                    subject.getRepairType(),
                    property.getNamedProperty(),
                    object.getIndividualInTheSaturation());
            if (reasonerWithoutTBox.isCovered(successorSet, object.getRepairType().getClassExpressions())) {
                repair.add(copiedAxiom.toAxiomInTheRepair());
            } else {
                typeHandler.findCoveringRepairTypes(object.getRepairType(), successorSet, object.getIndividualInTheSaturation())
                        .forEach(repairType -> {
                            if (!copiedOWLIndividualFactory.containsCopy(object.getIndividualInTheSaturation(), repairType)) {
                                addNewIndividualToTheRepair(copiedOWLIndividualFactory.newAnonymousIndividual(object.getIndividualInTheSaturation(), repairType));
                            }
                        });
            }
        }
    }

    private void addNewIndividualToTheRepair(CopiedOWLIndividual newIndividual) {
        ontology.classAssertionAxioms(newIndividual.getIndividualInTheSaturation())
                .map(OWLClassAssertionAxiom::getClassExpression)
                .filter(owlClassExpression ->
                        !newIndividual.getRepairType().getClassExpressions().contains(owlClassExpression))
//                    .filter(owlClassExpression -> !FreshNameProducer.isFreshOWLClass(owlClassExpression))
                .map(owlClassExpression ->
                        OWLManager.getOWLDataFactory().getOWLClassAssertionAxiom(owlClassExpression, newIndividual.getIndividualInTheRepair()))
                .forEach(repair::add);

        ontologyWithFurtherIndexes.objectPropertyAssertionAxiomsWithSubject(newIndividual.getIndividualInTheSaturation())
                .flatMap(axiom -> copiedOWLIndividualFactory.getCopiesOf(axiom.getObject()).stream()
                        .map(individual -> new CopiedOWLObjectPropertyAssertionAxiom(newIndividual, axiom.getProperty(), individual)))
                .forEach(queue::offer);

        ontologyWithFurtherIndexes.objectPropertyAssertionAxiomsWithObject(newIndividual.getIndividualInTheSaturation())
                .flatMap(axiom -> copiedOWLIndividualFactory.getCopiesOf(axiom.getSubject()).stream()
                        .filter(individual -> !individual.equals(newIndividual))
                        .map(individual ->
                                new CopiedOWLObjectPropertyAssertionAxiom(individual, axiom.getProperty(), newIndividual)))
                .forEach(queue::offer);
    }

    @Override
    @Deprecated
    protected void generateMatrix() throws OWLOntologyCreationException {
        /* The matrix is already populated during the above enumeration of all object names. */
    }
}
