// Copyright 2015 Volker Sorge
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


/**
 * @file   StructuralAnalysis.java
 * @author Volker Sorge <sorge@zorkstone>
 * @date   Wed Jun 11 21:42:42 2014
 * 
 * @brief  Main routines for structural analysis.
 * 
 * 
 */

//
package io.github.egonw.analysis;

import io.github.egonw.base.CMLNameComparator;
import io.github.egonw.base.Cli;
import io.github.egonw.base.Logger;
import io.github.egonw.connection.BridgeAtom;
import io.github.egonw.connection.ConnectingBond;
import io.github.egonw.connection.SharedAtom;
import io.github.egonw.connection.SharedBond;
import io.github.egonw.connection.SpiroAtom;
import io.github.egonw.graph.StructuralEdge;
import io.github.egonw.graph.StructuralGraph;
import io.github.egonw.structure.ComponentsPositions;
import io.github.egonw.structure.RichAliphaticChain;
import io.github.egonw.structure.RichAtom;
import io.github.egonw.structure.RichAtomSet;
import io.github.egonw.structure.RichBond;
import io.github.egonw.structure.RichChemObject;
import io.github.egonw.structure.RichFunctionalGroup;
import io.github.egonw.structure.RichFusedRing;
import io.github.egonw.structure.RichIsolatedRing;
import io.github.egonw.structure.RichMolecule;
import io.github.egonw.structure.RichSetType;
import io.github.egonw.structure.RichStructure;
import io.github.egonw.structure.RichSubRing;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

import org.jgrapht.alg.NeighborIndex;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Main functionality for the structural analysis of molecules.
 */

public class StructuralAnalysis {
    
    private int atomSetCount = 0;
    private final IAtomContainer molecule;

    private SortedMap<String, RichStructure<?>> richAtoms = 
        new TreeMap<>(new CMLNameComparator());
    private SortedMap<String, RichStructure<?>> richBonds = 
        new TreeMap<>(new CMLNameComparator());
    private SortedMap<String, RichStructure<?>> richAtomSets = 
        new TreeMap<>(new CMLNameComparator());

    private List<RichAtomSet> majorSystems;
    private List<RichAtomSet> minorSystems;
    private Set<String> singletonAtoms = new HashSet<String>();

    private StructuralGraph majorGraph;
    private StructuralGraph minorGraph;
    private StructuralGraph recursiveGraph;

    public ComponentsPositions majorPath;
    private ComponentsPositions minorPath;
    private ComponentsPositions componentPositions = new ComponentsPositions();

    public RichAtomSet top;


    public StructuralAnalysis(IAtomContainer molecule) {
        this.molecule = molecule;
        
        this.initStructure();
        
        this.rings();
        this.aliphaticChains();
        this.functionalGroups();
        
        this.contexts();

        this.atomSetsAttachments();
        this.connectingBonds();
        this.sharedComponents();

        this.singletonAtoms();
        this.majorSystems();
        this.minorSystems();

        this.makeTopSet();
        this.makeBottomSet();
    }
    
    public IAtomContainer getMolecule() {
    	return molecule;
    }

    public boolean isAtom(String id) {
        return this.richAtoms.containsKey(id);
    }

    public boolean isBond(String id) {
        return this.richBonds.containsKey(id);
    }

    public boolean isAtomSet(String id) {
        return this.richAtomSets.containsKey(id);
    }


    // TODO (sorge) Make these methods safe!
    public RichAtom getRichAtom(String id) {
        return (RichAtom)this.richAtoms.get(id);
    }

    private RichStructure<?> setRichAtom(IAtom atom) {
        return this.setRichStructure(this.richAtoms, atom.getID(), new RichAtom(atom));
    }
    


    public RichBond getRichBond(String id) {
        return (RichBond)this.richBonds.get(id);
    }

    public RichStructure<?> getRichBond(IBond bond) {
        return this.getRichBond(bond.getID());
    }

    private RichStructure<?> setRichBond(IBond bond) {
        return this.setRichStructure(this.richBonds, bond.getID(), new RichBond(bond));
    }
    


