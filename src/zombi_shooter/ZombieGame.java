package zombi_shooter;

import zombi_shooter.player.*;
import zombi_shooter.player.abilyti.AbilityManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;

import static java.awt.event.KeyEvent.*;

public class ZombieGame extends JPanel implements KeyListener, ActionListener, MouseMotionListener, MouseListener {

    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int MAP_WIDTH = 2000;
    private static final int MAP_HEIGHT = 1500;
    private static final Color BACKGROUND = new Color(43, 124, 0);
    private final int ZOMBI_SPAWN_DELAY = 5000;
    private final int CHEST_SPAWN_DELAY = 10000;
    private final int HORDE_SPAWN_DELAY = 6000;
    private final int MIN_HORDE_ZOMB = 15;
    private final int MAX_HORDE_ZOMB = 30;
    private final int WARNING_DURACION = 3000;
    // НОВЫЕ НАСТРОЙКИ УРОВНЕЙ (легко конфигурируемые)
    private final int ZOMBIES_TO_KILL_FOR_BOSS = 50; // Сколько зомби нужно убить для появления босса
    private final int BOSS_HEALTH = 100; // Здоровье босса
    private final int BOSS_SIZE = 60; // Размер босса
    private final int BOSS_SPEED = 2; // Скорость босса
    private final int BOSS_DAMAGE = 5; // Урон босса игроку
    private final int BOSS_REWARD = 500; // Награда за убийство босса
    private final int SHOOT_DELAY = 150; // миллисекунд между выстрелами
    private final int LEVEL_COMPLETE_DISPLAY_TIME = 3000; // 3 секунды показа сообщения
    private final int ACHIVEMENT_DISPLAY_DURACION = 3000;
    private boolean hordWarning = false;
    private long hordWarningTime = 0;
    private int HORDE_DURACION = 0;
    private String[] duracionName = { "", "сверху", "справа", "снизу", "слева" };
    private String[] arrowsDuracion = { "", "⬆️", "➡️", "⬇️", "⬅️" };
    private Player player;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private Timer timer;
    private int mouseX = 0;
    private int mouseY = 0;
    private java.util.List<Bullet> listOfBullet;
    private java.util.List<Zomboid> listOfZomboits;
    private java.util.List<Chest> listOfChests;
    private long lastZombieSpawn = 0;
    private long lastChestSpawn = 0;
    private long lastHordeSpawn = System.currentTimeMillis() + HORDE_SPAWN_DELAY;
    private Camera camera;
    private MapGenerator mapGenerator;
    private int score = 0;
    private boolean gameRunning = false;
    private boolean mouseHeld = false;
    private long lastShotTime = 0;
    // Новые переменные для системы лута
    private Weapon currentWeapon;
    private int totalAmmo = 100;
    // НОВЫЕ ПЕРЕМЕННЫЕ ДЛЯ СИСТЕМЫ УРОВНЕЙ
    private int currentLevel = 1;
    private int zombiesKilled = 0;
    private boolean bossActive = false;
    private BossZombie boss = null;
    private boolean levelCompleted = false;
    private long levelCompletedTime = 0;
    private java.util.List<Achievement> achievementList;
    private java.util.List<String> unlockedAchievement;
    private String currentAchievementMsg = "";
    private long achievementDisplayTime = 0;
    private int amountOfOpenSunduk = 0;
    private Sound sound;
    private PerkSelectionManeger perkSelectionManeger;
    private AbilityManager abilityManager;

    public ZombieGame() {
        init();
    }

    private void init() {
        perkSelectionManeger = new PerkSelectionManeger();
        perkSelectionManeger.startSelectionRandomPerk();
        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        player = new Player(100, MAP_WIDTH / 2, MAP_HEIGHT / 2);
        player.setBaseSpeed(5);
        camera = new Camera(player.getPositionX(), player.getPositionY(), WINDOW_WIDTH, WINDOW_HEIGHT);
        mapGenerator = new MapGenerator(MAP_WIDTH, MAP_HEIGHT);
        currentWeapon = new Weapon(Weapon.WeaponType.PISTOL);
        abilityManager = new AbilityManager();
        perkSelectionManeger.setAbilityManager(abilityManager);

        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        listOfBullet = new ArrayList<>();
        listOfZomboits = new ArrayList<>();
        listOfChests = new ArrayList<>();

        // Создаем начальных зомби
        createZombie();
        createZombie();
        createZombie();

        // Создаем начальные сундуки
        createChest();
        createChest();

        achievementList = new ArrayList<>();
        unlockedAchievement = new ArrayList<>();
        initAchievement();

        sound = new Sound();

        timer = new Timer(1000 / 60, this);
        timer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ZombieGame zombieGame = new ZombieGame();
            JFrame map = new JFrame("Zombie game 3000");
            map.getContentPane().setBackground(BACKGROUND);
            map.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            map.setResizable(false);
            map.setLocation(100, 50);
            map.add(zombieGame);
            map.pack();
            map.setVisible(true);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (perkSelectionManeger.isSelectionActive()) {
            perkSelectionManeger.draw(g2d, WINDOW_WIDTH, WINDOW_HEIGHT);
            return;
        }

        // Применяем трансформацию камеры
        Graphics2D worldGrafic = (Graphics2D) g2d.create();
        worldGrafic.translate(-camera.getX(), -camera.getY());

        // Отрисовываем карту как фон
        mapGenerator.draw(worldGrafic, 0, 0, MAP_WIDTH, MAP_HEIGHT);

        player.draw(worldGrafic);
        drawWorldObj(worldGrafic);
        drawBorder(worldGrafic);
        abilityManager.draw(worldGrafic);
        abilityManager.drawUI(g2d, WINDOW_WIDTH, WINDOW_HEIGHT);

        // НОВОЕ: Рисуем босса если он активен
        if (bossActive && boss != null) {
            boss.draw(worldGrafic);
        }

        worldGrafic.dispose();

        // Рисуем UI поверх всего
        drawUI(g2d);
    }

