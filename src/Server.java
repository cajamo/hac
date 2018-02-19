/**
 * Team Rusty Buckets
 * High Availability Cluster: Project 1
 * Cameron Moberg, Eli Charleville, Evan Gauer
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Server
{
	public String[] readIps() throws IOException
	{
		File file = new File("ips");
		BufferedReader br = new BufferedReader(new FileReader(file));

		String str;
		ArrayList<String> ipList = new ArrayList<>();

		while ((str = br.readLine()) != null)
		{
			ipList.add(str);
		}

		return ipList.toArray((new String[0]));
	}
}


