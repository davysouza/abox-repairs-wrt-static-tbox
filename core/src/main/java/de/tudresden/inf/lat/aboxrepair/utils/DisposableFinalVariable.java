package de.tudresden.inf.lat.aboxrepair.utils;

import javax.annotation.Nonnull;
import java.util.Objects;

public final class DisposableFinalVariable<T> {
    @Nonnull
    private T value;

    public DisposableFinalVariable(@Nonnull T value) throws IllegalArgumentException {
        if (value == null)
            throw new IllegalArgumentException("The value of an instance of DisposableFinalVariable cannot be null.");
        this.value = value;
    }

    public final T get() throws IllegalStateException {
        if (value == null)
            throw new IllegalStateException("This instance of DisposableFinalVariable was already disposed.");
        return value;
    }

    public final void dispose() {
        value = null;
    }

    public final boolean isDisposed() {
        return value == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        DisposableFinalVariable<?> that = (DisposableFinalVariable<?>) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "DisposableFinalVariable{ " + value + " }";
    }
}
