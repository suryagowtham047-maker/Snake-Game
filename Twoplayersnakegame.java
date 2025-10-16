import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class TwoPlayerSnake extends JPanel implements ActionListener, KeyListener {
    private final int WIDTH = 1000;
    private final int HEIGHT = 600;
    private final int CELL_SIZE = 20;
    private final int COLS = WIDTH / CELL_SIZE;
    private final int ROWS = HEIGHT / CELL_SIZE;

    private Timer gameTimer, countdownTimer, blinkTimer, starTimer, fadeTimer, introTimer;
    private Random random = new Random();

    private Snake snake1, snake2;
    private Point food1, food2;

    private boolean running1 = true, running2 = true;
    private boolean gameOver = false;
    private boolean paused = false;
    private boolean countingDown = false;
    private int countdown = 3;
    private int delay = 160;

    private static int highScore1 = 0;
    private static int highScore2 = 0;
    private final String SCORE_FILE = "highscores.txt";
    private boolean showHighScore1 = false, showHighScore2 = false;
    private boolean blinkState = true;
    private float fadeAlpha = 0f;

    private boolean intro = true;
    private ArrayList<Star> stars = new ArrayList<>();
    private JButton startButton, changeBgButton, resetScoreButton;
    private int currentBackground = 0; // 0=dark gradient,1=dark solid,2=orange morning

    // intro snakes
    private Snake introSnake1, introSnake2;
    private int introDir1 = 1, introDir2 = -1;

    public TwoPlayerSnake() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setLayout(null);
        addKeyListener(this);

        loadHighScores();
        initStars();
        initIntroSnakes();

        // Buttons
        startButton = new JButton("START GAME");
        changeBgButton = new JButton("Change Background");
        resetScoreButton = new JButton("Reset High Scores");

        startButton.setBounds(WIDTH / 2 - 80, HEIGHT / 2 + 30, 160, 40);
        changeBgButton.setBounds(WIDTH / 2 - 100, HEIGHT / 2 + 80, 200, 32);
        resetScoreButton.setBounds(WIDTH / 2 - 100, HEIGHT / 2 + 120, 200, 32);

        startButton.setFocusPainted(false);
        changeBgButton.setFocusPainted(false);
        resetScoreButton.setFocusPainted(false);

        startButton.addActionListener(e -> {
            intro = false;
            remove(startButton);
            remove(changeBgButton);
            remove(resetScoreButton);
            requestFocusInWindow();
            initGame();
        });

        changeBgButton.addActionListener(e -> {
            chooseBackground();
        });

        resetScoreButton.addActionListener(e -> {
            resetHighScores();
        });

        add(startButton);
        add(changeBgButton);
        add(resetScoreButton);

        // star twinkle timer
        starTimer = new Timer(80, e -> {
            for (Star s : stars) s.twinkle();
            repaint();
        });
        starTimer.start();

        // blink timer for high score message
        blinkTimer = new Timer(400, e -> {
            blinkState = !blinkState;
            repaint();
        });
        blinkTimer.start();

        // intro snake movement
        introTimer = new Timer(150, e -> {
            if (intro) {
                for (Snake s : new Snake[]{introSnake1, introSnake2}) {
                    Point head = s.getHead();
                    if (s == introSnake1) {
                        head.x += introDir1;
                        if (head.x > COLS / 2 - 4 || head.x < 0) introDir1 *= -1;
                    } else {
                        head.x += introDir2;
                        if (head.x < COLS / 2 || head.x > COLS - 4) introDir2 *= -1;
                    }
                    for (int i = s.body.size() - 1; i > 0; i--) {
                        s.body.set(i, new Point(s.body.get(i - 1)));
                    }
                }
                repaint();
            }
        });
        introTimer.start();
    }

    private void chooseBackground() {
        String[] options = {"Dark Gradient", "Dark Solid", "Orange Morning"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Select Background Theme",
                "Background Selection",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice >= 0) currentBackground = choice;
        repaint();
    }

    private void resetHighScores() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to reset high scores?",
                "Reset High Scores",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            highScore1 = 0;
            highScore2 = 0;
            saveHighScores();
        }
    }

    private void initIntroSnakes() {
        introSnake1 = new Snake(new Point(4, ROWS / 2 - 4), new Color(0x00BFFF), new Color(0x33FFFF), 0, COLS / 2);
        introSnake2 = new Snake(new Point(COLS - 5, ROWS / 2 + 4), new Color(0xFF2D95), new Color(0xFF66CC), COLS / 2, COLS);
    }

    private void initStars() {
        stars.clear();
        for (int i = 0; i < 160; i++) stars.add(new Star(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(3) + 1));
    }

    private void initGame() {
        snake1 = new Snake(new Point(4, ROWS / 2), new Color(0x00BFFF), new Color(0x33FFFF), 0, COLS / 2);
        snake2 = new Snake(new Point(COLS - 5, ROWS / 2), new Color(0xFF2D95), new Color(0xFF66CC), COLS / 2, COLS);

        spawnFood();
        running1 = true;
        running2 = true;
        gameOver = false;
        paused = false;

        countdown = 3;
        countingDown = true;
        if (countdownTimer != null) countdownTimer.stop();
        countdownTimer = new Timer(1000, ev -> {
            countdown--;
            if (countdown <= 0) {
                countdownTimer.stop();
                countingDown = false;
                startMainTimer();
            }
            repaint();
        });
        countdownTimer.start();

        fadeAlpha = 0f;
        showHighScore1 = false;
        showHighScore2 = false;
        delay = 160;
    }

    private void startMainTimer() {
        if (gameTimer != null) gameTimer.stop();
        gameTimer = new Timer(delay, this);
        gameTimer.start();
    }

    private void spawnFood() {
        food1 = new Point(random.nextInt(COLS / 2), random.nextInt(ROWS));
        food2 = new Point(COLS / 2 + random.nextInt(COLS / 2), random.nextInt(ROWS));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (intro) return;
        if (paused || countingDown || gameOver) return;

        if (running1) snake1.move();
        if (running2) snake2.move();

        if (running1 && snake1.getHead().equals(food1)) {
            snake1.grow();
            spawnFood();
            increaseSpeed();
            if (snake1.score > highScore1) {
                highScore1 = snake1.score;
                showHighScore1 = true;
                saveHighScores();
            }
        }
        if (running2 && snake2.getHead().equals(food2)) {
            snake2.grow();
            spawnFood();
            increaseSpeed();
            if (snake2.score > highScore2) {
                highScore2 = snake2.score;
                showHighScore2 = true;
                saveHighScores();
            }
        }

        if (running1 && snake1.collision()) {
            running1 = false;
            snake1.stopTime();
            if (!running2) endGame();
        }
        if (running2 && snake2.collision()) {
            running2 = false;
            snake2.stopTime();
            if (!running1) endGame();
        }

        repaint();
    }

    private void increaseSpeed() {
        delay = Math.max(50, delay - 6);
        if (gameTimer != null) gameTimer.setDelay(delay);
    }

    private void endGame() {
        gameOver = true;
        if (gameTimer != null) gameTimer.stop();
        saveHighScores();
        startFadeIn();
        SwingUtilities.invokeLater(this::showGameOverDialog);
    }

    private void startFadeIn() {
        if (fadeTimer != null) fadeTimer.stop();
        fadeAlpha = 0f;
        fadeTimer = new Timer(50, ev -> {
            fadeAlpha += 0.08f;
            if (fadeAlpha >= 1f) {
                fadeAlpha = 1f;
                fadeTimer.stop();
            }
            repaint();
        });
        fadeTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // background
        switch (currentBackground) {
            case 0 -> { // Dark gradient
                GradientPaint gp = new GradientPaint(0, 0, Color.BLACK, 0, HEIGHT, new Color(7, 26, 63));
                g2.setPaint(gp);
                g2.fillRect(0, 0, WIDTH, HEIGHT);
            }
            case 1 -> { // Dark solid
                g2.setColor(new Color(15, 15, 30));
                g2.fillRect(0, 0, WIDTH, HEIGHT);
            }
            case 2 -> { // Orange morning
                GradientPaint gp = new GradientPaint(0, 0, new Color(255, 153, 51), 0, HEIGHT, new Color(255, 204, 102));
                g2.setPaint(gp);
                g2.fillRect(0, 0, WIDTH, HEIGHT);
            }
        }

        // stars only for dark themes
        if (currentBackground != 2) {
            for (Star s : stars) s.draw(g2);
        }

        if (intro) {
            drawIntro(g2);
            g2.dispose();
            return;
        }

        g2.setColor(new Color(120, 120, 120, 120));
        g2.drawLine(WIDTH / 2, 0, WIDTH / 2, HEIGHT);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.drawString("Player 1 Score: " + (snake1 != null ? snake1.score : 0) + " | High: " + highScore1, 12, 18);
        g2.drawString("Player 2 Score: " + (snake2 != null ? snake2.score : 0) + " | High: " + highScore2, WIDTH / 2 + 12, 18);

        if (countingDown) {
            g2.setFont(new Font("Arial", Font.BOLD, 40));
            g2.setColor(Color.WHITE);
            String s = countdown > 0 ? String.valueOf(countdown) : "GO!";
            g2.drawString(s, WIDTH / 2 - 24, HEIGHT / 2);
            g2.dispose();
            return;
        }

        if (paused) {
            g2.setFont(new Font("Arial", Font.BOLD, 36));
            g2.setColor(Color.WHITE);
            g2.drawString("PAUSED", WIDTH / 2 - 70, HEIGHT / 2);
            g2.dispose();
            return;
        }

        // food
        if (food1 != null) drawGlowingOrb(g2, food1.x * CELL_SIZE, food1.y * CELL_SIZE, new Color(0x00E5FF), 12);
        if (food2 != null) drawGlowingOrb(g2, food2.x * CELL_SIZE, food2.y * CELL_SIZE, new Color(0xFF33CC), 12);

        // snakes
        if (running1) snake1.draw(g2);
        else {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            g2.drawString("Player 1 Lost!", 50, HEIGHT / 2);
        }
        if (running2) snake2.draw(g2);
        else {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            g2.drawString("Player 2 Lost!", WIDTH / 2 + 50, HEIGHT / 2);
        }

        // high score blink
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        if (showHighScore1 && blinkState) {
            g2.setColor(Color.ORANGE);
            g2.drawString("🎉 New High Score! 🎉", 60, 60);
        }
        if (showHighScore2 && blinkState) {
            g2.setColor(Color.MAGENTA);
            g2.drawString("🎉 New High Score! 🎉", WIDTH / 2 + 60, 60);
        }

        if (gameOver) drawSummary(g2);

        g2.dispose();
    }

    private void drawIntro(Graphics2D g2) {
        g2.setFont(new Font("Arial", Font.BOLD, 48));
        g2.setColor(Color.WHITE);
        g2.drawString("TWO PLAYER SNAKE GAME", WIDTH / 2 - 300, HEIGHT / 2 - 60);

        if (introSnake1 != null) introSnake1.draw(g2);
        if (introSnake2 != null) introSnake2.draw(g2);
    }

    private void drawGlowingOrb(Graphics2D g2, int x, int y, Color color, int size) {
        g2.setColor(color);
        g2.fillOval(x, y, size, size);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
        g2.fillOval(x - 2, y - 2, size + 4, size + 4);
    }

    private void drawSummary(Graphics2D g2) {
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));

        int boxW = 450, boxH = 220;
        int x = WIDTH / 2 - boxW / 2;
        int y = HEIGHT / 2 - boxH / 2;

        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(x, y, boxW, boxH, 30, 30);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString("GAME OVER", x + 140, y + 40);

        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.drawString("Player 1 Score: " + snake1.score + " | Time: " + (int) snake1.getPlayTime() + "s", x + 40, y + 80);
        g2.drawString("Player 2 Score: " + snake2.score + " | Time: " + (int) snake2.getPlayTime() + "s", x + 40, y + 120);

        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(Color.YELLOW);
        g2.drawString(getResult(), x + 120, y + 170);

        g2.setFont(new Font("Arial", Font.ITALIC, 16));
        g2.setColor(Color.WHITE);
        g2.drawString("Press R to Restart", x + 140, y + 200);

        g2.setComposite(old);
    }

    private String getResult() {
        double t1 = snake1.getPlayTime();
        double t2 = snake2.getPlayTime();
        if (snake1.score > snake2.score) return "🏆 Player 1 Wins!";
        if (snake2.score > snake1.score) return "🏆 Player 2 Wins!";
        if (t1 > t2) return "🏆 Player 1 Wins by Time!";
        if (t2 > t1) return "🏆 Player 2 Wins by Time!";
        return "It's a Tie!";
    }

    private void showGameOverDialog() {
        String[] options = {"Play Again", "Change Background", "Reset High Score", "OK"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Player 1: " + snake1.score + " | Time: " + (int) snake1.getPlayTime() + "s\n" +
                "Player 2: " + snake2.score + " | Time: " + (int) snake2.getPlayTime() + "s\n" +
                getResult(),
                "TWO PLAYER SNAKE GAME",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        switch (choice) {
            case 0 -> initGame(); // Play Again
            case 1 -> { chooseBackground(); returnToIntro(); } // Change Background
            case 2 -> { resetHighScores(); returnToIntro(); } // Reset High Score
            default -> returnToIntro(); // OK
        }
    }

    private void returnToIntro() {
        intro = true;
        add(startButton);
        add(changeBgButton);
        add(resetScoreButton);
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (intro) return;

        if (key == KeyEvent.VK_P && !countingDown && !gameOver) {
            paused = !paused;
            repaint();
            return;
        }

        if (!gameOver && !countingDown && !paused) {
            // player 1
            if (key == KeyEvent.VK_W) snake1.setDirection(0, -1);
            if (key == KeyEvent.VK_S) snake1.setDirection(0, 1);
            if (key == KeyEvent.VK_A) snake1.setDirection(-1, 0);
            if (key == KeyEvent.VK_D) snake1.setDirection(1, 0);
            // player 2
            if (key == KeyEvent.VK_UP) snake2.setDirection(0, -1);
            if (key == KeyEvent.VK_DOWN) snake2.setDirection(0, 1);
            if (key == KeyEvent.VK_LEFT) snake2.setDirection(-1, 0);
            if (key == KeyEvent.VK_RIGHT) snake2.setDirection(1, 0);
        } else if (key == KeyEvent.VK_R) {
            initGame();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    class Snake {
        ArrayList<Point> body = new ArrayList<>();
        Color bodyColor, headColor;
        int dx = 0, dy = 0;
        int score = 0;
        long startTime = System.currentTimeMillis(), endTime = -1;
        int leftBound, rightBound;
        boolean growing = false;

        Snake(Point start, Color bodyColor, Color headColor, int leftBound, int rightBound) {
            this.bodyColor = bodyColor;
            this.headColor = headColor;
            this.leftBound = leftBound;
            this.rightBound = rightBound;
            for (int i = 0; i < 4; i++) body.add(new Point(start.x - i, start.y));
        }

        void setDirection(int dx, int dy) {
            if (this.dx == -dx && this.dy == -dy) return;
            this.dx = dx;
            this.dy = dy;
        }

        void move() {
            if (dx == 0 && dy == 0) return;
            Point head = getHead();
            Point newHead = new Point(head.x + dx, head.y + dy);
            body.add(0, newHead);
            if (!growing) body.remove(body.size() - 1);
            else growing = false;
        }

        void grow() {
            score++;
            growing = true;
            endTime = System.currentTimeMillis();
        }

        boolean collision() {
            Point head = getHead();
            if (head.x < leftBound || head.x >= rightBound || head.y < 0 || head.y >= ROWS) return true;
            for (int i = 1; i < body.size(); i++)
                if (head.equals(body.get(i))) return true;
            return false;
        }

        Point getHead() { return body.get(0); }

        void draw(Graphics2D g2) {
            for (int i = 0; i < body.size(); i++) {
                int px = body.get(i).x * CELL_SIZE;
                int py = body.get(i).y * CELL_SIZE;
                if (i == 0) {
                    g2.setColor(headColor);
                    g2.fillRect(px, py, (int) (CELL_SIZE * 1.1), (int) (CELL_SIZE * 1.1));
                } else {
                    g2.setColor(bodyColor);
                    g2.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        double getPlayTime() {
            if (endTime == -1) return (System.currentTimeMillis() - startTime) / 1000.0;
            return (endTime - startTime) / 1000.0;
        }

        void stopTime() { if (endTime == -1) endTime = System.currentTimeMillis(); }
    }

    private void saveHighScores() {
        try (PrintWriter out = new PrintWriter(new FileWriter(SCORE_FILE))) {
            out.println(highScore1);
            out.println(highScore2);
        } catch (IOException ignored) {}
    }

    private void loadHighScores() {
        File f = new File(SCORE_FILE);
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                highScore1 = Integer.parseInt(br.readLine());
                highScore2 = Integer.parseInt(br.readLine());
            } catch (Exception ignored) {}
        }
    }

    class Star {
        int x, y, size;
        boolean bright = true;

        Star(int x, int y, int size) { this.x = x; this.y = y; this.size = size; }

        void twinkle() { bright = !bright; }

        void draw(Graphics2D g2) {
            g2.setColor(bright ? Color.WHITE : Color.GRAY);
            g2.fillOval(x, y, size, size);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("TWO PLAYER SNAKE GAME");
        TwoPlayerSnake game = new TwoPlayerSnake();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}