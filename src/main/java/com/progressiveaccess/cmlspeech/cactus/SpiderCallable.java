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
 * @file   SpiderCallable.java
 * @author Volker Sorge
 *         <a href="mailto:V.Sorge@progressiveaccess.com">Volker Sorge</a>
 * @date   Tue Jul 21 20:12:00 2015
 *
 * @brief  Callable class for Spider Futures.
 *
 */

//

package com.progressiveaccess.cmlspeech.cactus;

import com.progressiveaccess.cmlspeech.base.Logger;

import org.openscience.cdk.interfaces.IAtomContainer;


/**
 * Callables for Spider Futures.
 */
public class SpiderCallable implements Runnable {

  private final String id;
  private final IAtomContainer container;
  private final SpiderNames names;

  /**
   * Constructs a thread for calls to Cactus web service.
   *
   * @param id
   *          Name of the thread.
   * @param container
   *          Atom container for query.
   * @param names
   *          The naming structure.
   */
  public SpiderCallable(final String id, final IAtomContainer container,
                        final SpiderNames names) {
    super();
    this.id = id;
    this.container = container;
    this.names = names;
  }


  /**
   * @return The id of the callable thread.
   */
  public String getId() {
    return this.id;
  }


  @Override
  public void run() throws CactusException {
    Logger.logging("Executing Spider call for " + this.id + "\n");
    Spider.getNames(this.container, this.names);
  }

}