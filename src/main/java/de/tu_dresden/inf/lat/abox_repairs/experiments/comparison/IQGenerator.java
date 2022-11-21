package de.tu_dresden.inf.lat.abox_repairs.experiments.comparison;

import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.inf.lat.abox_repairs.saturation.CanonicalModelGenerator;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generating IQs that, for a given ontology, give at least one answer
 */
public class IQGenerator {
    private final OWLOntology ontology;
    private final Random random;
    private final OWLDataFactory dataFactory;

    private double probabilityExists=0.5;
    private double probabilityConjunction=0.5;
    private int minDepth = 0;
    private int maxDepth = Integer.MAX_VALUE;

    /**
     * Careful: changes the ontology!
     */
    public IQGenerator(OWLOntology ontology) {
        this(ontology, new Random());
    }

    /**
     * Careful: changes the ontology!
     */
    public IQGenerator(OWLOntology ontology, Random random) {
        this.ontology=ontology;
        this.dataFactory=ontology.getOWLOntologyManager().getOWLDataFactory();
        this.random=random;

        // create the IQ-saturation
        new CanonicalModelGenerator(ReasonerFacade.newReasonerFacadeWithTBox(ontology)).saturate(ontology);
    }

    public OWLClassExpression generateIQ() throws IQGenerationException {
        List<OWLNamedIndividual> inds = ontology.individualsInSignature(Imports.INCLUDED)
                .collect(Collectors.toList());

        if(inds.isEmpty())
            throw new IQGenerationException("Ontology contains no individuals!");

        while(!inds.isEmpty()){
            OWLNamedIndividual root = takeRandom(inds);
            Optional<OWLClassExpression> opt = generateIQ(root,0);
            if(opt.isPresent())
                return opt.get();
        }

        throw new IQGenerationException("No IQ under the constraints could be constructed! (minDepth: "+minDepth+", maxDepth: "+maxDepth+")");

    }

    private Optional<OWLClassExpression> generateIQ(OWLIndividual ind, int currentDepth) {
        boolean hasSuccessors = ontology.objectPropertyAssertionAxioms(ind)
                .findAny()
                .isPresent();

        System.out.println("ind: "+ind);
        System.out.println("has successors: "+hasSuccessors);

        if(!hasSuccessors && currentDepth<minDepth) {
            System.out.println("cannot continue this path");
            return Optional.empty();
        }

        boolean chooseExists = hasSuccessors &&
                (currentDepth < minDepth
                        || (currentDepth < maxDepth && random.nextDouble() < probabilityExists ));

        System.out.println("Choose exists: "+chooseExists);

        OWLClassExpression result = null; //

        if(!chooseExists){

            List<OWLClass> names = ontology.classAssertionAxioms(ind)
                    .map(x -> x.getClassExpression())
                    .filter(x -> x instanceof OWLClass)
                    .map(x -> (OWLClass) x)
                    .collect(Collectors.toList());

            System.out.println("names: "+names);

            if(names.isEmpty())
                return Optional.of(dataFactory.getOWLThing());
            else
                result = takeRandom(names);

            assert result!=null;
        } else {
            // chooseExists
            List<OWLObjectPropertyAssertionAxiom> successors =
                    ontology.objectPropertyAssertionAxioms(ind)
                            .collect(Collectors.toList());

            boolean done = false;

            while(!done && !successors.isEmpty()) {
                OWLObjectPropertyAssertionAxiom ra = takeRandom(successors);
                Optional<OWLClassExpression> filler = generateIQ(ra.getObject(), currentDepth+1);
                if(filler.isPresent()){
                    result = dataFactory.getOWLObjectSomeValuesFrom(
                            ra.getProperty(),
                            filler.get());
                    done = true;
                }
            }

            if(!done && successors.isEmpty()) {
                System.out.println("no successor of "+ind+" was successful!");
                return Optional.empty();
            }
            assert result!=null;
        }
        assert result!=null;


        if(random.nextDouble()<probabilityConjunction){
            System.out.println("adding a conjunct!");
            System.out.println("Until here: "+result);
            OWLClassExpression conjunct = generateIQ(ind,currentDepth).get(); // we can assume this to be successful if we got that far
            return Optional.of(
                    dataFactory.getOWLObjectIntersectionOf(result,conjunct));
        } else
            return Optional.of(result);
    }


    private <T> T takeRandom(List<T> list) {
        return list.remove(
                random.nextInt(
                        list.size()));
    }

    public double getProbabilityExists() {
        return probabilityExists;
    }

    public void setProbabilityExists(double probabilityExists) {
        this.probabilityExists = probabilityExists;
    }

    public double getProbabilityConjunction() {
        return probabilityConjunction;
    }

    public void setProbabilityConjunction(double probabilityConjunction) {
        this.probabilityConjunction = probabilityConjunction;
    }

    public int getMinDepth() {
        return minDepth;
    }

    public void setMinDepth(int minDepth) {
        this.minDepth = minDepth;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }


}
