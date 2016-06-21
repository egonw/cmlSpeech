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
 * @file   RichAtomSet.java
 * @author Volker Sorge
 *         <a href="mailto:V.Sorge@progressiveaccess.com">Volker Sorge</a>
 * @date   Sat Feb 14 12:19:38 2015
 *
 * @brief  Rich atom set structures.
 *
 *
 */

//

package com.progressiveaccess.cmlspeech.structure;

import com.progressiveaccess.cmlspeech.base.CmlNameComparator;
import com.progressiveaccess.cmlspeech.base.Logger;
import com.progressiveaccess.cmlspeech.cactus.SpiderNames;
import com.progressiveaccess.cmlspeech.graph.StructuralGraph;
import com.progressiveaccess.cmlspeech.sre.SreAttribute;
import com.progressiveaccess.cmlspeech.sre.SreNamespace;
import com.progressiveaccess.cmlspeech.sre.SreUtil;

import com.google.common.base.Joiner;

import nu.xom.Document;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.xmlcml.cml.element.CMLAtom;
import org.xmlcml.cml.element.CMLAtomSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Base class for all atom sets with admin information.
 */
public abstract class RichAtomSet extends RichChemObject implements RichSet {

  private RichSetType type;
  private CMLAtomSet cml;
  private ComponentsPositions componentsPositions = new ComponentsPositions();
  private final SortedSet<String> connectingAtoms = new TreeSet<String>(
      new CmlNameComparator());
  private final SortedSet<String> internalBonds = new TreeSet<String>(
      new CmlNameComparator());

  private String iupac = "";
  private String molecularFormula = "";
  private String structuralFormula = "";

  private final SpiderNames names = new SpiderNames();

  /**
   * Constructor for rich atom sets.
   *
   * @param container
   *          The atom container comprising the set.
   * @param id
   *          The name of the set.
   * @param type
   *          The type of the set.
   */
  public RichAtomSet(final IAtomContainer container, final String id,
      final RichSetType type) {
    super(container);
    this.type = type;
    this.getStructure().setID(id);
    for (final IAtom atom : this.getStructure().atoms()) {
      this.getComponents().add(atom.getID());
    }
    for (final IBond bond : this.getStructure().bonds()) {
      this.getInternalBonds().add(bond.getID());
      this.getComponents().add(bond.getID());
    }
    this.makeCml();
  }


  /**
   * @return The component to positions mapping.
   */
  public ComponentsPositions getComponentsPositions() {
    return this.componentsPositions;
  }


  @Override
  public IAtomContainer getStructure() {
    return (IAtomContainer) super.getStructure();
  }


  @Override
  public RichSetType getType() {
    return this.type;
  }


  @Override
  public SortedSet<String> getConnectingAtoms() {
    return this.connectingAtoms;
  }


  @Override
  public SortedSet<String> getInternalBonds() {
    return this.internalBonds;
  }


  /**
   * @return The iupac of the set.
   */
  public String getIupac() {
    return this.iupac;
  }


  @Override
  public void setMolecularFormula(final String formula) {
    this.molecularFormula = formula;
  }


  @Override
  public String getMolecularFormula() {
    return this.molecularFormula;
  }


  @Override
  public void setStructuralFormula(final String formula) {
    this.structuralFormula = formula;
  }


  @Override
  public String getStructuralFormula() {
    return this.structuralFormula;
  }


  /**
   * Sets the iupac of the atom set.
   *
   * @param iupac
   *          The iupac of the set.
   */
  public void setIupac(final String iupac) {
    this.iupac = iupac;
  }


  /**
   * Walks the structure and computes the positions of its elements,
   * substructures etc.
   */
  protected abstract void walk();


  /**
   * Returns a list with two elements that are the connected atoms that lie on
   * the rim of the ring.
   *
   * @param atom
   *          The atom connections are computed for.
   *
   * @return List of connected atoms.
   */
  protected List<IAtom> getConnectedAtomsList(final IAtom atom) {
    return this.getStructure().getConnectedAtomsList(atom);
  }


