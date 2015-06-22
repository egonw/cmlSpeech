// Copyright 2015 Volker Sorge
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @file   StructureVisitor.java
 * @author Volker Sorge<a href="mailto:V.Sorge@progressiveaccess.com">Volker
 *         Sorge</a>
 * @date   Sat Apr 25 23:36:58 2015
 *
 * @brief  Visitor to construct the exploration structure.
 *
 *
 */

//

package com.progressiveaccess.cmlspeech.sre;

import com.progressiveaccess.cmlspeech.analysis.RichStructureHelper;
import com.progressiveaccess.cmlspeech.structure.RichAtom;
import com.progressiveaccess.cmlspeech.structure.RichAtomSet;
import com.google.common.collect.TreeMultimap;
import java.util.Comparator;
import com.progressiveaccess.cmlspeech.base.CmlNameComparator;
import java.util.Set;
import com.progressiveaccess.cmlspeech.connection.Connection;
import java.util.HashSet;
import java.util.stream.Collectors;
import com.progressiveaccess.cmlspeech.structure.ComponentsPositions;
import java.util.List;
import com.progressiveaccess.cmlspeech.structure.RichFunctionalGroup;
import com.progressiveaccess.cmlspeech.structure.RichMolecule;
import com.progressiveaccess.cmlspeech.structure.RichIsolatedRing;
import com.progressiveaccess.cmlspeech.structure.RichAliphaticChain;
import com.progressiveaccess.cmlspeech.structure.RichFusedRing;
import com.progressiveaccess.cmlspeech.connection.ConnectingBond;
import com.progressiveaccess.cmlspeech.connection.SpiroAtom;
import com.progressiveaccess.cmlspeech.connection.BridgeAtom;
import com.progressiveaccess.cmlspeech.connection.SharedAtom;
import com.progressiveaccess.cmlspeech.connection.SharedBond;
import com.progressiveaccess.cmlspeech.structure.RichSubRing;
import com.progressiveaccess.cmlspeech.structure.RichSuperSet;

/**
 * Constructs the exploration structure.
 */

public class StructureVisitor implements XmlVisitor {

  //private final SreAnnotations annotations = new SreAnnotations();

  private TreeMultimap<String, SreElement> annotations =
      TreeMultimap.create(new CmlNameComparator(), new SreComparator());
  private SreElement element;
  private RichAtomSet context;
  private ComponentsPositions positions;


  private class SreComparator implements Comparator<SreElement> {

  @Override
  public int compare(final SreElement element1, final SreElement element2) {
    return 1;
  }}


  /**
   * @return The annotation the visitor computes.
   */
  public SreElement getAnnotations() {
    SreElement element = new SreElement(SreNamespace.Tag.ANNOTATIONS);
    for (final String key : this.annotations.keySet()) {
      for (final SreElement value : this.annotations.get(key)) {
        element.appendChild(value);
      }
    }
    return element;
  }


  @Override
  public void visit(final RichIsolatedRing ring) {
    this.atomSetStructure(ring);
  }


  @Override
  public void visit(final RichFusedRing ring) {
    this.atomSetStructure(ring);
  }


  @Override
  public void visit(final RichSubRing ring) {
    this.atomSetStructure(ring);
  }


  @Override
  public void visit(final RichAliphaticChain chain) {
    this.atomSetStructure(chain);
  }


  @Override
  public void visit(final RichFunctionalGroup group) {
    this.atomSetStructure(group);
  }


  @Override
  public void visit(final RichMolecule molecule) {
    this.atomSetStructure(molecule);
  }


  // /**
  //  * Computes structure for an atom set in the context given context.
  //  *
  //  * @param set
  //  *          The rich atom set.
  //  */
  // private void atomSetStructure(final RichAtomSet set) {
  //   this.element = new SreElement(SreNamespace.Tag.ANNOTATION);
  //   annotations.put(set.getId(), this.element);
  //   this.context = RichStructureHelper.getRichMolecule();
  //   String id = set.getId();
  //   this.positions = ((RichMolecule)this.context).getPath();
  //   Integer position = this.positions.getPosition(id);
  //   this.element.appendChild(new SreElement(set.tag(), id));
  //   SreElement parent = new SreElement(SreNamespace.Tag.PARENTS);
  //   this.element.appendChild(parent);
  //   parent.appendChild(SreUtil.sreElement(this.context.getId()));
  //   this.element.appendChild(new SreElement(SreNamespace.Tag.POSITION,
  //                                           position.toString()));
  //   this.element.appendChild(SreUtil.sreSet(SreNamespace.Tag.COMPONENT,
  //                                           set.getComponents()));
  //   this.element.appendChild(SreUtil.sreSet(SreNamespace.Tag.CHILDREN,
  //                                           set.getSubSystems()));
  //   SreElement connElement = new SreElement(SreNamespace.Tag.NEIGHBOURS);
  //   this.element.appendChild(connElement);
  //   this.element = connElement;
  //   Set<Connection> internalConnections = this.connectionsInContext(set);
  //   for (Connection connection : internalConnections) {
  //     connection.accept(this);
  //     connElement.appendChild(this.element);
  //   }
  // }


