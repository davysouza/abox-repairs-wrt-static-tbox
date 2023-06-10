package de.tudresden.inf.lat.aboxrepair.repairmanager;

import de.tudresden.inf.lat.aboxrepair.generator.*;
import de.tudresden.inf.lat.aboxrepair.ontologytools.CycleChecker;
import de.tudresden.inf.lat.aboxrepair.ontologytools.OntologyPreparations;
import de.tudresden.inf.lat.aboxrepair.ontologytools.RepairRequestNormalizer;
import de.tudresden.inf.lat.aboxrepair.reasoner.ReasonerFacade;
import de.tudresden.inf.lat.aboxrepair.repairrequest.RepairRequest;
import de.tudresden.inf.lat.aboxrepair.saturation.ABoxSaturator;
import de.tudresden.inf.lat.aboxrepair.saturation.CanonicalModelGenerator;
import de.tudresden.inf.lat.aboxrepair.saturation.ChaseGenerator;
import de.tudresden.inf.lat.aboxrepair.saturation.DummySaturator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.EnumSet;
import java.util.Set;

public class RepairManagerBuilder {
    private static Logger logger = LogManager.getLogger(RepairManagerBuilder.class);

    public enum RepairVariant {IQ, IQ2, CQ, CANONICAL_IQ, CANONICAL_CQ}

    // private final RepairVariant variant;

    public static EnumSet<RepairVariant> IQ_ANY = EnumSet.of(RepairVariant.IQ, RepairVariant.IQ2, RepairVariant.CANONICAL_IQ);
    public static EnumSet<RepairVariant> CQ_ANY = EnumSet.of(RepairVariant.CQ, RepairVariant.CANONICAL_CQ);
    public static EnumSet<RepairVariant> CANONICAL_ANY = EnumSet.of(RepairVariant.CANONICAL_IQ, RepairVariant.CANONICAL_CQ);

    private OWLOntology ontology;
    private OWLOntology workingCopy;
    private ReasonerFacade reasonerWithoutTBox;
    private ReasonerFacade reasonerWithTBox;
    private RepairGenerator repairGenerator;
    private ABoxSaturator saturator;
    private RepairRequest repairRequest;
    private RepairRequest normalizedRepairRequest;
    private boolean saturate = true;
    private RepairVariant variant;

    // region public methods
    public RepairManager build() throws OWLOntologyCreationException {
        this.workingCopy = OntologyPreparations.prepare(ontology, variant);

        try {
            workingCopy.getOWLOntologyManager().saveOntology(workingCopy, new FileOutputStream(new File("el-fragment.owl")));
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        this.normalizedRepairRequest = RepairRequestNormalizer.normalizeRepairRequest(ontology.getOWLOntologyManager(), repairRequest);
        initReasonerFacades();

        if (!RepairRequest.checkValid(normalizedRepairRequest, reasonerWithTBox)) {
            throw new IllegalArgumentException("Invalid repair request.");
        }

        fixSaturator();
        fixRepairGenerator();

        return new RepairManager(ontology, workingCopy, reasonerWithoutTBox, reasonerWithTBox, repairGenerator, saturator, normalizedRepairRequest);
    }

    // region setters
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
        return this;
    }
    // endregion

    // endregion

    // region private methods
    private void initReasonerFacades() throws OWLOntologyCreationException {
        long start = System.nanoTime();

        if (ontology == null || repairRequest == null)
            throw new IllegalArgumentException("Ontology and repair request have to be set first!");

        Set<OWLClassExpression> additionalExpressions = normalizedRepairRequest.getNestedClassExpressions();

        logger.info("Init reasoner facade without TBox");
        additionalExpressions.addAll(workingCopy.getNestedClassExpressions());
        reasonerWithoutTBox = ReasonerFacade.newReasonerFacadeWithoutTBox(additionalExpressions, ontology.getOWLOntologyManager());

        logger.info("Init reasoner facade with TBox");
        reasonerWithTBox = ReasonerFacade.newReasonerFacadeWithTBox(workingCopy, additionalExpressions);

        logger.info("Initialising reasoner facades took " + ((double) System.nanoTime() - start) / 1_000_000_000);
    }

    private void fixSaturator() {
        CycleChecker cycleChecker = new CycleChecker(reasonerWithTBox);

        if (cycleChecker.cyclic())
            throw new IllegalArgumentException("Ontology is cyclic - chase cannot be computed!");

        if (!saturate) {
            saturator = new DummySaturator();
        } else if (IQ_ANY.contains(variant)) {
            saturator = new CanonicalModelGenerator(reasonerWithTBox);
        } else {
            assert CQ_ANY.contains(variant);
            saturator = new ChaseGenerator();
        }
    }

    private void fixRepairGenerator() {
        if (CANONICAL_ANY.contains(variant)) {
            repairGenerator = new CanonicalRepairGenerator(workingCopy);
        } else if (variant.equals(RepairVariant.IQ)) {
            repairGenerator = new IQRepairGenerator(workingCopy);
        } else if (variant.equals(RepairVariant.IQ2)) {
            repairGenerator = new IQRepairGenerator2(workingCopy);
        } else {
            assert variant.equals(RepairVariant.CQ);
            repairGenerator = new CQRepairGenerator(workingCopy);
        }
        repairGenerator.setReasoner(reasonerWithTBox,reasonerWithoutTBox);
    }
    // endregion
}
