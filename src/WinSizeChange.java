import org.w3c.dom.css.Rect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;

public class WinSizeChange extends ComponentAdapter {
    JFrame jf;Rectangle r;
    socketServer ua;
    public WinSizeChange(socketServer uartAssis) {
        ua=uartAssis;
    }

    @Override
    public void componentResized(ComponentEvent e) {
        jf=(JFrame)e.getSource();
        r=jf.getBounds();
        int width = r.getSize().width;
        ua.mb1.setBounds(width-50,8,20,20);
        ua.mb2.setBounds(width-100,8,20,20);
        ua.mb3.setBounds(width-150,8,20,20);

    }


}
