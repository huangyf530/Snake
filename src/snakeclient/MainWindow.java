package snakeclient;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.net.URL;

public class MainWindow extends JPanel {
    private int beforeGame;
    private int [][] situation; //0表示没有东西，1表示墙，2表示洞，3表示蛋，4表示蛇；
    private double oneRecWidth; //一格的宽度
    private double oneRecHeight;//一格的高度
    private int level;
    private ImageIcon backGround;
    private PauseButton pauseButton;//暂停按键
    private MusicButton musicButton;//音乐按键
    JFrame mainWindow;
    private Direction d;
    private int myRole;
    private JLabel[] livesLabel;
    private JLabel[] scoreLabel;
    private Socket socketClient;
    private BufferedReader brFromServer;
    private DataOutputStream dosToServer;
    private SendThread thread;
    private int myLives;
    private int opLives;
    private int isPause;
    private int myScore;
    private int opScore;
    int isOver;
    RCVThread rcvThread;
    private ImageIcon egg;
    private TextArea txarChat;
    private TextArea txarInput;
    private JButton btChat;
    private AudioInputStream currentSound;
    private Clip clip;
    private static URL musicURL = MainWindow.class.getResource("/resource/music.wav");
    MainWindow(){
        rcvThread = new RCVThread(this);
        getBeginInter();
        getMusicStream();
        System.out.println("hahhahah");
        //playMusic();
    }

