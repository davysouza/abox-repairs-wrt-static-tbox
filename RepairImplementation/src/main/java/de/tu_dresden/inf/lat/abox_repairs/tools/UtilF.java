package de.tu_dresden.inf.lat.abox_repairs.tools;

import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class UtilF {

    private UtilF(){
        // utilities class
    }

    public static boolean isAtom(OWLClassExpression owlClassExpression) {
        return !owlClassExpression.isOWLThing() && owlClassExpression.asConjunctSet().size() == 1;
    }

    public static OWLClassExpression toAtom(OWLClassExpression owlClassExpression) {
        if (!isAtom(owlClassExpression))
            throw new IllegalArgumentException();
        return owlClassExpression.asConjunctSet().iterator().next();
    }

    public static <T> Set<T> newHashSet(Set<T> set, T element) {
        final Set<T> result = new HashSet<>(set);
        result.add(element);
        return result;
    }

    public static <K, V> Map<K, V> newHashMap(Map<K, V> map, K key, V value) {
        final Map<K, V> result = new HashMap<>(map);
        result.put(key, value);
        return result;
    }

    public static final <T> Set<T> getMaximalElements(Set<T> set, Function<T, T> transformer, BiPredicate<T, T> partialOrder) {
        final Set<T> maximalElements = new HashSet<>();
        for (T element : set) {
            final T transformedElement = transformer.apply(element);
            if (maximalElements.stream().noneMatch(otherElement -> partialOrder.test(transformedElement, otherElement))) {
                maximalElements.removeIf(otherElement -> partialOrder.test(otherElement, transformedElement));
                maximalElements.add(transformedElement);
            }
        }
        return maximalElements;
    }

    public static final <T> Set<T> getMinimalElements(Set<T> set, Function<T, T> transformer, BiPredicate<T, T> partialOrder) {
        return getMaximalElements(set, transformer, (x, y) -> partialOrder.test(y, x));
    }

}
