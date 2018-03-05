import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Team Rusty Buckets
 * High Availability Cluster: Project 1
 * Cameron Moberg, Eli Charleville, Evan Gauer
 */

public class AvailabilityPacket
{
	private static final int HEADER_SIZE = 3;

	private Map<InetAddress, PacketStatus> ips;
	private byte[] payload;
	private boolean heartbeat;

	public AvailabilityPacket(InetAddress addr, PacketStatus status, boolean heartbeat)
	{
		this.heartbeat = heartbeat;
		this.payload = encodeSingle(addr, status);
	}

	public AvailabilityPacket(Map<InetAddress, PacketStatus> ips, boolean heartbeat)
	{
		this.heartbeat = heartbeat;
		this.ips = ips;
		this.payload = encodeLists();
	}

	public AvailabilityPacket(byte[] payload)
	{
		this.payload = payload;
		this.ips = new HashMap<>();
	}

	public AvailabilityPacket decode()
	{
		int packetLength = (payload[0] << 8) + payload[1];
		byte flags = payload[2];
		this.heartbeat = ((flags >> 7) & 1) == 1;
		int counter = HEADER_SIZE;

		while (counter < packetLength)
		{
			int ipSize = payload[counter++];
			int statusCode = payload[counter++];
			byte[] ipAddr = Arrays.copyOfRange(payload, counter, counter + ipSize);

			try
			{
				InetAddress address = InetAddress.getByAddress(ipAddr);
				PacketStatus status = PacketStatus.values()[statusCode];
				ips.put(address, status);
			} catch (UnknownHostException e)
			{
				e.printStackTrace();
			}
			counter += ipSize;
		}
		return this;
	}

	private int copyInetAddrToPayload(byte[] bytes, int counter, InetAddress address, PacketStatus status)
	{
		// 4 if Ipv4 (4 bytes), 16 if ipv6 (16 bytes)
		bytes[counter++] = (byte) ((address instanceof Inet6Address) ? 16 : 4);
		// Set bit to the packet status code.
		bytes[counter++] = (byte) status.getStatusCode();
		// Copy IP Address over
		byte[] ip = address.getAddress();
		System.arraycopy(ip, 0, bytes, counter, ip.length);
		counter += ip.length;

		return counter;
	}

	public byte[] encodeSingle(InetAddress inetAddress, PacketStatus status)
	{
		byte[] bytes = new byte[1024];
		int counter = HEADER_SIZE;

		counter = copyInetAddrToPayload(bytes, counter, inetAddress, status);

		//Give length field 2 bytes.
		bytes[0] = (byte) ((counter >> 8) & 0xFF);
		bytes[1] = (byte) (counter & 0xFF);
		// If heartbeat set 7th bit to 1, else 0
		bytes[2] = (byte) (heartbeat ? (bytes[2] | 1 << 7) : bytes[2] & ~(1 << 7));
		return bytes;
	}

	public byte[] encodeLists()
	{
		byte[] bytes = new byte[1024];
		//leave 2 bits for width
		int counter = HEADER_SIZE;

		if (ips != null)
		{
			for (Map.Entry<InetAddress, PacketStatus> entry : ips.entrySet())
			{
				counter = copyInetAddrToPayload(bytes, counter, entry.getKey(), entry.getValue());
			}
		}

		//Give length field 2 bytes.
		bytes[0] = (byte) ((counter >> 8) & 0xFF);
		bytes[1] = (byte) (counter & 0xFF);
		// If heartbeat set 7th bit to 1, else 0
		bytes[2] = (byte) (heartbeat ? (bytes[2] | 1 << 7) : bytes[2] & ~(1 << 7));

		return bytes;
	}

	public boolean isHeartbeat()
	{
		return this.heartbeat;
	}
	
	public Map<InetAddress, PacketStatus> getIps()
	{
		return ips;
	}

	public byte[] getPayload()
	{
		return this.payload;
	}
}
