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
 * @file LanguageFactory.java
 * @author Volker Sorge <a href="mailto:V.Sorge@progressiveaccess.com">Volker
 *         Sorge</a>
 * @date Sun Aug 2 12:58:22 2015
 *
 * @brief Factory for generating singltons of visitor and lookup tables.
 *
 *
 */

//

package com.progressiveaccess.cmlspeech.speech;

import com.progressiveaccess.cmlspeech.sre.SreException;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


/**
 * Factory for generating single instances of language specific tables and
 * visitors.
 *
 * @param <K>
 *          Generic parameter for singleton objects in the l10n folders.
 */

public class LanguageFactory<K> {

  private final Map<String, K> tables = new HashMap<>();
  private static final String PKG = "com.progressiveaccess.cmlspeech.speech";
  private String className;

  /**
   * Constructs a factory for language translation objects.
   *
   * @param className
   *          The class name of the objects to create.
   */
  LanguageFactory(final String className) {
    this.className = className;
  }


  /**
   * Retrieves a translation object for the given language.
   *
   * @param language
   *          The current language.
   *
   * @return A translation object.
   *
   * @throws ClassNotFoundException
   *          If the language is not implemented.
   * @throws SecurityException
   *          From class constructor call.
   * @throws NoSuchMethodException
   *          From class constructor call.
   * @throws InvocationTargetException
   *          From class constructor call.
   * @throws IllegalArgumentException
   *          From class constructor call.
   * @throws IllegalAccessException
   *          From class constructor call.
   * @throws InstantiationException
   *          From class constructor call.
   */
  @SuppressWarnings("unchecked")
  private K retrieve(final String language) throws ClassNotFoundException,
      InstantiationException, IllegalAccessException, IllegalArgumentException,
      InvocationTargetException, NoSuchMethodException, SecurityException {
    K table = tables.get(language);
    if (table != null) {
      return table;
    }
    Class<?> cls;
    cls = Class.forName(PKG + "." + language + "." + this.className);
    table = (K) cls.getConstructor().newInstance(new Object[] {});
    tables.put(language, table);
    return table;
  }


  /**
   * Constructs the bond table for the current language.
   *
   * @param language
   *          A language string.
   *
   * @return The bond table for the given language.
   */
  public K getLanguageVisitor(final String language) {
    try {
      return this.retrieve(language);
    } catch (Exception e) {
      System.out.println("Language class does not exist!");
      try {
        return this.retrieve("en");
      } catch (Exception f) {
        throw new SreException("Default language English does not exist!");
      }
    }
  }

}
