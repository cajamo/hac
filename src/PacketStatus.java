public enum PacketStatus
{
	OFFLINE(0), ONLINE(1), NEW(2), FAIL(3), REVIVE(4);

	private int statusCode;

	PacketStatus(int statusCode)
	{
		this.statusCode = statusCode;
	}

	public int getStatusCode()
	{
		return this.statusCode;
	}

}
