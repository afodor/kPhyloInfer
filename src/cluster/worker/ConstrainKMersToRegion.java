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

import bitManipulations.Encode;
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

				String key = getKey(splits[0].replace("_" + MakeMatrixWithAllKmers.EXPECTED_SUFFIX, ""), 
						splits[1].replace("_" + MakeMatrixWithAllKmers.EXPECTED_SUFFIX, ""));
				
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
	
	/*
	 * The outer key is the genome name.
	 * The inner key is a encoding of the k-mer (encoded with Encode.makeLong()
	 * The inner value is the # of times that k-mer was seen in the genome
	 */
	private static HashMap<String, HashMap<Long,Integer>> getBigMap(File kmerDir, String[] files,
			HashSet<String> constrainingSet) throws Exception
	{
		 HashMap<String, HashMap<Long,Integer>> bigMap  = new HashMap<String, HashMap<Long,Integer>>();
		 
		 for( int x=0; x < files.length; x++)
			 
		 {
			 File xFile = new File( kmerDir.getAbsolutePath() + File.separator +  files[x]);
			 
			 if( xFile.getName().toLowerCase().endsWith( MakeMatrixWithAllKmers.EXPECTED_SUFFIX))
			 {
				 String key = xFile.getName().replace("_" + MakeMatrixWithAllKmers.EXPECTED_SUFFIX, "");
				 
				 if( bigMap.containsKey(key))
					 throw new Exception("Duplicate key " + key);
				 
				 HashMap<String, Integer> countMap = MakeMatrixWithAllKmers.getCounts(xFile, constrainingSet);
				 
				 HashMap<Long, Integer> innerMap = new HashMap<Long,Integer>();
				 bigMap.put(key, innerMap);
				 
				 for(String s : countMap.keySet())
				 {
					 long aVal = Encode.makeLong(s);
					 
					 if( innerMap.containsKey(aVal))
						 throw new Exception("Duplicate long " + aVal + " " + s);
					 
					 innerMap.put(aVal, countMap.get(s));
				 }
			 }
		 }
		
		 return bigMap;
	}
	

	static long getSumSquare(HashMap<Long, Integer> counts)
	{
		long sum =0;
		
		for( Integer i : counts.values())
			sum = sum + i * i;
		
		return sum;
	}
	
	static float getDistance(HashMap<Long, Integer> aMap, HashMap<Long, Integer> bMap,
			long sumASquared, int kmerSize) throws Exception
	{
		long sumBSquared = getSumSquare(bMap);

		long topSum = 0;

		for( Long aLong : aMap.keySet() )
		{
			if( bMap.containsKey(aLong))
			{
				topSum += aMap.get(aLong) * bMap.get(aLong);
			}
			else
			{
				String s = Encode.getKmer(aLong, kmerSize);
				String reverse = Translate.reverseTranscribe(s);
				Long bLong = Encode.makeLong(reverse);
				
				if( bMap.containsKey( bLong))
				{
					topSum += aMap.get(aLong) * bMap.get(bLong);
				}
			}
		
		}

		return (float) (1- topSum / Math.sqrt(sumASquared * sumBSquared));
	}
	
	private static HashMap<String, Float> getResultsFromConstrained(
				HashSet<String> constrainingSet, HashMap<String, HashMap<Long,Integer>> bigMap,
				int kmerSize) throws Exception
	{
		HashMap<String, Float> resultsMap = new HashMap<String,Float>();
		
		List<String> genomeNames =  new ArrayList<>( bigMap.keySet());
		
		for( int x=0; x < genomeNames.size()-1; x++)
		{
			HashMap<Long, Integer> xMap = bigMap.get(genomeNames.get(x));
			long sumXSquared = getSumSquare(xMap);
				
			for(int y=x+1; y < genomeNames.size(); y++)
			{
				HashMap<Long, Integer> yMap = bigMap.get(genomeNames.get(y));						
						
				String key = getKey(genomeNames.get(x), genomeNames.get(y));
						
				if( resultsMap.containsKey(key))
					throw new Exception("Duplciate key " + key);
						
				resultsMap.put(key, getDistance(xMap, yMap, sumXSquared, kmerSize));
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
		HashMap<String, HashMap<Long,Integer>> bigMap =getBigMap(kmerDir, files, set);
		
		HashMap<String, Float> resultsFromAllTree = parseDistanceFileIgnoringComparisonsToSame(allTreeFile);
				
		HashMap<String, Float> resultsFromConstrainedSet = getResultsFromConstrained(set, bigMap, kmerSize);
		
		writeResults(resultsFromConstrainedSet,  resultsFromAllTree, 
						new File(args[7]), new File(args[1]), args[2], Integer.parseInt(args[3]), 
						Integer.parseInt(args[4]), set, bigMap);
	}
	
	
	private static void writeResults( HashMap<String, Float> constrainedMap, HashMap<String, Float> allTree,
			File outFile, File genomeFile, String contigName, int startPos, int endPos,
			HashSet<String> constrainedSet, HashMap<String, HashMap<Long, Integer>> bigMap) throws Exception
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
		
		Spearman spearman = Spearman.getSpear(list1, list2);
		writer.write(" Spearman distance = " + spearman.getRs() + "\n");
		writer.write(" Spearman p = " + spearman.getProbrs() + "\n");
		writer.write(" Number of comparisons = " + list1.size() + "\n");
		writer.write(" reference genome = " + genomeFile.getAbsolutePath() + "\n");
		writer.write(" contig = " + contigName + "\n");
		writer.write(" start to stop = "  + startPos + " " + endPos +  "\n");
		writer.write(" Number of kmers in constraining set : " + constrainedSet.size() + "\n");
		
		double sum = 0;
		int n=0;
		
		for( String s2 : bigMap.keySet())
		{
			sum += bigMap.get(s2).size();
			n++;
		}
		
		writer.write(" Average # of kmers in target genomes : " + sum / n  + "\n");
		
		writer.write("Unique k-mers for each genome \n" );
		for( String s2 : bigMap.keySet())
		{
			writer.write(s2 + " " + bigMap.get(s2).size() + "\n");
		}
		
		
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
	
	/*
	 * The constraining set has both orientations.
	 * The k-mers it is compared against should be constrained to only check one of the two..
	 */
	private static HashSet<String> getConstrainingSet(String seq, int kmerLength) throws Exception
	{	
		HashSet<String> set = new HashSet<String>();
		
		for( int x=0; x < seq.length()- kmerLength; x++)
		{
			String sub = seq.substring(x, x +  kmerLength);
			
			if( MakeKmers.isACGT(sub))
			{
				set.add(sub);
				set.add(Translate.reverseTranscribe(sub));
			}
		}
		
		
		return set;
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
