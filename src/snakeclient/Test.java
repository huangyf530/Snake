package snakeclient;

public class Test {
    public static void main(String[] args){
//        try {
//            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//        MainWindow window = new MainWindow();
//        window.connectToServer("192.168.244.1", 6888);
//        window.getRole();
//        window.getMessage();
//        window.getCountDown();
//        while(window.isOver == 0){
//            window.getMessage();
//        }
        MainWindow window = new MainWindow();
        try {
            window.rcvThread.join();
        }
        catch (InterruptedException e){
            System.out.println("A receive thread");
        }
    }
}
