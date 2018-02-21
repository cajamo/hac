import java.net.Inet4Address;
import java.net.InetAddress;
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

	public AvailabilityPacket(List<InetAddress> onlineIp, List<InetAddress> offlineIp)
	{
		this.onlineIp = onlineIp;
		this.offlineIp = offlineIp;
	}

	public byte[] encode()
	{
		byte[] bytes = new byte[1024];
		int counter = 0;

		for (InetAddress addr : onlineIp)
		{
			// 4 if Ipv4 (4 bytes), 16 if ipv6 (16 bytes)
			bytes[counter++] = (byte) ((addr instanceof Inet4Address) ? 4 : 16);
			// 1 for online
			bytes[counter++] = (byte) 1;
			// Copy IP Address over
			System.arraycopy(addr.getAddress(), 0, bytes, counter++, bytes[counter - 3]);
		}

		for (InetAddress addr : offlineIp)
		{
			bytes[counter++] = (byte) ((addr instanceof Inet4Address) ? 4 : 16);
			// 0 for offline
			bytes[counter++] = (byte) 0;
			System.arraycopy(addr.getAddress(), 0, bytes, counter++, bytes[counter - 3]);
		}
		return bytes;
	}
}
