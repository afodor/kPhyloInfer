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
		
		writer.write("referenceGenome" + "\t" + "contig" + "\t" + "numberOfKmersInGenome" + 
					"\t" + "numberOfKmersInAllTree" + "\t" + "startPos" + "\t" + "endPos" + "\t" + 
						"spearmanDistance" + "\n");
		
		for( Holder h : list)
		{
			writer.write( h.referenceGenome + "\t" + h.contig + "\t" + h.numberOfKmersInGenome+ "\t" + 
						h.numberOfKmersInAllTree + "\t" + h.startPos + "\t" + h.endPos + "\t" + 
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
		
		List<Holder> list = new ArrayList<Holder>();
		
		for(String s : fileNames)
			if( s.startsWith("result_"))
			{
				list.add(parseFile(resultsDir.getAbsolutePath() + File.separator + s));
			}
		
		Collections.sort(list);
		return list;
	}
	
	private static Holder parseFile(String filepath) throws Exception
	{
		/*
		Spearman s = Spearman.getSpear(list1, list2);
		writer.write(" Spearman distance = " + s.getRs() + "\n");
		writer.write(" Spearman p = " + s.getProbrs() + "\n");
		writer.write(" Number of kmers in Spearman = " + list1.size() + "\n");
		writer.write(" Number of kmers in all tree = " + allTree.size() + "\n");
		writer.write(" reference genome = " + genomeFile.getAbsolutePath() + "\n");
		writer.write(" contig = " + contigName + "\n");
		writer.write(" start to stop = "  + startPos + " " + endPos +  "\n");
		*/
		
		BufferedReader reader =new BufferedReader(new FileReader(new File(filepath)));
		
		Holder h = new Holder();
		
		h.spearmanDistance = extractDouble(reader.readLine(), " Spearman distance = ");
		reader.readLine();
		h.numberOfKmersInGenome = extractLong( reader.readLine(), " Number of kmers in Spearman = ");
		h.numberOfKmersInAllTree = extractLong(reader.readLine(),  " Number of kmers in all tree = ");
		h.referenceGenome = extractString(reader.readLine(), " reference genome = ");
		h.contig= extractString(reader.readLine(), " reference genome = ");
		
		String last =reader.readLine().replace(" start to stop = ", "");
		
		StringTokenizer sToken = new StringTokenizer(last);
		
		h.startPos = Long.parseLong(sToken.nextToken());
		h.endPos = Long.parseLong(sToken.nextToken());
		
		if( sToken.hasMoreTokens())
			throw new Exception("Unexpected format");
		
		reader.close();
		
		return h;
	}
	
	private static double extractDouble( String line, String prefix )  throws Exception
	{
		if( ! line.startsWith(prefix) )
			throw new Exception("Unexpected format");
		
		return Double.parseDouble(line.replace(prefix, ""));
		
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
		long numberOfKmersInGenome;
		long numberOfKmersInAllTree;
		long startPos;
		long endPos;
		
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
