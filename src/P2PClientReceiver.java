import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Team Rusty Buckets
 * High Availability Cluster: Project 1
 * Cameron Moberg, Eli Charleville, Evan Gauer
 */

public class P2PClientReceiver implements Runnable
{
	private static int NODE_OFFLINE = 30;
	private static int TIMEOUT = 2500;
	private static int PORT_NUM = 9999;

	private Map<String, Instant> onlineIpMap = new HashMap<>();
	private List<String> offlineIpList = new ArrayList<>();

	private DatagramSocket socket;

	public P2PClientReceiver() throws SocketException
	{
		this.socket = new DatagramSocket(PORT_NUM);
		this.socket.setSoTimeout(TIMEOUT);
	}

	public void pruneNodes()
	{
		for (Map.Entry<String, Instant> ip : onlineIpMap.entrySet())
		{
			Instant ipLastKnown = ip.getValue();
			if (Instant.now().isAfter(ipLastKnown.plusSeconds(NODE_OFFLINE)))
			{
				offlineIpList.add(ip.getKey());
				onlineIpMap.remove(ip.getKey());
			}
		}
	}

	public void listenHeartbeat()
	{
		byte[] buffer = new byte[2];
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
		String ipAddr = dP.getAddress().getHostAddress();

		onlineIpMap.put(ipAddr, Instant.now());
		offlineIpList.removeIf(T -> T.equals(ipAddr));
	}

	public void outputIps()
	{
		System.out.println("----- Online -----");
		for (String ip : onlineIpMap.keySet())
		{
			System.out.println(ip);
		}
		System.out.println("----- Offline -----");
		for (String ip : offlineIpList)
		{
			System.out.println(ip);
		}
	}

	@Override
	public void run()
	{
		while (true)
		{
			listenHeartbeat();
			pruneNodes();
			outputIps();
		}
	}
}
