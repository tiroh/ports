package org.timux.ports.types;

/**
 * A type representing missing information. When in use, <em>this class should always be instantiated</em>
 * so that the instanceof operator can be used. Null is not an admissible value for this type.
 *
 * @see Success
 * @see Failure
 * @see Nothing
 * @see Either
 * @see Either3
 *
 * @since 0.5.3
 */
public final class Unknown {

    public static final Unknown INSTANCE = new Unknown();

    private Unknown() {
        //
    }
}
