/**
 * @file   CMLEnricher.java
 * @author Volker Sorge <sorge@zorkstone>
 * @date   Mon Apr 28 01:41:35 2014
 * 
 * @brief  Main class to enrich CML files.
 * 
 * 
 */

package io.github.egonw;

import com.google.common.collect.Lists;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Nodes;

import org.apache.commons.io.FilenameUtils;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.io.CMLWriter;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.descriptors.molecular.LongestAliphaticChainDescriptor;
import org.openscience.cdk.ringsearch.AllRingsFinder;
import org.openscience.cdk.ringsearch.RingSearch;
import org.openscience.cdk.ringsearch.SSSRFinder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.element.CMLAtom;
import org.xmlcml.cml.element.CMLAtomSet;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import nu.xom.Namespace;
import nu.xom.Element;
import nu.xom.Attribute;
import nu.xom.Node;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import java.util.Collection;
import org.openscience.cdk.interfaces.IBond;

public class CMLEnricher {
    private final Cli cli;
    private final Logger logger;

    private Document doc;
    private IAtomContainer molecule;
    private int atomSetCount;
    private CactusExecutor executor = new CactusExecutor();


    /** 
     * Constructor
     * 
     * @param initCli The interpreted command line.
     * @param initLogger The logger structure.
     * 
     * @return The newly created object.
     */    
    public CMLEnricher(Cli initCli, Logger initLogger) {
        cli = initCli;
        logger = initLogger;
    }


    /** 
     * Enriches all CML files given as input arguments.
     * 
     */
    public void enrichFiles() {
        for (String file : cli.files) {
            enrichFile(file);
        }
    }


    /** 
     * Convenience method to enrich a CML file. Does all the error catching.
     * 
     * @param fileName File to enrich.
     */
    private void enrichFile(String fileName) {
        this.atomSetCount = 0;
        try {
            readFile(fileName);
            buildXOM();
            enrichCML();
            nameMolecule(this.doc.getRootElement().getAttribute("id").getValue(), this.molecule);
            executor.execute();
            executor.addResults(this.doc, this.logger);
            writeFile(fileName);
            executor.shutdown();
        } catch (Exception e) { 
            // TODO: Meaningful exception handling by exceptions/functions.
            this.logger.error("Something went wrong when parsing File " + fileName +
                              ":" + e.getMessage() + "\n");
            return;
        }
    }


    /** 
     * Loads current file into the molecule IAtomContainer.
     * 
     * @param fileName File to load.
     * 
     * @throws IOException Problems with loading file.
     * @throws CDKException Problems with CML file format.
     */
    private void readFile(String fileName) throws IOException, CDKException {
        InputStream file = new BufferedInputStream
            (new FileInputStream(fileName));
        ISimpleChemObjectReader reader = new ReaderFactory().createReader(file);
        IChemFile cFile = null;
        cFile = reader.read(SilentChemObjectBuilder.getInstance().
                            newInstance(IChemFile.class));
        reader.close();
        this.molecule = ChemFileManipulator.getAllAtomContainers(cFile).get(0);
        this.logger.logging(this.molecule);
    }


    /** 
     * Build the CML XOM element.
     * 
     * @throws IOException Problems with StringWriter
     * @throws CDKException Problems with CMLWriter
     * @throws ParsingException Problems with building CML XOM.
     */
    private void buildXOM() throws IOException, CDKException, ParsingException {
        StringWriter outStr = new StringWriter();
        CMLWriter cmlwriter = new CMLWriter(outStr);
        cmlwriter.write(this.molecule);
        cmlwriter.close();
        String cmlcode = outStr.toString();

        Builder builder = new CMLBuilder(); 
        this.doc = builder.build(cmlcode, "");
        this.logger.logging(this.doc.toXML());
    }


    /** 
     * Writes current document to a CML file.
     * 
     * @param fileName
     *
     * @throws IOException Problems with opening output file.
     * @throws CDKException Problems with writing the CML XOM.
     */
    private void writeFile(String fileName) throws IOException, CDKException {
        FilenameUtils fileUtil = new FilenameUtils();
        String basename = fileUtil.getBaseName(fileName);
        OutputStream outFile = new BufferedOutputStream
            (new FileOutputStream(basename + "-enr.cml"));
        PrintWriter output = new PrintWriter(outFile);
        this.doc.getRootElement().addNamespaceDeclaration
            (SreNamespace.getInstance().prefix, SreNamespace.getInstance().uri);
        output.write(this.doc.toXML());
        output.flush();
        output.close();
    }


