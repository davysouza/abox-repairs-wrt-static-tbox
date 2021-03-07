package de.tu_dresden.inf.lat.abox_repairs.generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairTypeHandler;
import de.tu_dresden.inf.lat.abox_repairs.saturation.AnonymousVariableDetector;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;


import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;

abstract public class RepairGenerator {


    private static Logger logger = LogManager.getLogger(IQRepairGenerator.class);

    protected OWLOntology ontology;
    protected OWLDataFactory factory;
    protected IRI iri;
    protected Map<OWLIndividual, RepairType> objectToRepairType;
    protected Map<OWLIndividual, OWLIndividual> copyToObject;
    protected Map<OWLIndividual, Integer> individualCounter;
    protected Map<OWLIndividual, Set<OWLIndividual>> objectToCopies;
    protected Multimap<OWLIndividual, RepairType> objectToTypesWithCopies;

    /* I find the name strange, as it indicates that this is a set of saturated individuals.  Instead, it consists of
     *  all object names in the saturation.  I would thus rather call it 'setOfObjectNamesInTheSaturation'. */
    // the set of all individuals contained in the saturation
    protected Set<OWLIndividual> inputObjectNames;

    // the set of all individuals that are needed for constructing the repair
    protected Set<OWLIndividual> setOfCollectedIndividuals;


    protected ReasonerFacade reasonerWithTBox;
    protected ReasonerFacade reasonerWithoutTBox;

    protected RepairTypeHandler typeHandler;

    protected OWLOntology repair;

    @Deprecated
    protected AnonymousVariableDetector anonymousDetector;

    protected abstract void generateVariables();


    protected SeedFunction inputSeedFunction;


    /**
     * Assumption: inputOntology is saturated (depending on the underlying repair method)
     */
    public RepairGenerator(OWLOntology inputOntology) {
        //                       Map<OWLNamedIndividual, RepairType> inputSeedFunction) {


        this.ontology = inputOntology;
        this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();
    }

    protected void initialise(){
        assert inputSeedFunction!=null;

        this.objectToRepairType = new HashMap<>(inputSeedFunction);
        this.objectToTypesWithCopies = HashMultimap.create();
        for(Map.Entry<OWLNamedIndividual, RepairType> seedFunctionEntry : inputSeedFunction.entrySet()){
            objectToTypesWithCopies.put(seedFunctionEntry.getKey(), seedFunctionEntry.getValue());
        }

        this.inputObjectNames = new HashSet<>();
        this.inputObjectNames.addAll(ontology.getIndividualsInSignature());
        this.inputObjectNames.addAll(ontology.getAnonymousIndividuals());

        this.copyToObject = new HashMap<>();
        this.objectToCopies = new HashMap<>(); // multimap and inverse
        this.individualCounter = new HashMap<>();

        // Initializing originalToCopy, and vice versa
        for (OWLIndividual originalIndividual : inputObjectNames) {
            Set<OWLIndividual> initSet = new HashSet<OWLIndividual>();
            initSet.add(originalIndividual);
            objectToCopies.put(originalIndividual, initSet);
            // TODO is this really correct? shouldn't those individuals be also have a type assigned, or be
            // TODO in setOfCollectedIndividuals?

            individualCounter.put(originalIndividual, 0);
            copyToObject.put(originalIndividual, originalIndividual);
        }

        // TODO not needed
//        Optional<IRI> opt = ontology.getOntologyID().getOntologyIRI();
//        this.iri = opt.get();
    }

    public void setSeedFunction(SeedFunction inputSeedFunction){
        assert inputSeedFunction!=null;
        this.inputSeedFunction=inputSeedFunction;
    }

    public void setReasoner(ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox) {
        this.reasonerWithTBox = reasonerWithTBox;
        this.reasonerWithoutTBox = reasonerWithoutTBox;
        this.typeHandler = new RepairTypeHandler(reasonerWithTBox, reasonerWithoutTBox);
    }

    /**
     * Performs a stepwise rule to compute a repair
     *
     * @throws OWLOntologyCreationException
     */
    public final void repair() throws OWLOntologyCreationException {

        initialise();

        long startTimeVariables = System.nanoTime();

        generateVariables();

        double timeVariables = (double) (System.nanoTime() - startTimeVariables) / 1_000_000_000;

        logger.info("Time for generating variables: " + timeVariables);
        logger.info("Variables introduced: " + getNumberOfCollectedIndividuals());

        logger.debug("\nAfter generating necessary variables");
		/*for(OWLNamedIndividual ind : setOfCollectedIndividuals) {
			logger.debug("- " + ind);
			if(seedFunction.get(ind)!= null) {
				logger.debug(seedFunction.get(ind).getClassExpressions());
				logger.debug("");
			}

		}*/

        long startTimeMatrix = System.nanoTime();

        generateMatrix();

        double timeMatrix = (double) (System.nanoTime() - startTimeMatrix) / 1_000_000_000;

        logger.info("Time for generating Matrix: " + timeMatrix);

        logger.debug("\nAfter generating matrix");
        repair.axioms().forEach(ax -> logger.debug(ax));
    }

