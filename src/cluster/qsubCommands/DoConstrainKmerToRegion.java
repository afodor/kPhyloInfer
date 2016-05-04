package cluster.qsubCommands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;


import utils.ConfigReader;
import utils.FastaSequence;

public class DoConstrainKmerToRegion
{
	private static BufferedWriter makeNewWriter( BufferedWriter allWriter, int fileNum, File scriptDir ) throws Exception
	{
		File aFile = new File(
				scriptDir.getAbsolutePath() + File.separator + 
					"run_" + fileNum + ".sh");
			
		BufferedWriter aWriter = new BufferedWriter(new FileWriter(aFile));
		
		allWriter.write("qsub -q \"viper_batch\" " + aFile.getAbsolutePath() + "\n");
		
		return aWriter;
			
	}
	
	
	public static void main(String[] args) throws Exception
	{
		if( args.length != 8)
		{
			System.out.println("usage kmerLength referenceGenomeFilepath windowSize kmerDirectory allTreeFile resultsDirectory numberOfJobsPerNodes scriptDir");
			System.exit(1);
		}
		
		Integer numJobsPerNode = null;
		
		try
		{
			numJobsPerNode = Integer.parseInt(args[6]);
		}
		catch(Exception ex)
		{
			
		}
		
		if( numJobsPerNode == null || numJobsPerNode <= 0 )
			throw new Exception("Number of jobs per nodes must be a positive integer");
		
		File scriptDir = new File(args[7]);
		
		if( ! scriptDir.exists() || ! scriptDir.isDirectory())
			throw new Exception("ScriptDir is not a valid directory " + args[7]);
		
		File resultsDiretory = new File(args[5]);
		
		if( ! resultsDiretory.exists() || ! resultsDiretory.isDirectory())
			throw new Exception("ResultsDirectory is not a valid directory " + args[5]);
		
		Integer windowSize = null;
		
		try
		{
			windowSize = Integer.parseInt( args[ 2]);
		}
		catch(Exception ex)
		{
			
		}
		
		if( windowSize == null || windowSize <= 0 )
			throw new Exception("Window size must be a positive integer " );
		
		File allTreeFile = new File(args[4]);
		
		if( ! allTreeFile.exists() || ! allTreeFile.getName().toLowerCase().endsWith("gz"))
			throw new Exception("All tree file must be a zipped comparison file; see MakeMatrixWithAllKmers");
		
		File kmerDir = new File(args[3]);
		
		if( ! kmerDir.exists() || ! kmerDir.isDirectory())
			throw new Exception(args[3] + " is not an exisiting directory");
		
		if( kmerDir.list().length == 0 )
			throw new Exception("kmer directory is empty");
		
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
		
		BufferedWriter allWriter  = new BufferedWriter(new FileWriter(new File(
				scriptDir+ File.separator + "runAll.sh")));
		
		int fileNum =0;
		int index = 0;
		
		BufferedWriter aWriter = makeNewWriter(allWriter, fileNum, scriptDir);
		
		File genomePath = new File(args[2]);
		
		if( ! genomePath.exists())
			throw new Exception("Could not find genome " + args[2]);
		
		List<FastaSequence> list = FastaSequence.readFastaFile(genomePath);
		
		for( FastaSequence fs : list)
		{
			String contig = fs.getFirstTokenOfHeader();
			String seq = fs.getSequence();
			int length = seq.length();
			int slice = Math.min(windowSize, length-1);
			
			for( int x=0; x < Math.max(length-slice, 1); x = x + 1000)
			{
				int end = Math.min(x+slice, length-1);
				
				//kmerLength referenceGenomeFilepath contig startPos endPos kmerDirectory allTreeFile  resultsFile
				aWriter.write("java -cp  " + ConfigReader.getJavaBinPath() 
						+ "cluster.worker.ConstrainKMersToRegion "  + kmerSize + " " + genomePath.getAbsolutePath() + " " + 
						 	contig  + " " + x + " " + end + " " + kmerDir.getAbsolutePath() + " " + 
						 		allTreeFile.getAbsolutePath() + " " + resultsDiretory.getAbsolutePath() + File.separator + 
						 			"results_" + fileNum + "\n");	
				aWriter.flush();
				index++;
				
				if( index == numJobsPerNode)
				{
					index=0;
					fileNum++;
					
					aWriter.flush(); aWriter.close();
					
					aWriter = makeNewWriter(allWriter, fileNum,resultsDiretory);
					
				}
			}
			
		}
		
		aWriter.flush(); aWriter.close();
		allWriter.flush(); allWriter.close();
	}
}
