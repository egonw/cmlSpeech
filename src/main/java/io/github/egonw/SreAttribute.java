
//
package io.github.egonw;

import nu.xom.Attribute;
import nu.xom.Element;

/**
 *
 */

public class SreAttribute extends Attribute {
 
    SreAttribute(String localName, String value) {
        super(SreNamespace.getInstance().prefix + ":" + localName,
              SreNamespace.getInstance().uri, value);
    }

    SreAttribute(SreNamespace.Attribute attr, String value) {
        super(SreNamespace.getInstance().prefix + ":" + attr.attribute,
              SreNamespace.getInstance().uri, value);
    }

    public void addValue(String value) {
        if (getValue() == "") {
            setValue(value);
        } else {
            setValue(getValue() + " " + value);
        }
    }

    public void addValue(Element node) {
        String localName = getLocalName();
        String namespace = getNamespaceURI();
        SreAttribute oldAttr = (SreAttribute)node.getAttribute(localName, namespace);
        if (oldAttr == null) {
            node.addAttribute(this);
        } else {
            oldAttr.setValue(oldAttr.getValue() + " " + getValue());
        }
    }

}
