package org.gnf.clustering;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.text.NumberFormat;
import java.util.Locale;

import org.gnf.clustering.classic.*;
import org.gnf.clustering.hybrid.*;
import org.gnf.clustering.sequentialcache.SequentialCacheClustering;
import org.apache.commons.cli.*;

public class RunCommandLine
{
	private RunCommandLine() {}
 
	private static CommandLine optPareser(String[] args) throws ParseException 
	{
	
		Options options = new Options();	
		
		Option optEis = new Option("eis","Input data is in Eisen format.");
				
		Option optI = OptionBuilder.withArgName("file").hasArg().isRequired(false).
				withDescription("Input file name.").create("i");
	
		//Option optD = new Option("d","Flag to indicate that the input is a distance matrix.");	
		
		Option optD = OptionBuilder.withArgName("file").hasArg().isRequired(false).
				withDescription("File with a precomputed distance matrix. You still need to\n\t\tspecify -i option for a .cdt file to be generated.").create("d");
	
		Option optO = OptionBuilder.withArgName("file").hasArg().isRequired(false).
				withDescription("Prefix for the output .gtr and .cdt files. If not specified, the\n\t\tdefault prefix is set to the input file's name minus extension\n\t\t(or distance matrix/fingerprints file if -i option is not\n\t\tprovided).").create("o");
		
		Option optN = OptionBuilder.withArgName("int").hasArg().isRequired(false).
				withDescription("The number of data points to cluster. Computed automatically\n\t\twhen not specified.").create("n");
		
		Option optM = OptionBuilder.withArgName("int").hasArg().isRequired(false).
				withDescription("Dimensionality of the data. Computed automatically when not\n\t\tspecified.").create("m");
		
		Option optSr = OptionBuilder.withArgName("int").hasArg().isRequired(false).
				withDescription("The number of rows to skip in the input file (headers). The\n\t\tdefault is 0, or computed automatically when the input is in\n\t\tEisen format (-eis option specified).").create("sr");
		Option optSc = OptionBuilder.withArgName("int").hasArg().isRequired(false).
				withDescription("The number of columns to skip in the input file (data ids,\n\t\tdescriptions, etc). The default is 0, or computed automatically\n\t\twhen the input is in Eisen format (-eis option specified).").create("sc");
		
		//Option optF = new Option("f","Flag to indicate that the input is fingerprints. Currently only\n\t\t32 bit signed integer representation is supported.");
		Option optF = OptionBuilder.withArgName("file").hasArg().isRequired(false).
				withDescription("File with structural fingerprints. Currently only 32 bit signed\n\t\tinteger representation is supported. You still need to specify\n\t\t-i option for a .cdt file to be generated.").create("f");
	
		
		Option optS = OptionBuilder.withArgName("sep").hasArg().isRequired(false).
				withDescription("Separator between the data entries. The default is tab (\"\\t\").").create("sep");
	
	
		Option optE = new Option("e","Euclidean distance. Default.");
		Option optT = new Option("t","Tanimoto distance.");
		Option optP = new Option("p","Pearson correlation.");
		Option optPa = new Option("pa","Absolute value of Pearson correlation.");
		OptionGroup dist = new OptionGroup(); 
		dist.addOption(optE);
		dist.addOption(optT);
		dist.addOption(optP);
		dist.addOption(optPa);
	
		Option optAvg = new Option("avg","Average linkage. Default.");
		Option optMin = new Option("min","Minimum (single) linkage.");
		Option optMax = new Option("max","Maximum (complete) linkage.");
		OptionGroup link = new OptionGroup(); 
		link.addOption(optAvg);
		link.addOption(optMin);
		link.addOption(optMax);
	
		Option optK = OptionBuilder.withArgName("k").hasArg().isRequired(false).
			withDescription("The number of partitions (k in k-means clustering). \n\t\tDefault value is computed automatically.").create("k");
	
		Option optC = new Option("hybrid","Use hybrid clustering clustering. The default is standard\n\t\thierarchical clustering.");

		Option optHelp = new Option( "help", "Print help message.");

	
		/// Generate help message
	
	
		String s = "Usage:\n";
	
		s+= " -" + optHelp.getOpt() + "\t\t" +  optHelp.getDescription() + "\n\n";
		
		s+= "Input options. If -i option specified, both .gtr and .cdt files are produced.\nIf either -d or -f "
				+ "option is specified without -i option, only a .gtr file is\nproduced. If either -d or -f option os specified together "
				+ "with -i option,\ndistance matrix or fingerprints files are used for clustering, and a .cdt\nfile "
				+ "is produced. -m, -n, -eis, -sr, -sc and -sep options are related to the\ninput file -i. Distance matrix and "
				+ "fingerprints files are parsed automatically.\n\n";
		
	
		s += " -" + optI.getOpt() + " <" + optI.getArgName() + ">\t" +  optI.getDescription() + "\n";
		options.addOption(optI);
	
		s += " -" + optD.getOpt() + " <" + optD.getArgName() + ">\t" +  optD.getDescription() + "\n";
		options.addOption(optD);
		
		s += " -" + optF.getOpt() + " <" + optF.getArgName() + ">\t" +  optF.getDescription() + "\n";
		options.addOption(optF);
		
		s += " -" + optEis.getOpt() + "\t\t" +  optEis.getDescription() + "\n";
		options.addOption(optEis);
		
		
		s += " -" + optN.getOpt() + " <" + optN.getArgName() + ">\t" +  optN.getDescription() + "\n";
		options.addOption(optN);
		s += " -" + optM.getOpt() + " <" + optM.getArgName() + ">\t" +  optM.getDescription() + "\n";
		options.addOption(optM);
		
	
		s += " -" + optSr.getOpt() + " <" + optSr.getArgName() + ">\t" +  optSr.getDescription() + "\n";
		options.addOption(optSr);
		s += " -" + optSc.getOpt() + " <" + optSc.getArgName() + ">\t" +  optSc.getDescription() + "\n";
		options.addOption(optSc);
		s += " -" + optS.getOpt() + " <" + optS.getArgName() + ">\t" +  optS.getDescription() + "\n\n";
		options.addOption(optS);
		
		s += " -" + optO.getOpt() + " <" + optO.getArgName() + ">\t" +  optO.getDescription() + "\n";
		options.addOption(optO);
		
		s += " -" + optC.getOpt() + "\t" +  optC.getDescription() + "\n";
		s += " -" + optK.getOpt() + " <" + optK.getArgName() + ">\t\t" +  optK.getDescription() + "\n\n";
	
	
		s+= "Distance options (mutually exclusive. The default is Euclidean. When -f option\nis specified, the default is Tanimoto).\n\n";
	
		s += " -" + optE.getOpt() + "\t\t" +  optE.getDescription() + "\n";
		s += " -" + optP.getOpt() + "\t\t" +  optP.getDescription() + "\n";
		s += " -" + optPa.getOpt() + "\t\t" +  optPa.getDescription() + "\n";
		s += " -" + optT.getOpt() + "\t\t" +  optT.getDescription() + "\n\n";
	
		options.addOptionGroup(dist);
	
		s+= "Hierarchical clustering linkage options (mutually exclusive, the default is\naverage linkage).\n\n";
		s += " -" + optAvg.getOpt() + "\t\t" +  optAvg.getDescription() + "\n";
		s += " -" + optMin.getOpt() + "\t\t" +  optMin.getDescription() + "\n";
		s += " -" + optMax.getOpt() + "\t\t" +  optMax.getDescription() + "\n\n";
	
		options.addOptionGroup(link);
	
		
	
		options.addOption(optK);
		options.addOption(optC);
		options.addOption(optHelp);
					
	
		CommandLineParser parser = new GnuParser();
		CommandLine cmd = parser.parse(options, args);
	
		if (cmd.hasOption("help"))
		{
			System.out.println(s);
		
		} 
		return cmd;
	}
	
