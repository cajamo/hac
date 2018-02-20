/**
 * Team Rusty Buckets
 * High Availability Cluster: Project 1
 * Cameron Moberg, Eli Charleville, Evan Gauer
 */

public class AvailabilityPacket
{
	private boolean isNodeChange;
	private boolean isNodeAlive;

	public AvailabilityPacket(boolean isNodeChange, boolean isNodeAlive)
	{
		this.isNodeChange = isNodeChange;
		this.isNodeAlive = isNodeAlive;
	}

	public byte[] encode()
	{
		byte[] bytes = {(byte) (isNodeAlive ? 1 : 0), (byte) (isNodeChange ? 1 : 0)};
		return bytes;
	}
}
