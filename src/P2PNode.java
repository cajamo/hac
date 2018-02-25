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

/**
 * Team Rusty Buckets
 * High Availability Cluster: Project 1
 * Cameron Moberg, Eli Charleville, Evan Gauer
 */

public class P2PNode
{
	private static int NODE_OFFLINE = 30;
	private static int PORT_NUM = 7000;

	private Map<InetAddress, Instant> onlineIpMap = new ConcurrentHashMap<>();
	private List<InetAddress> offlineIpList = new ArrayList<>();

	private String[] ips = readIps();
	private DatagramSocket socket;
	private Random random = new Random();

	/**
	 * Sends datagram packet with AvailabilityPacket as payload.
	 *
	 * @param proto
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
		handlePayload(dP);
	}

	public void handleStatus(InetAddress address, AvailabilityPacket.PACKET_STATUS status)
	{
		switch (status)
		{
			case NEW:
				System.out.println("New Node Available");
				onlineIpMap.put(address, Instant.now());
				break;
			case REVIVE:
				System.out.println("Node revived " + address.getHostAddress());
				onlineIpMap.put(address, Instant.now());
				offlineIpList.remove(address);
				break;
			case OFFLINE:
			case FAIL:
				if (!offlineIpList.contains(address))
				{
					System.out.println("Node Offline/Failed " + address.getHostAddress());
					offlineIpList.add(address);
					onlineIpMap.remove(address);
				}
				break;
			case ONLINE:
				if (!onlineIpMap.containsKey(address))
				{
					if (offlineIpList.contains(address))
					{
						System.out.println("New Node Available - Alerting (Revived)");
						onlineIpMap.put(address, Instant.now());
						offlineIpList.remove(address);
						sendPacket(new AvailabilityPacket(address, AvailabilityPacket.PACKET_STATUS.REVIVE));
					} else
					{
						System.out.println("New Node Available - Alerting (New)");
						onlineIpMap.put(address, Instant.now());
						sendPacket(new AvailabilityPacket(address, AvailabilityPacket.PACKET_STATUS.NEW));
					}
				}
				break;
		}
	}

	/**
	 * Handles the Datagram packet and using our protocol determines status of all ips.
	 *
	 * @param packet DataGram packet received with data in Availability Packet form.
	 */
	public void handlePayload(DatagramPacket packet)
	{
		//Handle sender of packet
		handleStatus(packet.getAddress(), AvailabilityPacket.PACKET_STATUS.ONLINE);

		AvailabilityPacket decoded = new AvailabilityPacket(packet.getData()).decode();

		for (Map.Entry<InetAddress, AvailabilityPacket.PACKET_STATUS> entry : decoded.getIps().entrySet())
		{
			InetAddress address = entry.getKey();
			AvailabilityPacket.PACKET_STATUS status = entry.getValue();

			handleStatus(address, status);
		}
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
			if (Instant.now().isAfter(ipLastKnown.plusSeconds(NODE_OFFLINE)))
			{
				System.out.println("Node Assumed Offline - Alerting (Failure): " + ip.getKey().getHostAddress());
				sendPacket(new AvailabilityPacket(ip.getKey(), AvailabilityPacket.PACKET_STATUS.FAIL));
				offlineIpList.add(ip.getKey());
				onlineIpMap.remove(ip.getKey());
			}
		}
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
	 * Combines onlineIp and offlineIp into a map that Availability packet can handle.
	 * Could be implemented in the Packet class.
	 *
	 * @return Map containing online/offline ip addresses.
	 */
	private Map<InetAddress, AvailabilityPacket.PACKET_STATUS> getAllPackets()
	{
		HashMap<InetAddress, AvailabilityPacket.PACKET_STATUS> map = new HashMap<>();
		for (InetAddress address : onlineIpMap.keySet())
		{
			map.put(address, AvailabilityPacket.PACKET_STATUS.ONLINE);
		}
		for (InetAddress address : offlineIpList)
		{
			map.put(address, AvailabilityPacket.PACKET_STATUS.OFFLINE);
		}
		return map;
	}

	/**
	 * Start up the 2 threads, one that recieves and outputs, and one that
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

		Thread listener = new Thread(() ->
		{
			while (true)
			{

				listenPacket();
				outputIps();
			}
		});

		Thread sender = new Thread(() ->
		{
			Instant nextBeat = Instant.now();

			while (true)
			{
				if (Duration.between(Instant.now(), nextBeat).isNegative())
				{
					int randSec = random.nextInt(30) + 1;
					nextBeat = Instant.now().plusSeconds(randSec);
					sendPacket(new AvailabilityPacket(getAllPackets()));
				} else
				{
					pruneNodes();
				}
			}

		});

		listener.start();
		sender.start();
	}
}
