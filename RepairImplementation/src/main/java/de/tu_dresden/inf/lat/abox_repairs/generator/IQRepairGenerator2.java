package de.tu_dresden.inf.lat.abox_repairs.generator;

import de.tu_dresden.inf.lat.abox_repairs.repair_manager.RepairManagerBuilder;
import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.inf.lat.abox_repairs.saturation.AnonymousVariableDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class IQRepairGenerator2 extends RepairGenerator {

    private static Logger logger = LogManager.getLogger(IQRepairGenerator.class);

    private final CopiedOWLIndividual.Factory copiedOWLIndividualFactory = new CopiedOWLIndividual.Factory();
    private final Queue<CopiedOWLIndividual> queue = new LinkedList<>();

    public IQRepairGenerator2(OWLOntology inputOntology) {
        super(inputOntology);
    }

    @Override
    public int getNumberOfCollectedIndividuals() {
        return copiedOWLIndividualFactory.size();
    }

    @Override
    protected void initialise() {
//        reasonerWithTBox.cleanOntology(); // TODO: bad code design
        try {
            repair = ontology.getOWLOntologyManager().createOntology();
//            repair.add(ontology.getTBoxAxioms(Imports.INCLUDED));
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
//        anonymousDetector = AnonymousVariableDetector.newInstance(true, RepairManagerBuilder.RepairVariant.IQ);
    }

    @Override
    protected void generateVariables() {
        ontology.getIndividualsInSignature().stream()
//                .filter(anonymousDetector::isNamed)
                .map(individualName ->
                        copiedOWLIndividualFactory.newNamedIndividual(individualName, inputSeedFunction.get(individualName))
                ).forEach(queue::offer);
        while (!queue.isEmpty()) {
            final CopiedOWLIndividual subject = queue.poll();
            ontology.classAssertionAxioms(subject.getIndividualInTheSaturation())
                    .map(OWLClassAssertionAxiom::getClassExpression)
                    .filter(owlClassExpression ->
                            !subject.getRepairType().getClassExpressions().contains(owlClassExpression))
//                    .filter(owlClassExpression -> !FreshNameProducer.isFreshOWLClass(owlClassExpression))
                    .map(owlClassExpression ->
                            OWLManager.getOWLDataFactory().getOWLClassAssertionAxiom(owlClassExpression, subject.getIndividualInTheRepair()))
                    .forEach(repair::add);
            ontology.objectPropertyAssertionAxioms(subject.getIndividualInTheSaturation())
                    .forEach(axiom -> {
                        typeHandler.findCoveringRepairTypes(
                                RepairType.empty(),
                                computeSuccessorSet(
                                        subject.getRepairType(),
                                        axiom.getProperty().getNamedProperty(),
                                        axiom.getObject()),
                                axiom.getObject()
                        ).forEach(repairType -> {
                            final Optional<CopiedOWLIndividual> optionalObject =
                                    copiedOWLIndividualFactory.getCopy(axiom.getObject(), repairType);
                            final CopiedOWLIndividual object =
                                    optionalObject.orElseGet(() -> copiedOWLIndividualFactory.newAnonymousIndividual(axiom.getObject(), repairType));
                            repair.add(
                                    OWLManager.getOWLDataFactory()
                                            .getOWLObjectPropertyAssertionAxiom(
                                                    axiom.getProperty(),
                                                    subject.getIndividualInTheRepair(),
                                                    object.getIndividualInTheRepair()));
                            if (!optionalObject.isPresent()) {
                                queue.offer(object);
                            }
                        });
                    });
        }
    }

    @Override
    @Deprecated
    protected void generateMatrix() throws OWLOntologyCreationException {
        /* The matrix is already populated during the above enumeration of all object names. */
    }

}
