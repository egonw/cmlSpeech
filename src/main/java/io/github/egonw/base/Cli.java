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
 * @file   Cli.java
 * @author Volker Sorge <sorge@zorkstone>
 * @date   Sat Feb 14 12:06:05 2015
 * 
 * @brief  Command line interface.
 * 
 * 
 */


//
package io.github.egonw.base;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Command line interface for enricher app.
 */

public final class Cli {

    private static CommandLine cl;

    private static List<String> files = new ArrayList<String>();

    protected Cli() {
    }

    public static void init(String[] args) {
	Cli.parse(args);
    }

    private static void parse( String[] args ) {
        Options options = new Options();
	// Basic Options
        options.addOption("help", false, "Print this message");
        options.addOption("d", "debug", false, "Debug mode");
	options.addOption("v", "verbose", false, "Verbose mode");
	// File Handling
	// options.addOption("i", "input", true, "Input File");
	options.addOption("o", "output", true, "Output file addition");
	options.addOption("l", "log", true, "Log File");
	options.addOption("x", "error", true, "Debug File");
	// Processing Options
        options.addOption("c", "cml", false,
                          "Also write a CML file without annotations: adds -simple to name");
        options.addOption("a", "ann", false, "Include annotations in CML output");
        options.addOption("r", "descr", false, "Include speech descriptions in CML output");
        options.addOption("nonih", "nonih", false, "Do not use the NIH naming service");
        options.addOption("s", "subrings", false, "Do not compute subrings");
        options.addOption("sssr", "sssr", false, "Use SSSR method for sub-ring computation");
        options.addOption("vis", "visualize", false, "Visualize the abstraction graph");
        options.addOption("vr", "vis_recursive", false,
                          "Visualize sub graphs recursively");
        options.addOption("vb", "vis_bw", false,
                          "Visualize graph black and white; default colour");
        options.addOption("vs", "vis_short", false, "Visualize bonds short");
        options.addOption("sf", "structuralformula", false, "Print the structural formula");
        options.addOption("sub", "subscript", false, "Use subscripts with structural formula");
        options.addOption("m", "molcom", true,
                          "Comparison heuristics for molecules given as a comma" +
                          "separated list. Currently available heuristics: type, weight, size");
        
        CommandLineParser parser = new BasicParser();
        try {
            Cli.cl = parser.parse(options, args);
        }
        catch (ParseException e) {
            usage(options, 1);
        }
        if (Cli.cl.hasOption("help")) {
            usage(options, 0);
        }

	for (int i = 0; i < Cli.cl.getArgList().size(); i++) {
	    String fileName = Cli.cl.getArgList().get(i).toString();
	    File f = new File(fileName);
	    if (f.exists() && !f.isDirectory()) {
		Cli.files.add(fileName);
	    } else {
		Cli.warning(fileName);
	    }
	}

    }

    private static void usage(Options options, int exitValue) {

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("enrich.sh", options);
        System.exit(exitValue);
    }

    private static void warning(String fileName) {
        System.err.println("Warning: File " 
                           + fileName + " does not exist. Ignored!");
    }


    public static boolean hasOption(String option) {
        return Cli.cl.hasOption(option);
    }


    public static String getOptionValue(String option) {
        return Cli.cl.getOptionValue(option);
    }

    public static List<String> getFiles() {
        return Cli.files;
    }    

}