    public RichAtomSet getRichAtomSet(String id) {
        return (RichAtomSet)this.richAtomSets.get(id);
    }

    private RichAtomSet setRichAtomSet(RichAtomSet atomSet) {
        return (RichAtomSet)this.setRichStructure(this.richAtomSets, atomSet.getId(), atomSet);
    }

    
    private RichStructure<?> setRichStructure(SortedMap<String, RichStructure<?>> map,
                                              String id, RichStructure<?> structure) {
        map.put(id, structure);
        return structure;
    }

    public RichStructure<?> getRichStructure(String id) {
        RichStructure<?> structure = this.richAtoms.get(id);
        if (structure != null) {
            return structure;
        } 
        structure = this.richBonds.get(id);
        if (structure != null) {
            return structure;
        } 
        return this.richAtomSets.get(id);
    }

    
    @SuppressWarnings("unchecked")
    public List<RichAtom> getAtoms() {
        return (List<RichAtom>)(List<?>)
            new ArrayList<RichStructure<?>>(this.richAtoms.values());
    }

    @SuppressWarnings("unchecked")
    public List<RichBond> getBonds() {
        return (List<RichBond>)(List<?>)
            new ArrayList<RichStructure<?>>(this.richBonds.values());
    }

    @SuppressWarnings("unchecked")
    public List<RichAtomSet> getAtomSets() {
        return (List<RichAtomSet>)(List<?>)
            new ArrayList<RichStructure<?>>(this.richAtomSets.values());
    }

    @SuppressWarnings("unchecked")
    public List<RichAtom> getSingletonAtoms() {
        return (List<RichAtom>)(List<?>)this.singletonAtoms.stream()
            .map(this::getRichAtom).collect(Collectors.toList());
    }


    /** Initialises the structure from the molecule. */
    private void initStructure() {
        this.molecule.atoms().forEach(this::setRichAtom);
        for (IBond bond : this.molecule.bonds()) {
            this.setRichBond(bond);
            for (IAtom atom : bond.atoms()) {
                RichStructure<?> richAtom = this.getRichAtom(atom.getID());
                richAtom.getContexts().add(bond.getID());
                richAtom.getExternalBonds().add(bond.getID());
            }
        }
    }


    /**
     * Adds a context element for a set of structures.
     * @param structures Set of structure names.
     * @param id Context element to be added.
     */
    private void setContexts(Set<String> structures, String id) {
        for (String structure : structures) {
            this.getRichStructure(structure).getContexts().add(id);
        }
    }
    

    private void makeTopSet() {
        String id = this.getAtomSetId();
        this.top = this.setRichAtomSet(new RichMolecule(this.molecule, id));
        this.setContexts(this.richAtoms.keySet(), id);
        this.setContexts(this.richBonds.keySet(), id);
        this.setContexts(this.richAtomSets.keySet(), id);
        this.top.getContexts().remove(id);
        for (RichAtomSet system : this.majorSystems) {
            system.getSuperSystems().add(id);
            this.top.getSubSystems().add(system.getId());
        }
        for (RichAtom atom : this.getSingletonAtoms()) {
            atom.getSuperSystems().add(id);
            this.top.getSubSystems().add(atom.getId());
        }
    }


    private void makeBottomSet() {
        for (RichAtomSet system : this.minorSystems) {
            for (String component : system.getComponents()) {
                if (this.isAtom(component)) {
                    this.getRichAtom(component).getSuperSystems().add(system.getId());
                    system.getSubSystems().add(component);
                }
            }
        }
    }


    /**
     * Returns atom set id and increments id counter.
     * @return A new unique atom set id.
     */
    private String getAtomSetId() {
        atomSetCount++;
        return "as" + atomSetCount;
    }


    private String valuesToString(SortedMap<String, RichStructure<?>> map) {
        return Joiner.on("\n").join(map.values()
                                    .stream().map(RichStructure::toString)
                                    .collect(Collectors.toList()));
    }

    public String toString() {
        return valuesToString(this.richAtoms) + "\n"
            + valuesToString(this.richBonds) + "\n"
            + valuesToString(this.richAtomSets);
    }