  /**
   * Finds the next atom in the set that has not yet been visited.
   * This is aimed for sets that are rings or chains.
   *
   * @param visited
   *          The list of already visited atoms.
   * @param atom
   *          The source atom.
   *
   * @return atom that's next to the input atom.
   */
  protected final IAtom chooseNext(final List<IAtom> visited,
      final IAtom atom) {
    visited.add(atom);
    final List<IAtom> connected = this.getConnectedAtomsList(atom);
    if (!visited.contains(connected.get(0))) {
      return connected.get(0);
    }
    if (visited.size() > 1 && !visited.contains(connected.get(1))) {
      return connected.get(1);
    }
    return null;
  }


  /**
   * Walks the structure straight from a given atom.
   *
   * @param atom
   *          The start atom.
   */
  protected final void walkStraight(final IAtom atom) {
    this.walkStraight(atom, new ArrayList<IAtom>());
  }


  /**
   * Walks the structure straight from a given atom, maintaining a visited list.
   *
   * @param atom
   *          The start atom.
   * @param visited
   *          The list of atoms already visited during the walk.
   */
  protected final void walkStraight(final IAtom atom,
                                    final List<IAtom> visited) {
    if (visited.contains(atom)) {
      return;
    }
    this.getComponentsPositions().addNext(atom.getID());
    visited.add(atom);
    for (final IAtom connected : this.getConnectedAtomsList(atom)) {
      if (!visited.contains(connected)) {
        this.walkStraight(connected, visited);
        return;
      }
    }
  }


  @Override
  public String getAtom(final Integer position) {
    return this.getComponentsPositions().getAtom(position);
  }


  @Override
  public Integer getPosition(final String atom) {
    return this.getComponentsPositions().getPosition(atom);
  }


  @Override
  public Iterator<String> iterator() {
    return this.getComponentsPositions().iterator();
  }


  /**
   * Prints the positions of the components of this atom set.
   */
  public void printPositions() {
    Logger.logging(this.getId() + "\n"
                   + this.getComponentsPositions().toString());
  }


  @Override
  public String toString() {
    final String structure = super.toString();
    final Joiner joiner = Joiner.on(" ");
    return structure + "\nSuper Systems:" + joiner.join(this.getSuperSystems())
        + "\nSub Systems:" + joiner.join(this.getSubSystems())
        + "\nConnecting Atoms:" + joiner.join(this.getConnectingAtoms());
  }


  /**
   * Initialises the CML entry for this set.
   */
  private void makeCml() {
    this.cml = new CMLAtomSet();
    this.cml.setTitle(this.type.getName());
    this.cml.setId(this.getId());
  }


  @Override
  public CMLAtomSet getCml(final Document doc) {
    for (final IAtom atom : this.getStructure().atoms()) {
      final String atomId = atom.getID();
      final CMLAtom node = (CMLAtom) SreUtil.getElementById(doc, atomId);
      this.cml.addAtom(node);
    }
    this.attachAttribute("condensed", this.getStructuralFormula());
    this.attachAttribute("formula", this.getMolecularFormula());
    this.attachAttribute("name", super.getName());
    this.attachAttribute("iupac", this.getIupac());
    return this.cml;
  }


  /**
   * Attaches an attribute of a given name to the CML structure if the value is
   * not empty.
   *
   * @param attribute
   *          The attribute.
   * @param value
   *          Its value.
   */
  private void attachAttribute(final String attribute, final String value) {
    if (value != "") {
      this.cml.addAttribute(new SreAttribute(attribute, value));
    }
  }


  /**
   * @return  True if the atom set is a ring.
   */
  public boolean isRing() {
    return false;
  }


  @Override
  public void visualize() {
    final StructuralGraph graph = new StructuralGraph(this.getSubSystems());
    graph.visualize(this.getId());
  }


  @Override
  public SreNamespace.Tag tag() {
    return SreNamespace.Tag.ATOMSET;
  }


  /**
   * @return The naming structure.
   */
  public SpiderNames getNames() {
    return this.names;
  }

}
