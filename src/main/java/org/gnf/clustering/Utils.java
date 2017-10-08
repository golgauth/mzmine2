package org.gnf.clustering;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

public class Utils
{
	private Utils() {}

	static final int MAX_ROW_COUNT = 200000;//46000;
	public static final int [] MTX_LENGTHS = new int[MAX_ROW_COUNT];
	static
	{
		for(int nRow = 0; nRow < MTX_LENGTHS.length; ++nRow)
		{
			MTX_LENGTHS[nRow] = (int)((nRow*(nRow+1))/2);
		}
	}

	public static DistanceMatrix	distancematrix(final int nRowCount, final int nColCount,
			final DataSource data,	final DistanceCalculator calculator,	final ProgressCounter counter)
	{
		if(nRowCount == 0)
			throw new IllegalArgumentException("The number of rows cannot be null.");

		//final int nLength = (n-1)*n/2;
		final int nLength = ((nRowCount*(nRowCount-1))/2);//MTX_LENGTHS[n-1];
		final DistanceMatrix mtx = nRowCount < 20000 ? new DistanceMatrix1D(nRowCount) : new DistanceMatrix2D(nRowCount); 


		if(counter != null)
		{
			counter.setOperationName("Building Distance Metrics...");
			counter.setTotal(nLength);
		}

		final int nCPUCount = Runtime.getRuntime().availableProcessors();
		final int nCPUCountToUse = nCPUCount < 2 || nLength <= nCPUCount ? 1 : nCPUCount-1;  
		final ExecutorService executor = Executors.newFixedThreadPool(nCPUCountToUse);
		final FutureTask <Boolean[]> [] arTasks = new FutureTask[nCPUCountToUse];

		int nFr = 0;
		int nTo = 0;
		int nCountPerCPU = nLength/nCPUCountToUse;
		if(nCountPerCPU == 0)
		{
			nCountPerCPU = 1;
		}

		final DistMatrixCalculator [] arDstMtxCalcs = new DistMatrixCalculator[nCPUCountToUse];

		for(int nCPU = 0; nCPU < nCPUCountToUse; ++nCPU)
		{
			nFr = nCountPerCPU*nCPU;
			nTo = nCPU == nCPUCountToUse-1 ? nLength -1 : nCountPerCPU*(nCPU+1) -1;

			//Afx.TRACE("nFr= " + nFr + " nTo= " + nTo);
			arDstMtxCalcs[nCPU] = new DistMatrixCalculator(calculator, data, nFr, nTo, mtx, counter);
			arTasks[nCPU] = new FutureTask(arDstMtxCalcs[nCPU]);
			executor.execute(arTasks[nCPU]);
		}

		int nTotalDone = 0;
		for(int nCPU = 0; nCPU < nCPUCountToUse; ++nCPU)
		{
			try{arTasks[nCPU].get();}
			catch(Exception ex)
			{
				executor.shutdown();
				throw new RuntimeException("Couldn't calculate distance matrix", ex);
			}

			nTotalDone += (nTo- nFr+1);
		}
		//worker.setMsg("Creating compound objects... " + ((nTotalDone/nRowCount)*100) + "% completed...");
		executor.shutdown();//Important!!!!!!!!!!!!!!

		return mtx;
	}

