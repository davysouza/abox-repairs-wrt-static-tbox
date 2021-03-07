package de.tu_dresden.inf.lat.abox_repairs.ontology_tools;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * Used to check whether an ontology is fully supported, i.e., does not contain language constructs which cannot be
 * handled by our repair approach and its implementation.  Specifically, we currently allow for, possibly after a
 * transformation, instances of the following classes:
 * <ul>
 *   <li>Individual names:          {@link org.semanticweb.owlapi.model.OWLNamedIndividual}</li>
 *   <li>Variables:                 {@link org.semanticweb.owlapi.model.OWLAnonymousIndividual} (with transformation)</li>
 *   <li>Concept names:             {@link org.semanticweb.owlapi.model.OWLClass} (which includes the top concept {@link uk.ac.manchester.cs.owl.owlapi.InternalizedEntities#OWL_THING}, but not the bottom concept {@link uk.ac.manchester.cs.owl.owlapi.InternalizedEntities#OWL_NOTHING})</li>
 *   <li>Role names:                {@link org.semanticweb.owlapi.model.OWLObjectProperty} (but neither the top role {@link uk.ac.manchester.cs.owl.owlapi.InternalizedEntities#OWL_TOP_OBJECT_PROPERTY} nor the bottom role {@link uk.ac.manchester.cs.owl.owlapi.InternalizedEntities#OWL_BOTTOM_OBJECT_PROPERTY})</li>
 * </ul>
 * <ul>
 *   <li>Conjunctions:              {@link org.semanticweb.owlapi.model.OWLObjectIntersectionOf}</li>
 *   <li>Existential restrictions:  {@link org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom}</li>
 *   <li>At-least restrictions:     {@link org.semanticweb.owlapi.model.OWLObjectMinCardinality} (with transformation, only for cardinality 0 or 1)</li>
 * </ul>
 * <ul>
 *   <li>Class assertions:          {@link org.semanticweb.owlapi.model.OWLClassAssertionAxiom}</li>
 *   <li>Role assertions:           {@link org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom}</li>
 *   <li>Concept inclusions:        {@link org.semanticweb.owlapi.model.OWLSubClassOfAxiom}</li>
 *   <li>Concept equivalences:      {@link org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom}</li>
 *   <li>Domain restrictions:       {@link org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom} (with transformation)</li>
 * </ul>
 * <br>
 * In a future version, we will be able to additionally allow for the following:
 * <ul>
 *   <li>Bottom concept:            {@link uk.ac.manchester.cs.owl.owlapi.InternalizedEntities#OWL_NOTHING}</li>
 *   <li>Nominals:                  {@link org.semanticweb.owlapi.model.OWLObjectOneOf} (but with only one individual)</li>
 *   <li>Self restrictions:         {@link org.semanticweb.owlapi.model.OWLObjectHasSelf}</li>
 *   <li>Inverse roles:             {@link org.semanticweb.owlapi.model.OWLObjectInverseOf}</li>
 *   <li>Role conjunction           (currently not supported by the OWL API)</li>
 *   <li>Concept disjointness:      {@link org.semanticweb.owlapi.model.OWLDisjointClassesAxiom} (with transformation)</li>
 *   <li>Range restrictions:        {@link org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom}</li>
 *   <li>Role inclusions:           {@link org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom}</li>
 *   <li>Role equivalences:         {@link org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom}</li>
 * </ul>
 * Note that we then specifically support, after transformation, Horn-𝒜ℒ𝒞 TBoxes which allow for limited use of the following only in concept inclusions:
 * <ul>
 *   <li>Value restrictions:        {@link org.semanticweb.owlapi.model.OWLObjectAllValuesFrom}</li>
 *   <li>Negation:                  {@link org.semanticweb.owlapi.model.OWLObjectComplementOf}</li>
 *   <li>Disjunction:               {@link org.semanticweb.owlapi.model.OWLObjectUnionOf}</li>
 * </ul>
 * <br>
 * By a suitable copying approach, we will later also be able to support annotations and annotation axioms:
 * <ul>
 *   <li>Annotation:                {@link org.semanticweb.owlapi.model.OWLAnnotation}</li>
 *   <li>Annotation properties:     {@link org.semanticweb.owlapi.model.OWLAnnotationProperty}</li>
 *   <li>Annotation values:         {@link org.semanticweb.owlapi.model.OWLAnnotationValue}</li>
 *   <li>Annotation axioms:         {@link org.semanticweb.owlapi.model.OWLAnnotationAxiom}</li>
 *   <li>and the various other classes used for representing annotations and annotation axioms</li>
 * </ul>
 * <br>
 */
public final class ExpressivityChecker implements OWLObjectVisitor {

    private boolean isSupported = true;
    private boolean needsTransformation = false;

    public ExpressivityChecker() {
        // do nothing
    }

    public final boolean isSupported() {
        return isSupported;
    }

    public final boolean needsTransformation() {
        return needsTransformation;
    }

    @Override
    public void doDefault(Object object) {
        isSupported = false;
    }

    @Override
    public void visit(OWLOntology ontology) {
        ontology.axioms(Imports.INCLUDED).forEach(axiom -> axiom.accept(this));
        ontology.annotations().forEach(annotation -> annotation.accept(this));
    }

    @Override
    public void visit(OWLDeclarationAxiom axiom) {
        axiom.getEntity().accept(this);
    }

    @Override
    public void visit(OWLClassAssertionAxiom axiom) {
        if (!isSupported)
            return;
        axiom.getIndividual().accept(this);
        axiom.getClassExpression().accept(this);
        axiom.annotations().forEach(annotation -> annotation.accept(this));
    }

    @Override
    public void visit(OWLObjectPropertyAssertionAxiom axiom) {
        if (!isSupported)
            return;
        axiom.getSubject().accept(this);
        axiom.getProperty().accept(this);
        axiom.getObject().accept(this);
        axiom.annotations().forEach(annotation -> annotation.accept(this));
    }

    @Override
    public void visit(OWLSubClassOfAxiom axiom) {
        if (!isSupported)
            return;
        axiom.getSubClass().accept(this);
        axiom.getSuperClass().accept(this);
        axiom.annotations().forEach(annotation -> annotation.accept(this));
    }

    @Override
    public void visit(OWLEquivalentClassesAxiom axiom) {
        if (!isSupported)
            return;
        axiom.classExpressions().forEach(ce -> ce.accept(this));
        axiom.annotations().forEach(annotation -> annotation.accept(this));
    }

    @Override
    public void visit(OWLObjectPropertyDomainAxiom axiom) {
        if (!isSupported)
            return;
        axiom.getProperty().accept(this);
        axiom.getDomain().accept(this);
        axiom.annotations().forEach(annotation -> annotation.accept(this));
        needsTransformation = true;
    }

    @Override
    public void visit(OWLNamedIndividual individual) {
        // is supported
    }

    @Override
    public void visit(OWLAnonymousIndividual individual) {
        // is supported
    }

    @Override
    public void visit(OWLClass ce) {
        if (!isSupported)
            return;
        if (ce.isOWLNothing())
            isSupported = false;
    }

    @Override
    public void visit(OWLObjectProperty property) {
        if (!isSupported)
            return;
        if (property.isOWLTopObjectProperty() || property.isOWLBottomObjectProperty())
            isSupported = false;
    }

    @Override
    public void visit(OWLObjectIntersectionOf ce) {
        if (!isSupported)
            return;
        ce.operands().forEach(operand -> operand.accept(this));
    }

    @Override
    public void visit(OWLObjectSomeValuesFrom ce) {
        if (!isSupported)
            return;
        ce.getProperty().accept(this);
        ce.getFiller().accept(this);
    }

    @Override
    public void visit(OWLObjectMinCardinality ce) {
        if (!isSupported)
            return;
        if (ce.getCardinality() > 1) {
            isSupported = false;
            return;
        }
        ce.getProperty().accept(this);
        ce.getFiller().accept(this);
        needsTransformation = true;
    }

}
