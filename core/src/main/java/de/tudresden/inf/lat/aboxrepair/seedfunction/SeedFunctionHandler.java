package de.tudresden.inf.lat.aboxrepair.seedfunction;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import de.tudresden.inf.lat.aboxrepair.reasoner.ReasonerFacade;
import de.tudresden.inf.lat.aboxrepair.repairrequest.RepairRequest;
import de.tudresden.inf.lat.aboxrepair.repairtype.RepairType;
import de.tudresden.inf.lat.aboxrepair.repairtype.RepairTypeHandler;
import de.tudresden.inf.lat.aboxrepair.saturation.AnonymousVariableDetector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SeedFunctionHandler {
    // TODO: make random choices determined by Random object

    // private Map<OWLNamedIndividual, RepairType> seedFunction;
    // private Map<OWLNamedIndividual, Set<OWLClassExpression>> hittingSetFunction;
    // private RepairRequest repairRequest;
    private OWLOntology ontology;
    private final RepairTypeHandler typeHandler;
    private final AnonymousVariableDetector detector;
    private final ReasonerFacade reasonerWithTBox;
    private final ReasonerFacade reasonerWithoutTBox;

    public SeedFunctionHandler(OWLOntology ontology,
                               // RepairRequest repairRequest,
                               // RepairManager.RepairVariant variant,
                               ReasonerFacade reasonerWithTBox,
                               ReasonerFacade reasonerWithoutTBox,
                               AnonymousVariableDetector detector) {
        // this.repairRequest = repairRequest;

        this.ontology = ontology;
        this.detector = detector;
        this.reasonerWithTBox = reasonerWithTBox;
        this.reasonerWithoutTBox = reasonerWithoutTBox;
        this.typeHandler = new RepairTypeHandler(reasonerWithTBox, reasonerWithoutTBox);
    }

    /*
     * The way we construct our seed function at the moment is still not optimal, in the sense that
     * the generated random seed function is not minimal yet w.r.t. covering relation.
     *
     * This would be challenging to find a good approach for directly constructing a minimal hitting set or
     * a minimal seed function so that the resulting repair can also be optimal
     */

    /**
     * Given a repair request, the method constructs a seed function that
     * maps each individual in the repair request to a repair type
     * <p>
     * Assumption: every individual in repairRequest is named
     *
     * @param repairRequest
     * @return
     */
    public SeedFunction computeRandomSeedFunction(RepairRequest repairRequest) {
        Map<OWLNamedIndividual, Set<OWLClassExpression>> hittingSetFunction = computeRandomHittingSet(repairRequest);

        SeedFunction seedFunction = new SeedFunction();
        Set<OWLNamedIndividual> setOfMappedIndividuals = hittingSetFunction.keySet();

        for(OWLNamedIndividual individual : setOfMappedIndividuals) {
            RepairType type = typeHandler.convertToRandomRepairType(hittingSetFunction.get(individual), individual);
            seedFunction.put(individual, type);
        }

        /*
         *  In the beginning, the iteration is needed in order to map individuals that are not contained in the repair request.
         *  And to map those individuals, we need an additional feature to distinguish whether the individuals in the
         *  saturated ontology is named or anonymous, so that the anonymous detector is used.
         *  But now we could easily employ the getter method that has been provided in the class SeedFunction.java.
         *  This implies that we can now remove the creation of setOfRemainingIndividuals and the below iteration.
         */
        Set<OWLNamedIndividual> setOfRemainingIndividuals = ontology.getIndividualsInSignature()
                .stream()
                .filter(ind -> !setOfMappedIndividuals.contains(ind) && detector.isNamed(ind))
                .collect(Collectors.toSet());

        for(OWLNamedIndividual individual : setOfRemainingIndividuals) {
            RepairType type = typeHandler.newMinimisedRepairType(new HashSet<>());
            seedFunction.put(individual, type);
        }

        return seedFunction;
    }

    /**
     * Given a repair request, for each individual in the request, the method
     * constructs a hitting set of the top-level conjuncts of concepts that are
     * asserted to the individual.
     *
     * @param repairRequest
     * @return
     */
    private Map<OWLNamedIndividual, Set<OWLClassExpression>> computeRandomHittingSet(RepairRequest repairRequest) {
        // TODO: use Multimap
        Map<OWLNamedIndividual, Set<OWLClassExpression>> hittingSetFunction = new HashMap<>();

        for(OWLNamedIndividual individual : repairRequest.individuals()) {
            for(OWLClassExpression concept : repairRequest.get(individual)) {
                if(reasonerWithTBox.instanceOf(individual, concept) ) {
                    if(concept instanceof OWLObjectIntersectionOf) {
                        OWLClassExpression atom = concept.asConjunctSet().stream()
                                .filter(con -> !reasonerWithTBox.equivalentToOWLThing(con))
                                .findAny().orElseThrow(() -> new IllegalArgumentException("Invalid Repair Request"));
                        concept = atom;
                    }

                    if(!hittingSetFunction.containsKey(individual))
                        hittingSetFunction.put(individual, new HashSet<>());

                    hittingSetFunction.get(individual).add(concept);
                }
            }
        }
        return hittingSetFunction;
    }
}
