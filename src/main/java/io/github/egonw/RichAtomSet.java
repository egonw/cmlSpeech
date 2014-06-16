
//
package io.github.egonw;

import org.xmlcml.cml.element.CMLAtomSet;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.AtomContainer;
import java.util.List;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import java.util.SortedSet;
import java.util.TreeSet;
import nu.xom.Document;
import nu.xom.Element;
import org.xmlcml.cml.element.CMLAtom;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

/**
 *
 */

public class RichAtomSet extends RichChemObject {
    
    public enum Type {
        ALIPHATIC ("Aliphatic chain"),
        FUSED ("Fused ring"),
        ISOLATED ("Isolated ring"),
        SMALLEST ("Subring");

        protected final String name;

        private Type (String name) {
            this.name = name;
        }
    }

    public Type type;
    public CMLAtomSet cml;

    private SortedSet<String> sup = new TreeSet<String>(new CMLNameComparator());
    private SortedSet<String> sub = new TreeSet<String>(new CMLNameComparator());
    private SortedSet<String> connectingAtoms = new TreeSet<String>(new CMLNameComparator());
    public BiMap<Integer, String> elementPositions = HashBiMap.create();

    // To remove!
    public SortedSet<IAtom> atomConnections = new TreeSet<IAtom>();
    public SortedSet<IAtom> setConnections = new TreeSet<IAtom>();


    private RichAtomSet (IAtomContainer container) {
        super(container);
    }

    private RichAtomSet (IAtomContainer container, Type type) {
        super(container);
        this.type = type;
    }

    public RichAtomSet (IAtomContainer container, Type type, String id) {
        super(container);
        this.getStructure().setID(id);
        this.type = type;
        for (IAtom atom : this.getStructure().atoms()) {
            this.getComponents().add(atom.getID());
        }
        for (IBond bond : this.getStructure().bonds()) {
            this.getComponents().add(bond.getID());
        }

        this.makeCML();
    }


    public SortedSet<String> getSubSystems() {
        return this.sub;
    }


    public SortedSet<String> getSuperSystems() {
        return this.sup;
    }


    public SortedSet<String> getConnectingAtoms() {
        return this.connectingAtoms;
    }


    // TODO(sorge): Refactor some of these functions!
    public void addSub(String sub) {
        this.sub.add(sub);
    }

    public void addSubs(List<String> subs) {
        this.sub.addAll(subs);
    }

    public void addSup(String sup) {
        this.sup.add(sup);
    }

    public void addSups(List<String> sups) {
        this.sup.addAll(sups);
    }


    public boolean isSub(String atomSet) {
        return this.getComponents().contains(atomSet);
    };

    public boolean isSub(RichAtomSet atomSet) {
        return this.isSub(atomSet.getId());
    };


    public boolean isSup(String atomSet) {
        return this.getContexts().contains(atomSet);
    };

    public boolean isSup(RichAtomSet atomSet) {
        return this.isSup(atomSet.getId());
    };


    @Override
    public IAtomContainer getStructure() {
        return (IAtomContainer)this.structure;
    }

    
    private void makeCML() {
        this.cml = new CMLAtomSet();
        this.cml.setTitle(this.type.name);
        this.cml.setId(this.getId());
    }


    public void addConnection(IAtom atom, RichAtomSet set, IBond bond) {
        this.setConnections.add(atom);
    }

    public void addConnection(IAtom atom, IAtom extAtom, IBond bond) {
        this.atomConnections.add(atom);
    }
    

    /**
     * Computes positions of atoms or substructures in the atom set.
     * We use the following heuristical preferences:
     * -- Always start with an element that has an external bond.
     * -- If multiple external elements we prefer one with an atom attached
     *    (or later with a functional group, as this can be voiced as substitution).
     * @param annotations Annotations for the atom set.
     *          
     */
    public void computePositions() {
        switch (this.type) {
        case FUSED:
            computeSubstructurePositions();
            break;
        case ALIPHATIC:
            computeAtomPositionsAliphatic();
            break;
        case SMALLEST:
        case ISOLATED:
        default:
            computeAtomPositionsIsolated();
        }
        printConnections();
    }


    private void computeAtomPositionsAliphatic() {
        IAtom startAtom = null;
        for (IAtom atom : this.getStructure().atoms()) {
            if (this.getStructure().getConnectedAtomsList(atom).size() == 1) {
                startAtom = atom;
                if (this.atomConnections.contains(startAtom)) {
                    return;
                }
            }
        }
        if (startAtom == null) {
            throw new SreException("Aliphatic chain without start atom!");
        }
        this.walkRing(startAtom, 1, new ArrayList<IAtom>());
    }

    private void computeSubstructurePositions() {
        // Not yet implemented...
    }


    private void computeAtomPositionsIsolated() {
        IAtom startAtom;
        if (this.atomConnections.size() == 0 && setConnections.size() == 0) {
            List<IAtom> atoms = Lists.newArrayList(this.getStructure().atoms());
            startAtom = atoms.get(0);
        } else if (this.atomConnections.size() == 0) {
            startAtom = this.setConnections.iterator().next();
        } else {
            startAtom = this.atomConnections.iterator().next();
        }
        this.walkRing(startAtom, 1, new ArrayList<IAtom>());
    }

    private void walkRing(IAtom atom, Integer count, List<IAtom> visited) {
        if (visited.contains(atom)) {
            return;
        }
        this.elementPositions.put(count, atom.getID());
        visited.add(atom);
        for (IAtom connected : this.getStructure().getConnectedAtomsList(atom)) {
            if (!visited.contains(connected)) {
                walkRing(connected, ++count, visited);
                return;
            }
        } 
    }


    public String getPositionAtom(Integer position) {
        return this.elementPositions.get(position);
    }


    public Integer getAtomPosition(String atom) {
        return this.elementPositions.inverse().get(atom);
    }


    public void printConnections () {
        for (Integer key : this.elementPositions.keySet()) {
            System.out.printf("%d: %s\n", key, this.elementPositions.get(key));
        }
    }

    
    @Override
    public String toString() {
        String structure = super.toString();
        Joiner joiner = Joiner.on(" ");
        return structure +
            "\nSuper Systems:" + joiner.join(this.getSuperSystems()) +
            "\nSub Systems:" + joiner.join(this.getSubSystems()) +
            "\nConnecting Atoms:" + joiner.join(this.getConnectingAtoms());
    }


    // This should only ever be called once!
    // Need a better solution!
    public CMLAtomSet getCML(Document doc) {
        for (IAtom atom : this.getStructure().atoms()) { 
            String atomId = atom.getID();
            CMLAtom node = (CMLAtom)SreUtil.getElementById(doc, atomId);
            this.cml.addAtom(node);
        }
        return this.cml;
    }

    public CMLAtomSet getCML() {
        return this.cml;
    }

}
