package eigenInference;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import cluster.worker.MakeMatrixWithAllKmers;

public class WriteOutSubsampledAll
{
	private static final Random RANDOM = new Random(324234);
	
	private static final int NUMBER_KMERS_PER_SAMPLE = 2000;
	
	private static final File IN_KMER_DIRECTORY = 
		new File("/nobackup/afodor_research/carolinaRefactor/resampled_" + NUMBER_KMERS_PER_SAMPLE);
	
	private static void writeResampledFile(  HashSet<String> set) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(
			IN_KMER_DIRECTORY	));
		
		List<String> list = new ArrayList<String>(set);
		Collections.sort(list);
		
		writer.write("genome");
		
		for( String s : list)
			writer.write("\t" + s);
		
		writer.write("\n");
		
		String[] files = IN_KMER_DIRECTORY.list();
		
		for( int x=0; x < files.length; x++)
		{
			File xFile = new File( IN_KMER_DIRECTORY.getAbsolutePath() + File.separator +  files[x]);
			
			if( xFile.getName().toLowerCase().endsWith( MakeMatrixWithAllKmers.EXPECTED_SUFFIX))
			{
				HashMap<String, Integer> map= MakeMatrixWithAllKmers.getCounts(xFile, set);
				
				writer.write(files[x].replace(".scaffolds.fasta_kmers.txt.gz", ""));
				
				for(String s : list)
				{
					Integer aVal = map.get(s);
					
					if( aVal == null)
						aVal = 0;
					
					writer.write("\t" + aVal);
				}
			}
			
			writer.write("\n");
			writer.flush();
		}
		
		writer.flush(); writer.close();
	}
	
	public static void main(String[] args) throws Exception
	{
		HashSet<String> set = getConstrainingSet();
		writeResampledFile(set);
	}
	
	private static HashSet<String> getConstrainingSet() throws Exception
	{
		HashSet<String> set = new HashSet<String>();
		
		String[] files = IN_KMER_DIRECTORY.list();
		
		for( int x=0; x < files.length; x++)
		{
			File xFile = new File( IN_KMER_DIRECTORY.getAbsolutePath() + File.separator +  files[x]);
			
			if( xFile.getName().toLowerCase().endsWith( MakeMatrixWithAllKmers.EXPECTED_SUFFIX))
			{
				HashMap<String, Integer> map= MakeMatrixWithAllKmers.getCounts(xFile, null);
				List<String> kmers = new ArrayList<String>(map.keySet());
				Collections.shuffle(kmers, RANDOM);
				
				for( int y=0;y  < NUMBER_KMERS_PER_SAMPLE; y++)
					set.add(kmers.get(y));
					
			}
		}
		
		
		return set;
	}

}
