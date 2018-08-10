package snake;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Vector;

public class Server {
    private ServerSocket serverSocket;
    private Vector<Socket> clients;
    private Vector<BufferedReader> brInFromClient;
    private Vector<DataOutputStream> dosOutToClient;
    private int [][] situation; //0表示没有东西，1表示墙，2表示洞，3表示蛋，4表示蛇；
    private int wallNumber;
    private Wall[] wall;
    private int level;
    private int holeNumber;
    private Hole[] holes;
    private Egg[] egg;
    private int foodnum;
    private int foodWait;
    private int[] reBornTime;
    private Snake[] snakes;
    private int[] snakeLeft;
    private int delay;
    private int[] score;
    private Timer timer;
    private rcvThread[] rcvthread;
    private final GameInfo gameInfo = new GameInfo();
    private final Time time = new Time();
    private static int[] speed = {500, 200, 100};

    public static void main(String[] args){
        System.out.println("Please input the listening port");
        Scanner console = new Scanner(System.in);
        int port = console.nextInt();
        while(port < 1024){
            System.out.println("Invalid port. PLease input again");
            port = console.nextInt();
        }
        Server server = new Server(port);
        server.listenRequest(port);
        server.delay = 200;
        server.getInitialScene();
        server.sendInitialSituation();
        server.countDown(5);
        server.beginGame();
        server.beginThread();
        try{
            server.rcvthread[0].join();
            server.rcvthread[1].join();
        }
        catch (InterruptedException e){
            System.out.println("Thread join");
        }
        server.printWinner();
    }

    private void printWinner(){
        if(gameInfo.isOver % 2 == 0) {
            System.out.println("Game over, Player 1 win!");
        }
        else{
            System.out.println("Game over, Player 2 win!");
        }
    }

    private void sendInitialSituation(){
        boolean t = false;
        if(gameInfo.isPause > 0){
            t = true;
        }
        String toSend = "";
        //printSituation();
        for(int i = 0; i < 30; i++){
            for(int j = 0; j < 30; j++){
                toSend = toSend + Integer.toString(situation[i][j]) + ",";
            }
        }
        String toSendi = toSend + gameInfo.isPause + "," + snakeLeft[0] + "," + snakeLeft[1] + "," + score[0] + "," + score[1]
                + ","+ gameInfo.isOver + "\n";
        byte[] send = toSendi.getBytes();
        //System.out.print(toSendi);
        //System.out.printf("Time Construct Message: %d\n", time.getTime());
        try {
            dosOutToClient.elementAt(0).write(send);
        }
        catch (IOException e){
            System.out.println("I am sending message to player 1! But player 1 is offline!");
        }
        try {
            dosOutToClient.elementAt(1).write(send);
        }
        catch (IOException e){
            System.out.println("I am sending message to player 2! But player 2 is offline!");
        }
        finally {
            //System.out.printf("Time: %d\n", time.getTime());
            if(t){
                timer.stop();
                time.Pause();
            }
            if(gameInfo.isOver > 0){
                timer.stop();
                time.Pause();
                try{
                    brInFromClient.elementAt(0).close();
                    brInFromClient.elementAt(1).close();
                }
                catch (IOException e){
                    System.out.println(e.getMessage());
                }
                rcvthread[0].interrupt();
                rcvthread[1].interrupt();
                System.out.println("Game Over!");
            }
        }
    }

//    private void sendSituation(){
//        try {
//            for(int i = 0; i < 2; i++) {
//                int len = snakes[i].bodyLocation.size();
//                dosOutToClient.elementAt(i).write(len);
//                for (int j = 0; j < len; j++) {
//                    int x = snakes[i].bodyLocation.elementAt(j).X;
//                    int y = snakes[i].bodyLocation.elementAt(j).Y;
//                    dosOutToClient.elementAt(i).write(x);
//                    dosOutToClient.elementAt(i).write(y);
//                }
//                dosOutToClient.elementAt(i).write();
//            }
//        }
//        catch (IOException e){
//            System.out.println("Sending snakes");
//        }
//    }

