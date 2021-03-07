package de.tu_dresden.inf.lat.abox_repairs.tools;

public final class DisposableFinalVariable<T> {

    private T value;

    public DisposableFinalVariable(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void dispose() {
        value = null;
    }

}
