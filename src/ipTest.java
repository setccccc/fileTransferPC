import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 通过这个测试发现了原来以前我只返回第一个ip地址
 */
public class ipTest {
    public static void main(String[] args){
        System.out.println(getWlanIp());
    }
    public static String getWlanIp(){
        String ipret="";
        String ip;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                    System.out.println(iface.getDisplayName() + "=" + ip);
                    /*if(ip.contains("192.168")){
                        ipret = ip;
                        break;
                    }*/
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return ipret;
    }
}