    private Server(int port){
        try{
            serverSocket = new ServerSocket(port);
            clients = new Vector<>();
            brInFromClient = new Vector<>();
            dosOutToClient = new Vector<>();
            situation = new int[30][30];
            for(int i = 0; i < 30; i++){
                for(int j = 0; j < 30; j++){
                    situation[i][j] = 0;
                }
            }
            reBornTime = new int[2];
            snakes = new Snake[2];
            snakeLeft = new int[2];
            score = new int[2];
            score[0] = score[1] = 0;
            gameInfo.isOver = 0;
            gameInfo.isPause = 0;
        }
        catch (IOException err){
            err.printStackTrace();
        }
    }

    private void listenRequest(int port){
        try{
            InetAddress inetAddress= InetAddress.getLocalHost();
            String ip = inetAddress.getHostAddress();
            System.out.printf("IP地址： %s\n监听端口：%d\n", ip, port);
            int i = 1;
            while (clients.size() < 2){
                System.out.println("Listening...");
                Socket newSocket = serverSocket.accept();
                clients.add(newSocket);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
                brInFromClient.add(bufferedReader);
                DataOutputStream dataOutputStream = new DataOutputStream(newSocket.getOutputStream());
                dosOutToClient.add(dataOutputStream);
                System.out.println("A new client add to the game...");
                dosOutToClient.elementAt(i - 1).write(i);
                i++;
            }
            System.out.println("Be Ready! Game is about to begin!");
        }
        catch (UnknownHostException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private void countDown(int k){
        try {
            for (int i = k; i >= 0; i--) {
                dosOutToClient.elementAt(0).write(i);
                dosOutToClient.elementAt(1).write(i);
                Thread.sleep(1200);
            }
        }
        catch (IOException e){
            e.printStackTrace();
            System.out.println("Sending countdown message");
        }
        catch (InterruptedException e){
            e.printStackTrace();
            System.out.println("InterruptedException while sending countdown");
        }
    }

    private void beginGame(){
        time.t = System.currentTimeMillis();
        timer.start();
        printSituation();
    }

    private void beginThread(){
        rcvthread = new rcvThread[2];
        rcvthread[0] = new rcvThread(0);
        rcvthread[1] = new rcvThread(1);
        rcvthread[0].start();
        rcvthread[1].start();
    }

    private void getDirection(int i){
        try{
            String rcv = brInFromClient.elementAt(i).readLine();
            System.out.println(rcv);
            String[] mess = rcv.split(",");
            synchronized (gameInfo) {
                int direction = Integer.parseInt(mess[0]);
                if (direction >= 0 && direction < 4 && !snakes[i].headInHole) {
                    snakes[i].direction = direction;
                }
                int temp = Integer.parseInt(mess[1]);
                if (temp > 0 && gameInfo.isPause == 0) {
                    gameInfo.isPause = temp;
                } else if (gameInfo.isPause > 0 && temp == 0) {
                    gameInfo.isPause = temp;
                    timer.start();
                    time.Begin();
                }
                System.out.printf("Snake[%d].direction = %d\n", i, snakes[i].direction);
            }
        }
        catch (IOException e){
            System.out.printf("Player %d is offline!\n", i + 1);
            gameInfo.isOver = i + 3;
        }
    }

    private void setWallNumber(){
        while (wallNumber < 3){
            wallNumber = (int)(Math.random() * 7) + 1;
        }
        System.out.printf("Wall number: %d\n", wallNumber);
    }

    private Wall getRandWall(){
        int direction = (int)(Math.random() * 2);
        int x = (int)(Math.random() * 30);
        int y = (int)(Math.random() * 30);
        if(direction == 0){
            int leftDistance = 30 - x;
            int width = (int)(Math.random() * leftDistance);
            for(int i = 0; i < width; i++){
                situation[y][x + i] = 1;
            }
            return new Wall(x, y, direction, width);
        }
        else{
            int leftDistance = 30 - y;
            int width = (int)(Math.random() * leftDistance);
            for(int i = 0; i < width; i++){
                situation[y + i][x] = 1;
            }
            return new Wall(x, y, direction, width);
        }
    }//生成随机的墙

    private void getHoles(){
        for(int i = 0; i < holeNumber; i++){
            int x = (int)(Math.random() * 30);
            int y = (int)(Math.random() * 30);
            while(!(checkHoleLocation(x, y) && (checkAmongHole(x, y, i)))){
                x = (int)(Math.random() * 30);
                y = (int)(Math.random() * 30);
            }
            holes[i] = new Hole(x, y);
            situation[y][x] = 2;
            System.out.printf("hole%d: (%d, %d)\n", i, x, y);
        }
    }

    private boolean checkHoleLocation(int X, int Y){
        for(int i = 0; i < wallNumber; i++){
            if(checkInTheWall(X, Y, wall[i])){
                return false;
            }
        }
        return true;
    }//判断产生的蛇洞是否与墙壁相交

    private boolean checkInTheWall(int X, int Y, Wall w){
        if(w.direction == 0){
            if((Y  < w.getFirstY() - 1) || (Y > w.getFirstY() + 1))
                return false;
            else{
                if((X  < w.getFirstX() - 1) || (X > w.getFirstX() + w.width)){
                    return false;
                }
                else {
                    return true;
                }
            }
        }
        else{
            if((X < w.getFirstX() - 1) || (X > w.getFirstX() + 1)){
                return false;
            }
            else if((Y < w.getFirstY() - 1) || (Y > w.getFirstY() + w.width)){
                return false;
            }
            else {
                return true;
            }
        }
    }

    private boolean checkAmongHole(int X, int Y, int count){
        for(int i = 0; i < count; i++){
            int distance = Math.abs(X - holes[i].X) + Math.abs(Y - holes[i].Y);
            if(distance <= 1){
                return false;
            }
        }
        return true;
    }//一个蛇洞与另一个蛇洞间隔大于1

    private void getNewEgg(){
        if(foodWait == 0) {
            for (int i = 0; i < 2; i++) {
                int x = (int) (Math.random() * 30);
                int y = (int) (Math.random() * 30);
                while (situation[y][x] != 0) {
                    x = (int) (Math.random() * 30);
                    y = (int) (Math.random() * 30);
                }
                egg[i].setX(x);
                egg[i].setY(y);
                egg[i].valid = true;
                situation[y][x] = 3;
                foodnum = 2;
                System.out.printf("Egg %d: (%d, %d)\n", i, y, x);
            }
        }
        else{
            System.out.printf("Egg wait: %d\n", foodWait);
            foodWait--;
        }
    }

    private void getInitialScene(){
        this.wallNumber = 0;
        this.setWallNumber();
        this.wall = new Wall[this.wallNumber];
        for(int i = 0; i < this.wallNumber; i++){
            wall[i] = this.getRandWall();
        }
        holeNumber = 0;
        while(holeNumber < 3){
            holeNumber = (int)(Math.random() * 5) + 1;
        }
        System.out.printf("Hole number: %d\n", holeNumber);
        holes = new Hole[holeNumber];
        getHoles();
        foodnum = 2;
        foodWait = 0;
        egg = new Egg[2];
        egg[0] = new Egg();
        egg[1] = new Egg();
        getNewEgg();
        snakeLeft[0] = 5;
        snakeLeft[1] = 5;
        snakes[0] = new Snake(situation, 0);
        snakes[1] = new Snake(situation, 1);
        timer = new Timer(delay, new MoveAdapter());
        printSituation();
    }

    private Hole getOutHole(Snake one){
        int i = (int)(Math.random() * holeNumber);
        while(!holes[i].valid || holes[i] == one.inHole){
            i = (int)(Math.random() * holeNumber);
        }
        return holes[i];
    }

    private void checkFood(int x, int y, int i){
        if(situation[y][x] == 3){
            score[i]++;
            foodnum--;
            getEgg(x, y).valid = false;
            if(foodnum == 0){
                foodWait = 2000 / delay;
            }
        }
    }

    private Egg getEgg(int x, int y){
        for(int i = 0; i < 2; i++){
            if((egg[i].X == x) && (egg[i].Y == y)){
                return egg[i];
            }
        }
        return null;
    }

    private void moveSnake(int i){
        if(snakes[i].dead){
            reBornTime[i]--;
            if(reBornTime[i] == 0){
                snakes[i] = new Snake(situation, i);
                snakes[i].setInHole(getOutHole(snakes[i]));
            }
            else{
                return;
            }
        }
        if(snakes[i].waitTime == 0){
            if(snakes[i].allInHole){
                snakes[i].getRandomDirection();
                Hole temp = getOutHole(snakes[i]);
                Location nextLocation = snakes[i].getNextPosition(temp.X, temp.Y, snakes[i].direction);
                checkFood(nextLocation.X, nextLocation.Y, i);
                snakes[i].getOut(temp);
            }
            else {
                Location nextLocation = snakes[i].getNextPosition(snakes[i].bodyLocation.elementAt(0).X,
                        snakes[i].bodyLocation.elementAt(0).Y, snakes[i].direction);
                if (situation[nextLocation.Y][nextLocation.X] == 2) {
                    snakes[i].rushHole(judgeHole(nextLocation.X, nextLocation.Y), 1000 / delay);
                }
                else if (situation[nextLocation.Y][nextLocation.X] == 0 || situation[nextLocation.Y][nextLocation.X] == 3) {
                    checkFood(nextLocation.X, nextLocation.Y, i);
                    snakes[i].moveFarward(nextLocation, situation[nextLocation.Y][nextLocation.X] == 3);
                }
                else if(situation[nextLocation.Y][nextLocation.X] == 1 || situation[nextLocation.Y][nextLocation.X] == 4 ||
                        situation[nextLocation.Y][nextLocation.X] == 5){
                    //printSituation();
                    snakes[i].goDie();
                    reBornTime[i] = 2000 / delay;
                    snakeLeft[i]--;
                    if(snakeLeft[i] == 0){
                        gameInfo.isOver = i + 1;
                    }
                }
            }
        }
        else {
            System.out.printf("Wait: %d\n", snakes[i].waitTime);
            snakes[i].waitTime--;
        }
        if(foodnum == 0){
            getNewEgg();
        }
    }

    private Hole judgeHole(int x, int y){
        for(int i = 0; i < holeNumber; i++){
            if((holes[i].X == x) && (holes[i].Y == y)){
                return holes[i];
            }
        }
        return null;
    }

    private void printSituation(){
        System.out.println("Current Situation: ");
        for(int i = 0; i < 30; i++){
            for(int j = 0; j < 30; j++){
                System.out.printf("%d ", situation[i][j]);
            }
            System.out.println();
        }
    }

    private class MoveAdapter implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            synchronized (gameInfo) {
                System.out.printf("%d Next move 4: ", time.getTime());
                moveSnake(0);
                System.out.printf("%d Next move 5: ", time.getTime());
                moveSnake(1);
                System.out.printf("%d \n", time.getTime());
                sendInitialSituation();
                /*send message*/
            }
        }
    }

    private class rcvThread extends Thread{
        int i;
        rcvThread(int i){
            this.i = i;
        }
        @Override
        public void run() {
            super.run();
            while(gameInfo.isOver == 0){
                getDirection(i);
            }
        }
    }
}

class GameInfo{
    int isOver; //1: player 1 lose, 2: player 2 lose, 3: player 1 offline, 4: player 2 offline
    int isPause;
    GameInfo(){
        isOver = 0;
        isPause = 0;
    }
}