    /**
     * Computes information on ring systems in the molecule.
     */
    private void rings() {
        RingSystem ringSystem = new RingSystem(this.molecule);
        Boolean sub = !Cli.hasOption("s");
        Boolean sssr = Cli.hasOption("sssr");

        for (IAtomContainer ring : ringSystem.fusedRings()) {
            RichStructure<?> fusedRing = this.setRichAtomSet(new RichFusedRing(ring, this.getAtomSetId()));
            if (sub) {
                for (IAtomContainer subSystem : ringSystem.subRings(ring, sssr)) {
                    RichStructure<?> subRing = this.setRichAtomSet(new RichSubRing(subSystem, this.getAtomSetId()));
                    String ringId = fusedRing.getId();
                    String subRingId = subRing.getId();
                    subRing.getSuperSystems().add(ringId);
                    subRing.getContexts().add(ringId);
                    fusedRing.getSubSystems().add(subRingId);
                }
            }
        }
        for (IAtomContainer ring : ringSystem.isolatedRings()) {
            this.setRichAtomSet(new RichIsolatedRing(ring, this.getAtomSetId()));
        }
    }


    /**
     * Computes the longest aliphatic chain for the molecule.
     */
    private void aliphaticChains() {
        if (this.molecule == null) { return; }
        AliphaticChain chain = new AliphaticChain(3);
        chain.calculate(this.molecule);
        for (IAtomContainer set : chain.extract()) {
            this.setRichAtomSet(new RichAliphaticChain(set, this.getAtomSetId()));
        }
    }


    /**
     * Computes functional groups.
     */
    private void functionalGroups() {
        FunctionalGroups fg = new FunctionalGroups(this.molecule);
        FunctionalGroupsFilter filter = new FunctionalGroupsFilter(this.getAtomSets(),
                                                                   fg.getGroups());
        Map<String, IAtomContainer> groups = filter.filter();
        for (String key : groups.keySet()) {
            IAtomContainer container = groups.get(key);
            RichAtomSet set = this.setRichAtomSet(new RichFunctionalGroup(groups.get(key), this.getAtomSetId()));
            set.name = key.split("-")[0];
            Logger.logging(set.name + ": " + container.getAtomCount() + " atoms "
                           + container.getBondCount() + " bonds");
        }
    }
    

    /** Computes the contexts of single atoms. */
    private void contexts() {
        for (String key : this.richAtomSets.keySet()) {
            this.setContexts(this.getRichAtomSet(key).getComponents(), key);
        }
    }


    private void atomSetsAttachments() {
        this.richAtomSets.values().
            forEach(as -> this.atomSetAttachments((RichAtomSet)as));
    }


    /**
     * Computes the external bonds and connecting atoms for an atom set.
     * @param atomSet A rich atom set.
     */
    private void atomSetAttachments(RichAtomSet atomSet) {
        IAtomContainer container = atomSet.getStructure();
        Set<IBond> externalBonds = externalBonds(container);
        for (IBond bond : externalBonds) {
            atomSet.getExternalBonds().add(bond.getID());
        }
        Set<IAtom> connectingAtoms = connectingAtoms(container, externalBonds);
        for (IAtom atom : connectingAtoms) {
            atomSet.getConnectingAtoms().add(atom.getID());
        }
    }


    /**
     * Compute the bonds that connects this atom container to the rest of the
     * molecule.
     * @param container The substructure under consideration.
     * @return List of bonds attached to but not contained in the container.
     */
    private Set<IBond> externalBonds(IAtomContainer container) {
        Set<IBond> internalBonds = Sets.newHashSet(container.bonds());
        Set<IBond> allBonds = Sets.newHashSet();
        for (IAtom atom : container.atoms()) {
            allBonds.addAll(this.molecule.getConnectedBondsList(atom));
        }
        return Sets.difference(allBonds, internalBonds);
    }


