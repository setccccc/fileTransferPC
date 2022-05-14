import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
实现模块化的接收器功能
 坑集合
 (1)重定向设置后到setVisible中间不能有任何的字体打印，否则错误，无法显示窗口，所以
 将initRedirect和setVisible放在一起
 */
public class socketServer {
    private JTextArea infoWin;//信息显示的窗口，收发情况，服务器情况都在这里
    private JFrame jf;
    private JButton openDir;//打开接收文件所在文件夹按钮
    private JButton sendFile;//发送文件按钮，点击后选择一个文件发送到手机
    private JLabel statusBar;
    private JButton sendMsg;//聊天信息发送按钮
    private JButton copyFile;//拷贝接收到的文件
    private JTextArea msgArea;//消息敲入区域
    private ArrayList<File> revFileList = new ArrayList<>();//接收到的文件列表
    private volatile Boolean msgRun=true;
    private String fileDir = "."+File.separator+"test"+File.separator;
	public socketServer(){
        File dir = new File(fileDir);
        if(!dir.exists()){
            dir.mkdir();
        }
		jf = new JFrame();
		jf.setLayout(null);
        jf.setSize(800,630);
		jf.setLocationRelativeTo(null);//居中，必须在设置size后调用
        jf.setUndecorated(true);
        initOther();
        initReDirect();//Visible之后才能够打印，否则有问题，要记住
        initBorder();
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.setVisible(true);


        try {
            p("程序启动完成,开启服务端socket，端口号8088\n"+getWlanIp());//得到ip地址这个只能window下
        } catch (Exception e) {
            e.printStackTrace();
        }
        startRevThread();

        reSizeEvent dg = new reSizeEvent(jf);
        /**添加两个监听器**/
        jf.addMouseListener(dg);
        jf.addMouseMotionListener(dg);
        jf.addComponentListener(new WinSizeChange(this));//监听窗口大小改变事件

    }

    mybutton1 mb1,mb2,mb3;
    private void initBorder() {
        mb1 = new mybutton1(mybutton1.CLOSE,jf);
        mb1.setBounds(jf.getWidth()-50,8,20,20);
        mb2 = new mybutton1(mybutton1.MAX,jf);
        mb2.setBounds(jf.getWidth()-100,8,20,20);
        mb3 = new mybutton1(mybutton1.MIN,jf);
        mb3.setBounds(jf.getWidth()-150,8,20,20);
        jf.add(mb1); jf.add(mb2); jf.add(mb3);
    }