	public static final Node[] UpdateNodes(final Node[] arTreeNodes,	final double [] arRowOrders,	final int [] arRowIndices,
			final ProgressCounter counter)
	{
		final int nRowCount = arRowIndices.length;
		final int nNodeCount = nRowCount - 1;

		if(counter != null && counter.isCancelled())
			return null;

		if(arTreeNodes == null)
			return null;

		final Node[] arTreeNodesOut = new Node[nNodeCount]; 

		int nNode = 0;
		//Scale all distances such that they are between 0 and 1

		double fScale = 0.0;
		for(nNode = 0; nNode < nNodeCount; ++nNode)
		{
			if(arTreeNodes[nNode].m_fDistance > fScale)
				fScale = arTreeNodes[nNode].m_fDistance;
		}

		if(fScale != 0.0)
		{
			for(nNode = 0; nNode < nNodeCount; ++nNode)
			{
				arTreeNodes[nNode].m_fDistance /= fScale;
			}
		}

		//Now we join nodes
		final double[] arNodeOrders = new double[nNodeCount];
		final int[] arNodeCounts = new int[nNodeCount];
		final String[] arNodeIDs = new String[nNodeCount];

		double fOrder1 = 0.0;
		double fOrder2 = 0.0;
		int nCounts1 = 0;
		int nCounts2 = 0;
		int nMin1 = 0;
		int nMin2 = 0;
		int nIndex1 = -1;
		int nIndex2 = -1;
		String strID1 = null;
		String strID2 = null;

		for(nNode = 0; nNode < nNodeCount; ++nNode)
		{
			nMin1 = arTreeNodes[nNode].m_nLeft;
			nMin2 = arTreeNodes[nNode].m_nRight; // min1 and min2 are the elements that are to be joined

			arNodeIDs[nNode] = Utils.MakeID("NODE", nNode/*+1*/);
			arTreeNodesOut[nNode] = new Node(arNodeIDs[nNode]);

			if(nMin1 < 0) //Node
			{
				nIndex1  = -nMin1-1;
				fOrder1 = arNodeOrders[nIndex1];
				nCounts1= arNodeCounts[nIndex1];
				strID1 	= arNodeIDs[nIndex1];
				arTreeNodes[nNode].m_fDistance = Math.max(arTreeNodes[nNode].m_fDistance, arTreeNodes[nIndex1].m_fDistance);

				arTreeNodesOut[nNode].m_nLeft = nIndex1;
			}
			else//nMin1 >= 0, Terminal
			{
				fOrder1 = arRowOrders[nMin1];
				nCounts1 = 1;
				//strID1 = MCBClstrUtilsNew.MakeID(strKeyword, nMin1);

				arTreeNodesOut[nNode].m_nLeft = -1;
				arTreeNodesOut[nNode].m_nTermLeft = nMin1;
			}

			if(nMin2 < 0)//Node
			{
				nIndex2 = -nMin2-1;
				fOrder2 = arNodeOrders[nIndex2];
				nCounts2= arNodeCounts[nIndex2];
				strID2  = arNodeIDs[nIndex2];
				arTreeNodes[nNode].m_fDistance = Math.max(arTreeNodes[nNode].m_fDistance, arTreeNodes[nIndex2].m_fDistance);

				arTreeNodesOut[nNode].m_nRight = nIndex2;
			}
			else //nMin2 >= 0, Terminal
			{
				fOrder2 = arRowOrders[nMin2];
				nCounts2 = 1;
				//strID2 = MCBClstrUtilsNew.MakeID(strKeyword, nMin2);

				arTreeNodesOut[nNode].m_nRight = -1;
				arTreeNodesOut[nNode].m_nTermRight = nMin2;
			}

			arTreeNodesOut[nNode].m_fDistance=arTreeNodes[nNode].m_fDistance + 0.1;

			arNodeCounts[nNode] = nCounts1 + nCounts2;
			arNodeOrders[nNode] = (nCounts1*fOrder1 + nCounts2*fOrder2) / (nCounts1 + nCounts2);
		}

		// Now set up order based on the tree structure
		Utils.TreeSort(nNodeCount, arRowOrders, arNodeOrders, arNodeCounts, arTreeNodes, arRowIndices, counter);

		for(nNode = 0; nNode < arTreeNodes.length; ++nNode)
		{
			if(Double.isNaN(arTreeNodes[nNode].m_fDistance))
				throw new RuntimeException();
		}

		return arTreeNodesOut;
	}