    /**
     * Compute the atoms that have bonds not internal to the molecule.
     * @param container The substructure under consideration.
     * @param bonds External bonds.
     * @return List of atoms with external connections.
     */
    private Set<IAtom> connectingAtoms(IAtomContainer container, Set<IBond> bonds) {
        Set<IAtom> allAtoms = Sets.newHashSet(container.atoms());
        Set<IAtom> connectedAtoms = Sets.newHashSet();
        for (IBond bond : bonds) {
            connectedAtoms.addAll
                (Lists.newArrayList(bond.atoms()).stream().
                 filter(a -> allAtoms.contains(a)).collect(Collectors.toSet()));
        }
        return connectedAtoms;
    }


    /**
     * Compute the connecting bonds for the atom container from the set of
     * external bonds.
     * @param container The substructure under consideration.
     * @param externalBonds Bonds external to the substructure.
     * @return List of connecting bonds, i.e., external but not part of another
     *         substructure.
     */
    private void connectingBonds() {
        for (String bond : this.richBonds.keySet()) {
            RichStructure<?> richBond = this.getRichBond(bond);
            String first = ((TreeSet<String>)richBond.getComponents()).first();
            String last = ((TreeSet<String>)richBond.getComponents()).last();
            if (richBond.getContexts().isEmpty()) {
                // We assume each bond has two atoms only!
                this.addSetConnections(bond, first, last);
            }
            this.addConnectingBond(this.getRichStructure(first), bond, last);
            this.addConnectingBond(this.getRichStructure(last), bond, first);
        }
    }

    
    private void addConnectingBond(RichStructure<?> structure, String bond, String connected) {
        structure.getConnections().add
            (new ConnectingBond(bond, connected));
    }

    /**
     * Creates the context cloud for an atom, that is the list of all atom sets
     * in its context.
     * @param atom The input atom.
     * @return The resulting context cloud.
     */
    private Set<String> contextCloud(String atom) {
        Set<String> contextAtom = Sets.intersection
            (this.getRichAtom(atom).getContexts(), 
             this.richAtomSets.keySet());
        if (contextAtom.isEmpty()) {
            contextAtom = new HashSet<String>();
            contextAtom.add(atom);
        }
        return contextAtom;
    }
    

    /**
     * Adds connections to atom set structures.
     * @param bond The bond.
     * @param atomA The first atom in the bond.
     * @param atomB The second atom in the bond.
     */
    private void addSetConnections(String bond, String atomA, String atomB) {
        Set<String> contextAtomA = this.contextCloud(atomA);
        Set<String> contextAtomB = this.contextCloud(atomB);
        for (String contextA : contextAtomA) {
            RichStructure<?> richStructureA = this.getRichStructure(contextA);
            for (String contextB : contextAtomB) {
                RichStructure<?> richStructureB = this.getRichStructure(contextB);
                this.addConnectingBond(richStructureA, bond, contextB);
                this.addConnectingBond(richStructureB, bond, contextA);
            }
        }
    }


    /** Computes bridge atoms and bonds for structures that share components. */
    private void sharedComponents() {
        for (String atomSet : this.richAtomSets.keySet()) {
            TreeMultimap<String, String> connectionsSet =
                TreeMultimap.create(new CMLNameComparator(), new CMLNameComparator());
            RichAtomSet richAtomSet = this.getRichAtomSet(atomSet);
            for (String component : richAtomSet.getComponents()) {
                RichStructure<?> richComponent = this.getRichStructure(component);
                Set<String> contexts = Sets.intersection
                    (richComponent.getContexts(), this.richAtomSets.keySet());
                for (String context : contexts) {
                    if (richAtomSet.getSubSystems().contains(context) ||
                        richAtomSet.getSuperSystems().contains(context) ||
                        context.equals(atomSet)) {
                        continue;
                    }
                    connectionsSet.put(context, component);
                }
            }
            this.makeConnections(richAtomSet, connectionsSet);
        }
    }