  /**
   * Computes structure for an atom set in the context given context.
   *
   * @param set
   *          The rich atom set.
   */
  private void atomSetStructure(final RichAtomSet set) {
    this.element = new SreElement(SreNamespace.Tag.ANNOTATION);
    annotations.put(set.getId(), this.element);
    this.context = RichStructureHelper.getRichAtomSet(set.getSuperSystems().iterator().next());
    String id = set.getId();
    this.positions = ((RichSuperSet)this.context).getPath();
    Integer position = this.positions.getPosition(id);
    this.element.appendChild(new SreElement(set.tag(), id));
    SreElement parent = new SreElement(SreNamespace.Tag.PARENTS);
    this.element.appendChild(parent);
    parent.appendChild(SreUtil.sreElement(this.context.getId()));
    this.element.appendChild(new SreElement(SreNamespace.Tag.POSITION,
                                            position.toString()));
    this.element.appendChild(SreUtil.sreSet(SreNamespace.Tag.COMPONENT,
                                            set.getComponents()));
    this.element.appendChild(SreUtil.sreSet(SreNamespace.Tag.CHILDREN,
                                            set.getSubSystems()));
    SreElement connElement = new SreElement(SreNamespace.Tag.NEIGHBOURS);
    this.element.appendChild(connElement);
    this.element = connElement;
    Set<Connection> internalConnections = this.connectionsInContext(set);
    for (Connection connection : internalConnections) {
      connection.accept(this);
      connElement.appendChild(this.element);
    }
  }


  /**
   * Computes structure for an atom set in the context given context.
   *
   * @param set
   *          The rich atom set.
   */
  private void atomSetStructure(final RichMolecule set) {
    this.element = new SreElement(SreNamespace.Tag.ANNOTATION);
    annotations.put(set.getId(), this.element);
    String id = set.getId();
    this.element.appendChild(new SreElement(set.tag(), id));
    SreElement parent = new SreElement(SreNamespace.Tag.PARENTS);
    this.element.appendChild(parent);
    this.element.appendChild(new SreElement(SreNamespace.Tag.POSITION, "1"));
    this.element.appendChild(SreUtil.sreSet(SreNamespace.Tag.COMPONENT,
                                            set.getComponents()));
    this.element.appendChild(SreUtil.sreSet(SreNamespace.Tag.CHILDREN,
                                            set.getSubSystems()));
    SreElement connElement = new SreElement(SreNamespace.Tag.NEIGHBOURS);
    this.element.appendChild(connElement);
  }


  @Override
  public void visit(final RichAtom atom) {
    for (String parent : atom.getSuperSystems()) {
      this.element = new SreElement(SreNamespace.Tag.ANNOTATION);
      annotations.put(atom.getId(), this.element);
      this.context =  RichStructureHelper.getRichAtomSet(parent);
      this.atomStructure(atom);
    }
  }


  /**
   * Computes structure for an atom in the context of a set.
   *
   * @param atom
   *          The rich atom.
   */
  private void atomStructure(final RichAtom atom) {
    String id = atom.getId();
    this.positions = this.context.getComponentsPositions();
    Integer position = this.positions.getPosition(id);
    this.element.appendChild(new SreElement(atom.tag(), id));
    SreElement parent = new SreElement(SreNamespace.Tag.PARENTS);
    this.element.appendChild(parent);
    parent.appendChild(SreUtil.sreElement(this.context.getId()));
    this.element.appendChild(new SreElement(SreNamespace.Tag.POSITION,
                                            position.toString()));
    Set<Connection> internalConnections = this.bondsInContext(atom);
    this.element.appendChild(SreUtil.sreSet(SreNamespace.Tag.COMPONENT,
        internalConnections.stream().map(conn -> conn.getConnector())
                           .collect(Collectors.toList())));
    this.element.appendChild(new SreElement(SreNamespace.Tag.CHILDREN));
    SreElement connElement = new SreElement(SreNamespace.Tag.NEIGHBOURS);
    this.element.appendChild(connElement);
    if (position > 1) {
      internalConnections.stream()
        .filter(c -> c.getConnected() == this.positions.get(position - 1))
        .forEach(c -> {
            c.accept(this);
            connElement.appendChild(this.element);});
    }
    if (position < positions.size()) {
      internalConnections.stream()
        .filter(c -> c.getConnected() == this.positions.get(position + 1))
        .forEach(c -> {
            c.accept(this);
            connElement.appendChild(this.element);});
    }
  }


