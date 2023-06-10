package de.tudresden.inf.lat.aboxrepair.saturation;

import java.io.Serial;

/**
 * @author Patrick Koopmann
 */
public class SaturationException extends Exception {
    @Serial
    private static final long serialVersionUID = -4500987348944222545L;

    public SaturationException(String message, Throwable cause) {
        super(message, cause);
    }
}
