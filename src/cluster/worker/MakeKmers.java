package cluster.worker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

import utils.FastaSequence;
import utils.FastaSequenceOneAtATime;
import utils.Translate;

public class MakeKmers
{
	
	public static void main(String[] args) throws Exception
	{
		if( args.length != 3)
		{
			System.out.println("usage kmerSize inFastaFileToProduceKmers outFileOfKmers");
			System.exit(1);
		}
		
		File fastaFile = new File(args[1]);
		
		if( ! fastaFile.exists())
			throw new Exception("Could not find " + fastaFile.getAbsolutePath());
		
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
			throw new Exception("kmer argument (first argument) must be a postive integer");
		}
		
		HashMap<String, Integer> map = breakIntoKmers(fastaFile, kmerSize);
			
		File outFile = new File( args[2]);
					
		writeResults(outFile, map);
		
	}
	
	public static boolean isACGT(String s)
	{
		
		for( int x=0; x < s.length(); x++)
		{
			char c = s.charAt(x);
			
			if( c != 'A' && c != 'C' && c != 'G' && c != 'T')
				return false;
		}
		
		return true;
	}
	
	private static void writeResults( File outFile, HashMap<String, Integer> map) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
		
		for(String s : map.keySet())
			writer.write(s + "\t" + map.get(s) + "\n");
		
		writer.flush();  writer.close();
	}
	
	public static void addToMap(HashMap<String, Integer> map, String seq, int kmerLength) throws Exception
	{
		for( int x=0; x < seq.length()- kmerLength; x++)
		{
			String sub = seq.substring(x, x + kmerLength);
			
			if( isACGT(sub))
			{
				Integer count = map.get(sub);
				
				if( count == null)
				{
					String reverse = Translate.reverseTranscribe(sub);
					
					count = map.get(reverse);
					
					if( count != null)
						sub = reverse;
				}
				
				if( count == null)
					count =0;
				
				count++;
				
				map.put(sub,count);
			}
		}
	}
	
	public static void checkForNoReverse( HashMap<String, Integer>  map ) throws Exception
	{

		for(String s : map.keySet())
		{
			String reverse = Translate.reverseTranscribe(s);
			
			if( ! reverse.equals(s))
			{
				if( map.containsKey(reverse))
					throw new Exception("Logic error " + s + " " + Translate.reverseTranscribe(s));
			
			}
		}
	}
	
	static HashMap<String, Integer> breakIntoKmers(File inFile, int kmerLength) throws Exception
	{
		HashMap<String, Integer> map = new HashMap<String,Integer>();
		
		FastaSequenceOneAtATime fsoat = new FastaSequenceOneAtATime(inFile);
		
		for(FastaSequence fs = fsoat.getNextSequence(); fs != null;
						fs = fsoat.getNextSequence())
		{
			String seq =fs.getSequence().toUpperCase();
			addToMap(map, seq, kmerLength);
		}
	
		fsoat.close();
		
		// this should not throw if our logic is correct
		// todo: potentially take this out for performance gain
		checkForNoReverse(map);
			
		return map;
		
	}
}
