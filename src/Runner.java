import java.net.SocketException;

public class Runner
{
	public static void main(String[] args) throws SocketException
	{
		P2PClientReceiver p2p = new P2PClientReceiver();
		Thread thread1 = new Thread(p2p);

		P2PClientSender p2p1 = new P2PClientSender();
		Thread thread2 = new Thread(p2p1);

		thread1.start();
		thread2.start();
	}
}
