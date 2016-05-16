package chunks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import utils.Spearman;

public class MakeSquareMatrix
{
	public static void main(String[] args) throws Exception
	{
		if( args.length != 2)
		{
			System.out.println("Usage inputComparisonDirectory OutputFile");
			System.exit(1);
		}
		
		File topDir = new File(args[0]);
		
		if( ! topDir.exists() || ! topDir.isDirectory())
			throw new Exception(args[0] + "  is not a valid directory");
		
		writeResults(topDir, args[1]);
	} 	
	
	private static void writeResults( File topDir, String outFilePath ) throws Exception
	{
		String[] files = topDir.list();
		
		List<File> list =new ArrayList<File>();
		
		for( String s : files)
			if( s.endsWith(".txt"))
				list.add(new File(topDir.getAbsolutePath() + File.separator + s));
		
		Collections.sort(list);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
			outFilePath	)));
		
		for( int x=0;x  < list.size(); x++ )
		{
			System.out.println( x + " " + list.size());
			writer.write(list.get(x).getName().replace(".txt", ""));
			
			HashMap<String, Float> xMap = parseFile(list.get(x));
			
			for( int y=0; y < list.size(); y++)
			{
				HashMap<String, Float> yMap = parseFile(list.get(y));
				writer.write("\t" + getDistance(xMap, yMap));
				
			}
			
			writer.write("\n");
		}
		
		writer.flush();  writer.close();
	}
	
	private static double getDistance(HashMap<String, Float> map1, HashMap<String, Float> map2)
		throws Exception
	{
		List<Float> list1 = new ArrayList<Float>();
		List<Float> list2 = new ArrayList<Float>();
		
		for(String s : map1.keySet())
			if( map2.containsKey(s))
			{
				list1.add(map1.get(s));
				list2.add(map2.get(s));
			}
		
		if( list1.size() < 3 ) 
			throw new Exception("Must have at least 3 comparisons");
		
		return Spearman.getSpear(list1, list2).getRs();
 	}
	
	
	private static HashMap<String,Float> parseFile(File f) throws Exception
	{
		HashMap<String, Float> map = new HashMap<String,Float>();
		
		BufferedReader reader = new BufferedReader(new FileReader(f));
		
		reader.readLine();
		
		for(String s= reader.readLine(); s != null; s = reader.readLine())
		{
			String[] splits = s.split("\t");
			
			if( splits.length != 3)
				throw new Exception("Parsing error");
			
			
			if( splits[0].indexOf("@") != -1 || splits[1].indexOf("@") != -1)
				throw new Exception("Genome names cannot contain @");
			
			List<String> list = new ArrayList<String>();
			list.add(splits[0]);
			list.add(splits[1]);
			Collections.sort(list);
			String key = list.get(0) + "@" + list.get(1);
			
			if( map.containsKey(key))
				throw new Exception("Duplicate key " + key);
			
			map.put(key, Float.parseFloat(splits[2]));
			
		}
		
		reader.close();
		
		return map;
	}
}