  @Override
  public void visit(final SpiroAtom spiroAtom) {
    System.out.println(spiroAtom);
    this.makeConnection(spiroAtom);
  }


  @Override
  public void visit(final BridgeAtom bridgeAtom) {
    this.makeConnection(bridgeAtom);
  }


  @Override
  public void visit(final ConnectingBond bond) {
    this.makeConnection(bond);
  }
  

  @Override
  public void visit(final SharedAtom sharedAtom) {
    this.makeConnection(sharedAtom);
  }


  @Override
  public void visit(final SharedBond sharedBond) {
    this.makeConnection(sharedBond);
  }


  private void makeConnection(Connection connection) {
    this.element = this.makeNeighbour
      (connection.getConnected(),
       this.makeVia(connection.getConnector(),
                    this.positions.getPosition(connection.getConnected())));
  }

  
  /** 
   * Creates a via element.
   * 
   * @param via
   *          The connector.
   * @param position
   *          The position of the connected element in the current context.
   * 
   * @return The newly create via element.
   */
  private SreElement makeVia(String via, Integer position) {
    SreElement viaElement = new SreElement(SreNamespace.Tag.VIA);
    viaElement.appendChild(SreUtil.sreElement(via));
    viaElement.appendChild(new SreElement(SreNamespace.Tag.POSITION,
                                          position.toString()));
    return viaElement;
  }


  /** 
   * Creates a neighbour element.
   * 
   * @param neighbour
   *          The connected element.
   * @param via
   *          The via element representing how the element is connected.
   * 
   * @return The newly create neighbour element.
   */
  private SreElement makeNeighbour(String neighbour, SreElement via) {
    SreElement element = new SreElement(SreNamespace.Tag.NEIGHBOUR);
    element.appendChild(SreUtil.sreElement(neighbour));
    element.appendChild(via);
    return element;
  };

  
  /** 
   * Creates a neighbour element.
   * 
   * @param neighbour The connector.
   * @param vias The list of via elements representing how the element is
   *     connected.
   * 
   * @return The newly create neighbour element.
   */
  private SreElement makeNeighbour(String neighbour, List<SreElement> vias) {
    SreElement element = new SreElement(SreNamespace.Tag.NEIGHBOUR);
    element.appendChild(SreUtil.sreElement(neighbour));
    vias.stream().forEach(element::appendChild);
    return element;
  }


  /**
   * Computes connections of an atom in the context of a set.
   *
   * @param atom
   *          The rich atom.
   *
   * @return The connections of the atom that belong to the set.
   */
  private Set<Connection> bondsInContext(final RichAtom atom) {
    if (!this.context.getConnectingAtoms().contains(atom.getId())) {
      return atom.getConnections();
    }
    Set<Connection> internal = new HashSet<>();
    for (Connection connection : atom.getConnections()) {
      if (this.context.getInternalBonds().contains(connection.getConnector())) {
        internal.add(connection);
      }
    }
    return internal;
  }


  /**
   * Computes connections of an atom in the context of a set.
   *
   * @param set
   *          The rich atom set.
   *
   * @return The connections of the atom that belong to the set.
   */
  private Set<Connection> connectionsInContext(final RichAtomSet set) {
    Set<Connection> internal = new HashSet<>();
    for (Connection connection : set.getConnections()) {
      if (this.positions.contains(connection.getConnected())) {
        internal.add(connection);
      }
    }
    return internal;
  }


  private String nextElement(final String id) {
    ComponentsPositions positions = this.context.getComponentsPositions();
    Integer current = positions.getPosition(id);
    return (current >= positions.size()) ? null : positions.get(current + 1);
  }

  private String previousElement(final String id) {
    ComponentsPositions positions = this.context.getComponentsPositions();
    Integer current = positions.getPosition(id);
    return (current <= 1) ? null : positions.get(current - 1);
  }

}
