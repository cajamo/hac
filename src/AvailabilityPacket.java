import java.net.InetAddress;

/**
 * Team Rusty Buckets
 * High Availability Cluster: Project 1
 * Cameron Moberg, Eli Charleville, Evan Gauer
 */

public class AvailabilityPacket
{
	private boolean isNodeChange = false;
	private boolean isNodeUp = false;
	private boolean heartbeat = false;

	private InetAddress address;
	private int port;

	public AvailabilityPacket(InetAddress address, int port)
	{
		this.address = address;
		this.port = port;
	}

	public InetAddress getAddress()
	{
		return address;
	}

	public int getPort()
	{
		return port;
	}
}
