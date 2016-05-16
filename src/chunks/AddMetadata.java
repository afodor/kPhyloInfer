package chunks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class AddMetadata
{	
	public static void main(String[] args) throws Exception
	{
		if( args.length !=2 )
		{
			System.out.println("Usage fileFromR outputFile");
			System.exit(1);
		}
		
		BufferedReader reader = new BufferedReader(new FileReader(new File(
				args[0])));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(args[1])));
		
		writer.write("originalKey\tgenoneName\tcontig\tstart\tstop\ttype\t");
		
		writer.write(reader.readLine() + "\n");
		
		
		for(String s = reader.readLine(); s != null; s =reader.readLine())
		{
			String[] splits = s.split("\t");
			String[] nameSplits =  s.split("\t")[0].replaceAll("\"", "").split("_");
			
			writer.write(splits[0].replaceAll("\"", "") + "\t");
			
			StringBuffer buff = new StringBuffer();
			for( int x=0 ; x < nameSplits.length -4; x++)
				buff.append(nameSplits[x].replaceAll("\"", "") + (x< nameSplits.length - 5 ? "_" : ""));
			writer.write(buff.toString() + "\t");
			
			writer.write("" + nameSplits[nameSplits.length -4] + "\t");
			writer.write("" + nameSplits[nameSplits.length -3] + "\t");
			writer.write("" + nameSplits[nameSplits.length -2] + "\t");
			writer.write("" + nameSplits[nameSplits.length -1] );
			
			for( int x=1; x < splits.length ; x++)
				writer.write("\t" + splits[x]);
			
			writer.write("\n");
			writer.flush();
		}
		
		
		writer.flush(); writer.close();
		reader.close();
	}
	
	
}
