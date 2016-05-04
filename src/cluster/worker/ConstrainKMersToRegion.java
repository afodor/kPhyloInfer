package cluster.worker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

import utils.FastaSequence;
import utils.Spearman;
import utils.Translate;

public class ConstrainKMersToRegion
{
	
	public static HashMap<String, Float> parseDistanceFileIgnoringComparisonsToSame( File file) throws Exception
	{
		HashMap<String, Float> map = new HashMap<String, Float>();
		
		BufferedReader reader = new BufferedReader( new InputStreamReader(new GZIPInputStream(new 
				FileInputStream(file))));
		
		reader.readLine();
		
		for(String s = reader.readLine(); s != null; s= reader.readLine())
		{
			String[] splits = s.split("\t");
			
			if( splits.length != 3)
				throw new Exception("Unexpected file format " + file.getAbsolutePath());
			
			if( ! splits[0].equals(splits[1]) )
			{

				String key = getKey(splits[0], splits[1]);
				
				if( map.containsKey(key))
					throw new Exception("Duplicate key " + key + " " + file.getAbsolutePath());
				
				map.put(key, Float.parseFloat(splits[2]));
			}
			
		}
		
		reader.close();
		
		return map;
	}
	
	private static String getKey(String a, String b) throws Exception
	{
		a = a.replace("_"+ MakeMatrixWithAllKmers.EXPECTED_SUFFIX, "");
		b = b.replace("_" + MakeMatrixWithAllKmers.EXPECTED_SUFFIX, "");
		
		if( a.equals(b))
			throw new Exception("Comparing identical objects");
		
		if( a.compareTo(b) >0 )
		{
			return a + "@" + b;
		}
		
		return b + "@" + a;
	}
	
	private static HashMap<String, Float> getResultsFromConstrained(File kmerDir, String[] files,
				HashSet<String> constrainingSet) throws Exception
	{
		HashMap<String, Float> resultsMap = new HashMap<String,Float>();
		
		for( int x=0; x < files.length-1; x++)
		{
			File xFile = new File( kmerDir.getAbsolutePath() + File.separator +  files[x]);
			
			if( xFile.getName().toLowerCase().endsWith( MakeMatrixWithAllKmers.EXPECTED_SUFFIX))
			{
				HashMap<String, Integer> xMap = MakeMatrixWithAllKmers.getCounts(xFile, constrainingSet);
				long sumXSquared = MakeMatrixWithAllKmers.getSumSquare(xMap);
				
				for(int y=x+1; y < files.length; y++)
				{
					File yFile = new File( kmerDir.getAbsolutePath() + File.separator +  files[y]);
					
					if( yFile.getName().toLowerCase().endsWith(MakeMatrixWithAllKmers.EXPECTED_SUFFIX))
					{
						HashMap<String, Integer> yMap = MakeMatrixWithAllKmers.getCounts(yFile, constrainingSet);
						
						
						String key = getKey(xFile.getName(), yFile.getName());
						
						if( resultsMap.containsKey(key))
							throw new Exception("Duplciate key " + key);
						
						resultsMap.put(key, MakeMatrixWithAllKmers.getDistance(xMap, yMap, sumXSquared));
						
					}
				}
			}
		}
		
		return resultsMap;
	}
	
	public static void main(String[] args) throws Exception
	{
		if( args.length != 8)
		{
			System.out.println("usage kmerLength referenceGenomeFilepath contig startPos endPos kmerDirectory allTreeFile  resultsFile");
			System.exit(1);
		}
		
		File allTreeFile = new File(args[6]);
		
		if( ! allTreeFile.exists() || ! allTreeFile.getName().toLowerCase().endsWith("gz"))
			throw new Exception("All tree file must be a zipped comparison file; see MakeMatrixWithAllKmers");
		
		File kmerDir = new File(args[5]);
		
		if( ! kmerDir.exists() || ! kmerDir.isDirectory())
			throw new Exception(args[5] + " is not an exisiting directory");
		
		String[] files = kmerDir.list();
		
		String seq = getConstrainingString(args[1], args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
		
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
		
		HashSet<String> set = getConstrainingSet(seq, kmerSize);
		
		HashMap<String, Float> resultsFromAllTree = parseDistanceFileIgnoringComparisonsToSame(allTreeFile);
		HashMap<String, Float> resultsFromConstrainedSet = getResultsFromConstrained(kmerDir, files, set);
		
		writeResults(resultsFromConstrainedSet, resultsFromAllTree, 
						new File(args[7]), new File(args[1]), args[2], Integer.parseInt(args[3]), 
						Integer.parseInt(args[4]));
	}
	
	
	private static void writeResults( HashMap<String, Float> constrainedMap, HashMap<String, Float> allTree,
			File outFile, File genomeFile, String contigName, int startPos, int endPos) throws Exception
	{

		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
		
		List<Float> list1 = new ArrayList<Float>();
		List<Float> list2 = new ArrayList<Float>();
		
		for( String s : constrainedMap.keySet())
		{
			if( allTree.containsKey(s) )
			{
				list1.add(constrainedMap.get(s));
				list2.add(allTree.get(s));
			}
		}
		
		Spearman s = Spearman.getSpear(list1, list2);
		writer.write(" Spearman distance = " + s.getRs() + "\n");
		writer.write(" Spearman p = " + s.getProbrs() + "\n");
		writer.write(" Number of kmers in Spearman = " + list1.size() + "\n");
		writer.write(" Number of kmers in all tree = " + allTree.size() + "\n");
		writer.write(" reference genome = " + genomeFile.getAbsolutePath() + "\n");
		writer.write(" contig = " + contigName + "\n");
		writer.write(" start to stop = "  + startPos + " " + endPos +  "\n");
		
		writer.flush(); writer.close();
		
	}
	
	private static String getConstrainingString(String filepath, String contig, int startPos, int endPos) throws Exception
	{
		List<FastaSequence> list = 
				FastaSequence.readFastaFile(
						filepath);
		
		String toFind = contig;
	
		for(FastaSequence fs : list)
			if(fs.getFirstTokenOfHeader().equals(toFind))
			{
				return fs.getSequence().substring(startPos, endPos).toUpperCase();
			}
		
		throw new Exception("No");
	}
	
	private static HashSet<String> getConstrainingSet(String seq, int kmerLength) throws Exception
	{	
		HashMap<String, Integer> map = new HashMap<String,Integer>();
		
		
		for( int x=0; x < seq.length()- kmerLength; x++)
		{
			String sub = seq.substring(x, x +  kmerLength);
			
			if( MakeKmers.isACGT(sub))
			{
				MakeKmers.addToMap(map, seq, kmerLength);
			}
		}
		
		MakeKmers.checkForNoReverse(map);
		
		return new HashSet<String>(map.keySet());
	}
	
	public static void addToMap(HashMap<String, Integer> map, String seq, int kmerLength) throws Exception
	{
		for( int x=0; x < seq.length()- kmerLength; x++)
		{
			String sub = seq.substring(x, x + kmerLength);
			
			if(  MakeKmers.isACGT(sub))
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

}
