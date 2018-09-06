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
import java.nio.charset.StandardCharsets;

public class MainWindow extends JPanel {
    private int beforeGame;
    private int [][] situation; //0表示没有东西，1表示墙，2表示洞，3表示蛋，4表示蛇；
    private double oneRecWidth; //一格的宽度
    private double oneRecHeight;//一格的高度
    private int level;
    private ImageIcon backGround;
    private PauseButton pauseButton;//暂停按键
    private MusicButton musicButton;//音乐按键
    private SpeedButton speedDownButton;
    private SpeedButton speedUPButton;
    private JFrame mainWindow;
    private Direction d;
    private int myRole;
    private JLabel[] livesLabel;
    private JLabel[] scoreLabel;
    private Socket socketClient;
    private BufferedReader brFromServer;
    private DataOutputStream dosToServer;
    private SendThread thread;
    private String myName;
    private int myLives;
    private int opLives;
    private int isPause;
    private int myScore;
    private int opScore;
    int isOver;
    RCVThread rcvThread;
    private ImageIcon egg;
    private JTextArea txarChat;
    private JTextArea txarRank;
    private JTextArea txarInput;
    private JButton btChat;
    private AudioInputStream currentSound;
    private Clip clip;
    private static URL musicURL = MainWindow.class.getResource("/resource/music.wav");
    private JTextField txfdIPAdress;
    private JTextField txfdPort;
    MainWindow(){
        System.out.println("New one");
        rcvThread = new RCVThread(this);
        getBeginInter();
        getMusicStream();
    }

