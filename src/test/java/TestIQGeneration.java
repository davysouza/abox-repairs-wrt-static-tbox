import com.google.common.annotations.VisibleForTesting;
import de.tu_dresden.inf.lat.abox_repairs.experiments.comparison.IQGenerationException;
import de.tu_dresden.inf.lat.abox_repairs.experiments.comparison.IQGenerator;
import de.tu_dresden.inf.lat.abox_repairs.virtual_iq_repairs.FullOntologyIQView;
import de.tu_dresden.inf.lat.abox_repairs.virtual_iq_repairs.IQBlackbox;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestIQGeneration {
    @Test
    public void testIQGeneration() throws OWLOntologyCreationException, IQGenerationException {
        OWLOntology ontology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(
                        this.getClass()
                                .getResourceAsStream("AnotherExample.owl"));

        IQBlackbox iqBlackbox = new FullOntologyIQView(ontology);

        ontology.axioms().forEach(System.out::println);

        for(int i=0; i<3; i++){
            IQGenerator iqGenerator = new IQGenerator(ontology);
            OWLClassExpression iq = iqGenerator.generateIQ();

            System.out.println();
            System.out.println("QUERY: "+iq);

            Collection<OWLNamedIndividual> answers = iqBlackbox.query(iq);


            System.out.println("ANSWERS: "+String.join(", ",answers.stream().map(x -> x.toString()).collect(Collectors.toList())));

            assertFalse(answers.isEmpty());
            System.out.println();
        }
    }
}
