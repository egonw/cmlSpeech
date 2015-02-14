/**
 * @file   SreException.java
 * @author Volker Sorge <sorge@zorkstone>
 * @date   Sun May  4 14:48:23 2014
 * 
 * @brief  Exception for Sre classes.
 * 
 * 
 */

//
package io.github.egonw.sre;


/**
 * Exception for the Sre classes.
 */

public class SreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SreException(String message) {
	super(message);
    }

    public SreException(String message, Throwable throwable) {
	super(message, throwable);
    }

}

