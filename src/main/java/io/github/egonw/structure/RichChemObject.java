/**
 * @file   RichChemObject.java
 * @author Volker Sorge <sorge@zorkstone>
 * @date   Wed Jun 11 15:14:55 2014
 * 
 * @brief  Annotated ChemObject structure.
 * 
 * 
 */

//
package io.github.egonw.structure;

import org.openscience.cdk.interfaces.IChemObject;

/**
 * Chemical objects with admin information.
 */

public class RichChemObject extends AbstractRichStructure<IChemObject> implements RichStructure<IChemObject> {
    

    RichChemObject(IChemObject structure) {
        super(structure);
    };


    @Override
    public String getId() {
        return this.structure.getID();
    }
    
}