    private void makeConnections(RichAtomSet atomSet, TreeMultimap<String, String> connectionsSet) {
        for (String key : connectionsSet.keySet()) {
            NavigableSet<String> allConnections = connectionsSet.get(key);
            SortedSet<String> sharedAtoms = new TreeSet<String>(new CMLNameComparator());
            for (String bond : allConnections.descendingSet()) {
                if (!this.isBond(bond)) {
                    break;
                }
                atomSet.getConnections().add
                    (new SharedBond(bond, key));
                for (IAtom atom : this.getRichBond(bond).getStructure().atoms()) {
                    sharedAtoms.add(atom.getID());
                }
            }
            for (String shared : sharedAtoms) {
                atomSet.getConnections().add
                    (new BridgeAtom(shared, key));
            }
            Boolean ring = RichAtomSet.isRing(atomSet);
            for (String connection : Sets.difference(allConnections, sharedAtoms)) {
                if (!this.isAtom(connection)) {
                   break;
                }
                if (ring && RichAtomSet.isRing(this.getRichAtomSet(key))) {
                    atomSet.getConnections().add
                        (new SpiroAtom(connection, key));
                } else {
                    atomSet.getConnections().add
                        (new SharedAtom(connection, key));
                }
            }
        }
    }
    

    /**
     * Computes the siblings of this atom set if it is a subring.
     * @param atomSet The given atom set.
     * @return A list of siblings.
     */
    public Set<String> siblingsNEW(RichAtomSet atomSet) {
        Set<String> result = new HashSet<String>();
        if (atomSet.getType() == RichSetType.SMALLEST) {
            for (String superSystem : atomSet.getSuperSystems()) {
                result.addAll((this.getRichAtomSet(superSystem)).getSubSystems());
            }
        }
        result.remove(atomSet);
        return result;
    }

    
    /**
     * Computes the siblings of this atom set if it is a subring.
     * @param atomSet The given atom set.
     * @return A list of siblings.
     */
    public Set<RichStructure<?>> siblings(RichAtomSet atomSet) {
        Set<String> result = new HashSet<String>();
        if (atomSet.getType() == RichSetType.SMALLEST) {
            for (String superSystem : atomSet.getSuperSystems()) {
                result.addAll(this.getRichAtomSet(superSystem).getSubSystems());
            }
        }
        result.remove(atomSet.getId());
        return result.stream()
            .map(this::getRichAtomSet)
            .collect(Collectors.toSet());
    }

    
    private void singletonAtoms() {
        Set<String> atomSetComponents = new HashSet<String>();
        this.richAtomSets.values().
            forEach(as -> atomSetComponents.addAll(as.getComponents()));
        for (String atom : this.richAtoms.keySet()) {
            if (!atomSetComponents.contains(atom)) {
                this.singletonAtoms.add(atom);
            }
        }
    }


    /** Compute the major systems, i.e., all systems that are not part of a
     * larger supersystem. */
    private void majorSystems() {
        this.majorSystems = this.getAtomSets().stream()
            .filter(as -> as.type != RichSetType.SMALLEST)
            .collect(Collectors.toList());
        this.majorGraph = new StructuralGraph(this.getMajorSystems(),
                                              this.getSingletonAtoms());
        this.majorPath = this.path(this.majorGraph);
    }
    

    /**
     * Returns the major systems.
     * @return List of major systems.
     */
    public List<RichAtomSet> getMajorSystems() {
        return this.majorSystems;
    }


    /** Compute the minor systems, i.e., all systems that have no non-atomic
     * sub-system. */
    private void minorSystems() {
        this.minorSystems = this.getAtomSets().stream()
            .filter(as -> as.type != RichSetType.FUSED)
            .collect(Collectors.toList());
        this.minorGraph = new StructuralGraph(this.getMinorSystems(),
                                              this.getSingletonAtoms());
        this.minorPath = this.path(this.minorGraph);
    }
    

    /**
     * Returns the minor systems.
     * @return List of minor systems.
     */
    public List<RichAtomSet> getMinorSystems() {
        return this.minorSystems;
    }