    /** Enriches the current CML documment. */
    private void enrichCML() {
        RingSearch ringSearch = new RingSearch(this.molecule);

        if (this.cli.cl.hasOption("s")) {
            getFusedRings(ringSearch);
        } else {
            getFusedRings(ringSearch, this.cli.cl.hasOption("sssr") ?
                          (ring) -> sssrSubRings(ring) : 
                          (ring) -> smallestSubRings(ring));
        }
        getIsolatedRings(ringSearch);
        //Object chain = getAliphaticChain();
        //this.logger.logging(chain);
    }


    /**
     * Creates a deep clone of an atom container catching possible errors.
     * @param container The container to be cloned.
     * @return The cloned container. Possibly null if cloning failed!
     */
    private IAtomContainer checkedClone(IAtomContainer container) {
        IAtomContainer newcontainer = null;
        try {
            newcontainer = container.clone();
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(newcontainer);
            CDKHydrogenAdder.getInstance(SilentChemObjectBuilder.getInstance()).addImplicitHydrogens(newcontainer);
        } catch (CloneNotSupportedException e){
            this.logger.error("Something went wrong cloning atom container: " + e.getMessage());
        } catch (Throwable e) {
            this.logger.error("Error " + e.getMessage());
        }
        return newcontainer;
    }


    /**
     * Computes the longest aliphatic chain for the molecule.
     * @return The value of the aliphatic chain.
     */
    private Object getAliphaticChain() {
        IAtomContainer container = checkedClone(this.molecule);
        if (container == null) { return 0; }
        LongestAliphaticChainDescriptor chain = 
            new LongestAliphaticChainDescriptor();
        DescriptorValue result = chain.calculate(container);
        this.logger.logging(result.getValue());
        return(result.getValue());
    }   
    

    /** 
     * Computes Isolated rings.
     * 
     * @param ringSearch The current ringsearch.
     */
    private void getIsolatedRings(RingSearch ringSearch) {
        List<IAtomContainer> ringSystems = ringSearch.isolatedRingFragments();
        for (IAtomContainer ring : ringSystems) {
            appendAtomSet("Isolated ring", ring);
        }
    }


    /**
     * Computes fused rings without subsystems.
     * 
     * @param ringSearch The current ringsearch.
     */    
    private void getFusedRings(RingSearch ringSearch) {
        List<IAtomContainer> ringSystems = ringSearch.fusedRingFragments();
        for (IAtomContainer ring : ringSystems) {
            appendAtomSet("Fused Ring", ring);
        }
    }


    /** 
     * Computes fused rings and their subsystems.
     * 
     * @param ringSearch 
     * @param subRingMethod Method to compute subrings.
     */    
    private void getFusedRings(RingSearch ringSearch,
                               Function<IAtomContainer, List<IAtomContainer>> 
                               subRingMethod) {
        List<IAtomContainer> ringSystems = ringSearch.fusedRingFragments();
        for (IAtomContainer ring : ringSystems) {
            String ringId = appendAtomSet("Fused ring", ring);
            List<IAtomContainer> subRings = subRingMethod.apply(ring);
            for (IAtomContainer subRing : subRings) {
                appendAtomSet("Subring", subRing, ringId);
            }
        }
    }


    /** 
     * Predicate that tests if a particular ring has no other ring as proper subset.
     * 
     * @param ring The ring to be tested.
     * @param restRings The other rings (possibly including the first ring).
     * 
     * @return True if ring has smallest coverage.
     */
    // This is quadratic and should be done better!
    // All the iterator to list operations should be done exactly once!
    private static boolean isSmallest(IAtomContainer ring, 
                                      List<IAtomContainer> restRings) {
        List<IAtom> ringAtoms = Lists.newArrayList(ring.atoms());
        for (IAtomContainer restRing : restRings) {
            if (ring == restRing) {
                continue;
            }
            List<IAtom> restRingAtoms = Lists.newArrayList(restRing.atoms());
            if (ringAtoms.containsAll(restRingAtoms)) {
                return false;
            };
        }
        return true;
    };


