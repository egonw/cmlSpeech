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
 * @file   SpiderExector.java
 * @author Volker Sorge
 *         <a href="mailto:V.Sorge@progressiveaccess.com">Volker Sorge</a>
 * @date   Tue Jul 21 20:12:00 2015
 *
 * @brief  Class for multi-threaded Spider call.
 *
 */

//

package com.progressiveaccess.cmlspeech.cactus;

import com.progressiveaccess.cmlspeech.base.Cli;
import com.progressiveaccess.cmlspeech.base.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Executes Spider calls and collects their results.
 */

public class SpiderExecutor {

  /** Pool of callables for Spider. */
  private final List<SpiderCallable> pool = new ArrayList<>();
  /** Registry for threads expecting results from Spider calls. */
  private final Multimap<SpiderCallable, Thread> registry =
      HashMultimap.create();
  /** The executor service that runs the callables. */
  private ThreadFactory executor;


  /**
   * Register callables for cactus in the pool.
   *
   * @param callable
   *          A callable to register.
   */
  public void register(final SpiderCallable callable) {
    this.pool.add(callable);
  }


  /** Execute all callables currently in the pool. */
  public void execute() {
    this.executor = Executors.defaultThreadFactory();
    Integer time = null;
    if (Cli.hasOption("time_nih")) {
      try {
        time = Integer.parseInt(Cli.getOptionValue("time_nih"));
      } catch (NumberFormatException e) {
        Logger.error("Spider Error: Illegal time format.\n");
      }
    }
    for (final SpiderCallable callable : this.pool) {
      final Thread thread = this.executor.newThread(callable);
      this.registry.put(callable, thread);
      thread.start();
      if (time != null) {
        try {
          Thread.sleep(time);
        } catch (final Throwable e) {
          Logger.error("Spider Error: " + e.getMessage() + "\n");
        }
      }
    }
    this.awaitResults();
  }


  /**
   * Awaits all the threads of the Spider naming service to complete.
   */
  private void awaitResults() {
    for (final Map.Entry<SpiderCallable, Thread> entry
           : this.registry.entries()) {
      final Thread thread = entry.getValue();
      try {
        thread.join();
      } catch (final Throwable e) {
        Logger.error("Spider Error: " + e.getMessage() + "\n");
        continue;
      }
    }
  }

}