package chunks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class CovertHitsToGTF
{
	private static final File IN_FILE = new File( "C:\\af_broad\\chs_11_plus_cards.txt");
	private static final File OUT_FILE = new File("C:\\bioLockProjects\\resistantAnnotation\\mgsTochs11.gtf");
	
	public static void main(String[] args) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(IN_FILE));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(OUT_FILE));
		
		reader.readLine();
		
		for(String s= reader.readLine(); s != null; s = reader.readLine())
		{
			String[] splits = s.split("\t");
			
			if( splits[11].trim().length() > 0 && ! splits[4].equals("NA"))
			{
				//7E+15	blast	gb|AY265889|0-458|ARO:3002477|LEN-14	31387	30929	627	+	0	aGene	aGene

				writer.write(splits[4].replace("contig_", "") + "\t");
				writer.write("blast\t");
				writer.write(splits[11]  + "\t");
				writer.write(((int) Float.parseFloat( splits[5])) + "\t");
				writer.write(((int) Float.parseFloat( splits[6])) + "\t");
				writer.write(((int) Float.parseFloat( splits[7])) + "\t");
				writer.write("+\t0\taGene\taGene\n");
			}
		}
		
		writer.flush();  writer.close();
	}
}
