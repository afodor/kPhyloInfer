/** 
 * Author:  anthony.fodor@gmail.com    
 * This code is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version,
* provided that any use properly credits the author.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details at http://www.gnu.org * * */


package utils;

import java.io.InputStream;

import java.util.Properties;

public class ConfigReader
{
	public static final String PROPERTIES_FILE = "kPhyloInfer.properties";

	private static ConfigReader configReader = null;
	private static Properties props = new Properties();
	
	public static final String JAVA_BIN_PATH = "JAVA_BIN_PATH";
	
	public static String getJavaBinPath() throws Exception 
	{
		return getConfigReader().getAProperty(JAVA_BIN_PATH);
	}


	private static String getAProperty(String namedProperty) throws Exception
	{
		Object obj = props.get(namedProperty);

		if (obj == null)
			throw new Exception("Error!  Could not find " + namedProperty
					+ " in " + PROPERTIES_FILE);

		return obj.toString();
	}

	
	private ConfigReader() throws Exception
	{
		Object o = new Object();

		InputStream in = o.getClass().getClassLoader()
				.getSystemResourceAsStream(PROPERTIES_FILE);

		if (in == null)
			throw new Exception("Error!  Could not find " + PROPERTIES_FILE
					+ " anywhere on the current classpath");

		props = new Properties();
		props.load(in);

	}

	private static synchronized ConfigReader getConfigReader() throws Exception
	{
		if (configReader == null)
		{
			configReader = new ConfigReader();
		}

		return configReader;
	}
}
