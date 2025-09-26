package zombi_shooter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapGenerator {
    private static final Color GRASS_COLOR = new Color(34, 139, 34);
    private static final Color DARK_GRASS_COLOR = new Color(25, 100, 25);
    private static final Color ROAD_COLOR = new Color(64, 64, 64);
    private static final Color DIRT_COLOR = new Color(139, 116, 80);
    private static final Color TREE_COLOR = new Color(0, 100, 0);
    private static final Color ROCK_COLOR = new Color(105, 105, 105);
    
    private BufferedImage mapTexture;
    private int mapWidth;
    private int mapHeight;
    private Random random;
    private List<Rectangle> roads;
    private List<Point> trees;
    private List<Point> rocks;
    
    public MapGenerator(int width, int height) {
        this.mapWidth = width;
        this.mapHeight = height;
        this.random = new Random();
        this.roads = new ArrayList<>();
        this.trees = new ArrayList<>();
        this.rocks = new ArrayList<>();
        generateMap();
    }
    
    private void generateMap() {
        mapTexture = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = mapTexture.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 1. Основной фон - трава
        drawGrassBackground(g2d);
        
        // 2. Добавляем вариации травы
        addGrassVariations(g2d);
        
        // 3. Рисуем дороги
        generateRoads(g2d);
        
        // 4. Добавляем деревья
        generateTrees(g2d);
        
        // 5. Добавляем камни и детали
        generateRocks(g2d);
        
        // 6. Добавляем текстуру
        addTexture(g2d);
        
        g2d.dispose();
    }
    
    private void drawGrassBackground(Graphics2D g2d) {
        // Создаем градиент травы
        GradientPaint grassGradient = new GradientPaint(
            0, 0, GRASS_COLOR,
            mapWidth, mapHeight, DARK_GRASS_COLOR
        );
        g2d.setPaint(grassGradient);
        g2d.fillRect(0, 0, mapWidth, mapHeight);
    }
    
    private void addGrassVariations(Graphics2D g2d) {
        // Добавляем случайные пятна более темной и светлой травы
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(mapWidth);
            int y = random.nextInt(mapHeight);
            int size = 20 + random.nextInt(80);
            
            Color grassVariation;
            if (random.nextBoolean()) {
                grassVariation = new Color(40, 160, 40, 60);
            } else {
                grassVariation = new Color(20, 80, 20, 60);
            }
            
            g2d.setColor(grassVariation);
            g2d.fillOval(x - size/2, y - size/2, size, size);
        }
    }
    
    private void generateRoads(Graphics2D g2d) {
        g2d.setColor(ROAD_COLOR);
        g2d.setStroke(new BasicStroke(40, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Главная горизонтальная дорога
        int roadY = mapHeight / 2;
        g2d.drawLine(0, roadY, mapWidth, roadY);
        roads.add(new Rectangle(0, roadY - 20, mapWidth, 40));
        
        // Главная вертикальная дорога
        int roadX = mapWidth / 2;
        g2d.drawLine(roadX, 0, roadX, mapHeight);
        roads.add(new Rectangle(roadX - 20, 0, 40, mapHeight));
        
        // Добавляем несколько второстепенных дорог
        for (int i = 0; i < 3; i++) {
            int x1 = random.nextInt(mapWidth);
            int y1 = random.nextInt(mapHeight);
            int x2 = random.nextInt(mapWidth);
            int y2 = random.nextInt(mapHeight);
            
            g2d.setStroke(new BasicStroke(25, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // Добавляем разметку дорог
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                                     10.0f, new float[]{20.0f, 20.0f}, 0.0f));
        g2d.drawLine(0, roadY, mapWidth, roadY);
        g2d.drawLine(roadX, 0, roadX, mapHeight);
    }
    
    private void generateTrees(Graphics2D g2d) {
        for (int i = 0; i < 80; i++) {
            int x = random.nextInt(mapWidth);
            int y = random.nextInt(mapHeight);
            
            // Не размещаем деревья на дорогах
            if (isOnRoad(x, y)) continue;
            
            drawTree(g2d, x, y);
            trees.add(new Point(x, y));
        }
    }
    
    private void drawTree(Graphics2D g2d, int x, int y) {
        // Ствол дерева
        g2d.setColor(new Color(101, 67, 33));
        g2d.fillRect(x - 3, y - 5, 6, 15);
        
        // Крона дерева
        g2d.setColor(TREE_COLOR);
        g2d.fillOval(x - 12, y - 20, 24, 24);
        
        // Добавляем тень
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillOval(x - 8, y + 8, 16, 8);
        
        // Более светлая часть кроны для объема
        g2d.setColor(new Color(50, 150, 50));
        g2d.fillOval(x - 8, y - 16, 16, 16);
    }
    
    private void generateRocks(Graphics2D g2d) {
        for (int i = 0; i < 30; i++) {
            int x = random.nextInt(mapWidth);
            int y = random.nextInt(mapHeight);
            
            // Не размещаем камни на дорогах
            if (isOnRoad(x, y)) continue;
            
            drawRock(g2d, x, y);
            rocks.add(new Point(x, y));
        }
    }
    
    private void drawRock(Graphics2D g2d, int x, int y) {
        int size = 5 + random.nextInt(15);
        
        // Основной камень
        g2d.setColor(ROCK_COLOR);
        g2d.fillOval(x - size/2, y - size/2, size, size);
        
        // Светлая часть для объема
        g2d.setColor(new Color(150, 150, 150));
        g2d.fillOval(x - size/3, y - size/3, size/2, size/2);
        
        // Тень
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(x - size/4, y + size/3, size/2, size/4);
    }
    
    private void addTexture(Graphics2D g2d) {
        for (int i = 0; i < mapWidth * mapHeight / 200; i++) {
            int x = random.nextInt(mapWidth);
            int y = random.nextInt(mapHeight);
            
            if (isOnRoad(x, y)) continue;
            
            int red = Math.max(0, Math.min(255, GRASS_COLOR.getRed() + random.nextInt(40) - 20));
            int green = Math.max(0, Math.min(255, GRASS_COLOR.getGreen() + random.nextInt(40) - 20));
            int blue = Math.max(0, Math.min(255, GRASS_COLOR.getBlue() + random.nextInt(20) - 10));
            
            Color textureColor = new Color(red, green, blue, 100);
            
            g2d.setColor(textureColor);
            g2d.fillRect(x, y, 1, 1);
        }
    }
    
    private boolean isOnRoad(int x, int y) {
        for (Rectangle road : roads) {
            if (road.contains(x, y)) {
                return true;
            }
        }
        return false;
    }
    
    public void draw(Graphics2D g2d, int offsetX, int offsetY, int viewWidth, int viewHeight) {
        int startX = Math.max(0, offsetX);
        int startY = Math.max(0, offsetY);
        int endX = Math.min(mapWidth, offsetX + viewWidth);
        int endY = Math.min(mapHeight, offsetY + viewHeight);
        
        if (startX < endX && startY < endY) {
            BufferedImage subImage = mapTexture.getSubimage(
                startX, startY, endX - startX, endY - startY
            );
            g2d.drawImage(subImage, startX - offsetX, startY - offsetY, null);
        }
    }
    
    public BufferedImage getMapTexture() {
        return mapTexture;
    }
    
    public List<Point> getTrees() {
        return trees;
    }
    
    public List<Point> getRocks() {
        return rocks;
    }
    
    public List<Rectangle> getRoads() {
        return roads;
    }
}