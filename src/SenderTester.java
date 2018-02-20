import java.net.SocketException;

public class SenderTester
{
	public static void main(String[] args) throws SocketException
	{
		P2PClient p2p = new P2PClient();
		p2p.sendHeartbeat();
	}
}