    private void getBeginInter(){
        JFrame beginInter = new JFrame("Snake");
        BeginPanel contentPane = new BeginPanel();
        beginInter.setContentPane(contentPane);
        contentPane.setPreferredSize(new Dimension(600, 600));
        contentPane.setLayout(new GridLayout(12, 6));
        JButton beginButton = new JButton("开始游戏");
        beginButton.setFont(new Font("黑体", Font.ITALIC, 15));
        for(int i = 0; i < 12; i++){
            for(int j = 0; j < 6; j++){
                if(i == 7 && j == 1){
                    contentPane.add(beginButton);
                    continue;
                }
                contentPane.add(new JLabel());
            }
        }
        beginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                beginInter.dispose();
                Initial();
                rcvThread.start();
            }
        });
        beginInter.pack();
        beginInter.setVisible(true);
        beginInter.setLocationRelativeTo(null);
    }

    private void getMusicStream(){
        try{
            currentSound = AudioSystem.getAudioInputStream(MainWindow.musicURL);
            clip = AudioSystem.getClip();
            clip.open(currentSound);
            clip.setLoopPoints(0, clip.getFrameLength() - 1);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
        catch (UnsupportedAudioFileException e){
            System.out.println(e.getMessage());
        }
        catch (LineUnavailableException e){
            System.out.println(e.getMessage());
        }
        finally {
            System.out.println(currentSound.getFrameLength());
        }
    }

    private void Initial(){
        myLives = opLives = 5;
        myScore = opScore = 0;
        isPause = 0;
        isOver = 0;
        mainWindow = new JFrame("Snake");
        mainWindow.add(this, "Center");
        this.level = level;
        backGround = new ImageIcon("./src/resource/background.jpg");
        //setBackGround(mainWindow);//set the background from source "./data/background.jpg"
        JPanel contentPanel = (JPanel)mainWindow.getContentPane();
        JPanel paneWest = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(paneWest, "Center");
        paneWest.setLayout(new BorderLayout());
        paneWest.add(this, "Center");
        paneWest.setOpaque(false);
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(backGround.getIconWidth(), backGround.getIconHeight()));
        oneRecWidth = (double)backGround.getIconWidth() / 30;
        oneRecHeight = (double)backGround.getIconHeight() / 30;
        paneWest.add(getFunctionPanel(), "South");
        contentPanel.add(setChatPanel(), "East");
        situation = new int[30][30];
        for(int i = 0; i < 30; i++){
            for(int j = 0; j < 30; j++){
                situation[i][j] = 0;
            }
        }
        beforeGame = -1;
        d = new Direction(-1, isPause, isOver);
        egg = new ImageIcon("./src/resource/egg.png");


        //mainWindow.setSize(727, 800);
        mainWindow.pack();
        mainWindow.setVisible(true);
        mainWindow.requestFocus();
        mainWindow.setLocationRelativeTo(null);
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //mainWindow.setResizable(false);
    }

    private JPanel setChatPanel(){
        JPanel paneChat = new JPanel();
        JPanel paneInput = new JPanel();
        txarChat = new TextArea();
        txarInput = new TextArea();
        btChat = new JButton("Send");
        paneChat.setLayout(new BorderLayout());
        paneChat.add(txarChat, "Center");
        paneChat.add(paneInput, "South");
        paneInput.setLayout(new BorderLayout());
        paneInput.add(txarInput, "Center");
        paneInput.add(btChat, "East");
        txarChat.setPreferredSize(new Dimension(backGround.getIconWidth() / 4, this.getHeight()));
        paneInput.setPreferredSize(new Dimension(backGround.getIconWidth() / 4, 60));
        txarInput.setPreferredSize(new Dimension(backGround.getIconWidth() * 4 / 5, 60));
        paneChat.setPreferredSize(new Dimension(backGround.getIconWidth() / 4, backGround.getIconHeight() + 60));
        return paneChat;
    }

    void connectToServer(String ip, int port){
        System.out.printf("Connect to server %s at port %d\n", ip, port);
        try{
            socketClient = new Socket(ip, port);
            brFromServer = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            dosToServer = new DataOutputStream(socketClient.getOutputStream());
            System.out.println("Connect Successfully");
            thread = new SendThread(d, dosToServer);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    void getRole(){
        try{
            myRole = brFromServer.read();
            mainWindow.setTitle("Snake Player" + myRole);
            System.out.printf("I am player %d\n", myRole);
        }
        catch (IOException e){
            e.printStackTrace();
            System.out.println("I am receiving my role number");
        }
    }

    void getMessage(){
        try{
            String temp = brFromServer.readLine();
            //System.out.println(temp);
            String[] mess = temp.split(",");
            for(int i = 0; i < 30; i++){
                for(int j = 0; j < 30; j++){
                    situation[i][j] = Integer.parseInt(mess[i * 30 + j]);
                }
            }
            int k = Integer.parseInt(mess[29 * 30 + 29 + 1]);
            if(k == 3 - myRole){
                pauseButton.setEnabled(false);
            }
            if(k == 0 && isPause > 0){
                pauseButton.setEnabled(true);
            }
            isPause = k;
            int getLives = Integer.parseInt(mess[29 * 30 + 29 + myRole + 1]);
            if(getLives < myLives){
                d.direction = -1;
            }
            myLives = getLives;
            opLives = Integer.parseInt(mess[29 * 30 + 29 + 3 - myRole + 1]);
            livesLabel[myRole - 1].setText("Lives: " + myLives);
            livesLabel[3 - myRole - 1].setText("Lives: " + opLives);
            myScore = Integer.parseInt(mess[29 * 30 + 29 + 3 + myRole]);
            opScore = Integer.parseInt(mess[29 * 30 + 29 + 3 + 3 - myRole]);
            scoreLabel[myRole - 1].setText("Score: " + myScore);
            scoreLabel[3 - myRole - 1].setText("Score: " + opScore);
            isOver = Integer.parseInt(mess[29 * 30 + 29 + 3 + 3]);
            if(isOver > 0){
                d.setIsOver(isOver);
                pauseButton.setEnabled(false);
            }
        }
        catch (IOException e){
            System.out.println("Server break! Game Over!");
            isOver = 5;
            d.setIsOver(isOver);
            pauseButton.setEnabled(false);
        }
        finally {
            repaint();
        }
    }

    void getCountDown(){
        try{
            beforeGame = brFromServer.read();
            repaint();
            while(beforeGame > 0){
                beforeGame = brFromServer.read();
                repaint();
            }
        }
        catch (IOException e){
            e.printStackTrace();
            System.out.println("Receiving countdown");
        }
        finally {
            mainWindow.addKeyListener(new MyKeyAdapter());
            mainWindow.requestFocus();
            thread.start();
        }
    }

//    private void setBackGround(JFrame frame){
//        backGround = new ImageIcon("./src/resource/background.jpg");
//        JLabel backLabel = new JLabel(backGround);
//        frame.getLayeredPane().add(backLabel, new Integer(-30001));
//        System.out.println(backGround.getIconHeight());
//        System.out.println(backGround.getIconWidth());
//        frame.getLayeredPane().setPreferredSize(new Dimension(backGround.getIconWidth(), backGround.getIconHeight()));
//        backLabel.setBounds(0,0, backGround.getIconWidth(), backGround.getIconHeight());
//    }

    private JPanel getFunctionPanel(){
        JPanel fctPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        //Insets inset = new Insets(5, 5, 5,5);
        //constraints.insets = inset;
        fctPanel.setLayout(gridbag);
        JLabel playerLabel1 = new JLabel("Player 1");
        playerLabel1.setBackground(new Color(90, 160, 25));
        playerLabel1.setOpaque(true);
        playerLabel1.setHorizontalAlignment(JLabel.CENTER);
        Border labelBoarder =BorderFactory.createLineBorder(Color.red);
        playerLabel1.setBorder(labelBoarder);
        fctPanel.add(playerLabel1);
        gridbag.setConstraints(playerLabel1, constraints);
        JLabel playerLabel2 = new JLabel("Player 2");
        playerLabel2.setBackground(new Color(90, 160, 25));
        playerLabel2.setOpaque(true);
        playerLabel2.setHorizontalAlignment(JLabel.CENTER);
        playerLabel2.setBorder(labelBoarder);
        fctPanel.add(playerLabel2);
        constraints.gridx = 0;
        constraints.gridy = 1;
        gridbag.setConstraints(playerLabel2, constraints);
        livesLabel = new JLabel[2];
        livesLabel[0] = new JLabel("Lives: " + myLives);
        livesLabel[0].setBackground(new Color(90, 160, 25));
        livesLabel[0].setOpaque(true);
        livesLabel[0].setHorizontalAlignment(JLabel.CENTER);
        livesLabel[0].setBorder(labelBoarder);
        fctPanel.add(livesLabel[0]);
        constraints.gridx = 1;
        constraints.gridy = 0;
        gridbag.setConstraints(livesLabel[0], constraints);
        scoreLabel = new JLabel[2];
        scoreLabel[0] = new JLabel("Score: " + myScore);
        scoreLabel[0].setBackground(new Color(90, 160, 25));
        scoreLabel[0].setOpaque(true);
        scoreLabel[0].setHorizontalAlignment(JLabel.CENTER);
        scoreLabel[0].setBorder(labelBoarder);
        fctPanel.add(scoreLabel[0]);
        constraints.gridx = 2;
        gridbag.setConstraints(scoreLabel[0], constraints);
        livesLabel[1] = new JLabel("Lives: " + opLives);
        livesLabel[1].setBackground(new Color(90, 160, 25));
        livesLabel[1].setOpaque(true);
        livesLabel[1].setHorizontalAlignment(JLabel.CENTER);
        livesLabel[1].setBorder(labelBoarder);
        fctPanel.add(livesLabel[1]);
        constraints.gridx = 1;
        constraints.gridy = 1;
        gridbag.setConstraints(livesLabel[1], constraints);
        scoreLabel[1] = new JLabel("Score: " + opScore);
        scoreLabel[1].setBackground(new Color(90, 160, 25));
        scoreLabel[1].setOpaque(true);
        scoreLabel[1].setHorizontalAlignment(JLabel.CENTER);
        scoreLabel[1].setBorder(labelBoarder);
        fctPanel.add(scoreLabel[1]);
        constraints.gridx = 2;
        gridbag.setConstraints(scoreLabel[1], constraints);
        JPanel btnPanel = getButtonPanel();
        btnPanel.setBorder(labelBoarder);
        fctPanel.add(btnPanel);
        fctPanel.setPreferredSize(new Dimension(backGround.getIconWidth(), 60));
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        gridbag.setConstraints(btnPanel, constraints);
        return fctPanel;
    }

    private JPanel getButtonPanel(){
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(new Color(90, 160, 25));
        pauseButton = new PauseButton();
        pauseButton.setSize(30, 30);
        pauseButton.setFocusPainted(false);
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(pauseButton.isPause){
                    pauseButton.isPause = false;
                    isPause = 0;
                    d.setIsPause(isPause);
                    pauseButton.repaint();
                    mainWindow.requestFocus();
                }
                else{
                    isPause = myRole;
                    pauseButton.isPause = true;
                    d.setIsPause(isPause);
                    pauseButton.repaint();
                    System.out.println("I Paused");
                }
            }
        });
        btnPanel.add(pauseButton);
        musicButton = new MusicButton();
        musicButton.setSize(30, 30);
        musicButton.setFocusPainted(false);
        musicButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(musicButton.isMute){
                    FloatControl floatControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    floatControl.setValue(0);
                    musicButton.isMute = false;
                    musicButton.repaint();
                    mainWindow.requestFocus();
                }
                else{
                    FloatControl floatControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float db = (float)(Math.log(0.0001) / Math.log(10.0) * 20.0);
                    floatControl.setValue(db);
                    musicButton.isMute = true;
                    musicButton.repaint();
                    mainWindow.requestFocus();
                }
            }
        });
        btnPanel.add(musicButton);

        /*add button*/

        return btnPanel;
    }

