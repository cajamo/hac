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

/**
 * This class contains the implementation at the lowest level of the
 * packet.
 */
public class AvailabilityPacket
{
	private static final int HEADER_SIZE = 4;
	private static final int VERSION = 1;

	private Map<InetAddress, PacketStatus> ips;
	private byte[] payload;
	private boolean heartbeat;
	private byte version;

	/**
	 * Populates payload with a single InetAddress, useful for sending
	 * updates in peering mode.
	 *
	 * @param addr      InetAddress to encode
	 * @param status    PacketState of InetAddress
	 * @param heartbeat Whether or not this also should heartbeat.
	 */
	public AvailabilityPacket(InetAddress addr, PacketStatus status, boolean heartbeat)
	{
		this.heartbeat = heartbeat;
		this.version = VERSION;
		this.payload = encodeSingle(addr, status);
	}

	/**
	 * Populates payload with InetAddress and the corresponding status of that
	 * InetAddress.
	 *
	 * @param ips       Map of InetAddress and PacketStatuses to encode
	 * @param heartbeat Whether or not this also should heartbeat.
	 */
	public AvailabilityPacket(Map<InetAddress, PacketStatus> ips, boolean heartbeat)
	{
		this.heartbeat = heartbeat;
		this.version = VERSION;
		this.ips = ips;
		this.payload = encodeLists();
	}

	/**
	 * Sets the payload, does not automatically decode anything; decode must be
	 * called.
	 *
	 * @param payload Payload received using AvailabilityPacket protocol.
	 */
	public AvailabilityPacket(byte[] payload)
	{
		this.payload = payload;
		this.ips = new HashMap<>();
	}

	/**
	 * Given the current instance payload, decodes it into InetAddresses and
	 * statuses.
	 *
	 * @return returns packet with decoded ips.
	 */
	public AvailabilityPacket decode()
	{
		this.version = payload[0];

		int packetLength = (payload[1] << 8) + payload[2];
		byte flags = payload[3];
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

	/**
	 * Encodes a single InetAddress and status.
	 *
	 * @return InetAddress/Status encoded as our protocol.
	 */
	public byte[] encodeSingle(InetAddress inetAddress, PacketStatus status)
	{
		byte[] bytes = new byte[1024];
		int counter = HEADER_SIZE;

		counter = copyInetAddrToPayload(bytes, counter, inetAddress, status);

		bytes[0] = this.version;
		//Give length field 2 bytes.
		bytes[1] = (byte) ((counter >> 8) & 0xFF);
		bytes[2] = (byte) (counter & 0xFF);
		// If heartbeat set 7th bit to 1, else 0
		bytes[3] = (byte) (heartbeat ? (bytes[2] | 1 << 7) : bytes[2] & ~(1 << 7));
		return bytes;
	}

	/**
	 * Encodes current ips that packet was initialized with.
	 *
	 * @return returns payload encoded as byte[].
	 */
	public byte[] encodeLists()
	{
		byte[] bytes = new byte[1024];
		//leave 2 bits for width
		int counter = HEADER_SIZE;

		if (ips != null)
		{
			for (Map.Entry<InetAddress, PacketStatus> entry : ips.entrySet())
			{
				InetAddress address = entry.getKey();
				PacketStatus status = entry.getValue();
				counter = copyInetAddrToPayload(bytes, counter, address, status);
			}
		}

		bytes[0] = this.version;
		//Give length field 2 bytes.
		bytes[1] = (byte) ((counter >> 8) & 0xFF);
		bytes[2] = (byte) (counter & 0xFF);
		// If heartbeat set 7th bit to 1, else 0
		bytes[3] = (byte) (heartbeat ? (bytes[2] | 1 << 7) : bytes[2] & ~(1 << 7));

		return bytes;
	}

	/**
	 * Adds InetAddress and Status to payload.
	 *
	 * @param bytes   Array to be copied to.
	 * @param counter Starting byte index of payload byte[] array.
	 * @param address InetAddress to be copied.
	 * @param status  Status of address to be copied.
	 * @return New counter number in payload.
	 */
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
