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

	public enum PACKET_STATUS
	{
		OFFLINE(0), ONLINE(1), NEW(2), FAIL(3), REVIVE(4);

		private int statusCode;

		PACKET_STATUS(int statusCode)
		{
			this.statusCode = statusCode;
		}

		public int getStatusCode()
		{
			return this.statusCode;
		}
	}

	public AvailabilityPacket(InetAddress addr, PACKET_STATUS status)
	{
		this.payload = encodeSingle(addr, status);
	}

	public AvailabilityPacket(List<InetAddress> onlineIp, List<InetAddress> offlineIp)
	{
		this.onlineIp = onlineIp;
		this.offlineIp = offlineIp;
		this.payload = encodeLists();
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

	private int copyInetAddrToPayload(byte[] bytes, int counter, InetAddress address, PACKET_STATUS status)
	{
		// 4 if Ipv4 (4 bytes), 16 if ipv6 (16 bytes)
		bytes[counter++] = (byte) ((address instanceof Inet4Address) ? 4 : 16);
		// Set bit to the packet status code.
		bytes[counter++] = (byte) status.getStatusCode();
		// Copy IP Address over
		byte[] ip = address.getAddress();
		System.arraycopy(ip, 0, bytes, counter, ip.length);
		counter += ip.length;

		return counter;
	}

	public byte[] encodeSingle(InetAddress inetAddress, PACKET_STATUS status)
	{
		byte[] bytes = new byte[1024];
		int counter = 2;

		counter = copyInetAddrToPayload(bytes, counter, inetAddress, status);

		//Give length field 2 bytes.
		bytes[0] = (byte) ((counter >> 8) & 0xFF);
		bytes[1] = (byte) (counter & 0xFF);
		return bytes;
	}

	public byte[] encodeLists()
	{
		byte[] bytes = new byte[1024];
		//leave 2 bits for width
		int counter = 2;

		if (onlineIp != null)
		{
			for (InetAddress address : onlineIp)
			{
				counter = copyInetAddrToPayload(bytes, counter, address, PACKET_STATUS.ONLINE);
			}
		}
		if (offlineIp != null)
		{
			for (InetAddress address : offlineIp)
			{
				counter = copyInetAddrToPayload(bytes, counter, address, PACKET_STATUS.ONLINE);
			}
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

	public byte[] getPayload()
	{
		return this.payload;
	}
}
