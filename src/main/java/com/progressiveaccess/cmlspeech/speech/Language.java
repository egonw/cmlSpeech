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
 * @file Language.java
 * @author Volker Sorge <a href="mailto:V.Sorge@progressiveaccess.com">Volker
 *         Sorge</a>
 * @date Thu Jul 30 05:04:37 2015
 *
 * @brief Abstract Factory that holds localisable visitors, etc.
 *
 *
 */

//

package com.progressiveaccess.cmlspeech.speech;


/**
 * Holds the visitors producing speech output.
 */

public final class Language {

  private static String currentLanguage = "en";
  private static AtomTable atomTable;
  private static BondTable bondTable;
  private static FunctionalGroupTable fgTable;
  private static SpeechVisitor expertSpeechVisitor;
  private static SpeechVisitor simpleSpeechVisitor;



  /** Dummy constructor. */
  private Language() {
    throw new AssertionError("Instantiating utility class...");
  }


  /**
   * @return The iso abbreviation for the current language.
   */
  public static String getLanguage() {
    return Language.currentLanguage;
  }


  /**
   * @return The atom table for the current language.
   */
  public static AtomTable getAtomTable() {
    return Language.atomTable;
  }


  /**
   * @return The bond table for the current language.
   */
  public static BondTable getBondTable() {
    return Language.bondTable;
  }


  /**
   * @return The functional group table for the current language.
   */
  public static FunctionalGroupTable getFunctionalGroupTable() {
    return Language.fgTable;
  }


  /**
   * @return The simple speech visitor for the current language.
   */
  public static SpeechVisitor getSimpleSpeechVisitor() {
    return Language.simpleSpeechVisitor;
  }


  /**
   * @return The expert speech visitor for the current language.
   */
  public static SpeechVisitor getExpertSpeechVisitor() {
    return Language.expertSpeechVisitor;
  }


  /**
   * Resets the language object to a specific language.
   *
   * @param language
   *          The new language.
   */
  public static void reset(final String language) {
    currentLanguage = IsoTable.lookup(language);
    atomTable = AtomTableFactory.getAtomTable(currentLanguage);
    bondTable = BondTableFactory.getBondTable(currentLanguage);
    fgTable = FunctionalGroupTableFactory
        .getFunctionalGroupTable(currentLanguage);
    simpleSpeechVisitor =
        SimpleSpeechVisitorFactory.getSpeechVisitor(currentLanguage);
    expertSpeechVisitor =
        ExpertSpeechVisitorFactory.getSpeechVisitor(currentLanguage);
  }

}