	private static void TreeSort(final int nNodes, final double[] arOrders,
			final double[] arNodeOrders, final int[] arNodeCounts, final Node[] arNodes,
			final int[] arRowIndicies,	final ProgressCounter counter)
	{
		if(counter != null)
		{
			counter.setOperationName("Sorting Clusters...");
			counter.setTotal(nNodes);
		}

		final int nRowCount = arRowIndicies.length;
		final int nElements = nNodes + 1;
		int i;
		final double[] neworder = new double[nElements];
		final int[] clusterids = new int[nElements];

		for(i = 0; i < nElements; i++) {clusterids[i] = i;}

		int i1 = 0;
		int i2 = 0;
		double order1 = 0.0;
		double order2 = 0.0;
		int count1 = 0;
		int count2 = 0;
		int clusterid = 0;
		double increase = 0.0;

		for(i = 0; i < nNodes; i++)
		{
			i1 = arNodes[i].m_nLeft;
			i2 = arNodes[i].m_nRight;
			order1 = (i1<0) ? arNodeOrders[-i1-1] : arOrders[i1];
			order2 = (i2<0) ? arNodeOrders[-i2-1] : arOrders[i2];
			count1 = (i1<0) ? arNodeCounts[-i1-1] : 1;
			count2 = (i2<0) ? arNodeCounts[-i2-1] : 1;
			// If order1 and order2 are equal, their order is determined by the order in which they were clustered */
			if(i1<i2)
			{
				increase = (order1<order2) ? count1 : count2;
				int j;
				for (j = 0; j < nElements; j++)
				{
					clusterid = clusterids[j];
					if(clusterid==i1 && order1>=order2)
						neworder[j] += increase;
					if(clusterid==i2 && order1<order2)
						neworder[j] += increase;
					if(clusterid==i1 || clusterid==i2)
						clusterids[j] = -i-1;
				}
			}
			else
			{
				increase = (order1<=order2) ? count1 : count2;
				int j;
				for (j = 0; j < nElements; j++)
				{
					clusterid = clusterids[j];
					if(clusterid==i1 && order1>order2)
						neworder[j] += increase;
					if(clusterid==i2 && order1<=order2)
						neworder[j] += increase;
					if(clusterid==i1 || clusterid==i2)
						clusterids[j] = -i-1;
				}
			}
			if(counter != null)
				counter.increment(1, true);
		}

		for(i=0; i<nRowCount; i++) {arRowIndicies[i] = i;}
		SortUtils.sort3(neworder, 0, nRowCount, arRowIndicies);
	}

	private static String MakeID(final String name, int i)
	{ 
		final String strID = name + i;
		return strID;
	}

