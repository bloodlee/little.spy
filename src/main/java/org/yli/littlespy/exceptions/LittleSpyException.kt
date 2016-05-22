package org.yli.littlespy.exceptions

/**
 * Exception of {@link LittleSpyException}
 *
 * Created by yli on 5/21/2016.
 */
class LittleSpyException: Throwable {

    /**
     * Constructor.
     *
     * @param msg the exception message.
     */
    constructor(msg : String) : super(msg, null) {

    }

    /**
     * Constructor.
     *
     * @param msg the exception message
     * @param cause the cause.
     */
    constructor(msg : String, cause : Throwable) : super(msg, cause) {

    }

}