//    private void playMusic(){
//        File f = new File("./src/resource/music.wav");
//        URI uri = f.getAbsoluteFile().toURI();
//        Media media = new Media(uri.toString());
//        MediaPlayer player = new MediaPlayer(media);
//
//        System.out.println(player.autoPlayProperty());
//        player.setAutoPlay(true);
//    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);//info: every rectangle is 20 * 20
        oneRecWidth = (double)(this.getWidth()) / 30;
        oneRecHeight = (double)(this.getHeight()) / 30;
        //System.out.printf("Width %f Height%f\n", oneRecWidth, oneRecHeight);
        drawBackGround(g);
        for(int i = 0; i < 30; i++){
            for(int j = 0; j < 30; j++){
                if(situation[i][j] == 1){
                    drawWall(g, j, i);
                }
                if(situation[i][j] == 2){
                    drawHole(g, j, i);
                }
                if(situation[i][j] == 3){
                    drawEgg(g, j ,i);
                }
                if(situation[i][j] == 4){
                    drawSnake(g, j, i, 4);
                }
                if(situation[i][j] == 5){
                    drawSnake(g, j, i, 5);
                }
            }
        }
        if(isOver == 0 && myRole != 0) {
            if (beforeGame > 0) {
                g.setColor(Color.BLACK);
                g.setFont(new Font("黑体", Font.BOLD, 30));
                g.drawString(Integer.toString(beforeGame), (int)(14 * oneRecWidth), (int)(14 * oneRecHeight));
            }
            if (beforeGame == 0) {
                g.setColor(Color.BLACK);
                g.setFont(new Font("黑体", Font.BOLD, 30));
                g.drawString("Game Begin!", (int)(12 * oneRecWidth), (int)(14 * oneRecHeight));
                beforeGame--;
            }
            if (isPause == myRole) {
                g.setColor(Color.BLACK);
                g.setFont(new Font("黑体", Font.BOLD, 30));
                g.drawString("You paused the game!", (int)(8 * oneRecWidth), (int)(14 * oneRecHeight));
            }
            if (isPause == 3 - myRole) {
                g.setColor(Color.BLACK);
                g.setFont(new Font("黑体", Font.BOLD, 30));
                g.drawString("Opponent paused the game!", (int)(8 * oneRecWidth), (int)(14 * oneRecHeight));
            }
        }
        if(isOver == 3 - myRole && myRole != 0){
            g.setColor(Color.RED);
            g.setFont(new Font("黑体", Font.BOLD, 30));
            g.drawString("Congratulations! You WIN!", (int)(6 * oneRecWidth), (int)(14 * oneRecHeight));
        }
        if(isOver == myRole && myRole != 0){
            g.setColor(Color.RED);
            g.setFont(new Font("黑体", Font.BOLD, 30));
            g.drawString("Sorry! You LOSE!", (int)(8 * oneRecWidth), (int)(14 * oneRecHeight));
        }
        if(isOver == 5 - myRole && myRole != 0){
            g.setColor(Color.RED);
            g.setFont(new Font("黑体", Font.BOLD, 30));
            g.drawString("Opponent is offline! You WIN!", (int)(4 * oneRecWidth), (int)(14 * oneRecHeight));
        }
        if(isOver == 5){
            g.setColor(Color.RED);
            g.setFont(new Font("黑体", Font.BOLD, 30));
            g.drawString("Sorry, server break down!", (int)(4 * oneRecWidth), (int)(14 * oneRecHeight));
        }
