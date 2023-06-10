package de.tudresden.inf.lat.aboxrepair.ontologytools;

import de.tudresden.inf.lat.aboxrepair.repairmanager.RepairManagerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * Prepare ontology to be adherent to preconditions in the CADE-21 paper:
 * <p>
 * 1. TBox is pure EL, 2. ABox is flat
 */
public class OntologyPreparations {
    private static Logger logger = LogManager.getLogger(OntologyPreparations.class);

    /**
     * Check whether the ontology expressiveness is supported or not and normalize the ontology according to the
     * specified repair variant.
     * @param ontology {@link OWLOntology} object
     * @param repairVariant {@link RepairManagerBuilder.RepairVariant} object
     * @return Returns the normalized {@link OWLOntology} object
     */
    public static OWLOntology prepare(OWLOntology ontology, RepairManagerBuilder.RepairVariant repairVariant) {
        return prepare(ontology, repairVariant, false);
    }

    /**
     * If <b>isExperiment</b> is false, check whether the ontology expressiveness is supported or not and normalize the
     * ontology according to the specified repair variant. If <b>isExperiment</b> is true, it restricts the ontology by
     * using the {@link ELRestrictor}, flattens it with the {@link ABoxFlattener} and then remove unused names.
     * @param ontology {@link OWLOntology} object
     * @param repairVariant {@link RepairManagerBuilder.RepairVariant} object
     * @param isExperiment A boolean value to says whether we are running an experiment or not
     * @return Depending on <b>isExperiment</b> value, returns an prepared {@link OWLOntology} object.
     */
    public static OWLOntology prepare(OWLOntology ontology, RepairManagerBuilder.RepairVariant repairVariant, boolean isExperiment) {

        if (isExperiment) {
            /* For the experiments it is totally fine to remove unsupported language constructs.  However, to make the
             * implementation robust for usage in practice, we must rather abort handling of ontologies that contain
             * unsupported constructs. */
            OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
            new ELRestrictor(factory).restrict(ontology);
            new ABoxFlattener(factory).flatten(ontology);

            // restrict signature to used names
            // this way, we avoid names that are not used in any axiom to occur in a repair type or somewhere else
            ontology.removeAxioms(ontology.getAxioms(AxiomType.DECLARATION));

            return ontology;
        }

        /** The below check does not produce the expected result, as the OWL API assumes that the description logic EL
         * allows for domain and range restrictions, see {@link org.semanticweb.owlapi.util.Languages.EL}. */
        // new DLExpressivityChecker(Collections.singleton(ontology)).isWithin(Languages.EL);

        /** We need to check expressivity as follows. */
        final ExpressivityChecker expressivityChecker = new ExpressivityChecker();
        ontology.accept(expressivityChecker);

        if (!expressivityChecker.isSupported()) {
            if (isExperiment) {
                logger.error("The expressivity of " + ontology.getOntologyID() + " is not supported.");
            } else {
                throw new IllegalArgumentException("The expressivity of " + ontology.getOntologyID() + " is not supported.");
            }
        }

        if (expressivityChecker.needsTransformation())
            logger.info("The ontology " + ontology.getOntologyID() + " must be transformed into a supported format.");

        /** Next, we will normalize the ontology. */
        try {
            final OntologyNormalizer.Entailment entailment =
                    RepairManagerBuilder.CQ_ANY.contains(repairVariant)
                        ? OntologyNormalizer.Entailment.CQ
                        : OntologyNormalizer.Entailment.IQ;

            final OWLOntology normalization = new OntologyNormalizer(ontology, entailment).getNormalization(false);
            return normalization;
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
    }
}