    /** 
     * Method to compute smallest rings via subset coverage.
     * 
     * @param ring Fused ring to be broken up.
     * 
     * @return Subrings as atom containers.
     */    
    private List<IAtomContainer> smallestSubRings(IAtomContainer ring) {
        AllRingsFinder arf = new AllRingsFinder();
        List<IAtomContainer> subRings = new ArrayList<IAtomContainer>();
        IRingSet rs;
        try {
            rs = arf.findAllRings(ring);
        } catch (CDKException e) {
            this.logger.error("Error " + e.getMessage());
            return subRings;
        }
        // TODO: Refactor, as we don't need the i anymore.
        List<IAtomContainer> allRings = Lists.newArrayList(rs.atomContainers());
        int length = allRings.size();
        for (int i = 0; i < length; i++) {
            IAtomContainer subRing = allRings.get(i);
            if (isSmallest(subRing, allRings)) {
                subRings.add(subRing);
            }
        }
        return subRings;
    };


    /** 
     * Method to compute smallest rings via SSSR finder.
     * 
     * @param ring Fused ring to be broken up.
     * 
     * @return Subrings as atom containers.
     */
    private List<IAtomContainer> sssrSubRings(IAtomContainer ring) {
        this.logger.logging("SSSR sub ring computation.\n");
        SSSRFinder sssr = new SSSRFinder(ring);
        IRingSet essentialRings = sssr.findSSSR();
        return Lists.newArrayList(essentialRings.atomContainers());
    }


    /**
     * Returns atom set id and increments id counter.
     * @return A new unique atom set id.
     */
    private String getAtomSetId() {
        atomSetCount++;
        return "as" + atomSetCount;
    }


    /** 
     * Append an Atom Set to the CML documents.
     * 
     * @param title Title of the atom set to be added. 
     * @param atoms Iterable atom list.
     * 
     * @return The atom set id.
     */
    private String appendAtomSet(String title, IAtomContainer container) {
        CMLAtomSet set = new CMLAtomSet();
        String id = getAtomSetId();
        set.setTitle(title);
        set.setId(id);
        this.logger.logging(title + " has atoms:");
        // TODO (sorge) Refactor that eventually together with appendAtomSet.
        for (IAtom atom : container.atoms()) {
            String atomId = atom.getID();
            Element node = SreUtil.getElementById(this.doc, atomId);
            SreUtil.appendAttribute(node, "componentOf", id);
            set.addAtom((CMLAtom)node);
            this.logger.logging(" " + atomId);
        }
        this.logger.logging("\n");
        for (IBond bond : container.bonds()) {
            String bondId = bond.getID();
            Element node = SreUtil.getElementById(this.doc, bondId);
            SreUtil.appendAttribute(node, "componentOf", id);
            SreUtil.appendAttribute(set, "bonds", bondId);
        }
        this.doc.getRootElement().appendChild(set);
        nameMolecule(id, container);
        return(id);
    };


    /** 
     * Append an Atom Set to the CML documents.
     * 
     * @param title Title of the atom set to be added. 
     * @param atoms Iterable atom list.
     * @param superSystem Id of the super set.
     * 
     * @return The atom set id.
     */
    private String appendAtomSet(String title, IAtomContainer atoms, String superSystem) {
        String id = appendAtomSet(title, atoms);
        Element sup = SreUtil.getElementById(this.doc, superSystem);
        Element sub = SreUtil.getElementById(this.doc, id);
        SreUtil.appendAttribute(sup, "subsystem", id);
        SreUtil.appendAttribute(sub, "supersystem", superSystem);
        return(id);
    };


    /**
     * Computes some names for a molecule by registering calls to Cactus.
     * @param id The id of the atom set.
     * @param container The molecule to be named.
     */
    private void nameMolecule(String id, IAtomContainer container) {
        // TODO: catch the right exception.
        this.logger.logging("Registering calls for " + id);
        IAtomContainer newcontainer = checkedClone(container);
        if (newcontainer != null) {
            this.executor.register(new CactusCallable(id, Cactus.Type.IUPAC, newcontainer));
            this.executor.register(new CactusCallable(id, Cactus.Type.NAME, newcontainer));
            this.executor.register(new CactusCallable(id, Cactus.Type.FORMULA, newcontainer));
        }
    }

}