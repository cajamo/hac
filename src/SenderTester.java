import java.net.SocketException;

public class SenderTester
{
	public static void main(String[] args) throws SocketException
	{
		P2PClientSender p2p = new P2PClientSender();
		p2p.run();
	}
}
