import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class mybutton1 extends JButton{
    private int type;
    public static final int CLOSE = 1;
    public static final int MAX = 2;
    public static final int MIN = 3;
    JFrame jf;
    public mybutton1(int type, JFrame jf){
        super();
        this.type = type;
        this.jf = jf;
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        addActionListener(new bt1Event());
    }
    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        if(getModel().isPressed()){
                g2d.setColor(Color.BLUE);
        }else  if(getModel().isRollover()){
            g2d.setColor(Color.BLACK);
        }else{
            g2d.setColor(Color.RED);
        }
        g2d.setStroke(new BasicStroke(3.0f));
        paintImg(g2d);
    }
    public void paintImg(Graphics2D g2d){
        switch(type){
            case CLOSE:
                g2d.drawLine(3,3,17,17);
                g2d.drawLine(3,17,17,3);
                break;
            case MAX:
                if(jf.getExtendedState()==JFrame.MAXIMIZED_BOTH){
                    g2d.drawRect(2,6,10,10);
                    g2d.drawLine(5,3,15,3);
                    g2d.drawLine(15,3,15,13);
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(0.7f));
                    g2d.drawRect(1,4,13,13);
                }else{
                    g2d.drawRect(3,3,14,14);
                }
                break;
            case MIN:
                g2d.drawLine(3,17,17,17);
                break;
            default:
                break;
        }
    }

    private class bt1Event implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            switch(type){
                case CLOSE:
                    System.exit(0);
                    break;
                case MAX:
                    if(jf.getExtendedState()==JFrame.MAXIMIZED_BOTH){
                        jf.setExtendedState(JFrame.NORMAL);
                    }else{
                        jf.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                    break;
                case MIN:
                    jf.setExtendedState(JFrame.ICONIFIED);
                    break;
                default:
                    break;
            }
        }
    }
}
