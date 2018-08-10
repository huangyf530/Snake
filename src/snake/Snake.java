package snake;

import java.util.Vector;

public class Snake {
    boolean dead;
    Hole inHole;
    private Hole outHole;
    int waitTime;
    boolean headInHole;
    boolean allInHole;
    int direction; //up: 0, right: 1, down: 2, left: 3
    private int snakeLength;
    private int headLength;//前半段进洞的长度
    Vector<Location> bodyLocation;
    private int tailLength;//后半段洞内的长度
    private Vector<Location> foodLocation;
    private int[][] situation;
    private int snakeNumber;
    Snake(int [][] situation, int snakeNumber){
        dead = false;
        snakeLength = 3;
        headInHole = true;
        waitTime = 0;
        allInHole = true;
        headLength = 0;
        tailLength = snakeLength;
        bodyLocation = new Vector<>();
        foodLocation = new Vector<>();
        this.situation = situation;
        this.snakeNumber = snakeNumber;
    }

    void goDie(){
        dead = true;
        if(inHole != null) {
            inHole.valid = true;
        }
        if(outHole != null) {
            outHole.valid = true;
        }
        for(int i = 0; i < bodyLocation.size(); i++){
            situation[bodyLocation.elementAt(i).Y][bodyLocation.elementAt(i).X] = 0;
        }
    }

    void setInHole(Hole hole) {
        System.out.printf("Set in hole (%d, %d)\n",hole.X, hole.Y);
        this.inHole = hole;
    }

    int eatSomething(int addLength, Location location){
        snakeLength += addLength;
        foodLocation.add(location);
        return snakeLength;
    }

    void getRandomDirection(){
        direction = (int)(Math.random() * 4);
    }

    Location getNextPosition(int X, int Y, int direction){
        int x, y;
        if(direction == 0){
            x = X;
            y = (Y - 1 + 30) % 30;
        }
        else if(direction == 1){
            x =  (X + 1) % 30;
            y = Y;
        }
        else if(direction == 2){
            x = X;
            y = (Y + 1) % 30;
        }
        else{
            x =  (X - 1 + 30) % 30;
            y = Y;
        }
        return new Location(x, y);
    }

    public void getOut(Hole hole){
        System.out.printf("Get out from hole (%d, %d)\n", hole.X, hole.Y);
        this.outHole = hole;
        outHole.valid = false;
        Location nextLocation = getNextPosition(hole.X, hole.Y, direction);
        bodyLocation.add(0, nextLocation);
        if(situation[nextLocation.Y][nextLocation.X] == 3){
            eatSomething(1, nextLocation);
        }
        situation[nextLocation.Y][nextLocation.X] = snakeNumber + 4;
        allInHole = false;
        tailLength--;
        if(headLength == 0){
            headInHole = false;
        }
    }

    void rushHole(Hole hole, int waitTime){
        System.out.printf("Rush hole (%d, %d)\n", hole.X, hole.Y);
        this.inHole = hole;
        hole.valid = false;
        if(!headInHole){
            headLength++;
            headInHole = true;
        }
        if(tailLength != 0){
            tailLength--;
            if(tailLength == 0){
                this.outHole.valid = true;
            }
        }
        else {
            growBody();
            headLength++;
            if (bodyLocation.size() == 0) {
                allInHole = true;
                this.waitTime = waitTime;
                headLength = 0;
                tailLength = snakeLength;
                this.inHole.valid = true;
            }
        }
    }

    public void moveFarward(Location nextPosition, boolean hasFoot){
        System.out.printf("Move to next location (%d, %d)\n", nextPosition.X, nextPosition.Y);
        bodyLocation.add(0, nextPosition);
        situation[nextPosition.Y][nextPosition.X] = snakeNumber + 4;
        if(hasFoot){
            eatSomething(1, nextPosition);
        }
        if(tailLength != 0){
            tailLength--;
            if(tailLength == 0){
                this.outHole.valid = true;
            }
        }
        else{
            growBody();
        }
    }

    public void growBody(){
        if(!foodLocation.isEmpty()) {
            if (bodyLocation.elementAt(bodyLocation.size() - 1).equals(foodLocation.elementAt(0))){
                foodLocation.remove(0);
                return;
            }
        }
        situation[bodyLocation.elementAt(bodyLocation.size() - 1).Y]
                [bodyLocation.elementAt(bodyLocation.size() - 1).X] = 0;
        bodyLocation.remove(bodyLocation.size() - 1);
    }
}

class Location{
    public int X;
    public int Y;
    public Location(int X, int Y){
        this.X = X;
        this.Y = Y;
    }

    public boolean equals(Location a){
        if ((this.X == a.X) &&(this.Y == a.Y)){
            return true;
        }
        else{
            return false;
        }
    }
}
