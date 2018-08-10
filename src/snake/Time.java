package snake;

public class Time {
    long addTime;
    long t;
    Time(){
        addTime = 0;
        t = System.currentTimeMillis();
    }
    long getTime(){
        return System.currentTimeMillis() - t + addTime;
    }
    void Pause(){
        addTime = getTime();
    }
    void Begin(){
        t = System.currentTimeMillis();
    }
}
