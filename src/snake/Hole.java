package snake;

public class Hole {
    public static int radius = 10;
    public int X;
    public int Y;
    public boolean valid;
    public Hole(int X, int Y){
        this.X = X;
        this.Y = Y;
        valid = true;
    }
}
