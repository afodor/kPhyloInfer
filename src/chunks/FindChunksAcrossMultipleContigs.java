package chunks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FindChunksAcrossMultipleContigs
{
	private static double getBetweenZeroAndOneOrThrows( String s)
		throws Exception
	{
		Double d = null;
		
		try
		{
			d = Double.parseDouble(s);
		}
		catch(Exception ex)
		{
			
		}
		
		if( d == null || d <= 0 || d >= 1)
			throw new Exception("parameters must be between zero and one " + s);
		
		return d;
	}
	
	public static void main(String[] args) throws Exception
	{
		if( args.length != 4)
		{
			System.out.println("InputSummaryFile OutputChunkFile initializationThreshold extensionThreshold");
		}
		
		File inputSummaryFile = new File(args[0]);
		
		if( ! inputSummaryFile.exists())
			throw new Exception("Could not find " + inputSummaryFile.getAbsolutePath());
		
		List<IndividualHolder> list = getList(inputSummaryFile);
		
		double initializationThreshold = getBetweenZeroAndOneOrThrows(args[2]);
		double extensionThreshold = getBetweenZeroAndOneOrThrows(args[3]);
		
		List<ChunkHolder> chunks = getChunks(list, initializationThreshold,extensionThreshold);
		writeChunks(chunks, new File(args[1]));
 	}
	
	
	private static void writeChunks( List<ChunkHolder> list , File outFile) throws Exception
	{
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
		
		writer.write("Contig\tstart\tend\tlength\taverage\n");
		
		for(ChunkHolder ch : list)
		{
			writer.write("\"Contig_" +ch.contig +"\"" + "\t");
			writer.write(ch.start+ "\t");
			writer.write(ch.end+ "\t");
			writer.write(ch.n + "\t");
			writer.write( ch.spearmanSum / ch.n + "\n");
		}
		
		writer.flush();  writer.close();
	}
	
	
	private static List<IndividualHolder> getList(File inFile) throws Exception
	{
		List<IndividualHolder> list = new ArrayList<IndividualHolder>();
		
		BufferedReader reader = new BufferedReader(new FileReader(inFile));
		
		reader.readLine();
		
		for(String s =reader.readLine(); s != null; s= reader.readLine())
		{
			String[] splits = s.split("\t");
			
			if( splits.length != 8)
				throw new Exception("No");
			
			IndividualHolder h = new IndividualHolder();
			list.add(h);
			h.contig =  splits[1] + "@" + splits[0];
			h.startPos = Integer.parseInt(splits[5]);
			h.endPos = Integer.parseInt(splits[6]);
			h.spearman= Double.parseDouble(splits[7]);
		}
		
		Collections.sort(list);
		return list;
	}
	
	private static class ChunkHolder
	{
		private final String contig;
		private int start;
		private int end;
		double spearmanSum =0;
		int n;
		
		public ChunkHolder(String contig, int start, int end, double initialSpearman)
		{
			this.contig = contig;
			this.start= start;
			this.end = end;
			
			this.n = 1;
			this.spearmanSum += initialSpearman;
			
		}
		
		
	}
	

	private static boolean positionIsInChunk(List<ChunkHolder> list, int position)
	{
		for(ChunkHolder ch : list)
			if( ch.start>= position && ch.end<= position)
				return true;
		
		return false;
	}
	
	private static List<ChunkHolder> getChunks( List<IndividualHolder> list, double initializationThreshold,
				double extensionThreshold)
	{
		List<ChunkHolder> chunks = new ArrayList<ChunkHolder>();
		
		ChunkHolder currentChunk = null;
		
		int lastIndex = 0;
		
		while(lastIndex < list.size())
		{
			IndividualHolder thisHolder = list.get(lastIndex);
			if( currentChunk == null)
			{
				if(thisHolder.spearman<= initializationThreshold)
				{
					currentChunk = new ChunkHolder(thisHolder.contig,thisHolder.startPos,thisHolder.endPos,
							thisHolder.spearman);
					chunks.add(currentChunk);
					
					int lookBack = lastIndex -1;
					
					boolean stop = false;
					while(lookBack > 0 && ! stop)
					{
						IndividualHolder previous = list.get(lookBack);
						
						if( previous.contig.equals(currentChunk.contig) && 
										previous.spearman<= extensionThreshold&& 
								! positionIsInChunk(chunks, previous.startPos))
						{
							currentChunk.start= previous.startPos;
							currentChunk.n = currentChunk.n + 1;
							currentChunk.spearmanSum += thisHolder.spearman;
							lookBack--;
						}
						else
						{
							stop = true;
						}
					}
				}
			}
			else
			{
				if( thisHolder.spearman<= extensionThreshold
						&& thisHolder.contig.equals(currentChunk.contig))
				{
					currentChunk.end= thisHolder.endPos;
					currentChunk.n = currentChunk.n + 1;
					currentChunk.spearmanSum+= thisHolder.spearman;
				}
				else if( thisHolder.spearman<= initializationThreshold)
				{
					// if we are here, new chunk on new contig
					currentChunk = new ChunkHolder(thisHolder.contig,thisHolder.startPos,thisHolder.endPos,
							thisHolder.spearman);
					chunks.add(currentChunk);
				}
				else
				{
					currentChunk = null;
				}
			}
			
			lastIndex++;
		}
		
		return chunks;
	}
	
	private static class IndividualHolder implements Comparable<IndividualHolder>
	{
		String contig;
		int startPos;
		int endPos;
		double spearman;
		
		@Override
		public int compareTo(IndividualHolder o)
		{
			if( ! this.contig.equals(o.contig) )
			{
				return this.contig.compareTo(o.contig);
			}
			
			return this.startPos - o.startPos;
		}
	}
}
