package snake;

public class Egg {
    int X;
    int Y;
    boolean valid;
    public Egg(){
        X  = 0;
        Y = 0;
        valid = false;
    }

    public Egg(int x, int y){
        X = x;
        Y = y;
        valid = true;
    }

    public void setX(int x) {
        X = x;
    }

    public void setY(int y) {
        Y = y;
    }
}
