package de.tu_dresden.lat.abox_repairs;

import java.io.*;
import java.util.*;

import de.tu_dresden.lat.abox_repairs.ontology_tools.OntologyPreparations;
import de.tu_dresden.lat.abox_repairs.ontology_tools.RelevantSubOntologyExtractor;
import de.tu_dresden.lat.abox_repairs.saturation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import de.tu_dresden.lat.abox_repairs.generator.CQRepairGenerator;
import de.tu_dresden.lat.abox_repairs.generator.CanonicalRepairGenerator;
import de.tu_dresden.lat.abox_repairs.generator.IQRepairGenerator;
import de.tu_dresden.lat.abox_repairs.generator.RepairGenerator;
import de.tu_dresden.lat.abox_repairs.ontology_tools.CycleChecker;
import de.tu_dresden.lat.abox_repairs.ontology_tools.ELRestrictor;
import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;

import javax.print.attribute.standard.RequestingUserName;

/**
 * TODO: be able to handle the case where the given ontology is already saturated.
 */
public class Main {

	private static Logger logger = LogManager.getLogger(Main.class);

	/**
	 * Check: usually, static fields (variables) should be avoided where possible.
	 * In this case: check whether they are really needed to be outside the main method,
	 * and otherwise, add them. 
	 */
	private OWLOntology ontology;
	
	private Map<OWLNamedIndividual, RepairType> seedFunction;
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> repairRequest;
	
	private ReasonerFacade reasonerWithTBox, reasonerWithoutTBox;	

	public enum RepairVariant {IQ, CQ};

	private ABoxSaturator saturator;

	private final Random random;
	
