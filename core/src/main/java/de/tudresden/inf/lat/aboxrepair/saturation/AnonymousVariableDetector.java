package de.tudresden.inf.lat.aboxrepair.saturation;

import de.tudresden.inf.lat.aboxrepair.repairmanager.RepairManagerBuilder;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Deprecated
public abstract class AnonymousVariableDetector {

    public abstract boolean isAnonymous(OWLNamedIndividual individual);

    public static AnonymousVariableDetector newInstance(ABoxSaturator saturator) {
        if(saturator instanceof DummySaturator)
            return new AnonymousVariableDetector.DefaultVersion();

        if(saturator instanceof ChaseGenerator)
            return new AnonymousVariableDetector.CQVersion();

        assert saturator instanceof CanonicalModelGenerator;
        return new AnonymousVariableDetector.IQVersion();
    }

    public static AnonymousVariableDetector newInstance(boolean saturated, RepairManagerBuilder.RepairVariant variant) {
        if(!saturated)
            return new AnonymousVariableDetector.DefaultVersion();

        if(RepairManagerBuilder.CQ_ANY.contains(variant))
            return new AnonymousVariableDetector.CQVersion();

        assert RepairManagerBuilder.IQ_ANY.contains(variant);
        return new AnonymousVariableDetector.IQVersion();
    }

    /**
     * Use this for non-saturated ontologies.
     */
    private static class DefaultVersion extends AnonymousVariableDetector {
        @Override
        public boolean isAnonymous(OWLNamedIndividual individual) {
            return false;
        }
    }

    /**
     * Use this for CQ-saturated ontologies.
     */
    private static class CQVersion extends AnonymousVariableDetector {
        private Pattern pattern = Pattern.compile("[0-9_:]*(null)?[0-9_:]*");

        @Override
        public boolean isAnonymous(OWLNamedIndividual individual) {
            return pattern.matcher(individual.getIRI().toString()).matches();
        }
    }

    /**
     * Use this for IQ-saturated ontologies;
     */
    private static class IQVersion extends AnonymousVariableDetector {
        @Override
        public boolean isAnonymous(OWLNamedIndividual individual) {
            return individual.getIRI().toString().startsWith("__i__");
        }
    }

    public boolean isNamed(OWLNamedIndividual individual) {
        return !isAnonymous(individual);
    }

    public List<OWLNamedIndividual> getNamedIndividuals(OWLOntology ontology) {
        List<OWLNamedIndividual> result = new ArrayList<>();
        ontology.individualsInSignature().filter(this::isNamed).forEach(result::add);
        return result;
    }
}
