import java.net.SocketException;

public class Runner
{
	public static void main(String[] args) throws SocketException
	{
//		P2PNode p2p = new P2PNode();
//		p2p.begin();

		Server server = new Server();
		server.begin();

		Client client = new Client();
		client.begin();
	}
}
