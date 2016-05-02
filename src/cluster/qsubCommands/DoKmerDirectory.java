package cluster.qsubCommands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import utils.ConfigReader;

public class DoKmerDirectory
{
	public static void main(String[] args) throws Exception
	{
		if( args.length !=4)
		{
			System.out.println("usage kmerSize fastaFileDiretory kmerFileDirectory scriptDirectory");
			System.exit(1);
		}
		
		File fastFileDir = new File(args[1]);
		
		if( ! fastFileDir.exists() || ! fastFileDir.isDirectory())
			throw new Exception(args[1] + " must be a directory");
		
		File kmerFileDir = new File(args[2]);
		
		if( ! kmerFileDir .exists() || ! kmerFileDir .isDirectory())
			throw new Exception(args[2] + " must be a directory");
		
		File scriptDirectory = new File(args[3]);
			
		if( ! scriptDirectory .exists() || ! scriptDirectory .isDirectory())
				throw new Exception(args[3] + " must be a directory");
		
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
		
		int index =1;
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
			scriptDirectory.getAbsolutePath() + File.separator + "runAll.sh"	)));
		
		for(String s : fastFileDir.list())
		{
			if( s.toLowerCase().endsWith("fasta") ||s.toLowerCase().endsWith("fa") )
			{
				File aFile = new File(scriptDirectory.getAbsolutePath() + File.separator + 
						"run_" + index + ".sh"	);
				
				BufferedWriter aWriter = new BufferedWriter(new FileWriter(aFile));
				
				aWriter.write("java -Xmx20g -cp " + ConfigReader.getJavaBinPath() + 
					" cluster.worker.MakeKmers "  + kmerSize + " " +
								fastFileDir.getAbsolutePath() + File.separator + s + " " + 
										kmerFileDir.getAbsolutePath() + File.separator + 
											s + "_kmers.txt"+ "\n");
				
				writer.write("qsub -q \"viper_batch\" " + aFile.getAbsolutePath() + "\n");
				
				aWriter.flush();  aWriter.close();
				writer.flush();
				index++;
			}
		}
		
		writer.flush();  writer.close();
	}
}