	private static byte[] intToByteArray(int value)
	{
		long valueLong = Long.valueOf(value);
		if (valueLong < 0) valueLong = (long) Math.pow(2.0, 32.0 ) + valueLong ;
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) 
		{

			long mask = ((long)255) << i * 8;
			long part = (valueLong & mask) >> i * 8;
			if (part > 127) b[i] =  (byte) (part - 256 );
			else b[i] = (byte) part;
			//System.out.println(part + " " + (part - 256 ) + " " + b[i]);
			/*int offset = (b.length - 1 - i) * 8;
 			b[i] = (byte) ((valueLong >>> offset) & 0xFF);*/
		}
		return b;

	}


	/**
	 * Reads data from a file.
	 * @param fileName the name of the input data file.
	 * @param nRowCount the number of data rows.
	 * @param nColCount the number of data columns.
	 * @param separator separator value for the entries in a row.
	 * @param skipRows the number of rows to skip (header lines).
	 * @param skipCols the number of columns to skip (row names, descriptions etc).
	 * @isFingerprints true if the data is fingerprints (32 bit integers), false otherwise.
	 * @param rowNames array of data objects names, extracted from column 1 or automatically assigned.
	 * @param colNames array of experiment names, extracted from the header or automatically assigned.
	 * @return DataSource
	 * @throws NumberFormatException
	 * @throws IOException
	 */


	public static DataSource ReadDataFile(final String fileName, final int nRowCount, 
			final int nColCount, String separator, int skipRows, int skipCols, 
			boolean isFingerprints, final String[] rowNames, final String[] colNames) throws NumberFormatException, IOException

	{



		float [][] arDataF = null ;
		int [] arDataI = null;
		if (!isFingerprints)
			arDataF = new float[nRowCount][nColCount];
		else
			arDataI = new int[nRowCount * nColCount];
		//System.out.println("size" + (nRowCount * nColCount));			
		File file = new File(fileName);
		BufferedReader reader = null;

		int nRow = 0;
		int nCol = 0;

		reader = new BufferedReader(new FileReader(file)); 
		String nextLine = null; 

		if (rowNames != null && colNames!= null)
		{
			if (skipRows >= 1)
			{
				nextLine = reader.readLine(); 
				String[] words = nextLine.split(separator);

				if (words.length >= nColCount)
					for (int i = 0; i < nColCount; i++)
						colNames[nColCount - 1 - i] = words[words.length - 1 - i];
				else
					for (int i = 0; i < nColCount; i++)
						colNames[i] = "Exp" + String.valueOf(i);

				for (int i = 1; i < skipRows; i ++)
					nextLine = reader.readLine();
			}
		}
		else 
		{
			for (int i = 0; i < skipRows; i ++)
				nextLine = reader.readLine();
		}


		while((nextLine = reader.readLine()) != null && nRow < nRowCount)
		{

			String[] words = nextLine.split(separator);
			//System.out.println(nextLine);
			if (skipCols == 0) nCol = 0;
			else nCol = -skipCols;

			if (rowNames != null)
			{
				if (skipCols > 0) rowNames[nRow] = words[0];
				else rowNames[nRow] = "GENE" + String.valueOf(nRow + 1) + "X";
			}

			for (String word : words) 
			{
				//System.out.println(word + " " + word.length());
				if (word.length() == 0) continue;
				if (nRow < nRowCount && nCol < nColCount && nCol > -1)
				{
					if (!isFingerprints) 
						arDataF[nRow][nCol] = Float.valueOf(word);						
					else
						arDataI[nRow * nColCount + nCol] = Integer.valueOf(word);					
				}
				else if (nCol >= nColCount) break;
				nCol++;
			}
			nRow++;
		}
		DataSource source = null;
		//System.out.println("source" + (nRowCount * nColCount));	
		if (!isFingerprints)
		{
			long sourceSize = nRowCount * nColCount;
			//if (sourceSize < 100000000)

			//	source = new FloatSource1D(arDataF, nRowCount, nColCount);
			//else 
			source = new FloatSource2D(arDataF, nRowCount, nColCount);
		}
		else
		{
			byte [] arDataB  = new byte[nRowCount * nColCount * 4];
			for (int i = 0; i < nRowCount * nColCount; i++)
			{
				byte [] byteArray = intToByteArray(arDataI[i]);
				for (int j = 0; j < 4; j++)
				{
					arDataB[i*4 + j] = byteArray[j];
				}
			}
			source = new BitSource(arDataB, nRowCount, nColCount * 4);
		}

		/*System.out.print("Object_ID ");
		for (int j = 0; j < 9; j++)
			System.out.print("measurement" + String.valueOf(j+1) + " ");
		System.out.print("measurement" + String.valueOf(10)  + "\n");
		for (int i = 0; i < 100; i++)
		{
			System.out.print("object" + String.valueOf(i+1) + " ");
			for (int j = 0; j < 9; j++)
				System.out.print(source.getValue(i,j) + " ");
			System.out.print(source.getValue(i,9) + "\n");
		}*/

		reader.close();	
		return source;

	}


	/**
	 * Reads distance matrix from a file.
	 * @param fileName file that contains a distance matrix.
	 * @return distance matrix.
	 * @throws NumberFormatException
	 * @throws IOException
	 */

	public static DistanceMatrix ReadDistanceMatrix(final String fileName) throws NumberFormatException, IOException

	{

		File file = new File(fileName);
		BufferedReader reader = null;

		int nRow = 0;	
		reader = new BufferedReader(new FileReader(file)); 
		String nextLine = null; 		

		nextLine = reader.readLine(); 
		String[] words = nextLine.split(" ");
		int nRowCount = Integer.valueOf(words[0]);

		final int nLength = ((nRowCount*(nRowCount-1))/2);
		final DistanceMatrix mtx = nRowCount < 20000 ? new DistanceMatrix1D(nRowCount) : new DistanceMatrix2D(nRowCount); 

		int elem1 = 0, elem2 = 1;


		for (int i = 0; i < nRowCount-1; i++)
		{
			for (int j = i+1 ; j < nRowCount; j++) 
			{
				mtx.setValue(i, j, Float.valueOf(reader.readLine()));
			}
		}
		/*while((nextLine = reader.readLine()) != null)
		{

			mtx.setValue(elem1, elem2, Float.valueOf(reader.readLine()));
			if (elem2 >= nRowCount-1)
			{
				elem1 ++;
				elem2 = elem1 + 1;
			}
			else 
				elem2 ++;		
		}*/
		reader.close();
		return mtx;

	}




	/**
	 * Writes a tree to a file.
	 * @param fileName the name of the output file.
	 * @param nNodes the number of nodes in a tree.
	 * @param arNodes array of tree nodes.
	 * @param treeviewFormat if true, convert distance values (heights of the nodes) to similarity coefficients.
	 */

	public static void WriteTreeToFile(final String fileName, final int nNodes, Node [] arNodes, boolean treeviewFormat) 
	{	
		try
		{
			float maxDist = 0;	
			if (treeviewFormat)
			{					
				for(int nNode = 0; nNode < nNodes; nNode++)
					if (arNodes[nNode].m_fDistance > maxDist)
						maxDist = (float) arNodes[nNode].m_fDistance;
				if (maxDist < 1) maxDist = 1;
			}

			FileWriter fstream = new FileWriter(fileName);
			BufferedWriter writer = new BufferedWriter(fstream);

			for(int nNode = 0; nNode < nNodes; nNode++)
			{
				String nodeRecord = "NODE" + (nNode + 1) +  "X";
				if (arNodes[nNode].m_nLeft < 0) 
					nodeRecord += "\tNODE" + IntToStr(-arNodes[nNode].m_nLeft - 1 + 1) + "X";
				else nodeRecord += "\tGENE" + IntToStr(arNodes[nNode].m_nLeft + 1) + "X";

				if (arNodes[nNode].m_nRight < 0) 
					nodeRecord += "\tNODE" + IntToStr(-arNodes[nNode].m_nRight - 1 + 1) + "X";
				else nodeRecord += "\tGENE" + IntToStr(arNodes[nNode].m_nRight + 1) + "X";

				float val = 0;
				if (treeviewFormat) 
					val = (float) ((maxDist - arNodes[nNode].m_fDistance)/maxDist);
				else 
					val = (float) arNodes[nNode].m_fDistance;


				nodeRecord += "\t" + (FloatToStr(val,2));

				writer.write(nodeRecord + '\n');            
			}
			writer.close();
		} catch (Exception e){System.err.println("Error: " + e.getMessage());}

	}

	/**
	 * Sorts tree leaves recursively.
	 * @param arNodes tree nodes.
	 * @param node root of the subtree whose leaves are being sorted.
	 * @param orderCounter keeps the track of the current position in the array of sorted leaves.
	 * @param rowOrder the array of sorted leaves. rowOrder[i] is the original id of the leaf that has index i in the sorted array.
	 * @return current value of orderCounter.
	 */

	public static int NodeSort(Node [] arNodes, int node, int orderCounter,  final int[] rowOrder)
	{

		if (arNodes[node].m_nLeft < 0) 
		{
			//System.out.println("lnode " + (-arNodes[node].m_nLeft - 1));
			int leftChild = -arNodes[node].m_nLeft - 1;
			orderCounter = NodeSort(arNodes, leftChild, orderCounter, rowOrder);
		}
		else
		{

			int nRow = arNodes[node].m_nLeft;
			rowOrder[orderCounter] = nRow;
			//System.out.println(nRow + " " + orderCounter);
			orderCounter++;
		}
		if (arNodes[node].m_nRight < 0) 
		{
			//System.out.println("rnode " + (-arNodes[node].m_nRight - 1));
			int rightChild = -arNodes[node].m_nRight - 1;
			orderCounter = NodeSort(arNodes, rightChild, orderCounter, rowOrder);
		}
		else
		{
			int nRow = arNodes[node].m_nRight;
			rowOrder[orderCounter] = nRow;
			//System.out.println(nRow + " " + orderCounter);
			orderCounter++;
		}
		return orderCounter;
	}


	/**
	 * Generated a .*cdt file.
	 * @param outFileName the name of the output .cdt file.
	 * @param source DataSource.
	 * @param nRowCount the number of data rows.
	 * @param nColCount the number of data columns.
	 * @param separator separator value for the entries in a row.
	 * @param rowNames array of data objects names, extracted from column 1 or automatically assigned.
	 * @param colNames array of experiment names, extracted from the header or automatically assigned.	 
	 * @param rowOrder the array of sorted leaves. rowOrder[i] is the original id of the leaf that has index i in the sorted array.
	 * @throws IOException
	 */



	public static void GenerateCDT(final String outFileName, DataSource source, 
			int nRowCount, int nColCount, String separator,
			final String[] rowNames, final String[] colNames, int[] rowOrder) throws IOException 
	{	



		FileWriter fstream = new FileWriter(outFileName);
		BufferedWriter writer = new BufferedWriter(fstream);

		int nRow = 0;
		int nCol = 0;			
		String nextLine = null; 

		String outHead = "GID\tDESCR\tNAME\t";
		for (int i = 0; i < nColCount - 1 ; i++)
			outHead += colNames[i] + "\t";
		outHead += colNames[nColCount - 1] + "\n";
		writer.write(outHead);  
		for (int i = 0; i < nRowCount; i++)
		{
			int n = rowOrder[i];
			String outRow = "GENE" + (IntToStr(n + 1))  + "X\t";
			outRow += rowNames[n] + "\t" + rowNames[n] + "\t";
			for (int j = 0; j < nColCount - 1 ; j++)
				outRow += FloatToStr(source.getValue(n, j),-1) + "\t";
			outRow += FloatToStr(source.getValue(n, nColCount - 1),-1);
			if (i < nRowCount - 1) outRow += "\n";
			writer.write(outRow);           
		}			
		writer.close();

	}


	public static String FloatToStr(float num, int lenFrac)
	{
		Locale currentLocale = new Locale("en","US");
		NumberFormat numberFormatter = NumberFormat.getNumberInstance(currentLocale);
		if (lenFrac >=0 ) numberFormatter.setMaximumFractionDigits(lenFrac);		
		String locateString = numberFormatter.format(num);
		String formatString = locateString.replaceAll(",","");		
		return formatString;
	}


	public static String IntToStr(int num)
	{
		Locale currentLocale = new Locale("en","US");
		NumberFormat numberFormatter = NumberFormat.getNumberInstance(currentLocale);
		numberFormatter.setMaximumFractionDigits(0);		
		String locateString = numberFormatter.format(num);
		String formatString = locateString.replaceAll(",","");		
		return formatString;
	}


	/**
	 * Returns the automatically detected number of rows to skip in the input Eisen file.
	 * @param inputFilename input Eisen file name.
	 * @return the number of rows to skip in the input Eisen file.
	 * @throws IOException
	 */
	public static int GetEisenSkipRows(String inputFilename) throws IOException
	{

		String [] rownames = {"weight", "order", "eweight", "eorder", "aid", "eid", "exp"};
		File file = new File(inputFilename);
		BufferedReader reader = null;				
		reader = new BufferedReader(new FileReader(file)); 
		String nextLine = null; 

		int nRow = 1;	
		int nBad = 0;		
		while((nextLine = reader.readLine()) != null)
		{  
			boolean found = false;
			String[] words = nextLine.split("\t");
			for (String rowname: rownames)
				if (words[0].toLowerCase().contains(rowname))
					found = true;
			if (found)
				nBad = nRow;
			nRow++;
		}
		reader.close();
		if (nBad > 0)
			return nBad;
		else return 1;

	}

	/**
	 * Returns the automatically detected number of columns to skip in the input Eisen file.
	 * @param inputFilename input Eisen file name.
	 * @return the number of columns to skip in the input Eisen file.
	 * @throws IOException
	 */

	public static int GetEisenSkipCols(String inputFilename) throws IOException
	{
		String [] colnames = {"geneid", "gid", "genename", "gname", "name", 
				"gene", "gweight", "weight", "gorder", "order"};


		File file = new File(inputFilename);
		BufferedReader reader = null;


		reader = new BufferedReader(new FileReader(file)); 
		String nextLine = reader.readLine();
		String[] words = nextLine.split("\t");

		int nCol = words.length;
		int nGood = 0;
		for (int i = nCol - 1; i >= 0; i--) 
		{
			//System.out.print(words[i] + " ");
			boolean found = false;
			for (String colname: colnames)
			{
				//System.out.print("(" + colname + " " + words[i].toLowerCase().contains(colname) + ")");
				if (words[i].toLowerCase().contains(colname))
					found = true;
			}
			//System.out.print("\n");
			if (found)
				break;
			else nGood++;
			//System.out.println(nCol + " " + nGood);
		}
		//System.out.println(nCol + " " + nGood);
		reader.close();
		if (nCol - nGood > 0)
			return (nCol - nGood);
		else return 1;

	}

	/**
	 * Returns the automatically detected number of data objects in the input file.
	 * @param inputFilename input file name.
	 * @param skipRows the number of rows to skip in the input file.
	 * @return the number of data objects in the input file.
	 * @throws IOException
	 */

	public static int  GetNumRows(String inputFilename, int skipRows) throws IOException
	{
		File file = new File(inputFilename);
		BufferedReader reader = null;

		int nRow = 0;	
		reader = new BufferedReader(new FileReader(file)); 
		String nextLine = null; 		

		for (int i = 0; i < skipRows; i ++)
			nextLine = reader.readLine(); 	

		while((nextLine = reader.readLine()) != null)
		{

			nRow ++;			
		}
		reader.close();
		return nRow;

	}


	/**
	 * Returns the automatically detected dimensionality of data in the input file.
	 * @param inputFilename input file name.
	 * @param skipRows the number of rows to skip in the input file.
	 * @param skipCols the number of columns to skip in the input file.
	 * @param sep separator value for the entries in a row.
	 * @return dimensionality of data in the input file.
	 * @throws IOException
	 */

	public static int  GetNumCols(String inputFilename, int skipRows, int skipCols, String sep) throws IOException
	{
		File file = new File(inputFilename);
		BufferedReader reader = null;

		int nRow = 0;	
		reader = new BufferedReader(new FileReader(file)); 
		String nextLine = null; 		

		for (int i = 0; i < skipRows; i ++)
			nextLine = reader.readLine(); 

		nextLine = reader.readLine();
		String[] words = nextLine.split(sep);
		int nCols = words.length - skipCols;	
		reader.close();
		return nCols;
	}

}
