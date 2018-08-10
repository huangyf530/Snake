package snake;

public class Wall {
    private int firstX;
    private int firstY;
    public static final int HEIGHT = 20;
    int width;
    int direction; //0表示横向，1表示竖向
    Wall(int firstX, int firstY, int direction, int width){
        this.firstX = firstX;
        this.firstY = firstY;
        this.direction = direction;
        this.width = width;
    }

    int getFirstX() {
        return firstX;
    }

    int getFirstY() {
        return firstY;
    }
}