//        drawWall(g);
//        drawHole(g);
//        drawEgg(g);
//        if(!mySnake.dead) {
//            drawSnake(g, mySnake, Color.orange);
//        }
        //g.drawRect(1, 0, 600, 600);
    }

    private void drawWall(Graphics g, int x, int y){
        g.setColor(new Color(170, 231, 44));
        g.fillRect((int)(x * oneRecWidth), (int)(y * oneRecHeight), (int)(oneRecWidth), (int)(oneRecHeight));
    }

    private void drawHole(Graphics g, int x, int y){
        g.setColor(Color.BLACK);
        g.fillOval((int)(x * oneRecWidth), (int)(y * oneRecHeight), (int)(oneRecWidth), (int)(oneRecHeight));
    }

    private void drawEgg(Graphics g, int x, int y){
        g.drawImage(egg.getImage(), (int)(x * oneRecWidth), (int)(y * oneRecHeight), (int)(oneRecWidth), (int)(oneRecHeight), mainWindow);
    }

    private void drawSnake(Graphics g, int x, int y, int num){
        if(num == 4){
            g.setColor(Color.orange);
        }
        else{
            g.setColor(Color.BLUE);
        }
        g.fillOval((int)(x * oneRecWidth), (int)(y * oneRecHeight), (int)(oneRecWidth), (int)(oneRecHeight));
    }

    private void drawBackGround(Graphics g){
        g.drawImage(backGround.getImage(), 0, 0, this.getWidth(), this.getHeight(), mainWindow);
    }

    private class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            super.keyPressed(e);
            int keyCode = e.getKeyCode();
            if(isPause == 0) {
                if (myRole == 1) {
                    switch (keyCode) {
                        case KeyEvent.VK_W:
                            if (d.direction != 2) {
                                d.setDirection(0);
                                System.out.println("Go up!");
                            }
                            break;
                        case KeyEvent.VK_D:
                            if (d.direction != 3) {
                                d.setDirection(1);
                                System.out.println("Go right!");
                            }
                            break;
                        case KeyEvent.VK_S:
                            if (d.direction != 0) {
                                d.setDirection(2);
                                System.out.println("Go down!");
                            }
                            break;
                        case KeyEvent.VK_A:
                            if (d.direction != 1) {
                                d.setDirection(3);
                                System.out.println("Go left!");
                            }
                            break;
                        default:
                            System.out.println("Not valid!");
                    }
                } else {
                    switch (keyCode) {
                        case KeyEvent.VK_UP:
                            if (d.direction != 2) {
                                d.setDirection(0);
                                System.out.println("Go up!");
                            }
                            break;
                        case KeyEvent.VK_RIGHT:
                            if (d.direction != 3) {
                                d.setDirection(1);
                                System.out.println("Go right!");
                            }
                            break;
                        case KeyEvent.VK_DOWN:
                            if (d.direction != 0) {
                                d.setDirection(2);
                                System.out.println("Go down!");
                            }
                            break;
                        case KeyEvent.VK_LEFT:
                            if (d.direction != 1) {
                                d.setDirection(3);
                                System.out.println("Go left!");
                            }
                            break;
                        default:
                            System.out.println("Not valid!");
                    }
                }
            }
        }
    }
}

