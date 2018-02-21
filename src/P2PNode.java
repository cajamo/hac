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

public class P2PNode
{
	private static int NODE_OFFLINE = 30;
	private static int PORT_NUM = 9999;

	private Map<InetAddress, Instant> onlineIpMap = new HashMap<>();
	private List<InetAddress> offlineIpList = new ArrayList<>();

	private String[] ips = readIps();
	private DatagramSocket socket;
	private Random random = new Random();

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

	public void handlePayload(DatagramPacket packet)
	{
		//Add sender of message as online.
		if (!onlineIpMap.containsKey(packet.getAddress()))
		{
			onlineIpMap.put(packet.getAddress(), Instant.now());
		}

		AvailabilityPacket decoded = new AvailabilityPacket(packet.getData()).decode();

		for (Map.Entry<InetAddress, AvailabilityPacket.PACKET_STATUS> entry : decoded.getIps().entrySet())
		{
			InetAddress address = entry.getKey();
			AvailabilityPacket.PACKET_STATUS status = entry.getValue();

			switch (status)
			{
				case NEW:
					onlineIpMap.put(address, Instant.now());
					break;
				case REVIVE:
					onlineIpMap.put(address, Instant.now());
					offlineIpList.remove(address);
					break;
				case OFFLINE:
				case FAIL:
					offlineIpList.add(address);
					onlineIpMap.remove(address);
					break;
				case ONLINE:
					onlineIpMap.put(address, Instant.now());
					if (offlineIpList.contains(address))
					{
						offlineIpList.remove(address);
						sendPacket(new AvailabilityPacket(address, AvailabilityPacket.PACKET_STATUS.REVIVE));
					}
			}
		}
	}

	public void pruneNodes()
	{
		for (Map.Entry<InetAddress, Instant> ip : onlineIpMap.entrySet())
		{
			Instant ipLastKnown = ip.getValue();
			if (Instant.now().isAfter(ipLastKnown.plusSeconds(NODE_OFFLINE)))
			{
				sendPacket(new AvailabilityPacket(ip.getKey(), AvailabilityPacket.PACKET_STATUS.FAIL));
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
					sendPacket(new AvailabilityPacket(new ArrayList<>(onlineIpMap.keySet()), offlineIpList));
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
