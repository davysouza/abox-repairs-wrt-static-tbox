package de.tu_dresden.lat.abox_repairs.repairManager;

import de.tu_dresden.lat.abox_repairs.generator.*;
import de.tu_dresden.lat.abox_repairs.ontology_tools.CycleChecker;
import de.tu_dresden.lat.abox_repairs.ontology_tools.OntologyPreparations;
import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.lat.abox_repairs.saturation.ABoxSaturator;
import de.tu_dresden.lat.abox_repairs.saturation.CanonicalModelGenerator;
import de.tu_dresden.lat.abox_repairs.saturation.ChaseGenerator;
import de.tu_dresden.lat.abox_repairs.saturation.DummySaturator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class RepairManagerBuilder {

    private static Logger logger = LogManager.getLogger(RepairManagerBuilder.class);

    public enum RepairVariant {IQ, IQ2, CQ, CANONICAL_IQ, CANONICAL_CQ}

    //private final RepairVariant variant;

    public static EnumSet<RepairVariant> IQ_ANY = EnumSet.of(RepairVariant.IQ, RepairVariant.IQ2, RepairVariant.CANONICAL_IQ);
    public static EnumSet<RepairVariant> CQ_ANY = EnumSet.of(RepairVariant.CQ, RepairVariant.CANONICAL_CQ);
    public static EnumSet<RepairVariant> CANONICAL_ANY = EnumSet.of(RepairVariant.CANONICAL_IQ, RepairVariant.CANONICAL_CQ);

    private OWLOntology ontology;
    private ReasonerFacade reasonerWithoutTBox, reasonerWithTBox;
    private RepairGenerator repairGenerator;
    private ABoxSaturator saturator;
    private RepairRequest repairRequest;
    private boolean saturate = true;

    private RepairVariant variant;

    public RepairManagerBuilder setVariant(RepairVariant variant) {
        this.variant = variant;
        return this;
    }

    public RepairManagerBuilder setNeedsSaturation(boolean saturate) {
        this.saturate = saturate;
        return this;
    }

    public RepairManagerBuilder setRepairRequest(RepairRequest repairRequest) {
        this.repairRequest = repairRequest;
        return this;
    }

    public RepairManagerBuilder setOntology(OWLOntology ontology) {
        this.ontology = ontology;
        OntologyPreparations.prepare(ontology);

        try {
            ontology.getOWLOntologyManager().saveOntology(ontology, new FileOutputStream(new File("el-fragment.owl")));
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return this;
    }

    public RepairManager build() throws OWLOntologyCreationException {
        initReasonerFacades();

        if (!RepairRequest.checkValid(repairRequest, reasonerWithTBox)) {
            throw new IllegalArgumentException("Invalid repair request.");
        }

        fixSaturator();
        fixRepairGenerator();

        return new RepairManager(ontology, reasonerWithoutTBox, reasonerWithTBox, repairGenerator, saturator, repairRequest);
    }


    private void initReasonerFacades() throws OWLOntologyCreationException {
        long start = System.nanoTime();

        if (ontology == null || repairRequest==null)
            throw new IllegalArgumentException("Ontology and repair request have to be set first!");

        Set<OWLClassExpression> additionalExpressions = new HashSet<>();

        for (Collection<OWLClassExpression> exps : repairRequest.values()) {
            for (OWLClassExpression exp : exps) {
                additionalExpressions.add(exp);
                additionalExpressions.addAll(exp.getNestedClassExpressions());
            }
        }

        logger.info("Init reasoner facade without TBox");
        additionalExpressions.addAll(ontology.getNestedClassExpressions());
        reasonerWithoutTBox = ReasonerFacade.newReasonerFacadeWithoutTBox(additionalExpressions, ontology.getOWLOntologyManager());

        logger.info("Init reasoner facade with TBox");
        reasonerWithTBox = ReasonerFacade.newReasonerFacadeWithTBox(ontology, additionalExpressions);

        logger.info("Initialising reasoner facades took " + ((double) System.nanoTime() - start) / 1_000_000_000);
    }

    private void fixSaturator() {
        CycleChecker cycleChecker = new CycleChecker(reasonerWithTBox);
        if (cycleChecker.cyclic())
            throw new IllegalArgumentException("Ontology is cyclic - chase cannot be computed!");

        if (!saturate)
            saturator = new DummySaturator();
        else if (IQ_ANY.contains(variant))
            saturator = new CanonicalModelGenerator(reasonerWithTBox);
        else {
            assert CQ_ANY.contains(variant);
            saturator = new ChaseGenerator();
        }
    }

    private void fixRepairGenerator() {
        if (CANONICAL_ANY.contains(variant))
            repairGenerator = new CanonicalRepairGenerator(ontology);
        else if (variant.equals(RepairVariant.IQ))
            repairGenerator = new IQRepairGenerator(ontology);
        else if (variant.equals(RepairVariant.IQ2))
            repairGenerator = new IQRepairGenerator2(ontology);
        else {
            assert variant.equals(RepairVariant.CQ);
            repairGenerator = new CQRepairGenerator(ontology);
        }

        repairGenerator.setReasoner(reasonerWithTBox,reasonerWithoutTBox);
    }
}
