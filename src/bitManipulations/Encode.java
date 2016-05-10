package bitManipulations;

public class Encode
{
	private static final Long A_LONG = new Long(0x0000l);
	private static final Long C_LONG = new Long(0x0001l);
	private static final Long G_LONG = new Long(0x0002l);
	private static final Long T_LONG = new Long(0x0003l);
	
	public static String getKmer(Long l, int kmerLength)  throws Exception
	{
		StringBuffer buff = new StringBuffer();
		
		Long start = T_LONG;
		
		while(kmerLength > 0)
		{
			long myChar = l & start;
			
			if( myChar == A_LONG )
				buff.append("A");
			
			if( myChar == C_LONG)
				buff.append("C");
			
			if( myChar == G_LONG)
				buff.append("G");
			
			if( myChar == T_LONG)
				buff.append("T");
			
			l = l >> 2;
			kmerLength--;
		}
		
		return buff.toString();
	}
	
	public static long makeLong( String s) throws Exception
 	{
		long val =0;
		
		if( s.length() > 16)
			throw new Exception("Can't encode");
		
		for(int x=0; x < s.length(); x++)
		{
			char c = s.charAt(x);
			
			if( c== 'A')
			{
				val =val | ( A_LONG << (x*2));
			} 
			else if ( c== 'C')
			{
				val = val | (C_LONG << (x*2) );
			}
			else if ( c== 'G')
			{
				val = val | (G_LONG << (x*2) );
			}
			else if ( c == 'T')
			{
				val = val | (T_LONG << (x*2));
			}
			else throw new Exception("Unexpected character " + c);
		}
	

		return val;
	}
	
	public static void main(String[] args) throws Exception
	{
		String s = "TTTTTTTTTT";
	
		long aLong = makeLong(s);
		System.out.println( Long.toBinaryString(aLong));
		
		String kmer = getKmer(aLong, s.length());
		System.out.println(kmer);
		
		if( ! kmer.equals(s) )
			throw new Exception("FAIL!!!!");
		
		System.out.println("pass");
	
	}
}
