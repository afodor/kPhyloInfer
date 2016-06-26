package chunks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ConvertSummaryToIGV
{
	private static final File inFile = new File("C:\\bioLockProjects\\resistantAnnotation\\resultsSummaryRef11.txt");
	private static final File outFile = 
			new File("C:\\bioLockProjects\\resistantAnnotation\\distanceToAllTreeChs11.igv");
	
	
	public static void main(String[] args) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(inFile));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
		
		writer.write("Chromosome\tStart\tEnd\tFeature\tdistanceToAllTree\n");
		
		reader.readLine();
		
		for(String s = reader.readLine(); s != null; s= reader.readLine())
		{
			String[] splits = s.split("\t");
			
			if( splits.length != 8)
				throw new Exception("No");
			
			writer.write(splits[1] + "\t");
			writer.write(splits[5] + "\t");
			writer.write(splits[6] + "\t");
			writer.write("dist\t");
			writer.write(splits[7] + "\n");
		}
		
		writer.flush();  writer.close();
		
		reader.close();
	}
}
