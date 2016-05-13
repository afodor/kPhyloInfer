package bitManipulations;

public class Encode
{
	private static final Integer A_INT = new Long(0x0000l).intValue();
	private static final Integer C_INT = new Long(0x0001l).intValue();
	private static final Integer G_INT = new Long(0x0002l).intValue();
	private static final Integer T_INT = new Long(0x0003l).intValue();
	
	public static String getKmer(Integer i, int kmerLength)  throws Exception
	{
		StringBuffer buff = new StringBuffer();
		
		int start = T_INT;
		
		while(kmerLength > 0)
		{
			long myChar = i & start;
			
			if( myChar == A_INT )
				buff.append("A");
			
			if( myChar == C_INT)
				buff.append("C");
			
			if( myChar == G_INT)
				buff.append("G");
			
			if( myChar == T_INT)
				buff.append("T");
			
			i = i >> 2;
			kmerLength--;
		}
		
		return buff.toString();
	}
	
	public static int makeInteger( String s) throws Exception
 	{
		int val =0;
		
		if( s.length() > 16)
			throw new Exception("Can't encode");
		
		for(int x=0; x < s.length(); x++)
		{
			char c = s.charAt(x);
			
			if( c== 'A')
			{
				val =val | ( A_INT << (x*2));
			} 
			else if ( c== 'C')
			{
				val = val | (C_INT << (x*2) );
			}
			else if ( c== 'G')
			{
				val = val | (G_INT << (x*2) );
			}
			else if ( c == 'T')
			{
				val = val | (T_INT << (x*2));
			}
			else throw new Exception("Unexpected character " + c);
		}
	

		return val;
	}
	
	public static void main(String[] args) throws Exception
	{
		String s = "CGATTACTACCGAGAC";
	
		int aLong = makeInteger(s);
		System.out.println( Long.toBinaryString(aLong));
		
		String kmer = getKmer(aLong, s.length());
		System.out.println(kmer);
		
		if( ! kmer.equals(s) )
			throw new Exception("FAIL!!!!");
		
		System.out.println("pass");
	
	}
}
