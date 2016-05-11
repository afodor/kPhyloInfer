package cluster.worker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class GatherSpearmanResults
{
	public static void main(String[] args) throws Exception
	{
		// this gathers the output of running cluster.qsubCommands.DoConstrainKmerToRegion
		if( args.length != 2 )
		{
			System.out.println("Usage resultsDir summaryFile");
			System.exit(1);
		}
		
		List<Holder> list =parseFiles(args[0]);
		writeResults(list, args[1]);
	}
	
	private static void writeResults(List<Holder> list, String summaryFilePath) throws Exception
	{
		BufferedWriter writer =new BufferedWriter(new FileWriter(summaryFilePath));
		
		writer.write("referenceGenome" + "\t" + "contig" + "\t" + "numberOfKmersInConstrainingSet" + 
					"\t" + "AverageNumberOfKmersInGenome" + "\t" + "numberOfZeroKmers\t" + 
								"startPos" + "\t" + "endPos" + "\t" + 
						"spearmanDistance" + "\n");
		
		for( Holder h : list)
		{
			writer.write( h.referenceGenome + "\t" + h.contig + "\t" + h.numberOfKmersInConstrainingSet+ "\t" + 
						h.averagenumberOfKmersInTargetGenomes+ "\t" + h.numZeros + "\t" +  h.startPos + "\t" + h.endPos + "\t" + 
								h.spearmanDistance + "\n");
		}
		
		
		writer.flush(); writer.close();
	}
	
	private static List<Holder> parseFiles( String resultsFilePath) throws Exception
	{
		File resultsDir = new File(resultsFilePath);
		
		if( ! resultsDir.exists()  || ! resultsDir.isDirectory())
			throw new Exception(resultsDir.getAbsolutePath() + " is not a valid directory");
		
		String[] fileNames = resultsDir.list();
		
		System.out.println("Attempting to parse " + resultsDir.getAbsolutePath());
		
		List<Holder> list = new ArrayList<Holder>();
		
		for(String s : fileNames)
			if( s.startsWith("results_"))
			{
				list.add(parseFile(resultsDir.getAbsolutePath() + File.separator + s));
			}
		
		Collections.sort(list);
		return list;
	}
	
	private static void countZeros(BufferedReader reader , Holder h) throws Exception
	{
		for(String s= reader.readLine(); s != null; s= reader.readLine())
		{
			StringTokenizer sToken = new StringTokenizer(s);
			sToken.nextToken();
		
			if( Integer.parseInt(sToken.nextToken()) == 0 )
				h.numZeros++;
			
			if( sToken.hasMoreTokens())
				throw new Exception("Unexpected format " + s);
			
		}
	}
	
	private static Holder parseFile(String filepath) throws Exception
	{
		/*
		 Spearman distance = 0.5086806409453566
 		Spearman p = 0.0
 		Number of comparisons = 57291
 		reference genome = /nobackup/afodor_research/af_broad/carolina/klebsiella_pneumoniae_chs_11.0.scaffolds.fasta
 		contig = 7000000220927533
 		start to stop = 531000 536000
 		 Number of kmers in constraining set : 4984
 		 Average # of kmers in target genomes : 2882.2064896755164
		Unique k-mers for each genome
		*/
		
		BufferedReader reader =new BufferedReader(new FileReader(new File(filepath)));
		
		Holder h = new Holder();
		
		h.spearmanDistance = extractDouble(reader.readLine(), " Spearman distance = ");
		reader.readLine(); reader.readLine();
		h.referenceGenome = extractString(reader.readLine(), " reference genome = ");
		h.contig= extractString(reader.readLine(), " reference genome = ");
		
		String last =reader.readLine().replace(" start to stop = ", "");
		
		StringTokenizer sToken = new StringTokenizer(last);
		
		h.startPos = Long.parseLong(sToken.nextToken());
		h.endPos = Long.parseLong(sToken.nextToken());
		
		if( sToken.hasMoreTokens())
			throw new Exception("Unexpected format");
		
		h.numberOfKmersInConstrainingSet = extractLong(reader.readLine(), " Number of kmers in constraining set : ");
		h.averagenumberOfKmersInTargetGenomes = extractFloat(reader.readLine(), " Average # of kmers in target genomes :");
		
		reader.readLine();
		
		countZeros(reader, h);
		
		reader.close();
		
		return h;
	}
	
	private static double extractDouble( String line, String prefix )  throws Exception
	{
		if( ! line.startsWith(prefix) )
			throw new Exception("Unexpected format");
		
		return Double.parseDouble(line.replace(prefix, ""));
		
	}
	
	private static float extractFloat( String line, String prefix )  throws Exception
	{
		if( ! line.startsWith(prefix) )
			throw new Exception("Unexpected format");
		
		return Float.parseFloat(line.replace(prefix, ""));
		
	}
	
	private static String extractString( String line, String prefix )  throws Exception
	{
		if( ! line.startsWith(prefix) )
			throw new Exception("Unexpected format");
		
		return line.replace(prefix, "");
		
	}
	
	private static long extractLong( String line, String prefix )  throws Exception
	{
		if( ! line.startsWith(prefix) )
			throw new Exception("Unexpected format");
		
		return Long.parseLong(line.replace(prefix, ""));
		
	}
	
	private static class Holder implements Comparable<Holder>
	{
		double spearmanDistance;
		String referenceGenome;
		String contig;
		long numberOfKmersInConstrainingSet;
		float averagenumberOfKmersInTargetGenomes;
		long startPos;
		long endPos;
		
		int numZeros =0;
		
		@Override
		public int compareTo(Holder o)
		{
			if( !referenceGenome.equals(o.referenceGenome) )
				return referenceGenome.compareTo(o.referenceGenome);
			
			if( ! contig.equals(o.contig))
				return contig.compareTo(o.contig);
			
			return (int) (startPos - o.startPos);
		}
	}
}
