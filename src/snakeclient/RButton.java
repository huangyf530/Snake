package snakeclient;

import java.awt.*;
import java.awt.geom.*;
import java.net.URL;
import javax.swing.*;
class RButton extends JButton {
    public RButton() {
        super();
        // 这些声明把按钮扩展为一个圆而不是一个椭圆。
        Dimension size = getPreferredSize();
        size.width = size.height = Math.max(size.width,
                size.height);
        setPreferredSize(size);
        //这个调用使JButton不画背景，而允许我们画一个圆的背景。
        setContentAreaFilled(false);
        this.setBackground(Color.GRAY);
    }
    // 画圆的背景和标签
    protected void paintComponent(Graphics g) {
        if (getModel().isArmed()) {
            // 你可以选一个高亮的颜色作为圆形按钮类的属性
            g.setColor(Color.lightGray);
        }
        else {
            g.setColor(getBackground());
        }
        g.fillOval(0, 0, getSize().width - 1,
                getSize().height - 1);
        //这个调用会画一个标签和焦点矩形。
        super.paintComponent(g);
    }
    // 用简单的弧画按钮的边界。
    protected void paintBorder(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        g2d.setStroke(new BasicStroke(2f));
        g.setColor(Color.BLACK);
        g.drawOval(1, 1, getSize().width - 2,
                getSize().height - 2);
    }
    // 侦测点击事件
    Shape shape;
    public boolean contains(int x, int y) {
        // 如果按钮改变大小，产生一个新的形状对象。
        if (shape == null ||
                !shape.getBounds().equals(getBounds())) {
            shape = new Ellipse2D.Float(0, 0,
                    getWidth(), getHeight());
        }
        return shape.contains(x, y);
    }
}

class PauseButton extends RButton{
    boolean isPause;
    PauseButton(){
        super();
        isPause = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawPause(g);
    }

    private void drawPause(Graphics g){
        g.setColor(Color.BLACK);
        if(!isPause) {
            g.fillRect(10, 10, 3, 15);
            g.fillRect(20, 10, 3, 15);
        }
        else{
            int[] xPoints = new int[3];
            int[] yPoints = new int[3];
            xPoints[0] = 12;
            yPoints[0] = 8;
            xPoints[1] = 12;
            yPoints[1] = 26;
            xPoints[2] = 25;
            yPoints[2] = 17;
            g.fillPolygon(xPoints, yPoints, 3);
        }
    }
}

class MusicButton extends RButton{
    boolean isMute;
    static private URL musicURL = MusicButton.class.getResource("/resource/MusicButton.png");
    static private URL muteURL = MusicButton.class.getResource("/resource/MuteMusic.png");
    MusicButton(){
        super();
        isMute = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.black);
        if(isMute){
            ImageIcon icon = new ImageIcon(muteURL);
            g.drawImage(icon.getImage(), 0, 0, this.getWidth(), this.getHeight(), this);
        }
        else{
            ImageIcon icon = new ImageIcon(musicURL);
            g.drawImage(icon.getImage(), 0, 0, this.getWidth(), this.getHeight(), this);
        }
    }
}

class SpeedButton extends RButton{
    int[] xPoints;
    int[] yPoints;
    SpeedButton(int[] x, int[] y){
        super();
        xPoints = x;
        yPoints = y;
    }
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.black);
        g.fillPolygon(xPoints, yPoints, 7);
    }
}