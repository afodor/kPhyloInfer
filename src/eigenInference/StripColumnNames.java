package eigenInference;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class StripColumnNames
{
	public static void main(String[] args) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(
				"C:\\carolinaRefactor\\resampledAt_200.txt"));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
				"C:\\carolinaRefactor\\resampledAt_200replacedColumns.txt")));
		
		String[] splits = reader.readLine().split("\t");
		
		
		writer.write("genome");
		
		for( int x=1; x < splits.length; x++)
		{
			writer.write("\ts" + x);
		}
		
		writer.write("\n");
		
		for(String s = reader.readLine(); s != null; s = reader.readLine())
		{
			writer.write( s + "\n");
		}
		
		writer.flush(); writer.close();
		System.out.println(splits.length);
		
		reader.close();
	}
}
