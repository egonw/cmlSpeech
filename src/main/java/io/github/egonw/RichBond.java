/**
 * @file   RichBond.java
 * @author Volker Sorge <sorge@zorkstone>
 * @date   Wed Jun 11 15:14:55 2014
 * 
 * @brief  Annotated Bond structure.
 * 
 * 
 */


//
package io.github.egonw;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;

/**
 *
 */

public class RichBond extends RichChemObject {

    RichBond(IBond structure) {
        super(structure);

        for (IAtom atom : structure.atoms()) {
            this.getComponents().add(atom.getID());
        }

    };

}
