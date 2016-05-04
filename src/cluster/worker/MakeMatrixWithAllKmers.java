package cluster.worker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import utils.Translate;

public class MakeMatrixWithAllKmers
{
	
	public static String EXPECTED_SUFFIX = "kmers.txt.gz";
		
	/*
	 * This worker takes a directory of kmer files (created with MakeKmers )
	 * and outputs a distance matrix across all the files 
	 * based on pairwise comparison utilizing all kmers in both files
	 */
	public static void main(String[] args) throws Exception
	{
		if( args.length != 2)
		{
			System.out.println("Usage inKmerDirectory outDistanceFile");
			System.exit(1);
		}
		
		if( ! args[1].toLowerCase().endsWith("gz"))
		{
			System.out.println("output file must end with gz");
			System.exit(1);
		}
		
		BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(
				new GZIPOutputStream(new FileOutputStream(args[1]))));
		
		writer.write("genomeA\tgenomeB\tcosineDistance\n");
		
		File kmerDir = new File(args[0]);
		
		if( ! kmerDir.exists() || ! kmerDir.isDirectory())
			throw new Exception(args[1] + " is not an exisiting directory");
		
		String[] files = kmerDir.list();
		
		for( int x=0; x < files.length; x++)
		{
			File xFile = new File( kmerDir.getAbsolutePath() + File.separator +  files[x]);
			
			if( xFile.getName().toLowerCase().endsWith(EXPECTED_SUFFIX))
			{
				HashMap<String, Integer> xMap = getCounts(xFile, null);
				long sumXSquared = getSumSquare(xMap);
				
				for(int y=x; y < files.length; y++)
				{
					File yFile = new File( kmerDir.getAbsolutePath() + File.separator +  files[y]);
					
					if( yFile.getName().toLowerCase().endsWith(EXPECTED_SUFFIX))
					{
						HashMap<String, Integer> yMap = getCounts(yFile, null);
						
						writer.write(xFile.getName().replaceAll("_" + EXPECTED_SUFFIX, "") 
								+ "\t" + yFile.getName().replaceAll("_" + EXPECTED_SUFFIX, "") + "\t"
								+ getDistance(xMap, yMap, sumXSquared) + "\n");
						
						writer.flush();
					}
				}
			}
		}
		
		writer.flush(); writer.close();
		
			
	}
	
	static long getSumSquare(HashMap<String, Integer> counts)
	{
		long sum =0;
		
		for( Integer i : counts.values())
			sum = sum + i * i;
		
		return sum;
	}
	
	static float getDistance(HashMap<String, Integer> aMap, HashMap<String, Integer> bMap,
						long sumASquared) throws Exception
	{
		long sumBSquared = getSumSquare(bMap);
		
		long topSum = 0;
		
		for( String s : aMap.keySet() )
		{
			if( bMap.containsKey(s))
			{
				topSum += aMap.get(s) * bMap.get(s);
			}
			else
			{
				String reverse = Translate.reverseTranscribe(s);
				
				if( bMap.containsKey(reverse))
				{
					topSum += aMap.get(s) * bMap.get(reverse);
				}
			}
				
		}
			
		return (float) (1- topSum / Math.sqrt(sumASquared * sumBSquared));
	}
	
	/*
	 * Key is the kmer
	 * Integer is the # of times the kmers is present
	 * inFile should be a .gz file with two columns (kmer and count) separated by a tap
	 * 
	 * If constraining set is null, it is ignored.
	 * Otherwise only kmers that are in the constraining set will be included in the final count set
	 */
	static HashMap<String, Integer> getCounts(File inFile, HashSet<String> constrainingSet) throws Exception
	{
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		
		if( ! inFile.exists())
			throw new Exception("Could not find " + inFile.getAbsolutePath() );
		
		if( ! inFile.getName().toLowerCase().endsWith(EXPECTED_SUFFIX))
			throw new Exception("Expecting a gzip kmer file for " + inFile.getAbsolutePath());
		
		BufferedReader reader = new BufferedReader( new InputStreamReader(new GZIPInputStream(new 
				FileInputStream(inFile))));
		
		for(String s = reader.readLine(); s != null; s = reader.readLine())
		{
			String[] splits = s.split("\t");
			
			if( splits.length != 2)
				throw new Exception("File not in correct format " + inFile.getAbsolutePath());
			
			if( map.containsKey(splits[0]))
				throw new Exception("Duplicate kmer in " + inFile.getAbsolutePath() + " " + s+ splits[0]);
			
			if( constrainingSet == null || constrainingSet.contains(splits[0]) )
				map.put(splits[0], Integer.parseInt(splits[1]));
		}
		
		reader.close();
				
		return map;
	}

}