    /**
     * Generate the matrix of the repair.
     *
     * @throws OWLOntologyCreationException
     */
    protected void generateMatrix() throws OWLOntologyCreationException {

        reasonerWithTBox.cleanOntology(); // TODO: bad code design

        OWLOntologyManager man = OWLManager.createOWLOntologyManager();

        repair = man.createOntology();

//        repair.add(ontology.getTBoxAxioms(Imports.INCLUDED));

        for (OWLAxiom ax : ontology.getABoxAxioms(Imports.INCLUDED)) {

            if (ax instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom) ax;
                OWLIndividual originalInd = classAssertion.getIndividual();

                for (OWLIndividual copyInd : objectToCopies.get(originalInd)) {
                    assert !setOfCollectedIndividuals.contains(copyInd) || objectToRepairType.containsKey(copyInd);
                    assert !setOfCollectedIndividuals.contains(copyInd) || objectToRepairType.get(copyInd).getClassExpressions()!=null;

                    if (setOfCollectedIndividuals.contains(copyInd) && !objectToRepairType.get(copyInd).getClassExpressions()
                            .contains(classAssertion.getClassExpression())) {
                        OWLClassAssertionAxiom newAxiom = factory
                                .getOWLClassAssertionAxiom(classAssertion.getClassExpression(), copyInd);
                        repair.add(newAxiom);
                        logger.debug("New Class Assertion " + newAxiom);
                    }
                }
            } else if (ax instanceof OWLObjectPropertyAssertionAxiom) {
                OWLObjectPropertyAssertionAxiom roleAssertion = (OWLObjectPropertyAssertionAxiom) ax;
                OWLObjectProperty role = (OWLObjectProperty) roleAssertion.getProperty();
                OWLIndividual originalSubject = roleAssertion.getSubject();
                OWLIndividual originalObject = roleAssertion.getObject();

                for (OWLIndividual copySubject : objectToCopies.get(originalSubject)) {
                    for (OWLIndividual copyObject : objectToCopies.get(originalObject)) {
                        if(setOfCollectedIndividuals.contains(copySubject) && setOfCollectedIndividuals.contains(copyObject)) {
                            RepairType type1 = objectToRepairType.get(copySubject);
                            Set<OWLClassExpression> successorSet = computeSuccessorSet(
                                    type1, role, originalObject);
                            RepairType type2 = objectToRepairType.get(copyObject);
                            if (reasonerWithoutTBox.isCovered(successorSet, type2.getClassExpressions())) {
                                OWLObjectPropertyAssertionAxiom newAxiom = factory
                                        .getOWLObjectPropertyAssertionAxiom(role, copySubject, copyObject);
                                repair.add(newAxiom);

                                logger.debug("New Role Assertion " + newAxiom);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Take the set of all fillers of existential restrictions with the role name r
     * that occur in the repair type K such that
     * the individual u is an instance of each filler w.r.t. the saturation
     *
     * @param k type k
     * @param r name r
     * @param u u
     * @return
     */
    protected Set<OWLClassExpression> computeSuccessorSet(RepairType k, OWLObjectProperty r, OWLIndividual u) {
        Set<OWLClassExpression> set = new HashSet<>();
        for (OWLClassExpression concept : k.getClassExpressions()) {
            if (concept instanceof OWLObjectSomeValuesFrom &&
                    ((OWLObjectSomeValuesFrom) concept).getProperty().equals(r) &&
                    reasonerWithTBox.instanceOf(u, ((OWLObjectSomeValuesFrom) concept).getFiller())) {
                set.add(((OWLObjectSomeValuesFrom) concept).getFiller());
            }
        }

        return set;
    }

    /**
     * Make a copy of an individual with repair type K
     *
     * @param ind an individual name occurring in the input ontology
     * @param k the repair type to be assigned
     * @return the newly created individual
     */
    protected OWLAnonymousIndividual createCopy(OWLIndividual ind, RepairType k) {
        individualCounter.put(ind, individualCounter.get(ind) + 1);
//        OWLNamedIndividual freshIndividual = factory.getOWLNamedIndividual(
//                ind.getIRI().getFragment() +
//                        individualCounter.get(ind));
        OWLAnonymousIndividual freshIndividual = factory.getOWLAnonymousIndividual();

        /* With the paper in mind, it is misleading to call the below map 'seedFunction'.  A more suitable name would be
         *  'repairTypes' or 'repairTypeMapping' or similar. */
        objectToRepairType.put(freshIndividual, k);
        copyToObject.put(freshIndividual, ind);
        objectToCopies.get(ind).add(freshIndividual);
        objectToTypesWithCopies.put(ind, k);

        setOfCollectedIndividuals.add(freshIndividual);

        return freshIndividual;
    }


    public OWLOntology getRepair() {

        return repair;
    }

    public int getNumberOfCollectedIndividuals() {
        return setOfCollectedIndividuals.size();
    }
}
