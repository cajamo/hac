import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Team Rusty Buckets
 * High Availability Cluster: Project 1
 * Cameron Moberg, Eli Charleville, Evan Gauer
 */

public class AvailabilityPacket
{
	private List<InetAddress> onlineIp;
	private List<InetAddress> offlineIp;
	private byte[] payload;

	public AvailabilityPacket(List<InetAddress> onlineIp, List<InetAddress> offlineIp)
	{
		this.onlineIp = onlineIp;
		this.offlineIp = offlineIp;
	}

	public AvailabilityPacket(byte[] payload)
	{
		this.payload = payload;
		this.onlineIp = new ArrayList<>();
		this.offlineIp = new ArrayList<>();
	}

	public AvailabilityPacket decode()
	{
		int packetLength = (payload[0] << 8) + payload[1];
		int counter = 2;

		while (counter < packetLength)
		{
			int ipSize = payload[counter++];
			boolean online = payload[counter++] == 1;
			byte[] ipAddr = Arrays.copyOfRange(payload, counter, counter + ipSize);
			try
			{
				if (online)
				{
					onlineIp.add(InetAddress.getByAddress(ipAddr));
				} else
				{
					offlineIp.add(InetAddress.getByAddress(ipAddr));
				}
			} catch (UnknownHostException e)
			{
				e.printStackTrace();
			}
			counter += ipSize;
		}
		return this;
	}

	private int copyToPayload(byte[] bytes, int counter, List<InetAddress> ipList, boolean online)
	{
		for (InetAddress addr : ipList)
		{
			// 4 if Ipv4 (4 bytes), 16 if ipv6 (16 bytes)
			bytes[counter++] = (byte) ((addr instanceof Inet4Address) ? 4 : 16);
			// 1 for online
			if (online)
			{
				bytes[counter++] = (byte) 1;
			} else
			{
				bytes[counter++] = (byte) 0;
			}
			// Copy IP Address over
			byte[] ip = addr.getAddress();
			System.arraycopy(ip, 0, bytes, counter, ip.length);
			counter += ip.length;
		}
		return counter;
	}

	public byte[] encode()
	{
		byte[] bytes = new byte[1024];
		//leave 2 bits for width
		int counter = 2;

		if (onlineIp != null)
		{
			counter = copyToPayload(bytes, counter, onlineIp, true);
		}
		if (offlineIp != null)
		{
			counter = copyToPayload(bytes, counter, offlineIp, false);
		}

		//Give length field 2 bytes.
		bytes[0] = (byte) ((counter >> 8) & 0xFF);
		bytes[1] = (byte) (counter & 0xFF);
		return bytes;
	}

	public List<InetAddress> getOnlineIp()
	{
		return onlineIp;
	}

	public List<InetAddress> getOfflineIp()
	{
		return offlineIp;
	}
}
