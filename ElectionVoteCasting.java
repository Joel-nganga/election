import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ElectionVoteCasting {

    private static final String MULTICAST_ADDRESS = "230.0.0.0";
    private static final int PORT = 4446;
    private static final int ELECTORATE_COUNT = 5;

    public static void main(String[] args) {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);

            MulticastSocket socket = new MulticastSocket(PORT);

            // Get the network interface associated with the multicast address
            NetworkInterface networkInterface = getNetworkInterfaceForAddress(group);

            // Join the multicast group on this network interface
            if (networkInterface != null) {
                socket.joinGroup(new InetSocketAddress(group, PORT), networkInterface);
            } else {
                System.err.println("Failed to retrieve network interface for multicast address.");
                System.exit(1);
            }

            // Map to store votes of each electorate
            Map<Integer, Character> votes = new HashMap<>();

            // Listen for votes from electorates
            for (int i = 0; i < ELECTORATE_COUNT; i++) {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                // Extract vote and store it in the map
                char vote = (char) buf[0];
                votes.put(i, vote);

                // Display the received vote
                System.out.println("Electorate " + i + " voted for: " + vote);

                // Broadcast the vote to other electorates
                DatagramPacket reply = new DatagramPacket(buf, buf.length, group, PORT);
                socket.send(reply);
            }

            // Determine the winner
            int countA = 0, countB = 0;
            for (char vote : votes.values()) {
                if (vote == 'A') {
                    countA++;
                } else if (vote == 'B') {
                    countB++;
                }
            }

            if (countA > countB) {
                System.out.println("Candidate A wins with " + countA + " votes.");
            } else if (countB > countA) {
                System.out.println("Candidate B wins with " + countB + " votes.");
            } else {
                System.out.println("It's a tie.");
            }

            socket.leaveGroup(new InetSocketAddress(group, PORT), networkInterface);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to retrieve the network interface associated with the given multicast address
    private static NetworkInterface getNetworkInterfaceForAddress(InetAddress address) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address && addr.equals(address)) {
                            return networkInterface;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
}
