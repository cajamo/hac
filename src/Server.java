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
import java.util.concurrent.ConcurrentHashMap;

public class Server
{
	private static int PORT_NUM = 9999;
	private static int NODE_TIMEOUT = 31;

	private DatagramSocket socket = null;
	private Random random = new Random();

	private Map<InetAddress, Instant> onlineIpMap = new ConcurrentHashMap<>();
	private List<InetAddress> offlineIpList = new ArrayList<>();

	private String[] ips = readIps();

	/**
	 * Receives packet, and hands off to another method
	 */
	public void listenPacket()
	{
		byte[] buffer = new byte[1024];
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

		onlineIpMap.put(dP.getAddress(), Instant.now());
		offlineIpList.remove(dP.getAddress());
	}

	/**
	 * Sends datagram packet with AvailabilityPacket as payload.
	 *
	 * @param proto Packet wanting to send.
	 */
	public void sendPacket(AvailabilityPacket proto)
	{
		byte[] encodedPacket = proto.getPayload();

		if (encodedPacket == null)
		{
			return;
		}

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
	 * Combines onlineIp and offlineIp into a map that Availability packet can handle.
	 * Could be implemented in the Packet class.
	 *
	 * @return Map containing online/offline ip addresses.
	 */
	private Map<InetAddress, PacketStatus> combineIpsIntoMap()
	{
		HashMap<InetAddress, PacketStatus> map = new HashMap<>();
		for (InetAddress address : onlineIpMap.keySet())
		{
			map.put(address, PacketStatus.ONLINE);
		}
		for (InetAddress address : offlineIpList)
		{
			map.put(address, PacketStatus.OFFLINE);
		}
		return map;
	}

	/**
	 * Removes nodes that've been offline for more than NODE_OFFLINE time
	 * from onlineIp list. Also sends packet to all in ip file that a node has gone offline.
	 */
	private void pruneNodes()
	{
		for (Map.Entry<InetAddress, Instant> ip : onlineIpMap.entrySet())
		{
			Instant ipLastKnown = ip.getValue();
			if (Instant.now().isAfter(ipLastKnown.plusSeconds(NODE_TIMEOUT)))
			{
				System.out.println("Node Assumed Offline - Alerting (Failure): " + ip.getKey().getHostAddress());
				sendPacket(new AvailabilityPacket(ip.getKey(), PacketStatus.FAIL, false));
				offlineIpList.add(ip.getKey());
				onlineIpMap.remove(ip.getKey());
			}
		}
	}

	/**
	 * Reads in IPs from file.
	 *
	 * @return Array of ips in string format xxx.xxx.xxx.xxx:xxxx
	 */
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

	/**
	 * outputs all ips
	 */
	private void outputIps()
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
		System.out.println("----------------");
	}

	/**
	 * Start up the 2 threads, one that receives and outputs, and one that
	 * sends heartbeats out.
	 */
	public void begin()
	{
		try
		{
			this.socket = new DatagramSocket(PORT_NUM);
		} catch (SocketException e)
		{
			e.printStackTrace();
		}

		new Thread(() ->
		{
			while (true)
			{
				listenPacket();
				outputIps();
			}
		}).start();

		new Thread(() ->
		{
			Instant nextBeat = Instant.now();

			while (true)
			{
				if (Duration.between(Instant.now(), nextBeat).isNegative())
				{
					int randSec = random.nextInt(30) + 1;
					nextBeat = Instant.now().plusSeconds(randSec);
					sendPacket(new AvailabilityPacket(combineIpsIntoMap(), true));
				} else
				{
					pruneNodes();
				}
			}

		}).start();
	}
}
