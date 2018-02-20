import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;

/**
 * Team Rusty Buckets
 * High Availability Cluster: Project 1
 * Cameron Moberg, Eli Charleville, Evan Gauer
 */

public class P2PClientSender implements Runnable
{
	private String[] ips = readIps();
	private DatagramSocket socket;
	private Random random = new Random();

	public P2PClientSender() throws SocketException
	{
		this.socket = new DatagramSocket();
		System.out.println(socket.getLocalPort());
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
				DatagramPacket packet = new DatagramPacket(encodedPacket, encodedPacket.length, inetAddress, 45870);
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

	@Override
	public void run()
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

	}
}
