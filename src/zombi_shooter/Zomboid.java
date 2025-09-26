package zombi_shooter;

import java.awt.*;
public class Zomboid {
    private Color COLOR_OF_ZOMBOID = new Color(255, 0, 0);
    private int x;
    private int y;
    private int size;
    private double speed;

    public Zomboid(int x, int y) {
        this.x = x;
        this.y = y;
        this.size = 15 + (int) (Math.random() * 16);
        this.speed = 2.0 + (double) (Math.random()) * 3;
    }

    public Zomboid(Color COLOR_OF_ZOMBOID, int x, int y) {
        this.COLOR_OF_ZOMBOID = COLOR_OF_ZOMBOID;
        this.x = x;
        this.y = y;
        this.size = 15 + (int) (Math.random() * 16);
        this.speed = 2.0 + (double) (Math.random()) * 3;
    }

    public void update(int targetX, int targetY) {
        double deltaX = targetX - x;
        double deltaY = targetY - y;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (distance > 0) {
            double moveX = (deltaX / distance) * speed;
            double moveY = (deltaY / distance) * speed;
            x += moveX;
            y += moveY;
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(COLOR_OF_ZOMBOID);
        g2d.fillOval(x - size / 2, y - size / 2, size, size);

        // Добавляем глаза
        g2d.setColor(Color.BLACK);
        g2d.fillOval((int) (x - size / 4), (int) (y - size / 4), 3, 3);
        g2d.fillOval((int) (x + size / 6), (int) (y - size / 4), 3, 3);

        // Обводка
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawOval(x - size / 2, y - size / 2, size, size);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }
}
