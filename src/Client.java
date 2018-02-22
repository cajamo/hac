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
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Client
{
	private static int PORT_NUM = 9998;

	private DatagramSocket socket;
	private String masterIp = readMasterIp();
	private Random random = new Random();

	private List<InetAddress> onlineIpList = new ArrayList<>();
	private List<InetAddress> offlineIpList = new ArrayList<>();

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
				onlineIpList.add(address);
				break;
			case REVIVE:
				System.out.println("Node revived " + address.getHostAddress());
				onlineIpList.add(address);
				offlineIpList.remove(address);
				break;
			case OFFLINE:
			case FAIL:
				System.out.println("Node Offline/Failed " + address.getHostAddress());
				offlineIpList.add(address);
				onlineIpList.add(address);
				break;
			case ONLINE:
				if (!onlineIpList.contains(address))
				{
					if (offlineIpList.contains(address))
					{
						System.out.println("New Node Available - Alerting (Revived)");
						onlineIpList.add(address);
						offlineIpList.remove(address);
					} else
					{
						System.out.println("New Node Available - Alerting (New)");
						onlineIpList.add(address);
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

			if (address.equals(socket.getInetAddress()))
			{
				continue;
			}
			handleStatus(address, status);
		}
	}

	/**
	 * Sends datagram packet with a byte[0] as payload.
	 */
	private void sendHeartbeat()
	{
		System.out.println("Sending hb");

		//In file the ip is formatted like xxx.xxx.xxx.xxx:8888
		String ip = masterIp.split("\\:")[0];
		int port = Integer.valueOf(masterIp.split("\\:")[1]);

		try
		{
			InetAddress inetAddress = InetAddress.getByName(ip);
			DatagramPacket packet = new DatagramPacket(new byte[]{0, 0}, 2, inetAddress, port);
			socket.send(packet);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * outputs all ips
	 */
	private void outputIps()
	{
		System.out.println("----- Online -----");
		for (InetAddress ip : onlineIpList)
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
	private String readMasterIp()
	{
		File file = new File("masterIp");
		String masterIp = null;
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
			masterIp = br.readLine();

		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return masterIp;
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
					sendHeartbeat();
				}
			}

		});
		
		listener.start();
		sender.start();
	}

}
