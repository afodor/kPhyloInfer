package cluster.worker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;

public class WriteDistanceMatrixFromConstrainedRegion
{
	public static void main(String[] args) throws Exception
	{
		if( args.length != 7)
		{
			System.out.println("usage kmerLength referenceGenomeFilepath contig startPos endPos kmerDirectory resultsFile");
			System.exit(1);
		}
		
		File kmerDir = new File(args[5]);
		
		if( ! kmerDir.exists() || ! kmerDir.isDirectory())
			throw new Exception(args[5] + " is not an exisiting directory");
		
		String[] files = kmerDir.list();
		
		String seq = ConstrainKMersToRegion.getConstrainingString(args[1], args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
		
		Integer kmerSize = null;
		
		try
		{
			kmerSize = Integer.parseInt(args[0]);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		if( kmerSize == null || kmerSize <= 0 )
		{
			throw new Exception("kmer argument (argument 0) must be a postive integer");
		}
		
		HashSet<String> set = ConstrainKMersToRegion.getConstrainingSet(seq, kmerSize);
		HashMap<String, HashMap<Integer,Integer>> bigMap = ConstrainKMersToRegion.getBigMap(kmerDir, files, set);
		
		HashMap<String, Float> resultsFromConstrainedSet = ConstrainKMersToRegion.getResultsFromConstrained(set, bigMap, kmerSize);
		writeResults(new File(args[6]), resultsFromConstrainedSet);
		
	} 
	
	private static void writeResults(File outFile, HashMap<String, Float> resultsFromConstrainedSet ) 
				throws Exception
	{
		BufferedWriter writer =new BufferedWriter(new FileWriter(outFile));
		
		writer.write("genomeA\tgenomeB\tcosineDistance\n");
		
		for(String s : resultsFromConstrainedSet.keySet())
		{
			String[] vals = s.split("@");
			
			if( vals.length > 2)
				throw new Exception("Sorry.  Genome names must not contain an @ symbol.");
			
			if( vals.length != 2)
				throw new Exception("Parsing error");
			
			writer.write(vals[0] + "\t" + vals[1] + "\t" + resultsFromConstrainedSet.get(s) + "\n");
			
		}
		
		writer.flush(); writer.close();

	}
}