	public static void main(String args[]) throws IOException, OWLOntologyCreationException, SaturationException {
		
		Main m = new Main();
		
		int i = 0;
		while(i < args.length) {
			// Initialize ontology
			// m.ontologyInitialisation(args, i);

			m.ontology =
					OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(args[i]));

			logger.debug("after loading ontology: ");



			File file = new File(args[i+1]);
			RepairRequestParser rrParser = new RepairRequestParser(m.ontology);
			RepairRequest repairRequest = rrParser.repairRequestScanning(file);


			RepairVariant variant;
		

			switch(args[i+2]){
				case "IQ": variant = RepairVariant.IQ; break;
				case "CQ": variant = RepairVariant.CQ; break;
				default:
					System.out.println("Unknown repairVariant: "+args[i+2]);
					System.exit(1);
					variant = RepairVariant.CQ;
			}
		
			if(args[i+3].equals("true")) 
				m.performRepair(m.ontology, repairRequest, variant, true);
			else
				m.performRepair(m.ontology, repairRequest, variant, false);

				i+=4;
				if(i < args.length) System.out.println("\n" + "=================================================");
//			}
		}		
	}

	public Main(){
		this.random = new Random();
	}

	public Main(Random random){
		this.random=random;
	}

	public void performRepair(OWLOntology inputOntology,
							  RepairRequest repairRequest,
							  RepairVariant repairVariant,
							  boolean saturationRequired
							  ) throws OWLOntologyCreationException, SaturationException {


		//OWLOntology module = new RelevantSubOntologyExtractor(inputOntology)
		//		.relevantSubOntology(repairRequest);

		//setOntology(module);
		setOntology(inputOntology);
		this.repairRequest=repairRequest;

		long startTime = System.nanoTime();

		int oldIndividuals = ontology.getIndividualsInSignature().size();
		long oldAssertions = ontology.aboxAxioms(Imports.INCLUDED).count();

		//boolean tboxExists = true;
		if (ontology.getTBoxAxioms(Imports.INCLUDED).isEmpty()) {
			//tboxExists = false;
			saturationRequired = false; // TODO bad style - should not reassign parameter variables!
		}
		//saturationRequirednoSaturation = !saturationRequired||!tboxExists;

		if(!saturationRequired)
			saturator = new DummySaturator();

		if (repairVariant.equals(RepairVariant.CQ) && saturationRequired) {
			cqSaturate();

		}

		// Initialize reasoner
		initReasonerFacade();


		logger.debug("after initializing reasoners: ");


		if(!RepairRequest.checkValid(repairRequest, reasonerWithTBox)) {
			throw new IllegalArgumentException("Invalid repair request.");
		}

		// Saturate the ontology
		if (repairVariant.equals(RepairVariant.IQ) && saturationRequired) {
			iqSaturate();
		}



		if (isCompliantWith(repairRequest)) {
			System.out.println("\nThe ontology is compliant!");
		} else {
			System.out.println("\nThe ontology is not compliant!");
			
//			if(repairAlternative.equals(RepairAlternative.Optimized)) {
				seedFunctionConstruction(repairRequest);
				
				Set<OWLNamedIndividual> setIndividuals = seedFunction.keySet();
				Iterator<OWLNamedIndividual> iteSetIndividuals = setIndividuals.iterator();
				logger.debug("\nSeed Function");
				while (iteSetIndividuals.hasNext()) {
					OWLNamedIndividual oni = iteSetIndividuals.next();
					logger.debug("- " + oni);
					RepairType type = seedFunction.get(oni);
					logger.debug(type.getClassExpressions());
					logger.debug("");
				}

				if (repairVariant.equals(RepairVariant.IQ)) {
					IQRepair();

				} else  {
					CQRepair();
				}
//			}
//			else {
//				seedFunction = new HashMap<>();
//				CanonicalRepair();
//			}
			
			cleanOntology(); // <-- this should not be done if the reasoner facades is still used!

			double timeRepairing = (double)(System.nanoTime() - startTime)/1_000_000_000;


			/**
			 * Please do not remove! This is not debugging output, but used for the evaluation of the experiments.
			 */
			System.out.print("#Individuals (orig/sat/repair): "
					+ oldIndividuals+"/"
					+ (oldIndividuals+saturator.addedIndividuals())+"/"+
					ontology.getIndividualsInSignature().size());
			System.out.print(" #Assertions (orig/sat/repair): "
					+ oldAssertions+"/"
					+ (oldAssertions+saturator.addedAssertions())+"/"+
					ontology.aboxAxioms(Imports.EXCLUDED).count());
			System.out.println(" Duration (sat/repair sec.): "+saturator.getDuration()+"/"+timeRepairing );

			initReasonerFacade();


			if (isCompliantWith(repairRequest)) {
				System.out.println("The ontology is now compliant");
			} else {
				System.out.println("The ontology is still not compliant");
			}
		}
	}


	public void setOntology(OWLOntology ontology) {
		this.ontology=ontology;
		OntologyPreparations.prepare(ontology);

		try {
			ontology.getOWLOntologyManager().saveOntology(ontology, new FileOutputStream(new File("el-fragment.owl")));
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

//		logger.debug("Ontology:");
//		ontology.axioms().forEach(ax -> logger.debug(ax));

	}
	


	private void initReasonerFacade() throws OWLOntologyCreationException {
		long start = System.nanoTime();

		List<OWLClassExpression> additionalExpressions = new LinkedList<>();

		for(Collection<OWLClassExpression> exps:repairRequest.values()){
			for(OWLClassExpression exp: exps){
				additionalExpressions.add(exp);
				additionalExpressions.addAll(exp.getNestedClassExpressions());
			}
		}

		logger.info("Init reasoner facade without TBox");
		additionalExpressions.addAll(ontology.getNestedClassExpressions());
		reasonerWithoutTBox = ReasonerFacade.newReasonerFacadeWithoutTBox(additionalExpressions, ontology.getOWLOntologyManager());


		logger.info("Init reasoner facade with TBox");
		reasonerWithTBox = ReasonerFacade.newReasonerFacadeWithTBox(ontology, additionalExpressions);


		logger.info("Initialising reasoner facades took "+((double)System.nanoTime()-start)/1_000_000_000);

	}


	private void cqSaturate() throws SaturationException {
		System.out.println("\nCQ-saturation");
		saturator = new ChaseGenerator();
		saturator.saturate(ontology);
	}
	
	private void iqSaturate() throws SaturationException {
		System.out.println("\nIQ-saturation");
		saturator = new CanonicalModelGenerator(reasonerWithTBox);
		saturator.saturate(ontology);
	}
	
	private void seedFunctionConstruction(RepairRequest inputRepairRequest) {
		long time = System.nanoTime();

		SeedFunctionHandler seedFunctionHandler = new SeedFunctionHandler(reasonerWithTBox, reasonerWithoutTBox);
		seedFunctionHandler.constructSeedFunction(inputRepairRequest);
		seedFunction = seedFunctionHandler.getSeedFunction(random);

		logger.info("Seed function construction took: "+(((double)System.nanoTime()-time)/1_000_000_000));
	}
	
	private boolean isCompliantWith(RepairRequest inputRepairRequest) {
		boolean compliant = true;

		for(OWLNamedIndividual individual : inputRepairRequest.keySet()) {

			for(OWLClassExpression concept : inputRepairRequest.get(individual)) {
				if(reasonerWithTBox.instanceOf(individual, concept)) {
					System.out.println("Not Compliant! " + individual + " " + concept);
					return false;
				}
			}
			
		}
		
		return compliant;
	}

	private void CQRepair() throws OWLOntologyCreationException {
		RepairGenerator generator = new CQRepairGenerator(ontology, seedFunction);
		generator.setReasoner(reasonerWithTBox, reasonerWithoutTBox);
		generator.repair();
		ontology = generator.getRepair();
	}
	
	private void IQRepair() throws OWLOntologyCreationException {
		RepairGenerator generator = new IQRepairGenerator(ontology, seedFunction);
		generator.setReasoner(reasonerWithTBox, reasonerWithoutTBox);
		generator.repair();
		ontology = generator.getRepair();
	}
	
	private void CanonicalRepair() throws OWLOntologyCreationException {
		RepairGenerator generator = new CanonicalRepairGenerator(ontology, seedFunction);
		generator.setReasoner(reasonerWithTBox, reasonerWithoutTBox);
		generator.repair();
		
	}
	
	private void cleanOntology() {
		reasonerWithTBox.cleanOntology();
	}
}
