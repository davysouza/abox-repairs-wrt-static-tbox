package de.tudresden.inf.lat.aboxrepair.generator;

import com.google.common.collect.Sets;
import de.tudresden.inf.lat.aboxrepair.repairtype.RepairType;
import fr.pixelprose.count.generator.PowerSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Careful: this does not compute the canonical repair, but only the variables that would be needed by it.
 */
public class CanonicalRepairGenerator extends RepairGenerator {
    private static Logger logger = LogManager.getLogger(CanonicalRepairGenerator.class);
    private final Set<Pair<OWLIndividual, RepairType>> individualsInTheRepair = Sets.newConcurrentHashSet();

    public CanonicalRepairGenerator(OWLOntology inputOntology) {
        super(inputOntology);
        setOfCollectedIndividuals = new HashSet<>();
    }

    private boolean atomForRepairType(OWLClassExpression exp) {
        /* As you want to test whether 'exp' is an atom, you must replace the condition
         *  !reasonerWithTBox.equivalentToOWLThing(exp)
         *  with !reasonerWithoutTBox.equivalentToOWLThing(exp) */
        return (exp instanceof OWLClass || exp instanceof OWLObjectSomeValuesFrom)
                && !reasonerWithoutTBox.equivalentToOWLThing(exp);
    }

    // region Override methods
    @Override
    public int getNumberOfCollectedIndividuals() {
        return individualsInTheRepair.size();
    }

    @Override
    protected void generateVariables() {
        for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
            Set<OWLClassExpression> setOfAtoms = new HashSet<>();
            for (OWLClassExpression assertedConcept : reasonerWithTBox.instanceOfExcludingOWLThing(individual)) {
                if (atomForRepairType(assertedConcept)) {
                    setOfAtoms.add(assertedConcept);
                }
            }

			/*Set<Set<OWLClassExpression>> powerSet = Sets.powerSet(setOfAtoms);
			for(Set<OWLClassExpression> repairTypeCandidate: powerSet) {
				if(typeHandler.isPremiseSaturated(repairTypeCandidate, individual)) {
					RepairType type = typeHandler.newMinimisedRepairType(repairTypeCandidate);
					createCopy(individual, type);
				}
			}*/

            final Iterable<Iterable<OWLClassExpression>> powerSet = new PowerSet(setOfAtoms, 0, setOfAtoms.size());
            StreamSupport.stream(powerSet.spliterator(), true).forEach(repairTypeCandidate -> {
//            for (Iterable<OWLClassExpression> repairTypeCandidate : powerSet) {
                final Set<OWLClassExpression> _repairTypeCandidate = Sets.newHashSet(repairTypeCandidate);
                if (typeHandler.isPremiseSaturated(_repairTypeCandidate, individual)) {
                    RepairType type = typeHandler.newMinimisedRepairType(_repairTypeCandidate);
                    /*createCopy(individual, type);*/
                    individualsInTheRepair.add(Pair.of(individual, type));
                }
//            }
            });
        }
    }

    @Override
    protected void initialise() {
        // Do nothing.
    }

    @Override
    protected void generateMatrix() throws OWLOntologyCreationException {
        // Do nothing.
    }
    // endregion
}
