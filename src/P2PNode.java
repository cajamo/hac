import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Team Rusty Buckets
 * High Availability Cluster: Project 1
 * Cameron Moberg, Eli Charleville, Evan Gauer
 */

public class P2PNode implements Runnable
{
	private static int NODE_OFFLINE = 30;
	private static int TIMEOUT = 2500;
	private static int PORT_NUM = 9999;

	private Map<InetAddress, Instant> onlineIpMap = new HashMap<>();
	private List<InetAddress> offlineIpList = new ArrayList<>();

	private String[] ips = readIps();
	private DatagramSocket socket;
	private Random random = new Random();

	public P2PNode() throws SocketException
	{
		socket = new DatagramSocket(PORT_NUM);
		socket.setSoTimeout(TIMEOUT);
	}

	public void sendPacket(AvailabilityPacket proto)
	{
		byte[] encodedPacket = proto.encode();

		for (String socketAddr : ips)
		{
			//In file the ips are formatted like xxx.xxx.xxx.xxx:8888
			String ip = socketAddr.split("\\:")[0];
			int port = Integer.valueOf(socketAddr.split("\\:")[1]);

			try
			{
				InetAddress inetAddress = InetAddress.getByName(ip);
				DatagramPacket packet = new DatagramPacket(encodedPacket, encodedPacket.length,
						inetAddress, port);
				socket.send(packet);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Receives packet, and hands off to another method
	 */
	public void listenPacket()
	{
		byte[] buffer = new byte[6];
		DatagramPacket dP = new DatagramPacket(buffer, buffer.length);
		try
		{
			socket.receive(dP);
		} catch (SocketTimeoutException ignored)
		{
			return;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		handlePayload(dP);
	}

	public void handlePayload(DatagramPacket packet)
	{
		onlineIpMap.put(packet.getAddress(), Instant.now());
	}

	public void pruneNodes()
	{
		for (Map.Entry<InetAddress, Instant> ip : onlineIpMap.entrySet())
		{
			Instant ipLastKnown = ip.getValue();
			if (Instant.now().isAfter(ipLastKnown.plusSeconds(NODE_OFFLINE)))
			{
				offlineIpList.add(ip.getKey());
				onlineIpMap.remove(ip.getKey());
			}
		}
	}

	public void outputIps()
	{
		System.out.println("----- Online -----");
		for (InetAddress ip : onlineIpMap.keySet())
		{
			System.out.println(ip.getHostAddress());
		}
		System.out.println("----- Offline -----");
		for (InetAddress ip : offlineIpList)
		{
			System.out.println(ip.getHostAddress());
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

	@Override
	public void run()
	{
		Instant nextBeat = Instant.now();

		while (true)
		{
			if (!Duration.between(Instant.now(), nextBeat).isNegative())
			{
				listenPacket();
				pruneNodes();
				outputIps();
			} else
			{
				int randSec = random.nextInt(30) + 1;
				nextBeat = Instant.now().plusSeconds(randSec);
				sendPacket(new AvailabilityPacket(new ArrayList<>(onlineIpMap.keySet()), offlineIpList));
			}
		}
	}
}