    /** Compute the major systems, i.e., all systems that are not part of a
     * larger supersystem. */
    private void recursiveSystems() {
        List<RichAtomSet> subRings = this.getAtomSets().stream()
            .filter(as -> as.getType() == RichSetType.FUSED)
            .collect(Collectors.toList());
        for (RichAtomSet ring: subRings) {
            StructuralGraph ringGraph =
                new StructuralGraph(ring.getSubSystems().stream().
                                    map(this::getRichStructure).
                                    collect(Collectors.toList()));
            ringGraph.visualize(ring.getId());
        }
        
        Map<String, StructuralGraph> minorGraphs = new HashMap<String, StructuralGraph>();
        for (RichAtomSet system: this.getMinorSystems()) {
            List<RichStructure<?>> atoms = new ArrayList<RichStructure<?>>();
            for (String id: system.getComponents()) {
                RichAtom atom = this.getRichAtom(id);
                if (atom != null) {
                    atoms.add((RichStructure)atom);
                }
            }
            StructuralGraph minorGraph = new StructuralGraph(atoms);
            minorGraphs.put(system.getId(), minorGraph);
            minorGraph.visualize(system.getId());
        }
    }


    /**
     * Computes a path through the molecule.
     * @param graph An abstraction graph for the molecule.
     */
    public ComponentsPositions path(StructuralGraph graph) {
        ComponentsPositions path = new ComponentsPositions();
        NeighborIndex<String, StructuralEdge> index = new NeighborIndex<String, StructuralEdge>(graph);
        Comparator<String> comparator = new AnalysisCompare();
        Stack<String> rest = new Stack<String>();
        List<String> vertices = new ArrayList<String>(graph.vertexSet());
        Collections.sort(vertices, comparator);
        List<String> visited = new ArrayList<String>();
        rest.push(vertices.get(0));
        while (!rest.empty()) {
            String current = rest.pop();
            if (visited.contains(current)) {
                continue;
            }
            path.addNext(current);
            vertices = new ArrayList<String>(index.neighborsOf(current));
            Collections.sort(vertices, comparator);
            for (int i = vertices.size() - 1; i >= 0; i--) {
                rest.push(vertices.get(i));
            }
            visited.add(current);
        }
        return path;
    }


    public void visualize() {
        this.majorGraph.visualize("Major System Abstraction");
        this.minorGraph.visualize("Minor System Abstraction");
        if (Cli.hasOption("vis_recursive")) {
            this.recursiveSystems();
        }
    }


    // Comparison in terms of "interestingness". The most interesting is sorted to the front.
    public class AnalysisCompare implements Comparator<String> {
        
        String heur = Cli.hasOption("m") ? Cli.getOptionValue("m") : "";

        public int compare(String vertexA, String vertexB) {
            Comparator<RichChemObject> comparator = new Heuristics(heur);
            
            Integer aux = comparator.
                compare((RichChemObject)StructuralAnalysis.this.getRichStructure(vertexA),
                        (RichChemObject)StructuralAnalysis.this.getRichStructure(vertexB));
            return aux;
        }
    }


    // TODO (sorge): Refactor this into common positions mapping.
    public void computePositions() {
        for (String structure : this.majorPath) {
            if (this.isAtom(structure)) {
                this.componentPositions.addNext(structure);
            } else {
                RichAtomSet atomSet = this.getRichAtomSet(structure);
                if (atomSet.getType() == RichSetType.FUSED) {
                    for (String sub : atomSet.getSubSystems()) {
                        RichAtomSet subSystem = this.getRichAtomSet(sub);
                        this.appendPositions(subSystem);
                        atomSet.appendPositions(subSystem);
                    }
                } else {
                    this.appendPositions(atomSet);
                }
            }
        }
    }


    public void appendPositions(RichAtomSet atomSet) {
        atomSet.computePositions(this.componentPositions.size());
        this.componentPositions.putAll(atomSet.componentPositions);
    }


    public void printPositions () { 
        Logger.logging(this.componentPositions.toString());
        this.getAtomSets().forEach(RichAtomSet::printPositions);
    }


    public String getAtom(Integer position) {
        return this.componentPositions.getAtom(position);
    }


    public Integer getPosition(String atom) {
        return this.componentPositions.getPosition(atom);
    }

}
