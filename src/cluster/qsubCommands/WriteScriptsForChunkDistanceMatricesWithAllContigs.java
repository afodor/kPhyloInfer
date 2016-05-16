package cluster.qsubCommands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;

import utils.ConfigReader;
import utils.FastaSequence;

public class WriteScriptsForChunkDistanceMatricesWithAllContigs
{	
	private static void writeOneIfFileDoesNotExist(BufferedWriter allWriter, File scriptDirectory,
			File genomePath, File outFileDirectory, File kmerDir,
			String contig,
				int startPos, int endPos, int index, String type, int kmerSize) 
		throws Exception
	{
		File shFile = new File(
				scriptDirectory.getAbsolutePath() + File.separator + "run_" + index + ".txt"	);
		
		String outFileBase = genomePath.getName().replace(".scaffolds.fasta", "") 
					+ "_" + contig + "_" + startPos + "_" + endPos + "_" + type + ".txt";
		
		File outFile = new File( outFileDirectory.getAbsolutePath() + File.separator + outFileBase);
		
		if( !outFile.exists())
		{
			
			BufferedWriter aWriter = new BufferedWriter(new FileWriter(shFile));
				
			allWriter.write("qsub -q \"viper\" " +  shFile.getAbsolutePath() + "\n");
				
			aWriter.write("java -Xms20g -cp  " + ConfigReader.getJavaBinPath() 
			+ " cluster.worker.WriteDistanceMatrixFromConstrainedRegion "  
					+ kmerSize + " " + genomePath.getAbsolutePath() + " " + 
			 	contig  + " " + startPos + " " + endPos + " " + kmerDir.getAbsolutePath() + " " + 
						outFile.getAbsolutePath() + 
			 	 "\n");	
				
				allWriter.flush(); 
				
				aWriter.flush();  aWriter.close();

		}
	}
	
	private static class Holder
	{
		private final int start;
		private final int end;
		
		Holder( int start, int end)
		{
			this.start = start;
			this.end = end;
		}
	}
	
	private static HashMap<String, List<Holder>> getAsContigMap(File chunkFile) throws Exception
	{
		HashMap<String, List<Holder>>  map = new LinkedHashMap<String,List<Holder>>();
		
		BufferedReader reader = new BufferedReader(new FileReader(chunkFile));
		
		reader.readLine();
		
		for(String s= reader.readLine(); s != null; s= reader.readLine())
		{
			String[] splits = s.split("\t");
			
			if( splits.length != 5)
				throw new Exception("No");
			
			String contig = splits[0].replace("\"", "").replace("Contig_", "");
			
			StringTokenizer sToken = new StringTokenizer(contig, "@");
			
			contig = sToken.nextToken();
			sToken.nextToken();
			
			if( sToken.hasMoreTokens())
				throw new Exception("Sorry. Contig names can't have the @ symbol");
			
			List<Holder> list = map.get(contig);
			
			if( list == null)
			{
				list = new ArrayList<Holder>();
				map.put(contig, list);
			}
			
			Holder h = new Holder(Integer.parseInt(splits[1]), Integer.parseInt(splits[2]));
			list.add(h);
		}
		
		reader.close();
		
		return map;
	}
	
	public static void main(String[] args) throws Exception
	{
		
		if( args.length != 6)
		{
			System.out.println("usage kmerLength inputChunkFile scriptDirectory kmerDirectory resultsDirectory referenceGenome");
			System.exit(1);

		}
		
		Integer kmerSize = null;
				
		try
		{
			kmerSize = Integer.parseInt(args[0]);
		}
		catch(Exception ex)
		{
			
		}
		
		if( kmerSize == null || kmerSize <= 0 )
			throw new Exception("Kmer size must be a positive integer ");
		
		File scriptDir = new File(args[2]);
		
		if( ! scriptDir.exists() || ! scriptDir.isDirectory())
		{
			throw new Exception(args[2] + " is not a valid directory");
		}
		
		BufferedWriter allWriter  = new BufferedWriter(new FileWriter(new File(
			scriptDir.getAbsolutePath() + File.separator + 
			"runAll.sh")));
		
		File chunkFile = new File(args[1]);
		
		if( !chunkFile.exists())
			throw new Exception("Could not find " + chunkFile.getAbsolutePath());
		
		HashMap<String, List<Holder>> contigMap = getAsContigMap(chunkFile);
		
		File referenceGenome = new File(args[5]);
		
		if(! referenceGenome.exists())
			throw new Exception("Could not find " + referenceGenome.getAbsolutePath());
		
		File resultsDir = new File(args[4]);
		
		if(! resultsDir.exists() || ! resultsDir.isDirectory())
			throw new Exception(resultsDir.getAbsolutePath() + " must be a valid directory");
		
		File kMerDir = new File(args[3]);
		
		if(! kMerDir.exists() || ! kMerDir.isDirectory())
			throw new Exception( kMerDir.getAbsolutePath() + " must be a valid directory");
		
		
		HashMap<String, FastaSequence> fastaMap = FastaSequence.getFirstTokenSequenceMap(referenceGenome);
		HashSet<String> includedContigs = new HashSet<String>();
		
		int index = 1;
		for(String contig : contigMap.keySet())
		{
			List<Holder> list = contigMap.get(contig);
			includedContigs.add(contig);
			
			//BufferedWriter allWriter, File scriptDirectory,
			//File genomePath, File outFileDirectory, File kmerDir,
			//String contig,
				//int startPos, int endPos, int index, String type, int kmerSize
			if( list.size() == 1)
			{
				Holder h = list.get(0);
				writeOneIfFileDoesNotExist(allWriter, scriptDir, referenceGenome, resultsDir,kMerDir,
								contig, h.start, h.end, index,"singleton", kmerSize);
				index++;
			}
			else
			{
				int listIndex =0;
				int contigLength = fastaMap.get(contig).getSequence().length() -1;
				
				if( list.get(0).start >0)
				{
					writeOneIfFileDoesNotExist(allWriter, scriptDir, referenceGenome, resultsDir,kMerDir,
							contig, 0, list.get(0).start -1000, index,"initialBaseline", kmerSize);
					
					index++;
				}
					
				while(listIndex < list.size() -1)
				{
					
					writeOneIfFileDoesNotExist(allWriter, scriptDir, referenceGenome, resultsDir,kMerDir,
							contig, list.get(listIndex).start, list.get(listIndex).end, index,"peak", kmerSize);
					
					index++;
					listIndex++;
					
					if( list.get(listIndex-1).end +5000 < contigLength )
					{
						String type = "endBaseline";
						int endPos = contigLength;
						
						if( listIndex < list.size() -1 )
						{
							endPos = list.get(listIndex).start -1000;
							type = "baseline";
						}
						
						if( endPos - list.get(listIndex-1).end+1000 >= 5000)
						{
							
							writeOneIfFileDoesNotExist(allWriter, scriptDir, referenceGenome, resultsDir,kMerDir,
									contig, list.get(listIndex-1).end+1000,endPos, index,type, kmerSize);
							
							
							index++;

						}							
					}
					
				}
			}	
		}
		
		for(String s : fastaMap.keySet())
			if( ! includedContigs.contains(s))
			{

				writeOneIfFileDoesNotExist(allWriter, scriptDir, referenceGenome, resultsDir,kMerDir,
						s, 0,fastaMap.get(s).getSequence().length()-1, index,"singleton", kmerSize);
				
				index++;
			}
		
		allWriter.flush();  allWriter.close();
	}
}