	public static long GetUsedMemory()
	{
	MemoryUsage u = null;
	final java.util.List<MemoryPoolMXBean> listPools = ManagementFactory.getMemoryPoolMXBeans();
	MemoryPoolMXBean beanPool = null;
	long nUsed = 0;
	for(int nPool = 0; nPool < listPools.size(); ++nPool)
	{
	beanPool = listPools.get(nPool);
	u = beanPool.getUsage();
	nUsed += u.getUsed();
	}
	return nUsed;
	}

 
 
	public static void main(String[] args) throws NumberFormatException, IOException, ParseException 
	{
					    		
		long start = System.currentTimeMillis();
		CommandLine cmd = optPareser(args);
		if (cmd.hasOption("help")) return;
		Node[] arNodes = null;
		
		String[] rowNames = null;
		String[] colNames = null;
		
		DataSource source = null;
		
		String inputFilename = "";
		
		if(cmd.hasOption("i"))
			inputFilename = cmd.getOptionValue("i"); 
		//else 
		//	System.out.println("Please, specify the input file. Use -help option to see the usage instruction.");
		
		int skipRows = 0;
		int skipCols = 0;
		int nRowCount = 0;
		int nColCount = 0;
		String sep = "\t";
		
		
		DistanceCalculator calculator = null;
		
		if (cmd.hasOption("e"))
			calculator = new EuclidianCalculator();
		else if (cmd.hasOption("p"))
			calculator = new PearsonCalculator(false);
		else if (cmd.hasOption("pa"))
		{
			calculator = new PearsonCalculator(true);
		}
		else if (cmd.hasOption("t"))
			calculator = new TanimotoCalculator();
		else 
			calculator = new EuclidianCalculator();
	


		LinkageMode mode = null;
		mode = LinkageMode.AVG;

		if(cmd.hasOption("avg"))
			mode = LinkageMode.AVG;
		else if(cmd.hasOption("min"))
			mode = LinkageMode.MIN;
		else if(cmd.hasOption("max"))
			mode = LinkageMode.MAX;		
		else 
			mode = LinkageMode.AVG;
		
		// If distance matrix
		
		
		if(cmd.hasOption("d"))
		{
		/*	Runtime s_runtime = Runtime.getRuntime ();
			System.out.println(s_runtime.totalMemory () + " " +  s_runtime.freeMemory () + " " + ((s_runtime.totalMemory () -  s_runtime.freeMemory ())  / 1024 / 1024)) ;
*/
				String inputDM = cmd.getOptionValue("d"); 
			    System.out.println("Reading distance matrix...");
				final DistanceMatrix mtx = org.gnf.clustering.Utils.ReadDistanceMatrix(inputDM);
				
				//System.out.println(mtx.toString());
				
			/*	System.runFinalization();
				System.gc();
				System.runFinalization();
				System.gc(); 
				 
				long nMemory = GetUsedMemory();
				System.out.println(nMemory);

				System.out.println(s_runtime.totalMemory () + " " +  s_runtime.freeMemory () + " " + ((s_runtime.totalMemory () -  s_runtime.freeMemory ())  / 1024 / 1024)) ;
			*/	
				nRowCount = mtx.getRowCount();
				System.out.println("Clustering...");
				if(mtx != null)				
					arNodes = org.gnf.clustering.sequentialcache.SequentialCacheClustering.clusterDM(mtx,mode, null, nRowCount);
					
				//System.out.println(s_runtime.totalMemory () + " " +  s_runtime.freeMemory () + " " + ((s_runtime.totalMemory () -  s_runtime.freeMemory ())  / 1024 / 1024)) ;
		}
		
		
		else if(cmd.hasOption("f"))
		{
			String inputFingerprints = cmd.getOptionValue("f"); 
			if(cmd.hasOption("n")) 			
				nRowCount = Integer.valueOf(cmd.getOptionValue("n"));
			else 
				nRowCount = org.gnf.clustering.Utils.GetNumRows(inputFingerprints,1);
			
			System.out.println("Reading fingerprints...");
			DataSource sourceFingerprints = org.gnf.clustering.Utils.ReadDataFile(inputFingerprints, nRowCount, 
							16, "\t", 1, 1, true, null, null);
			
			
			
			final HierarchicalClustering clusteringHier= new SequentialCacheClustering(calculator, mode);
			System.out.println("Clustering...");
			if(!cmd.hasOption("hybrid"))
			{
				try	
				{
					arNodes = clusteringHier.cluster(sourceFingerprints, null);
				}
				catch(Exception ex)
				{
					throw new RuntimeException("Clustering Failed", ex);
				}
			}
			else
			{
				CentroidsCalculator calculatorCtr = new TanimotoCentroidsCalculator();
		
				final HybridClustering clusteringHybrid = new HybridClustering(clusteringHier, calculatorCtr);
				if(cmd.hasOption("k"))
					clusteringHybrid.setK(Integer.valueOf(cmd.getOptionValue("k")));	
	
				try	
				{
					arNodes = clusteringHybrid.cluster(sourceFingerprints, null);
				}	
				catch(Exception ex)
				{
					throw new RuntimeException("Clustering Failed", ex);
				}
			}	
			
		}	
		
		if(cmd.hasOption("i"))
		{
			
			
			sep = "\t";
			if(cmd.hasOption("sep")) sep = cmd.getOptionValue("sep");
			//System.out.println(cmd.hasOption("sep") + " " + cmd.getOptionValue("sep") + " " + sep );					
			
			if(cmd.hasOption("-eis"))
			{
				if(cmd.hasOption("sr")) 			
					skipRows = Integer.valueOf(cmd.getOptionValue("sr"));
				else 
					skipRows = org.gnf.clustering.Utils.GetEisenSkipRows(inputFilename);
				
				if(cmd.hasOption("sc")) 			
					skipCols = Integer.valueOf(cmd.getOptionValue("sc"));
				else 
					skipCols = org.gnf.clustering.Utils.GetEisenSkipCols(inputFilename);
				sep = "\t";
												
			}
			else 
			{
				if(cmd.hasOption("sr")) 			
					skipRows = Integer.valueOf(cmd.getOptionValue("sr"));
				if(cmd.hasOption("sc")) 			
					skipCols = Integer.valueOf(cmd.getOptionValue("sc"));
				
			}
			
			if(cmd.hasOption("n")) 			
				nRowCount = Integer.valueOf(cmd.getOptionValue("n"));
			else 
				nRowCount = org.gnf.clustering.Utils.GetNumRows(inputFilename,skipRows);
			
			if(cmd.hasOption("m")) 			
				nColCount = Integer.valueOf(cmd.getOptionValue("m"));
			else 
				nColCount = org.gnf.clustering.Utils.GetNumCols(inputFilename,skipRows, skipCols, sep);			

			rowNames = new String [nRowCount];
			colNames = new String [nColCount];
			System.out.println("Reading the input file...");
			source = org.gnf.clustering.Utils.ReadDataFile(inputFilename, nRowCount, 
				nColCount, sep, skipRows, skipCols, false, rowNames, colNames);
			
			System.out.println();
			if(!cmd.hasOption("sr")) 
				System.out.println("Omitting the first " + skipRows + " row(s).");
			if(!cmd.hasOption("sc")) 
				System.out.println("Omitting the first " + skipCols + " column(s).");
			if(!cmd.hasOption("n")) 
				System.out.println("Input file contains " + nRowCount + " data objects.");
			if(!cmd.hasOption("m")) 
				System.out.println("Data dimensionality is " + nColCount + ".");
			System.out.println();
			if(!cmd.hasOption("m") || !cmd.hasOption("n") || !cmd.hasOption("sr") || !cmd.hasOption("sc") ) 
			{
				System.out.println("If the values determined automatically are incorrect, please "
						+ "use -n, -m, -sr or -sc options to manually define them. Or see help (-help option).");
			}
			System.out.println();
			
			if(!cmd.hasOption("d") && !cmd.hasOption("f"))
			{
			
				
		

			
				final HierarchicalClustering clusteringHier= new SequentialCacheClustering(calculator, mode);
			
			
				//System.out.println(System.currentTimeMillis() - start);
				System.out.println("Clustering...");
				start = System.currentTimeMillis();
				if(!cmd.hasOption("hybrid"))
				{
					try	
					{
						arNodes = clusteringHier.cluster(source, null);
					}
					catch(Exception ex)
					{
						throw new RuntimeException("Clustering Failed", ex);
					}
				}
				else
				{
					CentroidsCalculator calculatorCtr = new DefaultCentroidsCalculator();
						
					final HybridClustering clusteringHybrid = new HybridClustering(clusteringHier, calculatorCtr);
					if(cmd.hasOption("k"))
						clusteringHybrid.setK(Integer.valueOf(cmd.getOptionValue("k")));	
	
					try	
					{
						arNodes = clusteringHybrid.cluster(source, null);
					}	
					catch(Exception ex)
					{
						throw new RuntimeException("Clustering Failed", ex);
					}
				}
			}
			

	
		}
		
		String outputPrefix = "";
		if(cmd.hasOption("o"))
			outputPrefix = cmd.getOptionValue("o");
		else if (cmd.hasOption("i"))
			outputPrefix = org.apache.commons.io.FilenameUtils.removeExtension(inputFilename);
		else if (cmd.hasOption("d"))
			outputPrefix = org.apache.commons.io.FilenameUtils.removeExtension(cmd.getOptionValue("d"));
		else if (cmd.hasOption("f"))
			outputPrefix = org.apache.commons.io.FilenameUtils.removeExtension(cmd.getOptionValue("f"));
		
		String outGtr = outputPrefix + ".gtr";
		String outCdt = outputPrefix + ".cdt";
		
		int[] rowOrder = new int[nRowCount];
		System.out.println("Sorting tree nodes...");
		org.gnf.clustering.Utils.NodeSort(arNodes, nRowCount - 2, 0,  rowOrder);
			//for (int i = 0; i < nRowCount; i++)
			//	System.out.print(rowOrder[i] + " ");
		System.out.println("Writing output to file...");
		if(cmd.hasOption("i")) 
		{
			org.gnf.clustering.Utils.GenerateCDT(outCdt, source, 
					nRowCount, nColCount, sep, rowNames, colNames, rowOrder);
		}
		
		
		org.gnf.clustering.Utils.WriteTreeToFile(outGtr, nRowCount - 1, arNodes,true);
		System.out.println();
	}
						
}