    private void initOther() {
	    openDir = new JButton("directory");
	    openDir.setBounds(10,60,100,30);
	    openDir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Runtime.getRuntime().exec("explorer.exe "+ fileDir);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
	    jf.add(openDir);

	    copyFile = new JButton("copy");
	    copyFile.setBounds(230,60,100,30);
	    copyFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileTransferable ft = new FileTransferable(revFileList);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ft, new ClipboardOwner() {
                    @Override
                    public void lostOwnership(Clipboard clipboard, Transferable contents) {
                        System.out.println("Lost ownership");
                    }
                });
            }
        });
	    jf.add(copyFile);

	    sendFile = new JButton("send");
	    sendFile.setBounds(120,60,100,30);
	    sendFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        selectedFileList.clear();//要清空，防止重复添加
                        File s[]  = util.filechooseMulti();
                        if(s==null) return;
                        for(int i=0;i<s.length;i++){
                            selectedFileList.add(s[i]);
                        }
                        if(s.length==1){
                            startSingleFileSend();
                        }else if(s.length>1){
                            startMultiFileSend();
                        }
                    }
                }).start();
            }
        });
	    jf.add(sendFile);


	    msgArea = new JTextArea();
	    msgArea.setBounds(20,170,500,100);
        msgArea.setFont(new Font("宋体", Font.PLAIN,20));
	    jf.add(msgArea);

	    sendMsg = new JButton("发送信息");
	    sendMsg.setBounds(530,170,150,100);
	    sendMsg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                msgList.add(msgArea.getText());
            }
        });
	    jf.add(sendMsg);

        statusBar = new JLabel("status:Normal");
        statusBar.setBounds(0,600,800,30);
        statusBar.setFont(new Font("宋体", Font.PLAIN,20));
        jf.add(statusBar);
    }

    public ArrayList<File> selectedFileList = new ArrayList<>();
    public void startMultiFileSend(){
        tickState = TICK_BUSY;
        //统计文件总数量,2G够用啦，不可能用socket发送总文件超过2G的
        int flens=0;
        for(int i=0;i<selectedFileList.size();i++){
            File f = selectedFileList.get(i);
            flens += f.length();
        }
        try{
            dataOutputStream.writeInt(DATA_MULTI_FILE);//发送的是文件集合
            //发送文件数量
            dataOutputStream.writeInt(selectedFileList.size());
            //发送全部文件长度
            dataOutputStream.writeInt(flens);
            //分别发送每个文件名长度和文件名
            for(int i=0;i<selectedFileList.size();i++){
                File f = selectedFileList.get(i);
                String fileName = f.getName();
                byte fnLen[]=fileName.getBytes("utf-8");
                dataOutputStream.writeInt(fnLen.length);
                serverOutputStream.write(fnLen,0,fnLen.length);//发送文件名
                dataOutputStream.writeInt((int)f.length());//发送文件长度,小于2G
            }
            //发送全部文件
            byte[] b = new byte[8192];//8KB块传输
            int length;
            for(int i=0;i<selectedFileList.size();i++){
                File file = selectedFileList.get(i);
                FileInputStream f = new FileInputStream(file);
                int fileSize = f.available();
                speedCheck sp = new speedCheck();
                sp.checkStart();
                int cnt = 0,timecnt=0;
                long preT,curT;
                preT = System.currentTimeMillis();
                if(i>=1){
                    p(getCurrentTime()+" "+"文件"+file.getName()+"传输完毕\n");
                }
                p(getCurrentTime()+" "+"文件"+file.getName()+"开始传输\n");//之前不能调用p函数，否则延时导致无法及时通过getText获取
                statusBar.setText("发送中，百分比"+sp.百分比(cnt,fileSize) + ",速度" + sp.speed(timecnt)+"\n");
                while((length=f.read(b,0,b.length))!=-1){
                    serverOutputStream.write(b, 0, length);
                    //速度检测
                    cnt+=length;
                    timecnt+=length;
                    curT = System.currentTimeMillis();
                    if(curT-preT>speedCheck.CHECK_TIME){
                        statusBar.setText("发送中,百分比"+sp.百分比(cnt,fileSize)+",速度"+sp.speed(timecnt)+"\n");
                        preT = curT;
                        timecnt=0;
                    }
                }
                statusBar.setText("发送中,百分比100%\n");
                f.close();
                serverOutputStream.flush();
                if(i==selectedFileList.size()-1)
                    p(getCurrentTime()+" "+"文件"+file.getName()+"传输完毕\n");
            }
        }catch(Exception e){
            e.printStackTrace();
            p("发送多个文件异常");
        }
        tickState = TICK_IDLE;
    }
    public void startSingleFileSend(){
        try {
            tickState = TICK_BUSY;
            File file = selectedFileList.get(0);
            FileInputStream fis = new FileInputStream(file);
            //发送文件名
            String fileName=file.getName();
            byte fnLen[]=fileName.getBytes("utf-8");
            int bmFnLen = fnLen.length;//文件名的字节长度
            dataOutputStream.writeInt(DATA_SINGLE_FILE);
            dataOutputStream.writeInt(bmFnLen);//发送文件名长度
            serverOutputStream.write(fnLen,0,bmFnLen);//发送文件名
            dataOutputStream.writeInt(fis.available());//发送文件长度

            int fileSize = fis.available();
            byte[] b = new byte[8192];//8KB块传输
            int length; int timecnt = 0,cnt=0;
            speedCheck sp = new speedCheck();
            sp.checkStart();
            String oldtxt = infoWin.getText();
            //需要统一发送，否则信息漏失
            oldtxt+=getCurrentTime()+" "+"选择要发送的文件为"+file.getName()+"\n";
            long preT,curT;
            preT = System.currentTimeMillis();
            while((length=fis.read(b,0,b.length))!=-1)
            {
                serverOutputStream.write(b, 0, length);
                timecnt+=length;
                cnt+=length;
                curT = System.currentTimeMillis();
                if(curT-preT>speedCheck.CHECK_TIME) {
                    statusBar.setText("发送中，百分比"+sp.百分比(cnt,fileSize) + ",速度" + sp.speed(timecnt));
                    preT = curT;
                    timecnt=0;
                }
            }
            statusBar.setText("发送中，百分比"+sp.百分比(fileSize,fileSize) + ",速度" + sp.speed(timecnt));
            System.out.println("");
            fis.close();
            serverOutputStream.flush();
            p("文件发送完毕");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        tickState = TICK_IDLE;
    }




    private ServerSocket server;
    private Socket client;
    private InputStream inFromServer;
    private DataInputStream dis;
    private OutputStream serverOutputStream;
    private DataOutputStream dataOutputStream;
    private final static int DATA_MSG = 0;
    private final static int DATA_SINGLE_FILE = 1;
    private final static int DATA_TICK = 2;
    private final static int DATA_MULTI_FILE = 3;
    public void startRevThread(){
		new Thread(new Runnable(){
			@Override
			public void run(){
				int len;
				byte[] buf = new byte[8192];//接收缓冲区
				try{
					server=new ServerSocket(8088);//创建socket服务器
					p("等待客户端连接,请确保手机VPN设置为绕过局域网，手机和PC同一wifi");
					client=server.accept();//获得socket对象，会堵塞到有客户端连接
                    client.setSoTimeout(10000);//read/write超时时间，到时间就停止read/write堵塞，并且报错,大于心跳时间就行
					InetSocketAddress isa = (InetSocketAddress)client.getRemoteSocketAddress();
					p("有客户端连接,ip="+",port="+isa.getPort());
					inFromServer = client.getInputStream();//得到socket输入路
					String fileName="";//接收文件名
					dis=new DataInputStream(inFromServer);//对字节输入流进行包装
					//往客户端发送数据,确认传输开始
					serverOutputStream = client.getOutputStream();
					dataOutputStream = new DataOutputStream(serverOutputStream);
					dataOutputStream.writeInt(1234);//确认

                    //开启心跳线程
                    initTick();
                    initMsgThread();

					while(true){
                        int dataType = -1;
                        try{
                            dataType=dis.readInt();//先读取发过来的四个四节，堵塞读取，头四个字节是文件名的长
                        }catch(SocketTimeoutException e){
                            e.printStackTrace();
                            if(tickState==TICK_BUSY) continue;
                        }
                        if(dataType == DATA_SINGLE_FILE){//接收单个文件
                            tickState = TICK_BUSY;
                            revFileList.clear();//先清空
                            int filenamelen = dis.readInt();
                            p("读取到"+filenamelen+"字节文件名");
                            byte[] fileNameBuf=new byte[filenamelen];
                            inFromServer.read(fileNameBuf,0,filenamelen);//然后继续堵塞读取，接收的是文件名
                            fileName=new String(fileNameBuf,"utf-8");//将文件名译码为utf-8字符串
                            int fileSize = dis.readInt();//读取文件字节长度
                            File f=new File(fileDir +fileName);//将接收的文件放在程序所在文件下
                            revFileList.add(f);
                            if(f.exists()) f.delete();//如果f盘存在这个文件就实现删除，否则就创建一个新的文件
                            else{
                                f.createNewFile();
                            }
                            FileOutputStream fr = new FileOutputStream(f);
                            int cnt = 0;//计数接收到多少文件字节长度
                            speedCheck sp = new speedCheck();
                            sp.checkStart();
                            long preT,curT;//当前时间，上一个时间
                            int timecnt = 0;
                            preT = System.currentTimeMillis();
                            while((len=inFromServer.read(buf,0,8192))!=-1)
                            {
                                fr.write(buf,0,len);
                                cnt+=len;
                                timecnt+=len;
                                curT = System.currentTimeMillis();
                                if(curT-preT>speedCheck.CHECK_TIME) {
                                    statusBar.setText("接收中，百分比"+sp.百分比(cnt,fileSize) + ",速度" + sp.speed(timecnt)+"\n");
                                    preT = curT;
                                    timecnt = 0;
                                }
                                if(cnt == fileSize){
                                    fr.close();
                                    statusBar.setText("接收中，百分比"+sp.百分比(fileSize,fileSize) + ",速度" + sp.speed(timecnt)+"\n");
                                    p("文件接收完毕");
                                    break;
                                }
                            }
                            tickState = TICK_IDLE;
                        }else if(dataType == DATA_MULTI_FILE){//接收多个文件
                            tickState = TICK_BUSY;
                            ArrayList<File> fileList = new ArrayList<>();
                            revFileList.clear();
                            int fn = dis.readInt();//文件数量
                            int fileLenArr[] = new int[fn];//文件长度集合
                            int flens =  dis.readInt();//文件全部长度
                            for(int i=0;i<fn;i++){
                                int filenamelen = dis.readInt();
                                p("读取到"+filenamelen+"字节文件名");
                                byte[] fileNameBuf=new byte[filenamelen];
                                inFromServer.read(fileNameBuf,0,filenamelen);//然后继续堵塞读取，接收的是文件名
                                fileName=new String(fileNameBuf,"utf-8");//将文件名译码为utf-8字符串
                                int fileSize = dis.readInt();//读取文件字节长度
                                File f=new File(fileDir +fileName);//将接收的文件放在程序所在文件下
                                fileList.add(f);
                                revFileList.add(f);
                                fileLenArr[i]=fileSize;
                            }
                            //接着就是文件流了，采用for循环，里面while的方法，末尾字节采用单字节读取的方法。
                            for(int i=0;i<fn;i++){
                                File f = fileList.get(i);
                                if(f.exists()) f.delete();//如果f盘存在这个文件就实现删除，否则就创建一个新的文件
                                else f.createNewFile();

                                int cnt = 0;//计数接收到多少文件字节长度

                                p("读取到文件名:"+f.getName());
                                p("读取到文件长度:"+fileLenArr[i]);
                                p("开始接收文件"+f.getName());

                                FileOutputStream fr = new FileOutputStream(f);
                                speedCheck sp = new speedCheck();
                                sp.checkStart();
                                int timecnt = 0;    long preT,curT;//当前时间，上一个时间

                                int BLOCK = 8192;   //根据文件大小设置BLOCK值
                                if(fileLenArr[i]<8192){
                                    BLOCK = fileLenArr[i]-1;
                                }
                                preT = System.currentTimeMillis();
                                while(true){//直到读完一个文件才退出
                                    //读取前先判断是否达到单字节读取的条件
                                    if(cnt+BLOCK > fileLenArr[i]){//如果再读取8192字节超过文件大小，就单字节读取
                                        BLOCK = 1;
                                    }
                                    len = inFromServer.read(buf,0,BLOCK);
                                    if(len>0){
                                        cnt+=len;
                                        timecnt+=len;
                                        curT = System.currentTimeMillis();
                                        if(curT-preT>speedCheck.CHECK_TIME) {
                                            statusBar.setText("接收中，百分比"+sp.百分比(cnt,fileLenArr[i]) + ",速度" + sp.speed(timecnt)+"\n");  //更新进度
                                            preT = curT;
                                            timecnt = 0;
                                        }
                                        if(cnt==fileLenArr[i]){//如果读取到的恰好是文件大小，则直接退出，显然单字节读取只会是这种情况
                                            fr.write(buf,0,len);
                                            fr.close();
                                            p("文件接收完毕"+f.getName());
                                            statusBar.setText("status:Normal");
                                            break;
                                        }
                                        fr.write(buf,0,len);
                                    }
                                }
                            }


                            tickState = TICK_IDLE;
                        }else if(dataType == DATA_MSG){//接收是聊天消息
                            tickState = TICK_BUSY;
                            int msgSize = dis.readInt();
                            String msg = "";
                            int cnt = 0;
                            //p("堵在这里","为什么");
                            //发现原因是当快速发送消息时，一堆数据全部挤在这里，卧槽明白了
                            while((len = inFromServer.read(buf,0,8192))!=-1){
                                //p("当前接收到"+len+"字节数据");
                                cnt+=len;
                                if(cnt<msgSize){
                                    msg += new String(buf,0,len,"utf-8");
                                    continue;//继续读取
                                }
                                if(cnt == msgSize){
                                    //停止读取
                                    msg += new String(buf,0,len,"utf-8");
                                    p(msg);
                                    break;
                                }
                                //当有多个消息一并发过来时
                                //这里处理得不是很好，电脑端发送过快也有问题
                                if(cnt>msgSize){
                                    int remain = cnt-msgSize;//多余的数据需要继续处理
                                    int curRemain = len - remain;//当前文件剩余的数据
                                    msg += new String(buf,0,curRemain,"utf-8");

                                    int bufIndex = curRemain;
                                    p(msg);
                                    //p("剩下的remain="+remain);
                                    while(remain>0){//剩下的数据肯定是type,msgSize和数据
                                        msg="";
                                        int type = util.getIntFromBytes(buf[bufIndex],buf[bufIndex+1],buf[bufIndex+2],buf[bufIndex+3]);
                                        bufIndex+=4;
                                        int msgS= util.getIntFromBytes(buf[bufIndex],buf[bufIndex+1],buf[bufIndex+2],buf[bufIndex+3]);
                                        bufIndex+=4;
                                        //p("继续类型="+type+"大小为"+msgS);
                                        msg += new String(buf,bufIndex,msgS,"utf-8");
                                        p(msg);
                                        bufIndex += msgS;//目前不考虑大于8192
                                        remain-= (msgS+8);
                                        //这里会错误
                                        //p("处理后remain="+remain);
                                    }
                                    break;//这个要
                                }
                            }
                            tickState = TICK_IDLE;
                        }else if(dataType == DATA_TICK) {
                            //p("客户端alive");
                        }else{
                            p("错误的数据类型"+dataType);
                        }

					}
				}catch(Exception e){
					e.printStackTrace();
					//停止这个线程
                    try {
                        inFromServer.close();//关闭输入流
                        serverOutputStream.close();//关闭输出流
                        client.close();//关闭客户端socket
                        server.close();//关闭服务端socket
                        p("关闭服务端socket,停止接收线程");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    return;
				}
			}
		}).start();
	}

	public volatile ArrayList<String> msgList;
    private void initMsgThread() {
        msgList = new ArrayList<>();
        new Thread(() -> {
            while(msgRun){
                if(msgList.size()>0){
                    tickState = TICK_BUSY;
                    String msg = msgList.remove(0);
                    p("\n服务端发送消息:\n"+msg);
                    try {
                        byte buf[] = msg.getBytes("utf-8");
                        dataOutputStream.writeInt(DATA_MSG);//写入0表示发送的是聊天消息
                        dataOutputStream.writeInt(buf.length);//消息长度
                        p("消息长度为"+buf.length);
                        serverOutputStream.write(buf,0,buf.length);
                        serverOutputStream.flush();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    tickState = TICK_IDLE;
                }else{//线程休眠，防止占用CPU
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            p("退出消息发送线程");
            msgRun = true;
        }).start();
    }

    private final static int TICK_BUSY = 0;//正在收发文件/信息
    private final static int TICK_IDLE = 1;//可以发送心跳包
    private volatile int tickState = TICK_IDLE;
    private volatile boolean acTick = false;//心跳确认
    private void initTick() {
        p("开启心跳线程");
        new Thread(new Runnable() {
            int timeoutcnt = 0;
            @Override
            public void run() {
                while(true){
                        try {
                            Thread.sleep(3*1000);
                            if(tickState == TICK_IDLE){
                                dataOutputStream.writeInt(DATA_TICK);
                            }
                        } catch (Exception e1) {//如果writeInt失败就表示客户端断开
                            e1.printStackTrace();
                            p("客户端连接断开");//这个需要重新进入accept状态
                            //关闭tick和消息发送线程
                            msgRun = false;
                            while(!msgRun) try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //重启服务端socket
                            startRevThread();
                            break;//退出心跳线程
                        }
                }
                p("停止心跳线程");
            }
        }).start();
    }

    public <T> void p(T t){
        System.out.println(getCurrentTime()+" "+t);
    }
    public <T> void p2(T t){
        System.out.print(getCurrentTime()+" "+t);
    }
    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        return sdf.format(date);
    }

    //内部类，重定向System.out到TextArea上
    JScrollBar scrollBar;
    private void initReDirect() {
        infoWin = new JTextArea();
        infoWin.setFont(new Font("宋体", Font.PLAIN,20));
        infoWin.setEditable(false);
        infoWin.setBounds(0,300,800,300);
        infoWin.setBackground(Color.black);
        infoWin.setForeground(Color.WHITE);
        JPopupMenu jm = new JPopupMenu();
        JMenuItem t = new JMenuItem("清空");
        t.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                infoWin.setText("");
            }
        });
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        JMenuItem copy = new JMenuItem("拷贝");
        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable tText = new StringSelection( infoWin.getSelectedText());
                clip.setContents(tText, null);
            }
        });
        jm.add(t);
        jm.add(copy);
        infoWin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3){
                    jm.show(infoWin, e.getX(), e.getY());
                }
            }
        });
        JTextAreaOutputStream out = new JTextAreaOutputStream (infoWin);
        System.setOut (new PrintStream (out));//设置输出重定向
        //错误输出不要重定向
        //System.setErr(new PrintStream(out));//将错误输出也重定向,用于e.pritnStackTrace
        JScrollPane jsp=new JScrollPane(infoWin);//设置滚动条
        scrollBar = jsp.getVerticalScrollBar();
        jsp.setBounds(0,300,800,300);
        jf.add(jsp);
        installDragFiles(infoWin);
    }

    class JTextAreaOutputStream extends OutputStream
    {
        private final JTextArea destination;
        public JTextAreaOutputStream (JTextArea destination)
        {
            if (destination == null)
                throw new IllegalArgumentException ("Destination is null");
            this.destination = destination;
        }
        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException
        {
            final String text = new String (buffer, offset, length);
            SwingUtilities.invokeLater(new Runnable ()
            {
                @Override
                public void run()
                {
                    destination.append (text);
                }
            });
        }
        @Override
        public void write(int b) throws IOException
        {
            write (new byte [] {(byte)b}, 0, 1);
        }
    }


    public static class FileTransferable implements Transferable {

        private java.util.List listOfFiles;

        public FileTransferable(java.util.List listOfFiles) {
            this.listOfFiles = listOfFiles;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            return listOfFiles;
        }
    }

    //安装组件为可拖拽
    public void installDragFiles(Component c)//定义的拖拽方法
    {
        /**
         * 创建一个DropTarget对象，传入匿名方法。
         * 第一个参数为支持传入拖拽的文件的组件，第二个参数拖拽类型。
         * 第三个参数为匿名方法。
         */
        new DropTarget(c, DnDConstants.ACTION_COPY_OR_MOVE,new dropE());
    }
    private class dropE extends DropTargetAdapter {
        private void processFiles(List<File> list){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    selectedFileList.clear();//要清空，防止重复添加
                    for(File file:list){
                        selectedFileList.add(file);
                    }
                    if(list.size()==1){
                        startSingleFileSend();
                    }else if(list.size()>1){
                        startMultiFileSend();
                    }
                }
            }).start();
        }
        @Override
        public void drop(DropTargetDropEvent dtde) {
            try
            {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor))//如果拖入的文件格式受支持
                {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);//接收拖拽来的数据,支持多个文件拖拽
                    List<File> list =  (List<File>) (dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));
                    processFiles(list);
                    dtde.dropComplete(true);//指示拖拽操作已完成
                }
                else
                {
                    dtde.rejectDrop();//否则拒绝拖拽来的数据
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    public static String getWlanIp(){
        return util.getIPStr();
    }
}
