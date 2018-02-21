import java.net.SocketException;

public class Runner
{
	public static void main(String[] args) throws SocketException
	{
		P2PNode p2p = new P2PNode();
		Thread thread2 = new Thread(p2p);
		thread2.start();
	}
}
