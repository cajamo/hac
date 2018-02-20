import java.net.SocketException;

public class ListenerTester
{
	public static void main(String[] args) throws SocketException
	{
		P2PClientReceiver p2p = new P2PClientReceiver();
		p2p.run();
	}
}
