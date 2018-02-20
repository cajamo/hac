import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Team Rusty Buckets
 * High Availability Cluster: Project 1
 * Cameron Moberg, Eli Charleville, Evan Gauer
 */

public class P2PClient
{
	private String[] ips = readIps();
	private DatagramSocket socket;

	public P2PClient() throws SocketException
	{
		this.socket = new DatagramSocket();
		System.out.println(socket.getLocalPort());
	}

	public void listenHeartbeat()
	{
		System.out.println("Listening");
		byte[] buffer = new byte[2];
		DatagramPacket dP = new DatagramPacket(buffer, buffer.length);
		try
		{
			socket.receive(dP);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		System.out.println(Arrays.toString(dP.getData()));
	}

	public void sendHeartbeat()
	{
		AvailabilityPacket heartbeat = new AvailabilityPacket(false, true);
		byte[] encodedPacket = heartbeat.encode();

		for (String ip : ips)
		{
			try
			{
				InetAddress inetAddress = InetAddress.getByName(ip);
				DatagramPacket packet = new DatagramPacket(encodedPacket, encodedPacket.length, inetAddress, 38528);
				socket.send(packet);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private String[] readIps()
	{
		File file = new File("ips");
		ArrayList<String> ipList = new ArrayList<>();

		try
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String str;
			while ((str = br.readLine()) != null)
			{
				ipList.add(str);
			}

		} catch (IOException e)
		{
			e.printStackTrace();
		}

		return ipList.toArray((new String[0]));
	}
}
