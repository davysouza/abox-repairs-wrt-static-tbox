package de.tu_dresden.inf.lat.abox_repairs.ontology_tools;

import de.tu_dresden.inf.lat.abox_repairs.ontology_tools.OntologyNormalizer.Entailment;
import de.tu_dresden.inf.lat.abox_repairs.repair_manager.RepairManagerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * Prepare ontology to be adhere to preconditions in the CADE-21 paper:
 * <p>
 * 1. TBox is pure EL, 2. ABox is flat
 */
public class OntologyPreparations {

    private static Logger logger = LogManager.getLogger(OntologyPreparations.class);

    public static OWLOntology prepare(OWLOntology ontology, boolean isExperiment, RepairManagerBuilder.RepairVariant repairVariant) {

//        if (isExperiment) {
//
//            /* For the experiments it is totally fine to remove unsupported language constructs.  However, to make the
//             * implementation robust for usage in practise, we must rather abort handling of ontologies that contain
//             * unsupported constructs. */
//            OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
//            new ELRestrictor(factory).restrict(ontology);
//            new ABoxFlattener(factory).flatten(ontology);
//
//            // restrict signature to used names
//            // this way, we avoid names that are not used in any axiom to occur in a repair type or somewhere else
//            ontology.removeAxioms(ontology.getAxioms(AxiomType.DECLARATION));
//
//            return ontology;
//
//        } else {

            /** The below check does not produce the expected result, as the OWL API assumes that the description logic EL
             * allows for domain and range restrictions, see {@link org.semanticweb.owlapi.util.Languages.EL}. */
            // new DLExpressivityChecker(Collections.singleton(ontology)).isWithin(Languages.EL);

            /** We need to check expressivity as follows. */
            final ExpressivityChecker expressivityChecker = new ExpressivityChecker();
            ontology.accept(expressivityChecker);
            if (!expressivityChecker.isSupported()) {
                if (isExperiment)
                    logger.error("The expressivity of " + ontology.getOntologyID() + " is not supported.");
                else
                    throw new IllegalArgumentException("The expressivity of " + ontology.getOntologyID() + " is not supported.");
            }
            if (expressivityChecker.needsTransformation())
                logger.info("The ontology " + ontology.getOntologyID() + " must be transformed into a supported format.");

            /** Next, we will normalize the ontology. */
            try {
                final Entailment entailment = RepairManagerBuilder.CQ_ANY.contains(repairVariant) ? Entailment.CQ : Entailment.IQ;
                final OWLOntology normalization = new OntologyNormalizer(ontology, entailment).getNormalization(isExperiment);
                return normalization;
            } catch (OWLOntologyCreationException e) {
                throw new RuntimeException(e);
            }

//        }
    }

}