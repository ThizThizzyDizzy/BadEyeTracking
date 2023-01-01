import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.OSCPortOut;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
/**
 *
 * @author Thiz
 */
public class Main extends javax.swing.JFrame{
    private TextSpeech tts;
    private boolean leftLightActive;
    private boolean rightLightActive;
    private int maxFPS;
    private void updateOSC(){
//        debugText = r(lxOut)+" "+r(lyOut)+" | "+r(rxOut)+" "+r(ryOut)+" "+(System.nanoTime()-lastLeftEye)/1000000+" "+(System.nanoTime()-lastRightEye)/1000000;
        try{
//            port.send(new OSCBundle(Arrays.asList(
//        if(eyeIndex==0){
                osc.send(new OSCMessage("/avatar/parameters/"+boxLeftEyeX.getText(), Arrays.asList(boxCombineLook.isSelected()?xOut:lxOut)));
                osc.send(new OSCMessage("/avatar/parameters/"+boxLeftEyeY.getText(), Arrays.asList(boxCombineLook.isSelected()?yOut:lyOut)));
                osc.send(new OSCMessage("/avatar/parameters/"+boxLeftEyeBlink.getText(), Arrays.asList(boxInvertBlink.isSelected()?lOpen:!lOpen)));
//        }else{
                osc.send(new OSCMessage("/avatar/parameters/"+boxRightEyeX.getText(), Arrays.asList(boxCombineLook.isSelected()?xOut:rxOut)));
                osc.send(new OSCMessage("/avatar/parameters/"+boxRightEyeY.getText(), Arrays.asList(boxCombineLook.isSelected()?yOut:ryOut)));
                osc.send(new OSCMessage("/avatar/parameters/"+boxRightEyeBlink.getText(), Arrays.asList(boxInvertBlink.isSelected()?rOpen:!rOpen)));
//        }
//            )));
        }catch(IOException|OSCSerializeException ex){
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private OSCPortOut osc;
    /**
     * Creates new form MainGUI
     */
    public Main(){
        initComponents();
        calibrateButtons = new JButton[]{buttonCalibrateAll, buttonCalibrateLeftCenter, buttonCalibrateLeftDown, buttonCalibrateLeftEye, buttonCalibrateLeftLeft, buttonCalibrateLeftRight, buttonCalibrateLeftUp, buttonCalibrateRightCenter, buttonCalibrateRightDown, buttonCalibrateRightEye, buttonCalibrateRightLeft, buttonCalibrateRightRight, buttonCalibrateRightUp};
        load();
        panelInputsRawDisplay.add(new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                BufferedImage img = leftEyeRaw;
                if(img!=null){
                    g.drawImage(img, 0, 0, null);
                    setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
                }
            }
        });
        panelInputsRawDisplay.add(new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                BufferedImage img = rightEyeRaw;
                if(img!=null){
                    g.drawImage(img, 0, 0, null);
                    setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
                }
            }
        });
        panelInputsTrackedDisplay.add(new JPanel(){
            {
                SpecialMouseListener sml = new SpecialMouseListener(this, 0);
                addMouseListener(sml);
                addMouseMotionListener(sml);
            }
            @Override
            protected void paintComponent(Graphics g){
                BufferedImage imgRaw = leftEyeRaw;
                BufferedImage img = leftEye;
                if(img!=null){
                    int scale = 1;
                    try{
                        scale = Math.min(imgRaw.getWidth()/img.getWidth(),imgRaw.getWidth()/img.getWidth());
                    }catch(Throwable t){}
                    g.drawImage(img, 0, 0, img.getWidth()*scale, img.getHeight()*scale, null);
                    setPreferredSize(new Dimension(img.getWidth()*scale, img.getHeight()*scale));
                }
            }
        });
        panelInputsTrackedDisplay.add(new JPanel(){
            {
                SpecialMouseListener sml = new SpecialMouseListener(this, 1);
                addMouseListener(sml);
                addMouseMotionListener(sml);
            }
            @Override
            protected void paintComponent(Graphics g){
                BufferedImage imgRaw = rightEyeRaw;
                BufferedImage img = rightEye;
                if(img!=null){
                    int scale = 1;
                    try{
                        scale = Math.min(imgRaw.getWidth()/img.getWidth(),imgRaw.getWidth()/img.getWidth());
                    }catch(Throwable t){}
                    g.drawImage(img, 0, 0, img.getWidth()*scale, img.getHeight()*scale, null);
                    setPreferredSize(new Dimension(img.getWidth()*scale, img.getHeight()*scale));
                }
            }
        });
        try{
            osc = new OSCPortOut(InetAddress.getByName("127.0.0.1"), 9000);
            startLeftEyeThread();
            startRightEyeThread();
            Thread t = new Thread(() -> {
                while(true){
                    try{
                        synchronized(leftFPS){
                            for(Iterator<Long> it = leftFPS.iterator(); it.hasNext();){
                                Long l = it.next();
                                if(l<System.nanoTime()-1_000_000_000l)it.remove();
                            }
                        }
                        synchronized(rightFPS){
                            for(Iterator<Long> it = rightFPS.iterator(); it.hasNext();){
                                Long l = it.next();
                                if(l<System.nanoTime()-1_000_000_000l)it.remove();
                            }
                        }
                        synchronized(eyeOpen){
                            for(Iterator<Long> it = eyeOpen.iterator(); it.hasNext();){
                                Long l = it.next();
                                if(l<System.nanoTime()-1_000_000l*sliderEyeLockThreshold.getValue())it.remove();
                            }
                            for(Iterator<Long> it = eyeClosed.iterator(); it.hasNext();){
                                Long l = it.next();
                                if(l<System.nanoTime()-1_000_000l*sliderEyeLockThreshold.getValue())it.remove();
                            }
                        }
                        maxFPS = Math.max(maxFPS, Math.max(leftFPS.size(), rightFPS.size()));
                        int msLeft = (int)((System.nanoTime()-lastLeftEye)/1000000);
                        labelStatusLeft.setText(statusLeft+" "+leftFPS.size()+"fps"+(msLeft<60_000?" "+msLeft+"ms":""));
                        int msRight = (int)((System.nanoTime()-lastRightEye)/1000000);
                        labelStatusRight.setText(statusRight+" "+rightFPS.size()+"fps"+(msRight<60_000?" "+msRight+"ms":""));
                        if(leftLightActive)panelStatusLightLeft.setBackground(colorScale(leftFPS.size()*1f/maxFPS));
                        if(rightLightActive)panelStatusLightRight.setBackground(colorScale(rightFPS.size()*1f/maxFPS));
                        xOut = getWeighted(lxOut, rxOut);
                        yOut = getWeighted(lyOut, ryOut);
                        double lw = getWeight(0);
                        double rw = getWeight(1);
                        boolean lb = lw<1-sliderBlinkThreshold.getValue()/1000f;
                        boolean rb = rw<1-sliderBlinkThreshold.getValue()/1000f;
                        if(lb&&rb){
                            lOpen = rOpen = eyeClosed.size()<=eyeOpen.size();
                        }
                        if(lb&&!rb){
                            lOpen = rOpen;
                            lxOut = rxOut;
                            lyOut = ryOut;
                        }
                        if(rb&&!lb){
                            rOpen = lOpen;
                            rxOut = lxOut;
                            ryOut = lyOut;
                        }
                        updateOSC();
                        Thread.sleep(10);
                    }catch(Exception ex){
                        System.err.println("ERROR IN MAIN THREAD LOOP: "+ex.getClass().getName()+" "+ex.getMessage());
                    }
                }
            });
            t.setDaemon(true);
            t.start();
            Thread shutdownThread = new Thread(() -> {
                while(true){
                    try{
                        Thread.sleep(100);
                        long shutdownAt = startupTime+(int)spinnerAutoShutdown.getValue()*60_000_000_000l;
                        if(System.nanoTime()>shutdownAt){
                            System.exit(0);
                        }
                    }catch(Exception ex){
                        System.err.println("ERROR IN SHUTDOWN THREAD: "+ex.getClass().getName()+" "+ex.getMessage());
                    }
                }
            });
            shutdownThread.setDaemon(true);
            shutdownThread.start();
            tts = new TextSpeech();
        }catch(IOException ex){
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    ArrayList<Long> leftFPS = new ArrayList<>();
    ArrayList<Long> rightFPS = new ArrayList<>();
    ArrayList<Long> eyeOpen = new ArrayList<>();
    ArrayList<Long> eyeClosed = new ArrayList<>();
    private Thread leftEyeThread, rightEyeThread;
    private BufferedImage leftEyeRaw, rightEyeRaw, leftEye, rightEye;
    private static int minThreshold = 0;
    private static int maxThreshold = 300;
    private static int lx,ly,rx,ry;
    private static int llx,lux,lrx,ldx,lcx,lly,luy,lry,ldy,lcy;
    private static int rlx,rux,rrx,rdx,rcx,rly,ruy,rry,rdy,rcy;
    private static int lfp1x,lfp1y,lfp2x,lfp2y;
    private static int rfp1x,rfp1y,rfp2x,rfp2y;
    private static float lxOut,lyOut,rxOut,ryOut,xOut,yOut;
    private static boolean lOpen = true,rOpen = true;
    private static long lastRightEye, lastLeftEye;
    private static String statusLeft, statusRight;
    private void startLeftEyeThread(){
        panelStatusLightLeft.setBackground(Color.BLACK);
        statusLeft = "Starting Up";
        leftLightActive = false;
        Thread[] lef = new Thread[1];
        Thread left = new Thread(() -> {
            while(leftEyeThread==lef[0]){
                try{
                    panelStatusLightLeft.setBackground(Color.pink);
                    statusLeft = "Connecting";
                    leftLightActive = false;
                    startImageStream(textFieldAddressLeft.getText(), (im)->{
                        synchronized(leftFPS){
                            leftFPS.add(System.nanoTime());
                        }
                        leftLightActive = true;
                        statusLeft = "Active";
                        leftEyeRaw = im;
                        leftEye = process2(process(im, 0), 0);
                        lastLeftEye = System.nanoTime();
                        calc(0);
                        panelInputs.repaint();
                    });
                }catch(Exception ex){
                    panelStatusLightLeft.setBackground(Color.magenta);
                    statusLeft = "Connection Lost";
                    leftLightActive = false;
                    System.err.println("Left Stream Lost");
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    try{
                        Thread.sleep(100);
                    }catch(InterruptedException ex1){
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    leftEye = null;
                }
            }
        });
        lef[0] = left;
        leftEyeThread = left;
        left.setDaemon(true);
        left.start();
    }
    private void startRightEyeThread(){
        panelStatusLightRight.setBackground(Color.BLACK);
        statusRight = "Starting Up";
        rightLightActive = false;
        Thread[] rig = new Thread[1];
        Thread right = new Thread(() -> {
            while(rightEyeThread==rig[0]){
                try{
                    panelStatusLightRight.setBackground(Color.pink);
                    statusRight = "Connecting";
                    rightLightActive = false;
                    startImageStream(textFieldAddressRight.getText(), (im)->{
                        synchronized(rightFPS){
                            rightFPS.add(System.nanoTime());
                        }
                        rightLightActive = true;
                        statusRight = "Active";
                        rightEyeRaw = im;
                        rightEye = process2(process(im, 1), 1);
                        lastRightEye = System.nanoTime();
                        calc(1);
                        panelInputs.repaint();
                    });
                }catch(Exception ex){
                    panelStatusLightRight.setBackground(Color.magenta);
                    statusRight = "Connection Lost";
                    rightLightActive = false;
                    System.err.println("Right Stream Lost");
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException ex1){
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    rightEye = null;
                }
            }
        });
        rig[0] = right;
        rightEyeThread = right;
        right.setDaemon(true);
        right.start();
    }
    public static InputStream getRemoteInputStream(String currentFile, final URLConnection urlconnection) throws Exception {
        final InputStream[] is = new InputStream[1];
        for (int j = 0; (j < 3) && (is[0] == null); j++) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        is[0] = urlconnection.getInputStream();
                    }catch (IOException localIOException){
                        System.err.println(localIOException.getMessage());
                    }
                }
            };
            t.setName("FileDownloadStreamThread");
            t.start();
            int iterationCount = 0;
            while ((is[0] == null) && (iterationCount++ < 5)){
                try {
                    t.join(1000L);
                } catch (InterruptedException localInterruptedException) {
                }
            }
            if (is[0] != null){
                continue;
            }
            try {
                t.interrupt();
                t.join();
            } catch (InterruptedException localInterruptedException1) {
            }
        }
        if (is[0] == null) {
            throw new Exception("Unable to download "+currentFile);
        }
        return is[0];
    }
    public static void startImageStream(String link, Consumer<BufferedImage> stream) throws Exception{
        URL url = new URL(link);
        URLConnection connection = url.openConnection();
        connection.setDefaultUseCaches(false);
        if ((connection instanceof HttpURLConnection)) {
            ((HttpURLConnection)connection).setRequestMethod("HEAD");
            int code = ((HttpURLConnection)connection).getResponseCode();
            if (code / 100 == 3) {
                return;
            }
        }
        boolean downloadFile = true;
        while (downloadFile) {
            downloadFile = false;
            URLConnection urlconnection = url.openConnection();
            if ((urlconnection instanceof HttpURLConnection)) {
                urlconnection.setRequestProperty("Cache-Control", "no-cache");
                urlconnection.connect();
            }
            try (InputStream inputstream=getRemoteInputStream(null, urlconnection)) {
                InputStreamReader isr;
//                BufferedReader reader = new BufferedReader(new InputStreamReader(inputstream));
                while(true){
                    while(inputstream.read()!='\n');
                    while(inputstream.read()!=':');
                    int i;
                    String s = "";
                    while((i = inputstream.read())!='\n'){
                        s+=(char)i;
                    }
                    int len = Integer.parseInt(s.trim());
                    inputstream.read();
                    inputstream.read();
                    byte[] bytes = new byte[len];
                    for(int j = 0; j<len; j++){
                        bytes[j] = (byte)inputstream.read();
                    }
                    File f = new File("out.txt");
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(bytes);
                    fos.close();
                    stream.accept(ImageIO.read(f));
                    while(inputstream.read()!='\n');
                    while(inputstream.read()!='\n');
//                    return;
                }
            }
        }
    }

    private BufferedImage process2(BufferedImage im, int eyeIndex){
        if(im==null)return im;
        int X = 0, Y = 0, count = 0;
        int fp1x = eyeIndex==0?lfp1x:rfp1x;
        int fp1y = eyeIndex==0?lfp1y:rfp1y;
        int fp2x = eyeIndex==0?lfp2x:rfp2x;
        int fp2y = eyeIndex==0?lfp2y:rfp2y;
        Color bg = getBackground();
        int bgr = bg.getRed();
        int bgg = bg.getGreen();
        int bgb = bg.getBlue();
        for(int x = 0; x<im.getWidth(); x++){
            for(int y = 0; y<im.getHeight(); y++){
                Color c = new Color(im.getRGB(x, y));
                int dist = (int)(Math.sqrt(Math.pow(x-fp1x,2)+Math.pow(y-fp1y,2))+Math.sqrt(Math.pow(x-fp2x,2)+Math.pow(y-fp2y,2))-Math.sqrt(Math.pow(fp2x-fp1x,2)+Math.pow(fp2y-fp1y,2)));
                if(dist>getRadius()){
                    im.setRGB(x, y, new Color(c.getRed()/2+bgr/2,c.getGreen()/2+bgg/2,c.getBlue()/2+bgb/2).getRGB());
                    continue;
                }
                int brightness = c.getRed()+c.getGreen()+c.getBlue();
                if(brightness>minThreshold&&brightness<maxThreshold){
                    c = Color.red;
                    X+=x;
                    Y+=y;
                    count++;
                    im.setRGB(x, y, c.getRGB());
                }
            }
        }
        Graphics g = im.getGraphics();
        g.setColor(Color.green);
        if(eyeIndex==0){
            drawPlus(g, lfp1x, lfp1y, lfp1o?2:1);
            drawPlus(g, lfp2x, lfp2y, lfp2o?2:1);
        }else{
            drawPlus(g, rfp1x, rfp1y, rfp1o?2:1);
            drawPlus(g, rfp2x, rfp2y, rfp2o?2:1);
        }
        g.setColor(Color.yellow);
        if(eyeIndex==0){
            drawPlus(g, llx, lly, 1);
            drawPlus(g, lrx, lry, 1);
            drawPlus(g, lux, luy, 1);
            drawPlus(g, ldx, ldy, 1);
            drawPlus(g, lcx, lcy, 1);
        }else{
            drawPlus(g, rlx, rly, 1);
            drawPlus(g, rrx, rry, 1);
            drawPlus(g, rux, ruy, 1);
            drawPlus(g, rdx, rdy, 1);
            drawPlus(g, rcx, rcy, 1);
        }
        if(eyeIndex==0){
            lOpen = count>0;
        }else{
            rOpen = count>0;
        }
        synchronized(eyeOpen){
            if(count>0)eyeOpen.add(System.nanoTime());
            else eyeClosed.add(System.nanoTime());
        }
        if(count>0&&count<im.getWidth()*im.getHeight()/4){
            g.setColor(Color.blue);
            X/=count;
            Y/=count;
            drawPlus(g, X, Y, 3);
            if(eyeIndex==0){
                lx=X;
                ly=Y;
            }else{
                rx=X;
                ry=Y;
            }
        }
        return im;
    }
    private BufferedImage[] last = new BufferedImage[2];
    private BufferedImage process(BufferedImage im, int eyeIndex){
        if(im==null)return im;
        if(boxAntiFlicker.isSelected()){
            BufferedImage lst = last[eyeIndex];
            last[eyeIndex] = im;
            if(lst!=null){
                long diff = 0;
                long maxDiff = 0;
                for(int x = 0; x<im.getWidth(); x++){
                    for(int y = 0; y<im.getHeight(); y++){
                        Color c1 = new Color(im.getRGB(x, y));
                        int b1 = c1.getRed()+c1.getGreen()+c1.getBlue();
                        Color c2 = new Color(lst.getRGB(x, y));
                        int b2 = c2.getRed()+c2.getGreen()+c2.getBlue();
                        diff+=Math.abs(b1-b2);
                        maxDiff+=255*3;
                    }
                }
                double diffPercent = diff/(double)maxDiff;
                if(diffPercent>0.1){
                    return null;
                }
            }
        }
        BufferedImage newImage;
        int sx,sy;
        boolean flipX, flipY;
        if(eyeIndex==1){//right
            newImage = new BufferedImage(im.getWidth()-(int)spinnerCropRightLeft.getValue()-(int)spinnerCropRightRight.getValue(), im.getHeight()-(int)spinnerCropRightTop.getValue()-(int)spinnerCropRightBottom.getValue(), im.getType());
            sx = (int)spinnerCropRightLeft.getValue();
            sy = (int)spinnerCropRightTop.getValue();
            flipX = boxRightFlipX.isSelected();
            flipY= boxRightFlipY.isSelected();
        }else{//left
            newImage = new BufferedImage(im.getWidth()-(int)spinnerCropLeftLeft.getValue()-(int)spinnerCropLeftRight.getValue(), im.getHeight()-(int)spinnerCropLeftTop.getValue()-(int)spinnerCropLeftBottom.getValue(), im.getType());
            sx = (int)spinnerCropLeftLeft.getValue();
            sy = (int)spinnerCropLeftTop.getValue();
            flipX = boxLeftFlipX.isSelected();
            flipY = boxLeftFlipY.isSelected();
        }
        int sx2 = sx+newImage.getWidth();
        int sy2 = sy+newImage.getHeight();
        if(flipX){
            int sxx = sx;
            sx = sx2;
            sx2 = sxx;
        }
        if(flipY){
            int syy = sy;
            sy = sy2;
            sy2 = syy;
        }
        newImage.getGraphics().drawImage(im, 0, 0, newImage.getWidth(), newImage.getHeight(), sx, sy, sx2, sy2, null);
        return newImage;
    }
    private void calc(int eyeIndex){
        int lx = eyeIndex==0?llx:rlx;
        int ly = eyeIndex==0?lly:rly;
        int rx = eyeIndex==0?lrx:rrx;
        int ry = eyeIndex==0?lry:rry;
        int ux = eyeIndex==0?lux:rux;
        int uy = eyeIndex==0?luy:ruy;
        int dx = eyeIndex==0?ldx:rdx;
        int dy = eyeIndex==0?ldy:rdy;
        int cx = eyeIndex==0?lcx:rcx;
        int cy = eyeIndex==0?lcy:rcy;
        int x = eyeIndex==0?Main.lx:Main.rx;
        int y = eyeIndex==0?Main.ly:Main.ry;
        float lDist = dist(lx,ly,x,y);
        float cDist = dist(cx,cy,x,y);
        float rDist = dist(rx,ry,x,y);
        float uDist = dist(ux,uy,x,y);
        float dDist = dist(dx,dy,x,y);
        float lfDist = dist(lx,ly,cx,cy);
        float rgDist = dist(cx,cy,rx,ry);
        float upDist = dist(ux,uy,cx,cy);
        float dnDist = dist(dx,dy,cx,cy);
        float xo = calc(lDist,cDist,rDist,lfDist,rgDist);
        float yo = calc(dDist,cDist,uDist,dnDist,upDist);
        if(eyeIndex==0){
            lxOut = xo;
            lyOut = yo;
        }else{
            rxOut = xo;
            ryOut = yo;
        }
    }
    private static float r(float f){
        return Math.round(f*100)/100f;
    }
    private static float calc(float left, float center, float right, float leftSpace, float rightSpace){
        return((left-center)/leftSpace+(center-right)/rightSpace)/2;
    }
    private static float dist(int x1,int y1,int x2,int y2){
        return (float)Math.sqrt(Math.pow(x1-x2,2)+Math.pow(y1-y2,2));
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelInputs = new javax.swing.JPanel();
        labelInputs = new javax.swing.JLabel();
        panelInputRaw = new javax.swing.JPanel();
        labelInputRaw = new javax.swing.JLabel();
        panelInputsRawDisplay = new javax.swing.JPanel();
        panelInputTracked = new javax.swing.JPanel();
        labelInputTracked = new javax.swing.JLabel();
        panelInputsTrackedDisplay = new javax.swing.JPanel();
        panelInputStatus = new javax.swing.JPanel();
        labelInputStatus = new javax.swing.JLabel();
        panelInputStatusDual = new javax.swing.JPanel();
        panelInputStatusLeft = new javax.swing.JPanel();
        panelStatusLightLeft = panelStatusLightLeft = new JPanel(){
            @Override
            public void paintComponent(Graphics g){
                super.paintComponent(g);
                drawStatus(panelStatusLightLeft, g, 0);
            }
        };
        labelStatusLeft = new javax.swing.JLabel();
        panelInputStatusRight = new javax.swing.JPanel();
        panelStatusLightRight = panelStatusLightRight = new JPanel(){
            @Override
            public void paintComponent(Graphics g){
                super.paintComponent(g);
                drawStatus(panelStatusLightRight, g, 1);
            }
        };
        labelStatusRight = new javax.swing.JLabel();
        panelControls = new javax.swing.JPanel();
        panelControlsLeft = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        panelControlsInput = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        textFieldAddressLeft = new javax.swing.JTextField();
        buttonReconnectLeft = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        textFieldAddressRight = new javax.swing.JTextField();
        buttonReconnectRight = new javax.swing.JButton();
        panelControlsCropping = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        panelCroppingMain = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        spinnerCropLeftLeft = new javax.swing.JSpinner();
        spinnerCropLeftRight = new javax.swing.JSpinner();
        spinnerCropLeftTop = new javax.swing.JSpinner();
        spinnerCropLeftBottom = new javax.swing.JSpinner();
        jPanel8 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        spinnerCropRightLeft = new javax.swing.JSpinner();
        spinnerCropRightRight = new javax.swing.JSpinner();
        spinnerCropRightTop = new javax.swing.JSpinner();
        spinnerCropRightBottom = new javax.swing.JSpinner();
        jPanel9 = new javax.swing.JPanel();
        boxLeftFlipX = new javax.swing.JCheckBox();
        boxLeftFlipY = new javax.swing.JCheckBox();
        jPanel10 = new javax.swing.JPanel();
        boxRightFlipX = new javax.swing.JCheckBox();
        boxRightFlipY = new javax.swing.JCheckBox();
        panelControlsEyeCropping = new javax.swing.JPanel();
        jLabel26 = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        jLabel27 = new javax.swing.JLabel();
        spinnerRadius = new javax.swing.JSpinner();
        boxAntiFlicker = new javax.swing.JCheckBox();
        buttonResetFocalPoints = new javax.swing.JButton();
        panelInputScanner = new javax.swing.JPanel();
        jLabel25 = new javax.swing.JLabel();
        buttonScanInputs = new javax.swing.JButton();
        panelOutput = new javax.swing.JPanel();
        jLabel29 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        textAreaOutput = new javax.swing.JTextArea();
        panelControlsRight = new javax.swing.JPanel();
        panelControlsCalibration = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        sliderThresholdMin = new javax.swing.JSlider();
        sliderThresholdMax = new javax.swing.JSlider();
        jPanel2 = new javax.swing.JPanel();
        buttonCalibrateLeftUp = new javax.swing.JButton();
        buttonCalibrateLeftLeft = new javax.swing.JButton();
        buttonCalibrateLeftCenter = new javax.swing.JButton();
        buttonCalibrateLeftRight = new javax.swing.JButton();
        buttonCalibrateLeftDown = new javax.swing.JButton();
        buttonCalibrateLeftEye = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        buttonCalibrateRightUp = new javax.swing.JButton();
        buttonCalibrateRightLeft = new javax.swing.JButton();
        buttonCalibrateRightCenter = new javax.swing.JButton();
        buttonCalibrateRightRight = new javax.swing.JButton();
        buttonCalibrateRightDown = new javax.swing.JButton();
        buttonCalibrateRightEye = new javax.swing.JButton();
        buttonCalibrateAll = new javax.swing.JButton();
        boxInvertBlink = new javax.swing.JCheckBox();
        panelControlsStability = new javax.swing.JPanel();
        jLabel28 = new javax.swing.JLabel();
        boxCombineLook = new javax.swing.JCheckBox();
        jLabel17 = new javax.swing.JLabel();
        sliderBlinkThreshold = new javax.swing.JSlider();
        labelEyeLock = new javax.swing.JLabel();
        sliderEyeLockThreshold = new javax.swing.JSlider();
        boxAutoShutdown = new javax.swing.JCheckBox();
        spinnerAutoShutdown = new javax.swing.JSpinner();
        panelControlsOSC = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        boxLeftEyeX = new javax.swing.JTextField();
        boxRightEyeX = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        boxLeftEyeY = new javax.swing.JTextField();
        boxRightEyeY = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        boxLeftEyeBlink = new javax.swing.JTextField();
        boxRightEyeBlink = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        panelSaveLoad = new javax.swing.JPanel();
        buttonSave = new javax.swing.JButton();
        buttonLoad = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Bad Eye Tracking 1.0.0");

        panelInputs.setLayout(new javax.swing.BoxLayout(panelInputs, javax.swing.BoxLayout.PAGE_AXIS));

        labelInputs.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelInputs.setText("INPUTS");
        panelInputs.add(labelInputs);

        panelInputRaw.setLayout(new java.awt.BorderLayout());

        labelInputRaw.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelInputRaw.setText("RAW");
        panelInputRaw.add(labelInputRaw, java.awt.BorderLayout.NORTH);

        panelInputsRawDisplay.setLayout(new javax.swing.BoxLayout(panelInputsRawDisplay, javax.swing.BoxLayout.LINE_AXIS));
        panelInputRaw.add(panelInputsRawDisplay, java.awt.BorderLayout.CENTER);

        panelInputs.add(panelInputRaw);

        panelInputTracked.setLayout(new java.awt.BorderLayout());

        labelInputTracked.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelInputTracked.setText("TRACKED");
        panelInputTracked.add(labelInputTracked, java.awt.BorderLayout.NORTH);

        panelInputsTrackedDisplay.setLayout(new javax.swing.BoxLayout(panelInputsTrackedDisplay, javax.swing.BoxLayout.LINE_AXIS));
        panelInputTracked.add(panelInputsTrackedDisplay, java.awt.BorderLayout.CENTER);

        panelInputs.add(panelInputTracked);

        panelInputStatus.setLayout(new java.awt.BorderLayout());

        labelInputStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelInputStatus.setText("STATUS");
        panelInputStatus.add(labelInputStatus, java.awt.BorderLayout.NORTH);

        panelInputStatusDual.setLayout(new java.awt.GridLayout(1, 0));

        panelInputStatusLeft.setLayout(new java.awt.BorderLayout());

        panelStatusLightLeft.setPreferredSize(new java.awt.Dimension(0, 20));

        javax.swing.GroupLayout panelStatusLightLeftLayout = new javax.swing.GroupLayout(panelStatusLightLeft);
        panelStatusLightLeft.setLayout(panelStatusLightLeftLayout);
        panelStatusLightLeftLayout.setHorizontalGroup(
            panelStatusLightLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        panelStatusLightLeftLayout.setVerticalGroup(
            panelStatusLightLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 190, Short.MAX_VALUE)
        );

        panelInputStatusLeft.add(panelStatusLightLeft, java.awt.BorderLayout.CENTER);

        labelStatusLeft.setText(" ");
        panelInputStatusLeft.add(labelStatusLeft, java.awt.BorderLayout.PAGE_START);

        panelInputStatusDual.add(panelInputStatusLeft);

        panelInputStatusRight.setLayout(new java.awt.BorderLayout());

        panelStatusLightRight.setPreferredSize(new java.awt.Dimension(0, 20));

        javax.swing.GroupLayout panelStatusLightRightLayout = new javax.swing.GroupLayout(panelStatusLightRight);
        panelStatusLightRight.setLayout(panelStatusLightRightLayout);
        panelStatusLightRightLayout.setHorizontalGroup(
            panelStatusLightRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        panelStatusLightRightLayout.setVerticalGroup(
            panelStatusLightRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 190, Short.MAX_VALUE)
        );

        panelInputStatusRight.add(panelStatusLightRight, java.awt.BorderLayout.CENTER);

        labelStatusRight.setText(" ");
        panelInputStatusRight.add(labelStatusRight, java.awt.BorderLayout.PAGE_START);

        panelInputStatusDual.add(panelInputStatusRight);

        panelInputStatus.add(panelInputStatusDual, java.awt.BorderLayout.CENTER);

        panelInputs.add(panelInputStatus);

        panelControls.setLayout(new java.awt.GridLayout());

        panelControlsLeft.setLayout(new javax.swing.BoxLayout(panelControlsLeft, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("CONTROLS");
        panelControlsLeft.add(jLabel1);

        panelControlsInput.setLayout(new javax.swing.BoxLayout(panelControlsInput, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("INPUT");
        panelControlsInput.add(jLabel2);

        jPanel3.setLayout(new java.awt.GridLayout(1, 0));

        jPanel5.setLayout(new java.awt.GridLayout(0, 1));

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Left Eye");
        jPanel5.add(jLabel3);

        textFieldAddressLeft.setText("http://192.168.0.0:80");
        jPanel5.add(textFieldAddressLeft);

        buttonReconnectLeft.setText("RECONNECT");
        buttonReconnectLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonReconnectLeftActionPerformed(evt);
            }
        });
        jPanel5.add(buttonReconnectLeft);

        jPanel3.add(jPanel5);

        jPanel6.setLayout(new java.awt.GridLayout(0, 1));

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Right Eye");
        jPanel6.add(jLabel4);

        textFieldAddressRight.setText("http://192.168.0.0:80");
        jPanel6.add(textFieldAddressRight);

        buttonReconnectRight.setText("RECONNECT");
        buttonReconnectRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonReconnectRightActionPerformed(evt);
            }
        });
        jPanel6.add(buttonReconnectRight);

        jPanel3.add(jPanel6);

        panelControlsInput.add(jPanel3);

        panelControlsLeft.add(panelControlsInput);

        panelControlsCropping.setLayout(new javax.swing.BoxLayout(panelControlsCropping, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("Image Cropping");
        panelControlsCropping.add(jLabel8);

        panelCroppingMain.setLayout(new java.awt.GridLayout(2, 2));

        jPanel7.setLayout(new java.awt.GridLayout(2, 4));

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("Left");
        jPanel7.add(jLabel9);

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("Right");
        jPanel7.add(jLabel10);

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel13.setText("Top");
        jPanel7.add(jLabel13);

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel14.setText("Bottom");
        jPanel7.add(jLabel14);

        spinnerCropLeftLeft.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jPanel7.add(spinnerCropLeftLeft);

        spinnerCropLeftRight.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jPanel7.add(spinnerCropLeftRight);

        spinnerCropLeftTop.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jPanel7.add(spinnerCropLeftTop);

        spinnerCropLeftBottom.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jPanel7.add(spinnerCropLeftBottom);

        panelCroppingMain.add(jPanel7);

        jPanel8.setLayout(new java.awt.GridLayout(2, 4));

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("Left");
        jPanel8.add(jLabel11);

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("Right");
        jPanel8.add(jLabel12);

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setText("Top");
        jPanel8.add(jLabel15);

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel16.setText("Bottom");
        jPanel8.add(jLabel16);

        spinnerCropRightLeft.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jPanel8.add(spinnerCropRightLeft);

        spinnerCropRightRight.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jPanel8.add(spinnerCropRightRight);

        spinnerCropRightTop.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jPanel8.add(spinnerCropRightTop);

        spinnerCropRightBottom.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jPanel8.add(spinnerCropRightBottom);

        panelCroppingMain.add(jPanel8);

        jPanel9.setLayout(new java.awt.GridLayout(1, 0));

        boxLeftFlipX.setText("Flip X");
        jPanel9.add(boxLeftFlipX);

        boxLeftFlipY.setText("Flip Y");
        jPanel9.add(boxLeftFlipY);

        panelCroppingMain.add(jPanel9);

        jPanel10.setLayout(new java.awt.GridLayout(1, 0));

        boxRightFlipX.setText("Flip X");
        jPanel10.add(boxRightFlipX);

        boxRightFlipY.setText("Flip Y");
        jPanel10.add(boxRightFlipY);

        panelCroppingMain.add(jPanel10);

        panelControlsCropping.add(panelCroppingMain);

        panelControlsLeft.add(panelControlsCropping);

        panelControlsEyeCropping.setLayout(new java.awt.GridLayout(0, 1));

        jLabel26.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel26.setText("TRACKING SETUP");
        panelControlsEyeCropping.add(jLabel26);

        jPanel12.setLayout(new javax.swing.BoxLayout(jPanel12, javax.swing.BoxLayout.LINE_AXIS));

        jLabel27.setText("Radius");
        jPanel12.add(jLabel27);

        spinnerRadius.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));
        spinnerRadius.setValue(10);
        jPanel12.add(spinnerRadius);

        boxAntiFlicker.setText("Anti-Flicker");
        boxAntiFlicker.setToolTipText("Ignores new frames that that differ greatly from the previous frame");
        jPanel12.add(boxAntiFlicker);

        panelControlsEyeCropping.add(jPanel12);

        buttonResetFocalPoints.setText("Reset Focal Points");
        buttonResetFocalPoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetFocalPointsActionPerformed(evt);
            }
        });
        panelControlsEyeCropping.add(buttonResetFocalPoints);

        panelControlsLeft.add(panelControlsEyeCropping);

        panelInputScanner.setLayout(new java.awt.GridLayout(0, 1));

        jLabel25.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel25.setText("INPUT SCANNER");
        panelInputScanner.add(jLabel25);

        buttonScanInputs.setText("Scan for Inputs");
        buttonScanInputs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScanInputsActionPerformed(evt);
            }
        });
        panelInputScanner.add(buttonScanInputs);

        panelControlsLeft.add(panelInputScanner);

        panelOutput.setLayout(new java.awt.BorderLayout());

        jLabel29.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel29.setText("STATUS/DEBUG");
        panelOutput.add(jLabel29, java.awt.BorderLayout.NORTH);

        textAreaOutput.setColumns(20);
        textAreaOutput.setRows(5);
        jScrollPane2.setViewportView(textAreaOutput);

        panelOutput.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        panelControlsLeft.add(panelOutput);

        panelControls.add(panelControlsLeft);

        panelControlsRight.setLayout(new javax.swing.BoxLayout(panelControlsRight, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("CALIBRATION");

        jPanel1.setLayout(new java.awt.GridLayout(0, 2));

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Lower threshold");
        jPanel1.add(jLabel6);

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Upper threshold");
        jPanel1.add(jLabel7);

        sliderThresholdMin.setMaximum(765);
        sliderThresholdMin.setValue(minThreshold);
        sliderThresholdMin.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderThresholdMinStateChanged(evt);
            }
        });
        jPanel1.add(sliderThresholdMin);

        sliderThresholdMax.setMaximum(765);
        sliderThresholdMax.setValue(maxThreshold);
        sliderThresholdMax.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderThresholdMaxStateChanged(evt);
            }
        });
        jPanel1.add(sliderThresholdMax);

        buttonCalibrateLeftUp.setText("U");
        buttonCalibrateLeftUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateLeftUpActionPerformed(evt);
            }
        });

        buttonCalibrateLeftLeft.setText("L");
        buttonCalibrateLeftLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateLeftLeftActionPerformed(evt);
            }
        });

        buttonCalibrateLeftCenter.setText("C");
        buttonCalibrateLeftCenter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateLeftCenterActionPerformed(evt);
            }
        });

        buttonCalibrateLeftRight.setText("R");
        buttonCalibrateLeftRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateLeftRightActionPerformed(evt);
            }
        });

        buttonCalibrateLeftDown.setText("D");
        buttonCalibrateLeftDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateLeftDownActionPerformed(evt);
            }
        });

        buttonCalibrateLeftEye.setText("Calibrate Left Eye");
        buttonCalibrateLeftEye.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateLeftEyeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(buttonCalibrateLeftEye, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(buttonCalibrateLeftLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(buttonCalibrateLeftDown)
                            .addComponent(buttonCalibrateLeftUp)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(buttonCalibrateLeftCenter)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonCalibrateLeftRight)))))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonCalibrateLeftUp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCalibrateLeftLeft)
                    .addComponent(buttonCalibrateLeftCenter)
                    .addComponent(buttonCalibrateLeftRight))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonCalibrateLeftDown)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonCalibrateLeftEye))
        );

        buttonCalibrateRightUp.setText("U");
        buttonCalibrateRightUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateRightUpActionPerformed(evt);
            }
        });

        buttonCalibrateRightLeft.setText("L");
        buttonCalibrateRightLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateRightLeftActionPerformed(evt);
            }
        });

        buttonCalibrateRightCenter.setText("C");
        buttonCalibrateRightCenter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateRightCenterActionPerformed(evt);
            }
        });

        buttonCalibrateRightRight.setText("R");
        buttonCalibrateRightRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateRightRightActionPerformed(evt);
            }
        });

        buttonCalibrateRightDown.setText("D");
        buttonCalibrateRightDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateRightDownActionPerformed(evt);
            }
        });

        buttonCalibrateRightEye.setText("Calibrate Right Eye");
        buttonCalibrateRightEye.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateRightEyeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(buttonCalibrateRightLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonCalibrateRightDown)
                    .addComponent(buttonCalibrateRightUp)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(buttonCalibrateRightCenter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCalibrateRightRight)))
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(buttonCalibrateRightEye, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonCalibrateRightUp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCalibrateRightLeft)
                    .addComponent(buttonCalibrateRightCenter)
                    .addComponent(buttonCalibrateRightRight))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonCalibrateRightDown)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonCalibrateRightEye))
        );

        buttonCalibrateAll.setText("CALIBRATE ALL");
        buttonCalibrateAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCalibrateAllActionPerformed(evt);
            }
        });

        boxInvertBlink.setText("Invert Blink");

        javax.swing.GroupLayout panelControlsCalibrationLayout = new javax.swing.GroupLayout(panelControlsCalibration);
        panelControlsCalibration.setLayout(panelControlsCalibrationLayout);
        panelControlsCalibrationLayout.setHorizontalGroup(
            panelControlsCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelControlsCalibrationLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelControlsCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonCalibrateAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelControlsCalibrationLayout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(boxInvertBlink, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelControlsCalibrationLayout.setVerticalGroup(
            panelControlsCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelControlsCalibrationLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelControlsCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonCalibrateAll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(boxInvertBlink)
                .addContainerGap())
        );

        panelControlsRight.add(panelControlsCalibration);

        jLabel28.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel28.setText("STABILITY");

        boxCombineLook.setText("Combined eye look");
        boxCombineLook.setToolTipText("<html>Combine the look direction of both eyes<br>\nThe combined direction is the average of each, weighted based on how recently the last frame was recieved");

        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel17.setText("Combined eye threshold");

        sliderBlinkThreshold.setMaximum(1000);
        sliderBlinkThreshold.setToolTipText("<html>Threshold at which both eyes will respond to one eye.<br>\n(Same weight as is used for combined eye look)<br>");
        sliderBlinkThreshold.setValue(500);

        labelEyeLock.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelEyeLock.setText("Eye Lock Time");

        sliderEyeLockThreshold.setMaximum(20000);
        sliderEyeLockThreshold.setToolTipText("<html>How long it takes for the fallback eye open state to settle.<br>\n(Adjust this if your eyes are stuck open or closed when the feed cuts out)");
        sliderEyeLockThreshold.setValue(5000);
        sliderEyeLockThreshold.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderEyeLockThresholdStateChanged(evt);
            }
        });

        boxAutoShutdown.setText("Auto shutdown (min)");
        boxAutoShutdown.setToolTipText("<html>Automatically closes BadEyeTracking after the specified time, in minutes.<br>\nSometimes the program frezzes up, I'm not sure why.<br>\nThis is designed to be used with an auto restart script to keep it running.");
        boxAutoShutdown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boxAutoShutdownActionPerformed(evt);
            }
        });

        spinnerAutoShutdown.setModel(new javax.swing.SpinnerNumberModel(10, 1, null, 1));
        spinnerAutoShutdown.setToolTipText("<html>Automatically closes BadEyeTracking after the specified time, in minutes.<br> Sometimes the program frezzes up, I'm not sure why.<br> This is designed to be used with an auto restart script to keep it running.");

        javax.swing.GroupLayout panelControlsStabilityLayout = new javax.swing.GroupLayout(panelControlsStability);
        panelControlsStability.setLayout(panelControlsStabilityLayout);
        panelControlsStabilityLayout.setHorizontalGroup(
            panelControlsStabilityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelControlsStabilityLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelControlsStabilityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel28, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(boxCombineLook, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelControlsStabilityLayout.createSequentialGroup()
                        .addGroup(panelControlsStabilityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(boxAutoShutdown, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(sliderBlinkThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(panelControlsStabilityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spinnerAutoShutdown)
                            .addComponent(sliderEyeLockThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(labelEyeLock, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        panelControlsStabilityLayout.setVerticalGroup(
            panelControlsStabilityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelControlsStabilityLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel28)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(boxCombineLook)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelControlsStabilityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(labelEyeLock))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelControlsStabilityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sliderBlinkThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sliderEyeLockThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelControlsStabilityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(boxAutoShutdown)
                    .addComponent(spinnerAutoShutdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelControlsRight.add(panelControlsStability);

        jPanel11.setLayout(new java.awt.GridLayout(6, 2));

        jLabel18.setText("Left Eye X");
        jPanel11.add(jLabel18);

        jLabel19.setText("Right Eye X");
        jPanel11.add(jLabel19);

        boxLeftEyeX.setText("LeftEyeX");
        jPanel11.add(boxLeftEyeX);

        boxRightEyeX.setText("RightEyeX");
        jPanel11.add(boxRightEyeX);

        jLabel20.setText("Left Eye Y");
        jPanel11.add(jLabel20);

        jLabel21.setText("Right Eye Y");
        jPanel11.add(jLabel21);

        boxLeftEyeY.setText("LeftEyeY");
        jPanel11.add(boxLeftEyeY);

        boxRightEyeY.setText("RightEyeY");
        jPanel11.add(boxRightEyeY);

        jLabel22.setText("Left Eye Blink");
        jPanel11.add(jLabel22);

        jLabel23.setText("Right Eye Blink");
        jPanel11.add(jLabel23);

        boxLeftEyeBlink.setText("LeftEyeBlink");
        jPanel11.add(boxLeftEyeBlink);

        boxRightEyeBlink.setText("RightEyeBlink");
        jPanel11.add(boxRightEyeBlink);

        jLabel24.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel24.setText("OSC SETTINGS");

        javax.swing.GroupLayout panelControlsOSCLayout = new javax.swing.GroupLayout(panelControlsOSC);
        panelControlsOSC.setLayout(panelControlsOSCLayout);
        panelControlsOSCLayout.setHorizontalGroup(
            panelControlsOSCLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelControlsOSCLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelControlsOSCLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel11, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                    .addComponent(jLabel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelControlsOSCLayout.setVerticalGroup(
            panelControlsOSCLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelControlsOSCLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel24)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        panelControlsRight.add(panelControlsOSC);

        buttonSave.setText("Save Configuration");
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });

        buttonLoad.setText("Load Configuration");
        buttonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLoadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelSaveLoadLayout = new javax.swing.GroupLayout(panelSaveLoad);
        panelSaveLoad.setLayout(panelSaveLoadLayout);
        panelSaveLoadLayout.setHorizontalGroup(
            panelSaveLoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSaveLoadLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSaveLoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonSave, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonLoad, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelSaveLoadLayout.setVerticalGroup(
            panelSaveLoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSaveLoadLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonLoad)
                .addContainerGap())
        );

        panelControlsRight.add(panelSaveLoad);

        panelControls.add(panelControlsRight);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelInputs, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelControls, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelInputs, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelControls, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void buttonReconnectLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonReconnectLeftActionPerformed
        startLeftEyeThread();
    }//GEN-LAST:event_buttonReconnectLeftActionPerformed
    private void buttonReconnectRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonReconnectRightActionPerformed
        startRightEyeThread();
    }//GEN-LAST:event_buttonReconnectRightActionPerformed
    private void buttonCalibrateAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateAllActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateBoth("up", () -> {
                lux = lx;
                luy = ly;
                rux = rx;
                ruy = ry;
            });
            calibrateBoth("down", () -> {
                ldx = lx;
                ldy = ly;
                rdx = rx;
                rdy = ry;
            });
            calibrateBoth("left", () -> {
                llx = lx;
                lly = ly;
                rlx = rx;
                rly = ry;
            });
            calibrateBoth("right", () -> {
                lrx = lx;
                lry = ly;
                rrx = rx;
                rry = ry;
            });
            calibrateBoth("center", () -> {
                lcx = lx;
                lcy = ly;
                rcx = rx;
                rcy = ry;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateAllActionPerformed
    private void buttonCalibrateLeftUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateLeftUpActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateLeft("up", () -> {
                lux = lx;
                luy = ly;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateLeftUpActionPerformed
    private void buttonCalibrateLeftLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateLeftLeftActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateLeft("left", () -> {
                llx = lx;
                lly = ly;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateLeftLeftActionPerformed
    private void buttonCalibrateLeftCenterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateLeftCenterActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateLeft("center", () -> {
                lcx = lx;
                lcy = ly;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateLeftCenterActionPerformed
    private void buttonCalibrateLeftRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateLeftRightActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateLeft("right", () -> {
                lrx = lx;
                lry = ly;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateLeftRightActionPerformed
    private void buttonCalibrateLeftDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateLeftDownActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateLeft("down", () -> {
                ldx = lx;
                ldy = ly;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateLeftDownActionPerformed
    private void buttonCalibrateRightUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateRightUpActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateRight("up", () -> {
                rux = rx;
                ruy = ry;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateRightUpActionPerformed
    private void buttonCalibrateRightLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateRightLeftActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateRight("left", () -> {
                rlx = rx;
                rly = ry;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateRightLeftActionPerformed
    private void buttonCalibrateRightCenterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateRightCenterActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateRight("center", () -> {
                rcx = rx;
                rcy = ry;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateRightCenterActionPerformed
    private void buttonCalibrateRightRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateRightRightActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateRight("right", () -> {
                rrx = rx;
                rry = ry;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateRightRightActionPerformed
    private void buttonCalibrateRightDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateRightDownActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateRight("down", () -> {
                rdx = rx;
                rdy = ry;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateRightDownActionPerformed
    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        save();
    }//GEN-LAST:event_buttonSaveActionPerformed
    private void buttonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLoadActionPerformed
        load();
    }//GEN-LAST:event_buttonLoadActionPerformed
    private void sliderThresholdMinStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderThresholdMinStateChanged
        minThreshold = sliderThresholdMin.getValue();
    }//GEN-LAST:event_sliderThresholdMinStateChanged

    private void sliderThresholdMaxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderThresholdMaxStateChanged
        maxThreshold = sliderThresholdMax.getValue();
    }//GEN-LAST:event_sliderThresholdMaxStateChanged

    private void buttonCalibrateLeftEyeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateLeftEyeActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateLeft("up", () -> {
                lux = lx;
                luy = ly;
            });
            calibrateLeft("down", () -> {
                ldx = lx;
                ldy = ly;
            });
            calibrateLeft("left", () -> {
                llx = lx;
                lly = ly;
            });
            calibrateLeft("right", () -> {
                lrx = lx;
                lry = ly;
            });
            calibrateLeft("center", () -> {
                lcx = lx;
                lcy = ly;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateLeftEyeActionPerformed
    private void buttonCalibrateRightEyeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCalibrateRightEyeActionPerformed
        new Thread(() -> {
            for(JButton b : calibrateButtons)b.setEnabled(false);
            calibrateRight("up", () -> {
                rux = rx;
                ruy = ry;
            });
            calibrateRight("down", () -> {
                rdx = rx;
                rdy = ry;
            });
            calibrateRight("left", () -> {
                rlx = rx;
                rly = ry;
            });
            calibrateRight("right", () -> {
                rrx = rx;
                rry = ry;
            });
            calibrateRight("center", () -> {
                rcx = rx;
                rcy = ry;
            });
            for(JButton b : calibrateButtons)b.setEnabled(true);
        }).start();
    }//GEN-LAST:event_buttonCalibrateRightEyeActionPerformed
    private void buttonResetFocalPointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetFocalPointsActionPerformed
        BufferedImage left = leftEye;
        if(left!=null){
            lfp1x = left.getWidth()/4;
            lfp1y = lfp2y = left.getHeight()/2;
            lfp2x = left.getWidth()*3/4;
        }
        BufferedImage right = rightEye;
        if(right!=null){
            rfp1x = right.getWidth()/4;
            rfp1y = rfp2y = right.getHeight()/2;
            rfp2x = right.getWidth()*3/4;
        }
    }//GEN-LAST:event_buttonResetFocalPointsActionPerformed

    private void sliderEyeLockThresholdStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderEyeLockThresholdStateChanged
        labelEyeLock.setText("Eye Lock Time: "+sliderEyeLockThreshold.getValue()/10/100d+"s");
    }//GEN-LAST:event_sliderEyeLockThresholdStateChanged

    private void boxAutoShutdownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxAutoShutdownActionPerformed
        resetAutoShutdown();
    }//GEN-LAST:event_boxAutoShutdownActionPerformed
    private void buttonScanInputsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScanInputsActionPerformed
        HashSet<String> addresses = new HashSet<>();
        String lef = textFieldAddressLeft.getText();
        if(lef.matches("(https?://)?\\d+\\.\\d+\\.\\d+\\.\\d+(:\\d+)?")){
            String[] splt = lef.split("\\.");
            for(int i = 0; i<=255; i++){
                addresses.add(splt[0]+"."+splt[1]+"."+splt[2]+"."+i+(splt[splt.length-1].contains(":")?":"+splt[splt.length-1].split(":")[1]:""));
            }
        }
        String rig = textFieldAddressLeft.getText();
        if(rig.matches("(https?://)?\\d+\\.\\d+\\.\\d+\\.\\d+(:\\d+)?")){
            String[] splt = rig.split("\\.");
            for(int i = 0; i<=255; i++){
                addresses.add(splt[0]+"."+splt[1]+"."+splt[2]+"."+i+(splt[splt.length-1].contains(":")?":"+splt[splt.length-1].split(":")[1]:""));
            }
        }
        scanForImageStreams(addresses.toArray(new String[addresses.size()]));
    }//GEN-LAST:event_buttonScanInputsActionPerformed
    private void scanForImageStreams(String[] addresses){
        Thread scan = new Thread(()->{
            println("Scanning "+addresses.length+" ip addresses...");
            ArrayList<String> good = new ArrayList<>();
            boolean[] done = new boolean[addresses.length];
            for(int i = 0; i<addresses.length; i++){
                int idx = i;
                String ip = addresses[idx];
                Thread t = new Thread(() -> {
                    boolean[] isGood = {false};
                    int[] tries = new int[1];
                    try{
                        startImageStream(ip, (im)->{
                            isGood[0] = true;
                            throw new RuntimeException();//stop the stream
                        });
                    }catch(Exception ex){
                        if(ex.getMessage().equals("Unable to download null"))isGood[0] = true;
                        if(isGood[0])println("Found "+ip+(ex.getMessage().equals("Unable to download null")?"?":"!"));
                        done[idx] = true;
                    }
                    if(isGood[0])good.add(ip);
                });
                t.setDaemon(true);
                t.start();
            }
            while(true){
                boolean allDone = true;
                for(boolean b : done){
                    allDone &= b;
                }
                if(allDone)break;
            }
            println("Done! found "+good.size()+" things"+(good.isEmpty()?"":":"));
            for(String s : good)println(s);
        });
        scan.setDaemon(true);
        scan.start();
    }
    private void calibrateLeft(String txt, Runnable calibrate){
        calibrate(txt, (t) -> {
            return lastLeftEye>t;
        }, calibrate);
    }
    private void calibrateRight(String txt, Runnable calibrate){
        calibrate(txt, (t) -> {
            return lastRightEye>t;
        }, calibrate);
    }
    private void calibrateBoth(String txt, Runnable calibrate){
        calibrate(txt, (t) -> {
            return lastRightEye>t&&lastLeftEye>t;
        }, calibrate);
    }
    private void calibrate(String txt, Function<Long, Boolean> calibrationCheck, Runnable calibrate){
        tts.say("Calibrating "+txt+" in 3. 2. 1.");
        tts.waitUntilSaid();
        long startTime = System.nanoTime();
        boolean check = false;
        for(int i = 0; i<10; i++){
            try{
                Thread.sleep(500);
            }catch(InterruptedException ex){}
            check = calibrationCheck.apply(startTime);
            if(check)break;
        }
        if(check){
            tts.say("Done.");
            calibrate.run();
        }else{
            tts.say("Calibration Failed.");
        }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]){
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try{
            for(javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()){
                if("Nimbus".equals(info.getName())){
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }catch(ClassNotFoundException ex){
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }catch(InstantiationException ex){
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }catch(IllegalAccessException ex){
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }catch(javax.swing.UnsupportedLookAndFeelException ex){
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        /* Set the Windows look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Windows (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try{
            for(javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()){
                if("Windows".equals(info.getName())){
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }catch(ClassNotFoundException ex){
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }catch(InstantiationException ex){
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }catch(IllegalAccessException ex){
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }catch(javax.swing.UnsupportedLookAndFeelException ex){
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable(){
            public void run(){
                new Main().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox boxAntiFlicker;
    private javax.swing.JCheckBox boxAutoShutdown;
    private javax.swing.JCheckBox boxCombineLook;
    private javax.swing.JCheckBox boxInvertBlink;
    private javax.swing.JTextField boxLeftEyeBlink;
    private javax.swing.JTextField boxLeftEyeX;
    private javax.swing.JTextField boxLeftEyeY;
    private javax.swing.JCheckBox boxLeftFlipX;
    private javax.swing.JCheckBox boxLeftFlipY;
    private javax.swing.JTextField boxRightEyeBlink;
    private javax.swing.JTextField boxRightEyeX;
    private javax.swing.JTextField boxRightEyeY;
    private javax.swing.JCheckBox boxRightFlipX;
    private javax.swing.JCheckBox boxRightFlipY;
    private javax.swing.JButton buttonCalibrateAll;
    private javax.swing.JButton buttonCalibrateLeftCenter;
    private javax.swing.JButton buttonCalibrateLeftDown;
    private javax.swing.JButton buttonCalibrateLeftEye;
    private javax.swing.JButton buttonCalibrateLeftLeft;
    private javax.swing.JButton buttonCalibrateLeftRight;
    private javax.swing.JButton buttonCalibrateLeftUp;
    private javax.swing.JButton buttonCalibrateRightCenter;
    private javax.swing.JButton buttonCalibrateRightDown;
    private javax.swing.JButton buttonCalibrateRightEye;
    private javax.swing.JButton buttonCalibrateRightLeft;
    private javax.swing.JButton buttonCalibrateRightRight;
    private javax.swing.JButton buttonCalibrateRightUp;
    private javax.swing.JButton buttonLoad;
    private javax.swing.JButton buttonReconnectLeft;
    private javax.swing.JButton buttonReconnectRight;
    private javax.swing.JButton buttonResetFocalPoints;
    private javax.swing.JButton buttonSave;
    private javax.swing.JButton buttonScanInputs;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel labelEyeLock;
    private javax.swing.JLabel labelInputRaw;
    private javax.swing.JLabel labelInputStatus;
    private javax.swing.JLabel labelInputTracked;
    private javax.swing.JLabel labelInputs;
    private javax.swing.JLabel labelStatusLeft;
    private javax.swing.JLabel labelStatusRight;
    private javax.swing.JPanel panelControls;
    private javax.swing.JPanel panelControlsCalibration;
    private javax.swing.JPanel panelControlsCropping;
    private javax.swing.JPanel panelControlsEyeCropping;
    private javax.swing.JPanel panelControlsInput;
    private javax.swing.JPanel panelControlsLeft;
    private javax.swing.JPanel panelControlsOSC;
    private javax.swing.JPanel panelControlsRight;
    private javax.swing.JPanel panelControlsStability;
    private javax.swing.JPanel panelCroppingMain;
    private javax.swing.JPanel panelInputRaw;
    private javax.swing.JPanel panelInputScanner;
    private javax.swing.JPanel panelInputStatus;
    private javax.swing.JPanel panelInputStatusDual;
    private javax.swing.JPanel panelInputStatusLeft;
    private javax.swing.JPanel panelInputStatusRight;
    private javax.swing.JPanel panelInputTracked;
    private javax.swing.JPanel panelInputs;
    private javax.swing.JPanel panelInputsRawDisplay;
    private javax.swing.JPanel panelInputsTrackedDisplay;
    private javax.swing.JPanel panelOutput;
    private javax.swing.JPanel panelSaveLoad;
    private javax.swing.JPanel panelStatusLightLeft;
    private javax.swing.JPanel panelStatusLightRight;
    private javax.swing.JSlider sliderBlinkThreshold;
    private javax.swing.JSlider sliderEyeLockThreshold;
    private javax.swing.JSlider sliderThresholdMax;
    private javax.swing.JSlider sliderThresholdMin;
    private javax.swing.JSpinner spinnerAutoShutdown;
    private javax.swing.JSpinner spinnerCropLeftBottom;
    private javax.swing.JSpinner spinnerCropLeftLeft;
    private javax.swing.JSpinner spinnerCropLeftRight;
    private javax.swing.JSpinner spinnerCropLeftTop;
    private javax.swing.JSpinner spinnerCropRightBottom;
    private javax.swing.JSpinner spinnerCropRightLeft;
    private javax.swing.JSpinner spinnerCropRightRight;
    private javax.swing.JSpinner spinnerCropRightTop;
    private javax.swing.JSpinner spinnerRadius;
    private javax.swing.JTextArea textAreaOutput;
    private javax.swing.JTextField textFieldAddressLeft;
    private javax.swing.JTextField textFieldAddressRight;
    // End of variables declaration//GEN-END:variables
    private JButton[] calibrateButtons;
    private void save(){
        File file = new File("calibration.txt");
        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))){
            writer.write(stringify(maxThreshold,getRadius(),llx,lux,lrx,ldx,lcx,lly,luy,lry,ldy,lcy,rlx,rux,rrx,rdx,rcx,rly,ruy,rry,rdy,rcy)
                            +"|"+textFieldAddressLeft.getText()
                            +"|"+textFieldAddressRight.getText()
                            +"|"+stringify((int)spinnerCropLeftLeft.getValue(), (int)spinnerCropLeftRight.getValue(), (int)spinnerCropLeftTop.getValue(), (int)spinnerCropLeftBottom.getValue(), (int)spinnerCropRightLeft.getValue(), (int)spinnerCropRightRight.getValue(), (int)spinnerCropRightTop.getValue(), (int)spinnerCropRightBottom.getValue())
                            +"|"+stringify(boxLeftFlipX.isSelected(),boxLeftFlipY.isSelected(),boxRightFlipX.isSelected(),boxRightFlipY.isSelected())
                            +"|"+stringify(boxCombineLook.isSelected(), boxInvertBlink.isSelected())
                            +"|"+stringify(sliderBlinkThreshold.getValue())
                            +"|"+boxLeftEyeX.getText()
                            +"|"+boxLeftEyeY.getText()
                            +"|"+boxLeftEyeBlink.getText()
                            +"|"+boxRightEyeX.getText()
                            +"|"+boxRightEyeY.getText()
                            +"|"+boxRightEyeBlink.getText()
                            +"|"+stringify(sliderEyeLockThreshold.getValue())
                            +"|"+stringify(minThreshold,lfp1x,lfp1y,lfp2x,lfp2y,rfp1x,rfp1y,rfp2x,rfp2y)
                            +"|"+stringify(boxAntiFlicker.isSelected(), boxAutoShutdown.isSelected())
                            +"|"+stringify((int)spinnerAutoShutdown.getValue())
            );
        }catch(IOException ex){
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void load(){
        File file = new File("calibration.txt");
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))){
            String[] settings = reader.readLine().trim().split("\\|");
            String[] calibs = settings[0].split(" ");
            int[] calib = new int[calibs.length];
            for(int i = 0; i<calib.length; i++){
                calib[i] = Integer.parseInt(calibs[i]);
            }
            sliderThresholdMax.setValue(calib[0]);
            spinnerRadius.setValue(calib[1]);
            llx = calib[2];
            lux = calib[3];
            lrx = calib[4];
            ldx = calib[5];
            lcx = calib[6];
            lly = calib[7];
            luy = calib[8];
            lry = calib[9];
            ldy = calib[10];
            lcy = calib[11];
            rlx = calib[12];
            rux = calib[13];
            rrx = calib[14];
            rdx = calib[15];
            rcx = calib[16];
            rly = calib[17];
            ruy = calib[18];
            rry = calib[19];
            rdy = calib[20];
            rcy = calib[21];
            textFieldAddressLeft.setText(settings[1]);
            textFieldAddressRight.setText(settings[2]);
            String[] crop = settings[3].split(" ");
            spinnerCropLeftLeft.setValue(Integer.parseInt(crop[0]));
            spinnerCropLeftRight.setValue(Integer.parseInt(crop[1]));
            spinnerCropLeftTop.setValue(Integer.parseInt(crop[2]));
            spinnerCropLeftBottom.setValue(Integer.parseInt(crop[3]));
            spinnerCropRightLeft.setValue(Integer.parseInt(crop[4]));
            spinnerCropRightRight.setValue(Integer.parseInt(crop[5]));
            spinnerCropRightTop.setValue(Integer.parseInt(crop[6]));
            spinnerCropRightBottom.setValue(Integer.parseInt(crop[7]));
            String[] flip = settings[4].split(" ");
            boxLeftFlipX.setSelected(Boolean.parseBoolean(flip[0]));
            boxLeftFlipY.setSelected(Boolean.parseBoolean(flip[1]));
            boxRightFlipX.setSelected(Boolean.parseBoolean(flip[2]));
            boxRightFlipY.setSelected(Boolean.parseBoolean(flip[3]));
            String[] stuff = settings[5].split(" ");
            boxCombineLook.setSelected(Boolean.parseBoolean(stuff[0]));
            boxInvertBlink.setSelected(Boolean.parseBoolean(stuff[1]));
            sliderBlinkThreshold.setValue(Integer.parseInt(settings[6]));
            boxLeftEyeX.setText(settings[7]);
            boxLeftEyeY.setText(settings[8]);
            boxLeftEyeBlink.setText(settings[9]);
            boxRightEyeX.setText(settings[10]);
            boxRightEyeY.setText(settings[11]);
            boxRightEyeBlink.setText(settings[12]);
            sliderEyeLockThreshold.setValue(Integer.parseInt(settings[13]));
            labelEyeLock.setText("Eye Lock Time: "+sliderEyeLockThreshold.getValue()/10/100d+"s");
            String[] calibs2 = settings[14].split(" ");
            int[] calib2 = new int[calibs2.length];
            for(int i = 0; i<calib2.length; i++){
                calib2[i] = Integer.parseInt(calibs2[i]);
            }
            sliderThresholdMin.setValue(calib2[0]);
            lfp1x = calib2[1];
            lfp1y = calib2[2];
            lfp2x = calib2[3];
            lfp2y = calib2[4];
            rfp1x = calib2[5];
            rfp1y = calib2[6];
            rfp2x = calib2[7];
            rfp2y = calib2[8];
            String[] flickerandshutdown = settings[15].split(" ");
            boxAntiFlicker.setSelected(Boolean.parseBoolean(flickerandshutdown[0]));
            boxAutoShutdown.setSelected(Boolean.parseBoolean(flickerandshutdown[1]));
            String[] sixteen = settings[16].split(" ");
            spinnerAutoShutdown.setValue(Integer.parseInt(sixteen[0]));
        }catch(Exception ex){
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private String stringify(int... ints){
        String str = "";
        for(int i : ints)str+=i+" ";
        return str.trim();
    }
    private String stringify(boolean... bools){
        String str = "";
        for(boolean b : bools)str+=b+" ";
        return str.trim();
    }
    private Color colorScale(float f){
        if(f<.25f)return new Color(1, f*4, 0);
        if(f<.5f)return new Color(-4*f+2, 1, 0);
        if(f<.75f)return new Color(0, 1, f*4-2);
        return new Color(0, (-4*f+4)/2+.5f, 1);
    }
    private double getWeight(int eye){
        if(eye==0){//left
            long lag = System.nanoTime()-lastLeftEye;
            return Math.exp(-lag/1_000_000_000d);
        }else{//right
            long lag = System.nanoTime()-lastRightEye;
            return Math.exp(-lag/1_000_000_000d);
        }
    }
    private float getWeighted(float left, float right){
        double leftWeight = getWeight(0);
        double rightWeight = getWeight(1);
        double normalizer = 1/(leftWeight+rightWeight);
        return (float)((left*leftWeight+right*rightWeight)*normalizer);
    }
    private int getWeighted(int left, int right){
        double leftWeight = getWeight(0);
        double rightWeight = getWeight(1);
        double normalizer = 1/(leftWeight+rightWeight);
        return (int)((left*leftWeight+right*rightWeight)*normalizer);
    }
    private void drawStatus(JPanel panel, Graphics g, int eye){
        float fx = 0, fy = 0;
        double weight = getWeight(eye);
        g.setColor(new Color(0, 0, 0, (float)weight));
        if(eye==1){
            if(!rOpen)return;
            fx = rxOut;
            fy = -ryOut;
        }
        if(eye==0){
            if(!lOpen)return;
            fx = lxOut;
            fy = -lyOut;
        }
        int x = (int)(panel.getWidth()*(fx+1)/2);
        int y = (int)(panel.getHeight()*(fy+1)/2);
        g.drawLine(x, 0, x, panel.getHeight());
        g.drawLine(0, y, panel.getWidth(), y);
        if(boxCombineLook.isSelected()){
            g.setColor(Color.white);
            x = (int)(panel.getWidth()*(xOut+1)/2);
            y = (int)(panel.getHeight()*(-yOut+1)/2);
            g.drawLine(x, 0, x, panel.getHeight());
            g.drawLine(0, y, panel.getWidth(), y);
        }
    }
    private void drawPlus(Graphics g, int x, int y, int size){
        g.drawLine(x, y-size, x, y+size);
        g.drawLine(x-size, y, x+size, y);
    }
    private int getRadius(){
        return (int)spinnerRadius.getValue();
    }
    private boolean lfp1o,lfp2o,rfp1o,rfp2o;
    long startupTime = System.nanoTime();
    private void resetAutoShutdown(){
        startupTime = System.nanoTime();
    }
    private synchronized void println(String string){
        System.out.println(string);
        textAreaOutput.setText(textAreaOutput.getText()+string+"\n");
    }
    private class SpecialMouseListener implements MouseListener, MouseMotionListener{
        private final JPanel panel;
        private final int eyeIndex;
        public SpecialMouseListener(JPanel panel, int eyeIndex){
            this.panel = panel;
            this.eyeIndex = eyeIndex;
        }
        private int getScale(){
            BufferedImage imgRaw = getRawImage();
            BufferedImage img = getImage();
            int scale = 1;
            try{
                scale = Math.min(imgRaw.getWidth()/img.getWidth(),imgRaw.getWidth()/img.getWidth());
            }catch(Throwable t){}
            return scale;
        }
        private BufferedImage getImage(){
            return eyeIndex==0?leftEye:rightEye;
        }
        private BufferedImage getRawImage(){
            return eyeIndex==0?leftEyeRaw:rightEyeRaw;
        }
        private int[] getFocusPoint(int idx){
            if(eyeIndex==0){
                return idx==0?new int[]{lfp1x,lfp1y}:new int[]{lfp2x,lfp2y};
            }else{
                return idx==0?new int[]{rfp1x,rfp1y}:new int[]{rfp2x,rfp2y};
            }
        }
        private void setFocusPoint(int idx, int x, int y){
            if(eyeIndex==0){
                if(idx==0){
                    lfp1x = x;
                    lfp1y = y;
                }else{
                    lfp2x = x;
                    lfp2y = y;
                }
            }else{
                if(idx==0){
                    rfp1x = x;
                    rfp1y = y;
                }else{
                    rfp2x = x;
                    rfp2y = y;
                }
            }
        }
        private void mouseover(int idx){
            if(eyeIndex==0){
                lfp1o = idx==0;
                lfp2o = idx==1;
            }else{
                rfp1o = idx==0;
                rfp2o = idx==1;
            }
        }
        private boolean isMouseover(int idx){
            if(eyeIndex==0){
                return idx==0?lfp1o:lfp2o;
            }else{
                return idx==0?rfp1o:rfp2o;
            }
        }
        private int getMouseover(){
            if(isMouseover(0))return 0;
            if(isMouseover(1))return 1;
            return -1;
        }
        private int getPoint(int x, int y){
            int[] fp1 = getFocusPoint(0);
            int d1 = getDist(x,y,fp1[0],fp1[1]);
            int[] fp2 = getFocusPoint(1);
            int d2 = getDist(x,y,fp2[0],fp2[1]);
            int minDist = Math.min(d1,d2);
            if(minDist>5)return -1;
            if(d2<d1)return 1;
            return 0;
        }
        private int getDist(int x1, int y1, int x2, int y2){
            return (int)Math.sqrt(Math.pow(x1-x2,2)+Math.pow(y1-y2,2));
        }
        int holding = -1;
        private int getHolding(){
            return getMouseover()==-1?-1:holding;
        }
        @Override
        public void mouseClicked(MouseEvent e){}
        @Override
        public void mousePressed(MouseEvent e){
            holding = getMouseover();
        }
        @Override
        public void mouseReleased(MouseEvent e){
            holding = -1;
        }
        @Override
        public void mouseEntered(MouseEvent e){}
        @Override
        public void mouseExited(MouseEvent e){
            mouseover(-1);
        }
        @Override
        public void mouseDragged(MouseEvent e){
            int scale = getScale();
            int x = e.getX()/scale;
            int y = e.getY()/scale;
            int pt = getHolding();
            if(pt!=1)setFocusPoint(pt, x, y);
            else mouseover(getPoint(e.getX()/scale, e.getY()/scale));
        }
        @Override
        public void mouseMoved(MouseEvent e){
            int scale = getScale();
            mouseover(getPoint(e.getX()/scale, e.getY()/scale));
        }
    }
}