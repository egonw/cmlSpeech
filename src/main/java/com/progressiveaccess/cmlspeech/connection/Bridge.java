/**
 * @file   Bridge.java
 * @author Volker Sorge<a href="mailto:V.Sorge@progressiveaccess.com">
 *          Volker Sorge</a>
 * @date   Mon Jun 29 11:23:22 2015
 *
 * @brief  Bridge connections for sub rings.
 *
 */

//

package com.progressiveaccess.cmlspeech.connection;

import com.progressiveaccess.cmlspeech.sre.SreNamespace;
import com.progressiveaccess.cmlspeech.sre.XmlVisitor;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Bridge for a neighbouring fused ring is a combination of shared bonds and
 * bridge atoms.
 */

public class Bridge extends Connection {

  private final SortedSet<Connection> bridges = new TreeSet<Connection>(
      new ConnectionComparator());

  /**
   * Constructs a bridge.
   *
   * @param connectors The list of bridges.
   * @param connected Name of connected structure.
   */
  public Bridge(final List<Connection> connectors, final String connected) {
    super("a1", connected);
    bridges.addAll(connectors);
  }


  /**
   * @return List of shared bonds and bridge atoms.
   */
  public SortedSet<Connection> getBridges() {
    return bridges;
  }


  @Override
  public ConnectionType getType() {
    return ConnectionType.BRIDGE;
  }

  @Override
  public SreNamespace.Tag tag() {
    return SreNamespace.Tag.BRIDGE;
  }

  @Override
  public void accept(final XmlVisitor visitor) {
  }

  @Override
  public String toString() {
    String result = "\nBridge to " + this.getConnected() + " via:";
    for (Connection bridge : this.getBridges()) {
      result += bridge.toString();
    }
    return result;
  }

}
