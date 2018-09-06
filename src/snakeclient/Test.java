package snakeclient;

public class Test {
    public static void main(String[] args){
        MainWindow window = new MainWindow();
        try {
            window.rcvThread.join();
        }
        catch (InterruptedException e){
            System.out.println("A receive thread");
        }
    }
}