    private void getBeginInter(){
        JFrame beginInter = new JFrame("Snake");
        BeginPanel contentPane = new BeginPanel();
        beginInter.setContentPane(contentPane);
        contentPane.setPreferredSize(new Dimension(600, 600));
        contentPane.setLayout(new GridLayout(12, 6));
        JButton beginButton = new JButton("开始游戏");
        beginButton.setFont(new Font("黑体", Font.ITALIC, 15));
        txfdIPAdress = new JTextField("IP Address");
        txfdPort = new JTextField("Port");
        for(int i = 0; i < 12; i++){
            for(int j = 0; j < 6; j++){
                if(i == 8 && j == 1){
                    contentPane.add(beginButton);
                    continue;
                }
                if(i == 4 && j == 1){
                    contentPane.add(txfdIPAdress);
                    continue;
                }
                if(i == 6 && j == 1){
                    contentPane.add(txfdPort);
                    continue;
                }
                contentPane.add(new JLabel());
            }
        }
        beginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myName = JOptionPane.showInputDialog(beginInter, "请输入你的昵称", "Snake", JOptionPane.INFORMATION_MESSAGE);
                beginInter.dispose();
                System.out.println(myName);
                String iPAdress = txfdIPAdress.getText();
                int port = Integer.parseInt(txfdPort.getText());
                Initial();
                //connectToServer("192.168.244.1", 6888);
                connectToServer(iPAdress, port);
                sendMyName(myName);
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
            System.out.println("Frame Length: " + currentSound.getFrameLength());
        }
    }

    private void Initial(){
        myLives = opLives = 5;
        myScore = opScore = 0;
        isPause = 0;
        isOver = 0;
        mainWindow = new JFrame("Snake");
        mainWindow.add(this, "Center");
        backGround = new ImageIcon(MainWindow.class.getResource("/resource/background.jpg"));
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
        d = new Direction(-1, isPause, isOver, 2);
        URL urlEgg = MainWindow.class.getResource("/resource/egg.png");
        egg = new ImageIcon(urlEgg);


        //mainWindow.setSize(727, 800);
        mainWindow.pack();
        mainWindow.setVisible(true);
        mainWindow.requestFocus();
        mainWindow.setLocationRelativeTo(null);
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //mainWindow.setResizable(false);
    }

    private void sendMyName(String name){
        try{
            String newName = new String(name.getBytes(), StandardCharsets.UTF_8) + "\n";
            dosToServer.write(newName.getBytes());
            //System.out.println(name.getBytes());
        }
        catch (IOException e){
            System.out.println("I am sending name to server");
        }
    }

    private JPanel setChatPanel(){
        JPanel paneChat = new JPanel();
        JPanel paneInput = new JPanel();
        txarChat = new JTextArea();
        txarInput = new JTextArea();
        txarRank = new JTextArea();
        txarRank.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        txarRank.setEditable(false);
        txarChat.setLineWrap(true);
        txarChat.setEditable(false);
        txarInput.setLineWrap(true);
        btChat = new JButton("Send");
        btChat.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String strSend = new String(txarInput.getText().getBytes("UTF-8"), "UTF-8");
                    byte[] messb = strSend.getBytes();
                    for(int j = 0; j < messb.length; j++) {
                        System.out.println(Integer.toHexString(messb[j]));
                    }
                    System.out.println(messb);
                    if (!strSend.equals("")) {
                        d.setStrToSend(strSend);
                        txarChat.append("Player " + myRole + ":" + strSend + "\n");
                        txarInput.setText("");
                    }
                }
                catch (UnsupportedEncodingException err){
                    System.out.println("Can't be encoded by UTF-8");
                    String strSend = txarInput.getText();
                    if (!strSend.equals("")) {
                        d.setStrToSend(strSend);
                        txarChat.append("Player " + myRole + ":" + strSend + "\n");
                        txarInput.setText("");
                    }
                }
                finally {
                    mainWindow.requestFocus();
                }
            }
        });
        paneChat.setLayout(new BorderLayout());
        JPanel paneRank = new JPanel();
        paneChat.add(paneRank, "North");
        paneRank.setLayout(new BorderLayout());
        JLabel labRank = new JLabel("排行榜");
        labRank.setHorizontalAlignment(JLabel.CENTER);
        paneRank.add(labRank, "North");
        paneRank.add(txarRank, "Center");
        paneRank.setPreferredSize(new Dimension(backGround.getIconWidth() / 4, backGround.getIconHeight() / 2));
        paneChat.add(txarChat, "Center");
        paneChat.add(paneInput, "South");
        paneInput.setLayout(new BorderLayout());
        paneInput.add(txarInput, "Center");
        paneInput.add(btChat, "East");
        txarChat.setPreferredSize(new Dimension(backGround.getIconWidth() / 4, backGround.getIconHeight() / 2));
        paneInput.setPreferredSize(new Dimension(backGround.getIconWidth() / 4, 60));
        paneInput.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        txarInput.setPreferredSize(new Dimension(backGround.getIconWidth() * 4 / 5, 60));
        paneChat.setPreferredSize(new Dimension(backGround.getIconWidth() / 4, backGround.getIconHeight() + 60));
        return paneChat;
    }

    private void connectToServer(String ip, int port){
        System.out.printf("Connect to server %s at port %d\n", ip, port);
        try{
            socketClient = new Socket(ip, port);
            brFromServer = new BufferedReader(new InputStreamReader(socketClient.getInputStream(), StandardCharsets.UTF_8));
            //ObjectInputStream inObject = new ObjectInputStream(socketClient.getInputStream());
            dosToServer = new DataOutputStream(socketClient.getOutputStream());
            System.out.println("Connect Successfully");
            String plrSet = brFromServer.readLine();
            System.out.println(plrSet);
            String[] plrSets = plrSet.split(",");
            for(int i = 0; i < plrSets.length; i++){
                txarRank.append(plrSets[i] + "\n");
            }
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
            String[] mess = temp.split(",", -1);
            for(int i = 0; i < 30; i++){
                for(int j = 0; j < 30; j++){
                    situation[i][j] = Integer.parseInt(mess[i * 30 + j]);
                }
            }
            int k = Integer.parseInt(mess[29 * 30 + 29 + 1]);
            if(k == 3 - myRole){
                pauseButton.setEnabled(false);
                speedUPButton.setEnabled(false);
                speedDownButton.setEnabled(false);
            }
            if(k == 0 && isPause > 0){
                pauseButton.setEnabled(true);
                speedDownButton.setEnabled(true);
                speedUPButton.setEnabled(true);
            }
            isPause = k;
            d.isPause = k;
            int getLives = Integer.parseInt(mess[29 * 30 + 29 + myRole + 1]);
            int direction = Integer.parseInt(mess[29 * 30 + 29 + 3 + 3 + 2]);
            //System.out.println(direction);
            if(getLives < myLives){
                txarChat.append("System: You are dead\n");
                System.out.println(d.direction);
                d.direction = -1;
            }
            else {
                if(direction < 0 && (d.direction >= 0)){
                    txarChat.append("System: You are in Hole\n");
                }
            }
            d.direction = direction;
            myLives = getLives;
            opLives = Integer.parseInt(mess[29 * 30 + 29 + 3 - myRole + 1]);
            livesLabel[myRole - 1].setText("Lives: " + myLives);
            livesLabel[3 - myRole - 1].setText("Lives: " + opLives);
            myScore = Integer.parseInt(mess[29 * 30 + 29 + 3 + myRole]);
            opScore = Integer.parseInt(mess[29 * 30 + 29 + 3 + 3 - myRole]);
            scoreLabel[myRole - 1].setText("Score: " + myScore);
            scoreLabel[3 - myRole - 1].setText("Score: " + opScore);
            isOver = Integer.parseInt(mess[29 * 30 + 29 + 3 + 3]);
            d.speed = Integer.parseInt(mess[29 * 30 + 29 + 6 + 1]);
            if(d.speed == 0){
                speedDownButton.setEnabled(false);
                speedUPButton.setEnabled(true);
            }
            else if(d.speed == 4){
                speedUPButton.setEnabled(false);
                speedDownButton.setEnabled(true);
            }
            else{
                speedDownButton.setEnabled(true);
                speedUPButton.setEnabled(true);
            }
            if(!mess[29 * 30 + 29 + 6 + 3].equals("")){
                txarChat.append("Player " + (3 - myRole) + ":" + mess[29 * 30 + 29 + 6 + 3] + "\n");
            }
            if(isOver > 0){
                d.setIsOver(isOver);
                pauseButton.setEnabled(false);
                speedUPButton.setEnabled(false);
                speedDownButton.setEnabled(false);
            }
        }
        catch (IOException e){
            System.out.println("Server break! Game Over!");
            isOver = 5;
            d.setIsOver(isOver);
            pauseButton.setEnabled(false);
            speedUPButton.setEnabled(false);
            speedDownButton.setEnabled(false);
        }
        finally {
            repaint();
        }
    }

    void getRank(){
        try {
            String plrSet = brFromServer.readLine();
            txarRank.setText("");
            System.out.println(plrSet);
            String[] plrSets = plrSet.split(",");
            for (int i = 0; i < plrSets.length; i++) {
                txarRank.append(plrSets[i] + "\n");
            }
        }
        catch (IOException e){
            System.out.println("Getting rank");
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
            System.out.println("Receiving countdown");
        }
        finally {
            mainWindow.addKeyListener(new MyKeyAdapter());
            mainWindow.requestFocus();
            thread.start();
        }
    }

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
        int[] xPoints = {5, 15, 15, 25, 25, 15, 15};
        int[] yPoints = {17, 10, 17, 10, 24, 17, 24};
        speedDownButton = new SpeedButton(xPoints, yPoints);
        speedDownButton.setSize(30, 30);
        speedDownButton.setFocusPainted(false);
        speedDownButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int temp = d.speed;
                d.setSpeed(temp - 1);
                if(d.speed == 0){
                    speedDownButton.setEnabled(false);
                }
                speedUPButton.setEnabled(true);
                mainWindow.requestFocus();
            }
        });
        int[] xPoints2 = {8, 18, 18, 28, 18, 18, 8};
        int[] yPoints2 = {10, 17, 10, 17, 24, 17, 24};
        speedUPButton = new SpeedButton(xPoints2, yPoints2);
        speedUPButton.setSize(30, 30);
        speedUPButton.setFocusPainted(false);
        speedUPButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int temp = d.speed;
                d.setSpeed(temp + 1);
                if(d.speed == 4){
                    speedUPButton.setEnabled(false);
                }
                speedDownButton.setEnabled(true);
                mainWindow.requestFocus();
            }
        });
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
                    speedUPButton.setEnabled(true);
                    speedDownButton.setEnabled(true);
                    mainWindow.requestFocus();
                }
                else{
                    isPause = myRole;
                    pauseButton.isPause = true;
                    d.setIsPause(isPause);
                    pauseButton.repaint();
                    System.out.println("I Paused");
                    speedUPButton.setEnabled(false);
                    speedDownButton.setEnabled(false);
                }
            }
        });
        btnPanel.add(speedDownButton);
        btnPanel.add(pauseButton);
        btnPanel.add(speedUPButton);
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
    }

    private void drawWall(Graphics g, int x, int y){
        g.setColor(new Color(170, 231, 44));
        int width = (int)((x + 1) * oneRecWidth) - (int)(x * oneRecWidth);
        int height = (int)((y + 1) * oneRecWidth) - (int)(y * oneRecWidth);
        g.fillRect((int)(x * oneRecWidth), (int)(y * oneRecHeight), width, height);
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
            if(isPause == 0 && d.direction >= 0) {
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
    String strToSend;
    int speed;
    static String[] Tran = {"UP","RIGHT", "DOWN", "LEFT"};
    Direction(int direction, int isPause, int isOver, int speed){
        this.direction = direction;
        this.isPause = isPause;
        this.isOver = isOver;
        strToSend = "";
        this.speed = speed;
    }
    synchronized void sendDirection(DataOutputStream out){
        try{
            String toSend = direction + "," + isPause + "," + speed + "," + strToSend + "\n";
            byte[] bytes = toSend.getBytes();
            out.write(bytes);
            strToSend = "";
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

    synchronized  void setStrToSend(String toSend){
        this.strToSend = toSend;
        notify();
    }

    synchronized  void setSpeed(int speed){
        this.speed = speed;
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
        ImageIcon icon = new ImageIcon(BeginPanel.class.getResource("/resource/begin.JPG"));
        g.drawImage(icon.getImage(), 0, 0, this.getWidth(), this.getHeight(), this);
    }
}

class RCVThread extends Thread{
    private MainWindow mainWindow;
    RCVThread(MainWindow mainWindow){
        this.mainWindow = mainWindow;
    }
    @Override
    public void run() {
        super.run();
        mainWindow.getRole();
        mainWindow.getMessage();
        mainWindow.getCountDown();
        while(mainWindow.isOver == 0){
            mainWindow.getMessage();
        }
        mainWindow.getRank();
    }
}