    private void drawBorder(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(5));
        g2d.drawRect(0, 0, MAP_WIDTH, MAP_HEIGHT);
    }

    private void drawWorldObj(Graphics2D g2d) {
        // Рисуем только видимые сундуки
        for (Chest chest : listOfChests) {
            if (camera.isVisible(chest.getX(), chest.getY(), chest.getSize())) {
                chest.draw(g2d);
            }
        }

        // Рисуем только видимые пули
        for (Bullet b : listOfBullet) {
            if (camera.isVisible((int) b.getX(), (int) b.getY(), b.getSIZE_OF_BULLET())) {
                b.draw(g2d);
            }
        }

        // Рисуем только видимых зомби
        for (Zomboid zomb : listOfZomboits) {
            if (camera.isVisible(zomb.getX(), zomb.getY(), zomb.getSize())) {
                zomb.draw(g2d);
            }
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Score: " + score, 10, 25);
        g2d.drawString("Zombies: " + listOfZomboits.size(), 10, 45);
        g2d.drawString("Bullets: " + listOfBullet.size(), 10, 65);
        g2d.drawString("Chests: " + listOfChests.size(), 10, 85);

        // Новая информация UI
        g2d.drawString("Health: " + player.getPlayerHealth() + "/" + player.getMaxHealth(), 10, 105);
        g2d.drawString("Weapon: " + currentWeapon.getName(), 10, 125);
        g2d.drawString("Ammo: " + currentWeapon.getAmmo() + "/" + currentWeapon.getMaxAmmo(), 10, 145);
        g2d.drawString("Total Ammo: " + totalAmmo, 10, 165);
        if (perkSelectionManeger.getSelectedPerk() != null) {
            g2d.drawString("Perk: " + perkSelectionManeger.getSelectedPerk().getName(), 10, 240);
        }

        // НОВОЕ: Информация об уровне
        g2d.drawString("Level: " + currentLevel, 10, 185);
        g2d.drawString("Zombies Killed: " + zombiesKilled + "/" + ZOMBIES_TO_KILL_FOR_BOSS, 10, 205);

        // Показываем информацию о боссе если он активен
        if (bossActive && boss != null) {
            g2d.setColor(Color.RED);
            g2d.drawString("BOSS: " + boss.getHealth() + "/" + BOSS_HEALTH, 10, 225);
            g2d.setColor(Color.WHITE);
        }

        // НОВОЕ: Красивое предупреждение об орде с направлением
        if (hordWarning && !bossActive) {
            long currentTime = System.currentTimeMillis();
            long timeLeft = WARNING_DURACION - (currentTime - hordWarningTime);

            if (timeLeft > 0) {
                drawHordeWarning(g2d, timeLeft);
            }
        }

        // Таймер до следующей орды (только если нет босса)
        if (!bossActive && !hordWarning) {
            long currentTime = System.currentTimeMillis();
            long timeToHorde = HORDE_SPAWN_DELAY - (currentTime - lastHordeSpawn);
            if (timeToHorde > WARNING_DURACION) {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("Next horde: " + (timeToHorde / 1000) + "s", 10, 245);
            }
        }

        // Полоска здоровья
        drawHealthBar(g2d);

        // НОВОЕ: Сообщение о завершении уровня
        if (levelCompleted) {
            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String levelComplete = "LEVEL " + (currentLevel - 1) + " COMPLETE!";
            int x = (WINDOW_WIDTH - fm.stringWidth(levelComplete)) / 2;
            int y = WINDOW_HEIGHT / 2 - 50;
            g2d.drawString(levelComplete, x, y);

            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            fm = g2d.getFontMetrics();
            String bonus = "Bonus: " + BOSS_REWARD + " points";
            x = (WINDOW_WIDTH - fm.stringWidth(bonus)) / 2;
            g2d.drawString(bonus, x, y + 60);

            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            fm = g2d.getFontMetrics();
            String nextLevel = "Starting Level " + currentLevel + "...";
            x = (WINDOW_WIDTH - fm.stringWidth(nextLevel)) / 2;
            g2d.drawString(nextLevel, x, y + 100);
        }

        if (!gameRunning) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String gameOver = "GAME OVER";
            int x = (WINDOW_WIDTH - fm.stringWidth(gameOver)) / 2;
            int y = WINDOW_HEIGHT / 2;
            g2d.drawString(gameOver, x, y);

            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            fm = g2d.getFontMetrics();
            String restart = "Press R to restart";
            x = (WINDOW_WIDTH - fm.stringWidth(restart)) / 2;
            g2d.drawString(restart, x, y + 60);
        }

        drawAchievementNotification(g2d);
    }

    private void drawHordeWarning(Graphics2D g2d, long timeLeft) {
        long currentTime = System.currentTimeMillis();
        int seconds = (int) (timeLeft / 1000) + 1;

        // Основное предупреждение в центре
        g2d.setFont(new Font("Segoe UI Emoji", Font.BOLD, 32));
        FontMetrics fm = g2d.getFontMetrics();

        // Мигающий эффект
        boolean flash = (currentTime / 300) % 2 == 0;
        g2d.setColor(flash ? Color.RED : Color.YELLOW);

        String warning = "⚠️ HORDE INCOMING! " + seconds + "s ⚠️";
        int warningX = (WINDOW_WIDTH - fm.stringWidth(warning)) / 2;
        g2d.drawString(warning, warningX, 120);

        // Показываем направление
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        fm = g2d.getFontMetrics();
        g2d.setColor(flash ? Color.ORANGE : Color.RED);

        String direction = "FROM " + duracionName[HORDE_DURACION];
        int directionX = (WINDOW_WIDTH - fm.stringWidth(direction)) / 2;
        g2d.drawString(direction, directionX, 150);

        // Рисуем стрелки по краям экрана
        drawDirectionArrows(g2d, flash);

        // Мини-карта направления в углу
        drawMiniDirectionIndicator(g2d, flash);
    }

    private void initAchievement() {
        Achievement achievement1 = new Achievement("first_blood", "First Blood",
                "Зделайте первый килл", 1);
        Achievement achievement2 = new Achievement("loot_n_treats", "Loot and treats",
                "Залутайте 15 сундуков", 15);
        Achievement achievement3 = new Achievement("touch_grass", "Touch grass",
                "Дойти до 50 уровня", 50);
        achievementList.add(achievement1);
        achievementList.add(achievement2);
        achievementList.add(achievement3);
    }

    private void checkAchievement() {
        for (Achievement a : achievementList) {
            if (!unlockedAchievement.contains(a.getId())) {
                boolean unlocked = false;
                switch (a.getId()) {
                    case "first_blood":
                        unlocked = zombiesKilled >= a.getRequirement();
                        break;
                    case "loot_n_treats":
                        unlocked = amountOfOpenSunduk >= a.getRequirement();
                        break;
                    case "touch_grass":
                        unlocked = currentLevel >= a.getRequirement();
                        break;
                }
                if (unlocked) {
                    unlockAchievement(a);
                }
            }
        }
    }

    private void unlockAchievement(Achievement achievement) {
        unlockedAchievement.add(achievement.getId());
        currentAchievementMsg = achievement.getDescription();
        achievementDisplayTime = System.currentTimeMillis();
    }

    private void drawAchievementNotification(Graphics2D g2d) {
        if (!currentAchievementMsg.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - achievementDisplayTime < ACHIVEMENT_DISPLAY_DURACION) {
                float alpha = 1.0f;
                long timeLeft = ACHIVEMENT_DISPLAY_DURACION - (currentTime - achievementDisplayTime);
                if (timeLeft < 500) {
                    alpha = timeLeft / 500.0f;
                }

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.setColor(new Color(255, 215, 0, (int) (255 * alpha))); // Золотой цвет
                g2d.setFont(new Font("Arial", Font.BOLD, 20));

                FontMetrics fm = g2d.getFontMetrics();
                int x = (WINDOW_WIDTH - fm.stringWidth(currentAchievementMsg)) / 2;
                g2d.drawString(currentAchievementMsg, x, 50);

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            } else {
                currentAchievementMsg = "";
            }
        }
    }

    private void drawDirectionArrows(Graphics2D g2d, boolean flash) {
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.setColor(flash ? Color.RED : Color.YELLOW);

        FontMetrics fm = g2d.getFontMetrics();

        switch (HORDE_DURACION) {
            case 1: // Сверху - стрелки вверху экрана
                for (int i = 0; i < 5; i++) {
                    int x = (WINDOW_WIDTH / 6) * (i + 1) - fm.stringWidth("⬇️") / 2;
                    g2d.drawString("⬇️", x, 50);
                }
                break;
            case 2: // Справа - стрелки справа
                for (int i = 0; i < 4; i++) {
                    int y = (WINDOW_HEIGHT / 5) * (i + 1) + 20;
                    g2d.drawString("⬅️", WINDOW_WIDTH - 60, y);
                }
                break;
            case 3: // Снизу - стрелки внизу
                for (int i = 0; i < 5; i++) {
                    int x = (WINDOW_WIDTH / 6) * (i + 1) - fm.stringWidth("⬆️") / 2;
                    g2d.drawString("⬆️", x, WINDOW_HEIGHT - 20);
                }
                break;
            case 4: // Слева - стрелки слева
                for (int i = 0; i < 4; i++) {
                    int y = (WINDOW_HEIGHT / 5) * (i + 1) + 20;
                    g2d.drawString("➡️", 10, y);
                }
                break;
        }
    }

    private void drawMiniDirectionIndicator(Graphics2D g2d, boolean flash) {
        int centerX = WINDOW_WIDTH - 80;
        int centerY = 300;
        int size = 30;

        // Рисуем мини-карту
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(centerX - size, centerY - size, size * 2, size * 2);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(centerX - size, centerY - size, size * 2, size * 2);

        // Игрок в центре
        g2d.setColor(Color.GREEN);
        g2d.fillOval(centerX - 3, centerY - 3, 6, 6);

        // Показываем направление орды
        g2d.setColor(flash ? Color.RED : Color.ORANGE);
        g2d.setStroke(new BasicStroke(3));

        switch (HORDE_DURACION) {
            case 1: // Сверху
                g2d.drawLine(centerX, centerY - size + 5, centerX, centerY - 8);
                g2d.fillPolygon(new int[] { centerX - 3, centerX + 3, centerX },
                        new int[] { centerY - 5, centerY - 5, centerY - 8 }, 3);
                break;
            case 2: // Справа
                g2d.drawLine(centerX + size - 5, centerY, centerX + 8, centerY);
                g2d.fillPolygon(new int[] { centerX + 5, centerX + 5, centerX + 8 },
                        new int[] { centerY - 3, centerY + 3, centerY }, 3);
                break;
            case 3: // Снизу
                g2d.drawLine(centerX, centerY + size - 5, centerX, centerY + 8);
                g2d.fillPolygon(new int[] { centerX - 3, centerX + 3, centerX },
                        new int[] { centerY + 5, centerY + 5, centerY + 8 }, 3);
                break;
            case 4: // Слева
                g2d.drawLine(centerX - size + 5, centerY, centerX - 8, centerY);
                g2d.fillPolygon(new int[] { centerX - 5, centerX - 5, centerX - 8 },
                        new int[] { centerY - 3, centerY + 3, centerY }, 3);
                break;
        }

        g2d.setStroke(new BasicStroke(1)); // Возвращаем обычную толщину линий

        // Подпись
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString("HORDE", centerX - 15, centerY + size + 15);
    }

    private void drawHealthBar(Graphics2D g2d) {
        int barWidth = 200;
        int barHeight = 20;
        int barX = WINDOW_WIDTH - barWidth - 10;
        int barY = 10;

        // Фон полоски здоровья
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(barX, barY, barWidth, barHeight);

        // Полоска здоровья
        float healthPercent = (float) player.getPlayerHealth() / player.getMaxHealth();
        int healthWidth = (int) (barWidth * healthPercent);

        if (healthPercent > 0.6f) {
            g2d.setColor(Color.GREEN);
        } else if (healthPercent > 0.3f) {
            g2d.setColor(Color.YELLOW);
        } else {
            g2d.setColor(Color.RED);
        }

        g2d.fillRect(barX, barY, healthWidth, barHeight);

        // Обводка
        g2d.setColor(Color.WHITE);
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }

    // НОВЫЙ МЕТОД: Создание босса
    private void createBoss() {
        int x = MAP_WIDTH / 2;
        int y = MAP_HEIGHT / 2;

        // Создаем босса подальше от игрока
        double distanceToPlayer = Math.sqrt(
                Math.pow(player.getPositionX() - x, 2) +
                        Math.pow(player.getPositionY() - y, 2));

        if (distanceToPlayer < 200) {
            // Если слишком близко, создаем в случайном месте
            x = 100 + (int) (Math.random() * (MAP_WIDTH - 200));
            y = 100 + (int) (Math.random() * (MAP_HEIGHT - 200));
        }

        boss = new BossZombie(x, y, BOSS_HEALTH, BOSS_SIZE, BOSS_SPEED, BOSS_DAMAGE);
        bossActive = true;

        // Убираем всех обычных зомби
        listOfZomboits.clear();
    }

    private void createZombie() {
        int x;
        int y;
        int randomSite = 1 + (int) (Math.random() * 4);
        switch (randomSite) {
            case 1:
                y = -50;
                x = (int) (Math.random() * MAP_WIDTH);
                break;
            case 2:
                y = (int) (Math.random() * MAP_HEIGHT);
                x = MAP_WIDTH + 50;
                break;
            case 3:
                y = MAP_HEIGHT + 50;
                x = (int) (Math.random() * MAP_WIDTH);
                break;
            default:
                y = (int) (Math.random() * MAP_HEIGHT);
                x = -50;
        }
        Zomboid zomboid = new Zomboid(x, y);
        listOfZomboits.add(zomboid);
    }

    private void createChest() {
        int x = 100 + (int) (Math.random() * (MAP_WIDTH - 200));
        int y = 100 + (int) (Math.random() * (MAP_HEIGHT - 200));

        // Проверяем, чтобы сундук не появился слишком близко к игроку
        double distanceToPlayer = Math.sqrt(
                Math.pow(player.getPositionX() - x, 2) +
                        Math.pow(player.getPositionY() - y, 2));

        if (distanceToPlayer > 100) { // Минимальное расстояние от игрока
            Chest chest = new Chest(x, y);
            listOfChests.add(chest);
        }
    }

    public void updateGame() {
        if (!gameRunning)
            return;

        // НОВОЕ: Обработка завершения уровня
        if (levelCompleted) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - levelCompletedTime > LEVEL_COMPLETE_DISPLAY_TIME) {
                levelCompleted = false;
                startNextLevel();
            }
            return; // Не обновляем игру во время показа сообщения
        }

        updatePlayer();
        updatePlayerDerection();
        updateBullets();
        checkAchievement();

        // НОВОЕ: Обновляем босса или обычных зомби
        if (bossActive && boss != null) {
            boss.update(player.getPositionX(), player.getPositionY());
        } else {
            updateZombi();
        }

        updateChests();

        // НОВОЕ: Спавним зомби только если босс не активен
        if (!bossActive) {
            spawnNewZombi();
            spawnHorde();
        }

        spawnNewChest();
        checkColision();
        checkPlayerColision();

        // НОВОЕ: Проверяем, нужно ли создать босса
        if (!bossActive && zombiesKilled >= ZOMBIES_TO_KILL_FOR_BOSS) {
            createBoss();
        }

        camera.folowPlayer(player.getPositionX(), player.getPositionY(), MAP_WIDTH, MAP_HEIGHT);

        if (mouseHeld) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastShotTime >= currentWeapon.getFireRate()) {
                shootBullet();
                lastShotTime = currentTime;
            }
        }
    }

    private void startNextLevel() {
        currentLevel++;
        zombiesKilled = 0;
        bossActive = false;
        boss = null;

        createZombie();
        createZombie();
        createZombie();

        player.setPlayerHealth(Math.min(player.getMaxHealth(), player.getPlayerHealth() + 20));

        totalAmmo += 50;
    }

    private void updateChests() {
        for (Chest chest : listOfChests) {
            chest.isPlayerNear(player.getPositionX(), player.getPositionY());
        }
    }

    private void spawnNewChest() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastChestSpawn > CHEST_SPAWN_DELAY) {
            createChest();
            lastChestSpawn = currentTime;
        }
    }

    private void openNearestChest() {
        for (Chest chest : listOfChests) {
            if (chest.canOpen() && !chest.isOpen()) {
                chest.open();
                amountOfOpenSunduk += 1;
                score += 50; // Бонус очков за открытие сундука
                break; // Открываем только один сундук за раз
            } else if (chest.canTakeItem()) {
                // Берем предмет из открытого сундука
                ChestItem item = chest.takeItem();
                if (item != null) {
                    applyItem(item);
                }
                break;
            }
        }
    }

    private void applyItem(ChestItem item) {
        switch (item.getType()) {
            case WEAPON:
                Weapon newWeapon = item.getWeapon();
                if (newWeapon != null) {
                    // Добавляем патроны от старого оружия к общему запасу
                    totalAmmo += currentWeapon.getAmmo();
                    currentWeapon = newWeapon;
                    // Заряжаем новое оружие из общего запаса
                    reloadFromTotalAmmo();
                }
                break;
            case AMMO:
                Integer ammoAmount = item.getIntValue();
                if (ammoAmount != null) {
                    totalAmmo += ammoAmount;
                    // Если текущее оружие не заряжено, попробуем зарядить
                    if (currentWeapon.getAmmo() == 0) {
                        reloadFromTotalAmmo();
                    }
                }
                break;
            case HEALTH:
                Integer healthAmount = item.getIntValue();
                if (healthAmount != null) {
                    player.setPlayerHealth(Math.min(player.getMaxHealth(), player.getPlayerHealth() + healthAmount));
                }
                break;
            case SCORE_BONUS:
                Integer scoreBonus = item.getIntValue();
                if (scoreBonus != null) {
                    score += scoreBonus;
                }
                break;
        }
    }

    private void reloadFromTotalAmmo() {
        int neededAmmo = currentWeapon.getMaxAmmo() - currentWeapon.getAmmo();
        int ammoToReload = Math.min(neededAmmo, totalAmmo);

        if (ammoToReload > 0) {
            // Временно устанавливаем патроны для перезарядки
            totalAmmo -= ammoToReload;
            // Создаем новое оружие с нужным количеством патронов
            currentWeapon = new Weapon(currentWeapon.getType());

            for (int i = 0; i < currentWeapon.getMaxAmmo() - ammoToReload; i++) {
                if (currentWeapon.canShoot()) {
                    currentWeapon.shoot();
                    sound.playSound("shoot");
                }
            }
        }
    }

    private void updateBullets() {
        // Обновляем позиции пуль
        for (Bullet b : listOfBullet) {
            b.update();
        }

        // Удаляем пули за пределами карты
        Iterator<Bullet> bullet = listOfBullet.iterator();
        while (bullet.hasNext()) {
            Bullet nextBullet = bullet.next();
            if (nextBullet.isBulletOutSideWindow(MAP_WIDTH, MAP_HEIGHT)) {
                bullet.remove();
            }
        }
    }

    private void checkColision() {
        Iterator<Bullet> itteradetListOfBullet = listOfBullet.iterator();
        while (itteradetListOfBullet.hasNext()) {
            Bullet nextBullet = itteradetListOfBullet.next();

            // НОВОЕ: Проверка столкновения с боссом
            if (bossActive && boss != null) {
                if (boss.collidesWith(nextBullet)) {
                    sound.playSound("hitZomb");
                    boss.takeDamage(nextBullet.getDamage());
                    itteradetListOfBullet.remove();

                    // Если босс умер
                    if (boss.getHealth() <= 0) {
                        score += BOSS_REWARD;
                        bossActive = false;
                        boss = null;
                        levelCompleted = true;
                        levelCompletedTime = System.currentTimeMillis();
                    }
                    break;
                }
            }

            // Проверка столкновения с обычными зомби
            Iterator<Zomboid> itteradetListOfZombie = listOfZomboits.iterator();
            while (itteradetListOfZombie.hasNext()) {
                Zomboid nextZombir = itteradetListOfZombie.next();
                if (nextBullet.collidesWith(nextZombir)) {
                    sound.playSound("hitZomb");
                    itteradetListOfZombie.remove();
                    itteradetListOfBullet.remove();
                    score += 10;
                    zombiesKilled++;// НОВОЕ: Увеличиваем счетчик убитых зомби
                    if (perkSelectionManeger.getSelectedPerk() instanceof RedKlinokPerk) {
                        player.setPlayerHealth(Math.min(player.getMaxHealth(), player.getPlayerHealth() + 1));
                    }
                    break;
                }
            }
        }
    }

    private void checkPlayerColision() {
        // НОВОЕ: Проверка столкновения с боссом
        if (bossActive && boss != null) {
            double distanceToBoss = Math.sqrt(
                    Math.pow(player.getPositionX() - boss.getX(), 2) +
                            Math.pow(player.getPositionY() - boss.getY(), 2));
            if (distanceToBoss < (BOSS_SIZE / 2 + 15)) { // Размер игрока + размер босса
                player.setPlayerHealth(player.getPlayerHealth() - boss.getDamage());
                if (player.getPlayerHealth() <= 0) {
                    gameRunning = false;
                }
                return;
            }
        }

        // Проверка столкновения с обычными зомби
        for (Zomboid zomb : listOfZomboits) {
            double distance = Math.sqrt(
                    Math.pow(player.getPositionX() - zomb.getX(), 2) +
                            Math.pow(player.getPositionY() - zomb.getY(), 2));
            if (distance < 25) { // Размер игрока + размер зомби
                player.setPlayerHealth(player.getPlayerHealth() - 1); // Урон от зомби
                if (player.getPlayerHealth() <= 0) {
                    gameRunning = false;
                }
                break;
            }
        }
    }

    private void updateZombi() {
        for (Zomboid zomb : listOfZomboits) {
            zomb.update(player.getPositionX(), player.getPositionY());
        }
    }

    private void spawnHorde() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHordeSpawn > HORDE_SPAWN_DELAY - WARNING_DURACION) {
            hordWarning = true;
            hordWarningTime = currentTime;
        }
        if (currentTime - lastHordeSpawn > HORDE_SPAWN_DELAY) {
            createHorde();
            lastHordeSpawn = currentTime;
            hordWarning = false;
        }
    }

    private void createHorde() {
        int hordeSize = (int) (MIN_HORDE_ZOMB + (Math.random() * (MAX_HORDE_ZOMB - MIN_HORDE_ZOMB + 1)));
        hordeSize += currentLevel;
        HORDE_DURACION = (int) (1 + (Math.random() * 4));
        for (int i = 0; i < hordeSize; i++) {
            createHordeZombi(HORDE_DURACION, i, hordeSize);
        }
    }

    private void createHordeZombi(int point, int zombI, int totalZomb) {
        int x;
        int y;
        System.out.println(point);
        switch (point) {
            case 1:
                x = (int) (Math.random() * MAP_WIDTH);
                y = -50 - (zombI * 30);
                break;
            case 2:
                x = MAP_WIDTH + 50 + (zombI * 30);
                y = (int) (Math.random() * MAP_HEIGHT);
                break;
            case 3:
                x = (int) (Math.random() * MAP_WIDTH);
                y = MAP_HEIGHT + 50 + (zombI * 30);
                break;
            case 4:
                x = -50 - (zombI * 30);
                y = (int) (Math.random() * MAP_HEIGHT);
                break;
            default:
                x = 0;
                y = 0;
        }
        Zomboid zomboid = new Zomboid(Color.orange, x, y);
        listOfZomboits.add(zomboid);
    }

    private void spawnNewZombi() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastZombieSpawn > ZOMBI_SPAWN_DELAY) {
            createZombie();
            lastZombieSpawn = currentTime;
        }
    }

    private void shootBullet() {
        if (!gameRunning || !currentWeapon.canShoot()) {
            // Попытка перезарядиться автоматически
            if (!currentWeapon.canShoot() && totalAmmo > 0) {
                reloadFromTotalAmmo();
            }
            if (!currentWeapon.canShoot()) {
                return;
            }
        }

        // обновляем направление игрока и то куда он смотрит
        updatePlayerDerection();
        if (currentWeapon.isShotgun()) {
            shoot3X();
            sound.playSound("shoot");
        } else {
            Bullet bullet = new Bullet(
                    player.getPositionX(),
                    player.getPositionY(),
                    player.getDirection(),
                    currentWeapon.getBulletSpeed(),
                    currentWeapon.getDamage(),
                    currentWeapon.getBulletColor());

            listOfBullet.add(bullet);
            sound.playSound("shoot");
            currentWeapon.shoot(); // Тратим патрон
        }
    }

    private void shoot3X() {
        Bullet bullet1 = new Bullet(player.getPositionX(),
                player.getPositionY(),
                player.getDirection(),
                currentWeapon.getBulletSpeed(),
                currentWeapon.getDamage(),
                currentWeapon.getBulletColor());
        Bullet bullet2 = new Bullet(player.getPositionX(),
                player.getPositionY(),
                player.getDirection() + 0.1,
                currentWeapon.getBulletSpeed(),
                currentWeapon.getDamage(),
                currentWeapon.getBulletColor());
        Bullet bullet3 = new Bullet(player.getPositionX(),
                player.getPositionY(),
                player.getDirection() - 0.1,
                currentWeapon.getBulletSpeed(),
                currentWeapon.getDamage(),
                currentWeapon.getBulletColor());
        listOfBullet.add(bullet1);
        listOfBullet.add(bullet2);
        listOfBullet.add(bullet3);
        currentWeapon.shoot();
    }

    private void updatePlayerDerection() {
        // Преобразуем координаты мыши в мировые координаты
        int worldMouseX = mouseX + camera.getX();
        int worldMouseY = mouseY + camera.getY();

        double deltaX = worldMouseX - player.getPositionX();
        double deltaY = worldMouseY - player.getPositionY();
        double angle = Math.atan2(deltaY, deltaX);

        player.setDirection(angle);
    }

    private void updatePlayer() {
        int stepX = 0;
        int stepY = 0;
        if (rightPressed) {
            stepX += 1;
        }
        if (leftPressed) {
            stepX -= 1;
        }
        if (upPressed) {
            stepY -= 1;
        }
        if (downPressed) {
            stepY += 1;
        }
        player.move(stepX, stepY, MAP_WIDTH, MAP_HEIGHT);
    }

    private void restartGame() {
        gameRunning = false;
        levelCompleted = false;
        bossActive = false;
        hordWarning = false;
        mouseHeld = false;

        score = 0;
        currentLevel = 1;
        zombiesKilled = 0;
        amountOfOpenSunduk = 0;

        player.setBaseHp();
        player.setPositionX(MAP_WIDTH / 2);
        player.setPositionY(MAP_HEIGHT / 2);
        player.setBaseSpeed(5);
        camera.setPosition(player.getPositionX(), player.getPositionY());
        currentWeapon = new Weapon(Weapon.WeaponType.PISTOL);
        totalAmmo = 100;

        boss = null;

        lastHordeSpawn = System.currentTimeMillis() + HORDE_SPAWN_DELAY;
        lastZombieSpawn = System.currentTimeMillis();
        lastChestSpawn = System.currentTimeMillis();
        lastShotTime = 0;

        levelCompletedTime = 0;
        hordWarningTime = 0;
        achievementDisplayTime = 0;

        listOfBullet.clear();
        listOfZomboits.clear();
        listOfChests.clear();
        currentAchievementMsg = "";

        leftPressed = false;
        rightPressed = false;
        upPressed = false;
        downPressed = false;

        mouseY = 0;
        mouseX = 0;

        createZombie();
        createZombie();
        createZombie();

        createChest();
        createChest();
        createChest();

        perkSelectionManeger = new PerkSelectionManeger();
        perkSelectionManeger.startSelectionRandomPerk();
        
        // Сбрасываем AbilityManager и устанавливаем связь
        abilityManager.cleanUp();
        perkSelectionManeger.setAbilityManager(abilityManager);
        
        if (sound != null) {
            sound.playBackgroudMusic("background");
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // ПЕРВАЯ ПРОВЕРКА: если активен экран выбора перков
        if (perkSelectionManeger.isSelectionActive()) {
            System.out.println("Handling perk selection input: " + keyCode);
            perkSelectionManeger.handleKeyboardInput(keyCode);

            // Проверяем, подтвердил ли игрок выбор
            if (perkSelectionManeger.isSelectedConfirm()) {
                Perk selectedPerk = perkSelectionManeger.getSelectedPerk();
                if (selectedPerk != null) {
                    System.out.println("Applying perk: " + selectedPerk.getName());
                    selectedPerk.applyEffect(player);

                    // Применяем специальные эффекты перков
                    if (selectedPerk instanceof SurvivalistPerk) {
                        SurvivalistPerk survPerk = (SurvivalistPerk) selectedPerk;
                        Weapon weapon = new Weapon(survPerk.getWeaponType());
                        currentWeapon = weapon;
                    }

                    // ЗАПУСКАЕМ ИГРУ
                    gameRunning = true;
                    System.out.println("Game started!");
                }
            }
            return; // Важно! Выходим из метода, чтобы не обрабатывать другие клавиши
        }

        // ВТОРАЯ ПРОВЕРКА: если игра не запущена, но перки не активны - перезапуск
        if (!gameRunning && !perkSelectionManeger.isSelectionActive()) {
            if (keyCode == VK_R) {
                restartGame();
                return;
            }
        }

        // ТРЕТЬЯ ПРОВЕРКА: обработка способностей во время игры
        if (gameRunning && abilityManager.handleKeyPress(keyCode)) {
            return;
        }

        // ОБЫЧНОЕ УПРАВЛЕНИЕ ИГРОЙ
        if (gameRunning) {
            if (keyCode == VK_W || keyCode == VK_UP) {
                upPressed = true;
            }
            if (keyCode == VK_S || keyCode == VK_DOWN) {
                downPressed = true;
            }
            if (keyCode == VK_A || keyCode == VK_LEFT) {
                leftPressed = true;
            }
            if (keyCode == VK_D || keyCode == VK_RIGHT) {
                rightPressed = true;
            }
            if (keyCode == VK_SPACE) {
                shootBullet();
            }
            if (keyCode == VK_E) {
                openNearestChest();
            }
            if (keyCode == VK_R) {
                // Ручная перезарядка
                reloadFromTotalAmmo();
            }
        }

        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == VK_W || keyCode == VK_UP) {
            upPressed = false;
        }
        if (keyCode == VK_S || keyCode == VK_DOWN) {
            downPressed = false;
        }
        if (keyCode == VK_A || keyCode == VK_LEFT) {
            leftPressed = false;
        }
        if (keyCode == VK_D || keyCode == VK_RIGHT) {
            rightPressed = false;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        if (!perkSelectionManeger.isSelectionActive()) {
            abilityManager.updatePlayerPosicion(player.getPositionX() + camera.getX(),
                    player.getPositionY() + camera.getY());
            abilityManager.updateMousePosicion(mouseX + camera.getX(), mouseY + camera.getY());
            abilityManager.update(1000 / 60, listOfZomboits, listOfBullet);
        }
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            shootBullet();
        }
        if (SwingUtilities.isRightMouseButton(e)) {
            abilityManager.handleMouseClick(e);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            shootBullet();
            mouseHeld = true;
        }
        mouseClicked(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            mouseHeld = false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}