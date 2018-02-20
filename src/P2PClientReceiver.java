import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Team Rusty Buckets
 * High Availability Cluster: Project 1
 * Cameron Moberg, Eli Charleville, Evan Gauer
 */

public class P2PClientReceiver implements Runnable
{
	private DatagramSocket socket;

	public P2PClientReceiver() throws SocketException
	{
		this.socket = new DatagramSocket();
		System.out.println(socket.getLocalPort());
	}

	public void listenHeartbeat()
	{
		System.out.println("Listening");
		byte[] buffer = new byte[2];
		DatagramPacket dP = new DatagramPacket(buffer, buffer.length);
		try
		{
			socket.receive(dP);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		System.out.println(dP.getAddress() + ":" + dP.getPort() + ": Online");
	}

	@Override
	public void run()
	{
		while(true)
		{
			listenHeartbeat();
		}
	}
}