class Direction{
    int direction;
    int isPause;
    int isOver;
    static String[] Tran = {"UP","RIGHT", "DOWN", "LEFT"};
    Direction(int direction, int isPause, int isOver){
        this.direction = direction;
        this.isPause = isPause;
        this.isOver = isOver;
    }
    synchronized void sendDirection(DataOutputStream out){
        try{
            String toSend = direction + "," + isPause + "\n";
            out.writeBytes(toSend);
            if(isOver == 0) {
                wait();
            }
        }
        catch (IOException e){
            System.out.printf("I am sending direction %d. But server is offline!\n", direction);
        }
        catch (InterruptedException e){
            System.out.println("Interrupted");
        }
    }
    synchronized void setDirection(int direction){
        this.direction = direction;
        notify();
    }
    synchronized void setIsPause(int isPause){
        this.isPause = isPause;
        notify();
    }

    synchronized  void setIsOver(int isOver){
        this.isOver = isOver;
        notify();
    }
}

class SendThread extends Thread{
    private Direction d;
    DataOutputStream out;
    SendThread(Direction direction, DataOutputStream out){
        this.d = direction;
        this.out = out;
    }

    @Override
    public void run() {
        super.run();
        while(d.isOver == 0) {
            d.sendDirection(out);
        }
        d.sendDirection(out);
    }
}

class BeginPanel extends JPanel{
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ImageIcon icon = new ImageIcon("./src/resource/begin.JPG");
        g.drawImage(icon.getImage(), 0, 0, this.getWidth(), this.getHeight(), this);
    }
}

class RCVThread extends Thread{
    MainWindow mainWindow;
    RCVThread(MainWindow mainWindow){
        this.mainWindow = mainWindow;
    }
    @Override
    public void run() {
        super.run();
        mainWindow.requestFocus();
        mainWindow.connectToServer("192.168.244.1", 6888);
        mainWindow.getRole();
        mainWindow.getMessage();
        mainWindow.getCountDown();
        while(mainWindow.isOver == 0){
            mainWindow.getMessage();
        }
    }
}