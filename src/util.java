import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by Administrator on 2018/11/15 0015.
 */
public class util {
    //单选文件
    public static File filechoose(){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        JFileChooser jf = new JFileChooser(".");
        if(jf.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            return jf.getSelectedFile();
        }else{
            return null;
        }
    }
    //多选文件
    public static File[] filechooseMulti(){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        JFileChooser jf = new JFileChooser(".");
        jf.setMultiSelectionEnabled(true);
        if(jf.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            return jf.getSelectedFiles();
        }else{
            return null;
        }
    }

    public static int getIntFromBytes(byte high_h, byte high_l, byte low_h, byte low_l) {
        return (high_h & 0xff) << 24 | (high_l & 0xff) << 16 | (low_h & 0xff) << 8 | low_l & 0xff;
    }

    //得到系统所有网卡的所有有效ip给用户参考
    public static String getIPStr(){
        String ipStr = "";
        String ip;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())//剃掉环回地址和不在线的网卡
                    continue;
                //过滤掉vmware网卡
                //p("网卡名字为"+iface.getDisplayName());
                if(iface.getDisplayName().contains("VMware")) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                    //不要ipv6
                    if(ip.contains(":")) continue;

                    //System.out.println(iface.getName()+" = "+iface.getDisplayName() + " = " + ip);
                    ipStr+="\t"+iface.getName()+" = "+iface.getDisplayName() + " = " + ip + "\n";
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return ipStr;
    }

    public static void main(String[] args){
        p(getIPStr());
    }

    public static <T>  void p(T t){
        System.out.println(t);
    }

    public static String getPercent(int cur,int total) {
        int i = (int)((float)cur*100.0/(float)total);
        return i+"%";
    }
}
