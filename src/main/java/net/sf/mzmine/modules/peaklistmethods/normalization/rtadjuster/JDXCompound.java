/*
 * Copyright 2006-2014 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
//import java.util.Collections;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import java.io.FileReader;

//import org.apache.commons.lang.ArrayUtils;
import org.jcamp.parser.JCAMPException;
import org.jcamp.parser.JCAMPReader;
import org.jcamp.parser.JCAMPWriter;
import org.jcamp.spectrum.ArrayData;
import org.jcamp.spectrum.MassSpectrum;
import org.jcamp.spectrum.OrderedArrayData;
import org.jcamp.spectrum.Spectrum;
import org.jcamp.spectrum.notes.Note;
import org.jcamp.spectrum.notes.NoteDescriptor;
import org.jcamp.units.AliasUnit;
import org.jcamp.units.BaseUnit;
import org.jcamp.units.CommonUnit;
import org.jcamp.units.DerivedUnit;

import com.google.common.primitives.Doubles;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

//import jspecview.source.;


public class JDXCompound extends SimplePeakIdentity {

	// Logger.
	private static final Logger LOG = Logger.getLogger(JDXCompound.class.getName());

	public static final int MAX_MZ = 800; 
	
    //private final URL compoundUrl;
    private final /*URL*/ File compoundJDXfile;
    //private final URL structure2dUrl;
    //private final URL structure3dUrl;
    private double bestScore;
    //private IsotopePattern isotopePattern;
    //private final OnlineDatabase database;
    private final String id;
    private final String name;
    private final String formula;
    private final double[] spectrum;
    private double maxY;
    
    private int minX, maxX;

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    public static final JDXCompound UNKNOWN_JDX_COMP = new JDXCompound("Unknown", null, null, null, null);
    
    /**
     * @param db      the database the compound is from.
     * @param id      the compound's ID in the database.
     * @param name    the compound's formula.
     * @param formula the compound's name.
     * @param urlDb   the URL of the compound in the database.
     * @param url2d   the URL of the compound's 2D structure.
     * @param url3d   the URL of the compound's 3D structure.
     */
    public JDXCompound(final String name,				/* TITLE */
                      final String id,					/* CAS REGISTRY NO */
                      final double[] spectrum,			/* PEAK TABLE=(XY..XY) */
                      final String formula,				/* MOLFORM */
                      final /*URL*/ File jdxFile) {			// Could have an URL, but let's do it locally using '.jdx' file path, for now.

        super(name, formula, "MZ/RT weighted matching search", id, (jdxFile != null) ? jdxFile.getPath() : null);
        
//        compoundUrl = urlDb;
//        structure2dUrl = url2d;
//        structure3dUrl = url3d;
//        isotopePattern = null;
        this.compoundJDXfile = jdxFile;
        this.id = id;
        this.name = name;
        this.formula = formula;
        this.spectrum = spectrum;
        this.maxY = (spectrum != null) ? Doubles.max(spectrum) : 0.0;		/* MAXY */
        this.bestScore = 0.0;
    }
    
    public static JDXCompound parseJDXfile(final /*URL*/ File jdxFile) throws JCAMPException {
    	JDXCompound jdxComp = null;
        String id = null;
        String name = null;
        String formula = null;
        double[] spectrum = new double[MAX_MZ];
        Arrays.fill(spectrum, 0.0);
        
        Spectrum jcampSpectrum = null;
        
        try {
            StringBuffer fileData = new StringBuffer(1000);
            BufferedReader reader = new BufferedReader(new FileReader(jdxFile));
            char[] buf = new char[1024];
            int numRead=0;

            while((numRead=reader.read(buf)) != -1){
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
            reader.close();


            jcampSpectrum = JCAMPReader.getInstance().createSpectrum(fileData.toString());
            //	        if (!(jcampSpectrum instanceof NMRSpectrum)) {
            //	        	throw new Exception("Spectrum in file is not an NMR spectrum!");
            //	        }
            //        NMRSpectrum nmrspectrum = (NMRSpectrum) jcampSpectrum;
            //        if (nmrspectrum.hasPeakTable()) {
            //        	assertEquals(nmrspectrum.getPeakTable().length,16384);
            //        }
            
            // Recover formula among parsed information
        	Iterator notesIt = jcampSpectrum.getNotes().iterator();
        	while (notesIt.hasNext()) {
        		Note note = (Note) notesIt.next();
        		NoteDescriptor descr = note.getDescriptor();
        		//if ("title".equals(descr.getKey()))
        		//System.out.println(descr.getKey() + " => " + note.getValue());
        		if ("molform".equals(descr.getKey()))
        			formula = note.getValue().toString();
        	}

        } catch (FileNotFoundException e) {
            throw new JCAMPException("File not found: " + e.getMessage());
        } catch (IOException | JCAMPException e) {
            throw new JCAMPException("JDX parsing error: " + e.getMessage());
        } catch (Exception e) {
            throw new JCAMPException("Unknown error: " + e.getMessage());
        }

//        String jcamp = null;
//        try {
//            FileReader dxIn = new FileReader(jdxFile);
//            if (dxIn != null) {
//                int size;
//                int read;
//                int r;
//                char[] data;
//                size = (int) jdxFile.length();
//                read = 0;
//                data = new char[size];
//                do {
//                    r = dxIn.read(data, read, size - read);
//                    read += r;
//                } while (r > 0);
//                dxIn.close();
//                jcamp = String.valueOf(data);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            JCAMPBlock block = new JCAMPBlock(jcamp);
//            System.out.println("Block Data:\n" + block.getType());
//            //System.out.println("Child Blocks: " + block.childBlocks.size());
//        } catch (JCAMPException e) {
//            e.printStackTrace();
//        }

        MassSpectrum msSpectrum = (MassSpectrum) jcampSpectrum;
        
        name = msSpectrum.getTitle();
        
        //if (jcampSpectrum != null) {
        //      LOG.info("This f...g lib is working and can parse : " + name);
        //}
//        spectrum = msSpectrum.getYData().toArray();
//        LOG.info("spectrum: " + Arrays.toString(spectrum));
        
//        Range1D.Double r = ((MassSpectrum)jcampSpectrum).getXFullViewRange();
//        r.set(0.0, 300.0);
//        ((MassSpectrum)jcampSpectrum).setXFullViewRange(r);
//        spectrum = ((MassSpectrum)jcampSpectrum).getYData().toArray();
        
        // Full fill the spectrum
//        int minX = 0, maxX = 0;
        for (int i=0; i < msSpectrum.getXData().getLength(); ++i) {
//        	if (msSpectrum.getYData().pointAt(i) > 0.0) {
//        		if (minX == 0) { minX = i; }
//        		else { maxX = i; }
//        	}
        	spectrum[(int) Math.round(msSpectrum.getXData().pointAt(i))] = msSpectrum.getYData().pointAt(i);
        }
        
        jdxComp = new JDXCompound(name, id, spectrum, formula, jdxFile);
        //jdxComp.maxY = Doubles.max(spectrum);
        
//        jdxComp.minX = minX;
//        jdxComp.maxX = maxX;
        jdxComp.minX = (int) Math.round(msSpectrum.getXData().pointAt(0));
        jdxComp.maxX = (int) Math.round(msSpectrum.getXData().pointAt((msSpectrum.getXData().getLength()-1)));
        
    	return jdxComp;
    }
    
    public static boolean saveAsJDXfile(final String title, final DataPoint[] dataPoints, final File jdxFile) throws JCAMPException {
        
        String id = null;
        String name = null;
        String formula = null;
        
        double[] spectrum_x = new double[dataPoints.length];
        double[] spectrum_y = new double[dataPoints.length];
        ///Arrays.fill(spectrum, 0.0);
        
        Spectrum jcampSpectrum = null;
        
        BufferedWriter writer = null;
        try {
            
            // If file doesnt exists, then create it
            if (!jdxFile.exists()) { jdxFile.createNewFile(); }


            for (int i=0; i < dataPoints.length; ++i) {
                spectrum_x[i] = dataPoints[i].getMZ();
                spectrum_y[i] = dataPoints[i].getIntensity();
            }
            
            jcampSpectrum = new MassSpectrum(
                    new OrderedArrayData(spectrum_x, new DerivedUnit("", "m/z", "m/z")), //CommonUnit.mz), //new AliasUnit(BaseUnit.generic, "m/z")), 
                    new ArrayData(spectrum_y, new DerivedUnit("", "relative intensity", "relative intensity"))//CommonUnit.intensity) //new AliasUnit(BaseUnit.generic, "relative intensity"))
                    );
            
            //jcampSpectrum = JCAMPReader.getInstance().createSpectrum(fileData.toString());
            jcampSpectrum.setTitle(title);
            
            String jdxText = JCAMPWriter.getInstance().toJCAMP(jcampSpectrum);
            
            System.out.println(jdxText);
            
            writer = new BufferedWriter(new FileWriter(jdxFile.getAbsoluteFile()));
            writer.write(jdxText);
            
        } catch (FileNotFoundException e) {
            throw new JCAMPException("File not found: " + e.getMessage());
        } catch (IOException | JCAMPException e) {
            throw new JCAMPException("JDX writing error: " + e.getMessage());
        } catch (Exception e) {
            throw new JCAMPException("Unknown error: " + e.getMessage());
        } finally {
            try {
                if (writer != null) writer.close();
            } catch ( IOException e) {
                throw new JCAMPException("JDX writing error: " + e.getMessage());
            }
        }        
        return true;
    }
    

    public double[] getSpectrum() {
    	return this.spectrum;
    }
    
    public String getFormula() {
    	return this.formula;
    }
    
    public int getMinMZ() {
    	return this.minX;
    }
    public int getMaxMZ() {
    	return this.maxX;
    }
    
    /**
     * Returns the isotope pattern score or null if the score was not calculated.
     *
     * @return isotope pattern score.
     */
    public Double getBestScore() {

        return bestScore;
    }

    /**
     * Set the isotope pattern score. of this compound.
     *
     * @param score the score.
     */
    public void setBestScore(final double score) {

        bestScore = score;
    }
    
    public static JDXCompound createUnknownCompound() {
        return new JDXCompound(UNKNOWN_JDX_COMP.getName(), null, null, null, null);
    }

    public static boolean isKnownIdentity(PeakIdentity peakId) {
        ////return (peakId != null && peakId.getName() != UNKNOWN_JDX_COMP.getName());
        return (peakId != null && !peakId.getName().equals(UNKNOWN_JDX_COMP.getName()));
    }
    
    public static void setPreferredPeakIdentity(PeakListRow row, PeakIdentity identity) {

        if (identity == null)
            return;

        // Make sure we don not set an existing identity twice
        // (PeakListRow.setPreferredPeakIdentity() doesn't guaranty that!)
        for (PeakIdentity p : row.getPeakIdentities()) {
            if (p.getName().equals(identity.getName())) {
                row.setPreferredPeakIdentity(p);
                return;
            }
        }
        
        row.setPreferredPeakIdentity(identity);
        
//        preferredIdentity = identity;
//
//        if (!identities.contains(identity)) {
//            identities.add(identity);
//        }

    }

    
//    @Override
//    public String toString() {
//    	String thisAsHashCode = this.getClass().getName() + "@" + Integer.toHexString(hashCode()); 
//		return thisAsHashCode + "[ name=" + name + ", formula=" + formula + ", spectrum=" + Arrays.toString(spectrum) + ", max=" + maxY + "]";
//    }

    @Override
    public boolean equals(Object peakIdentity) {
    	if (peakIdentity instanceof PeakIdentity && this.name.equals(((PeakIdentity)peakIdentity).getName())) {
    		return true;
    	}
    	return false;
    }
    
    @Override
    public synchronized @Nonnull Object clone() {

        JDXCompound jdxCompound = new JDXCompound(getName(),
                                                 getPropertyValue(PROPERTY_ID),
                                                 spectrum,
                                                 getPropertyValue(PROPERTY_FORMULA),
                                                 compoundJDXfile);
//        LOG.info("Original hash: " + this.getClass().getName() + "@" + Integer.toHexString(hashCode()));
//        LOG.info("Clone hash: " + jdxCompound.getClass().getName() + "@" + Integer.toHexString(jdxCompound.hashCode()));
        jdxCompound.setBestScore(bestScore);
        return jdxCompound;
    }
}
