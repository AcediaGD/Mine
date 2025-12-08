/*     ████
     ████████				████████████████████████████████████████████████████████████████
   ▒▒██████████▒▒			█                                                              █
   ▒▒   ███████▒▒			█    ██████  ██████  ██   ██     █     ███    ██    ██████	   █
   ██      ██████▒▒		    █   ██      ██    ██ ██   ██    █ █    ████   ██    █     █    █
    ███    ██████▒▒		    █   ██   ██ ██    ██ ███████   █████   ██ ██  ██    ██████     █
           ████████▒▒		█   ██    █ ██    ██ ██   ██  ██   ██  ██  ██ ██    █     █    █
         ▒▒████████▒▒██     █    ██████  ██████  ██   ██ ██     ██ ██   ████    ██████  █  █
       ▒▒▒▒████████▒▒▒▒██   █															   █
         ▒▒████████▒▒██     █			    ██		█	   ██		██		█			   █
           ████████         █			    ██	   █ █		██	   ██	   █ █			   █
    ███    ██████▒▒         █			    ██	  █████		 ██	  ██	  █████		       █
   ██      ██████▒▒         █		    ██	██	 ██	  ██	  ██ ██ 	 ██	  ██ 	       █
   ▒▒    ██████▒▒           █	   ██	  ██	██	   ██		█		██	   ██		   █
   ▒▒██████████▒▒           █															   █
     ████████               ████████████████████████████████████████████████████████████████
   	    ███	   */


// wild card imports 
import javax.swing.*; // for GUI components or Swing for lightweight components
import java.awt.*; // for GUI components or abstract window toolkit
import java.awt.event.ActionEvent; // for action events
import java.awt.event.ActionListener; // for button clicks
import java.awt.event.ComponentAdapter; // for component resize events
import java.awt.event.ComponentEvent; // for component resize events
import java.awt.image.BufferedImage; // for image handling
import java.io.File; // for file handling duh
import java.io.IOException; // for handling IO exceptions and errors
import java.util.ArrayDeque; //double-ended queue
import java.util.ArrayList; // I wonder what this is for
import java.util.List; // again I wonder what this is for

import javax.imageio.ImageIO;

public class MinesweeperGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    // Default per-cell icon size
    private static final int DEFAULT_TILE_SIZE = 32;

    // 1h 30m 0.42ms gate before a new game
    private static final long BEE_MOVIE_GATE_MS = 5_400_042L;

    // How much border of the board image to ignore
    private static final double OUTER_PAD_FRAC = 0.055;
    // Gap between tiles compared to tile size
    private static final double GAP_FRAC_OF_CELL = 0.20;

    // Reset animation timing (ms per frame)
    private static final int RESET_FRAME_DELAY_MS = 220;

    // Scale factor for reset dialog images
    private static final double RESET_DIALOG_SCALE = 2.5;

    private Grid grid;
    private JButton[][] cells;
    private boolean[][] revealed;
    private boolean gameOver;
    private int cellsToReveal;

    private BoardPanel gridPanel;
    private JLabel statusLabel;

    // Base images (original size)
    private Image boardBackgroundBase; // full 10x10 background
    private Image emptyBase;           // per-cell empty tile
    private Image bombBase;            // per-cell bomb tile

    // Reset animation frames “Colress Sprite#.ext”
    private List<ImageIcon> resetFrames;

    // Bee movie file handle
    private File beeMovieFile;

    // Current logical tile size (updated on resize)
    private int tileSize = DEFAULT_TILE_SIZE;

    // Prevent reset spam while gate is active
    private boolean resetLocked = false;

    public MinesweeperGUI() {
        super("Minesweeper");

        loadAssets();
        setBlankWindowIcon();
        // Remove Java coffee icon (macOS)

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        statusLabel = new JLabel("Click a cell. Press R to reset.");
        add(statusLabel, BorderLayout.NORTH);

        gridPanel = new BoardPanel();
        add(gridPanel, BorderLayout.CENTER);

        setupKeyBindings();
        startNewGame();

        setResizable(true);
        setMinimumSize(new Dimension(420, 480)); // keep grid from collapsing

        // Re-layout cells whenever the panel is resized
        gridPanel.addComponentListener(new ComponentAdapter() {
            //Override
            public void componentResized(ComponentEvent e) {
                layoutCellsToMatchBackground();
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Panel that paints the single board image as the background
    private class BoardPanel extends JPanel {
        private Image bg;

        public void setBackgroundImage(Image img) {
            this.bg = img;
            repaint();
        }

        //Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bg != null) {
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }

    // Load images + movie from several possible file locations
    private void loadAssets() {
    	
    boardBackgroundBase = loadImage(
            "10x10GridMinesweeper.jpeg",
            "10x10GridMinesweeper.jpg",
            "10x10GridMinesweeper.png",
            "src/10x10GridMinesweeper.jpeg",
            "src/10x10GridMinesweeper.jpg",
            "src/10x10GridMinesweeper.png"
    );

    emptyBase = loadImage(
            "GridEmpty.jpeg",
            "GridEmpty.jpg",
            "GridEmpty.png",
            "src/GridEmpty.jpeg",
            "src/GridEmpty.jpg",
            "src/GridEmpty.png"
    );

    bombBase = loadImage(
            "bomb.jpeg",
            "bomb.jpg",
            "bomb.png",
            "src/bomb.jpeg",
            "src/bomb.jpg",
            "src/bomb.png"
    );

    resetFrames = loadNumberedResetFrames("Colress Sprite");

    beeMovieFile = resolveFile(
            "BeeMovie.mp4",
            "src/BeeMovie.mp4"
     );
    }

    // Map R / r keys to reset action
    private void setupKeyBindings() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke('R'), "reset");
        im.put(KeyStroke.getKeyStroke('r'), "reset");

        am.put("reset", new AbstractAction() {
            //Override
            public void actionPerformed(ActionEvent e) {
                resetGame();
            }
        });
    }

    // Create a fresh Minesweeper board and buttons
    private void startNewGame() {
        gameOver = false;

        grid = new Grid();
        int rows = grid.getNumRows();
        int cols = grid.getNumColumns();

        cells = new JButton[rows][cols];
        revealed = new boolean[rows][cols];
        cellsToReveal = rows * cols - grid.getNumBombs();

        gridPanel.removeAll();
        gridPanel.setLayout(null);
        // manual positioning

        if (boardBackgroundBase != null) {
            gridPanel.setBackgroundImage(boardBackgroundBase);
        }

        tileSize = DEFAULT_TILE_SIZE;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                final int rr = r;
                final int cc = c;

                JButton btn = new JButton();
                btn.setMargin(new Insets(0, 0, 0, 0));
                btn.setFocusPainted(false);

                // Let background image provide hidden look
                btn.setOpaque(false);
                btn.setContentAreaFilled(false);
                btn.setBorderPainted(false);
                btn.setBorder(BorderFactory.createEmptyBorder());

                applyHiddenAppearance(btn);

                btn.setHorizontalTextPosition(SwingConstants.CENTER);
                btn.setVerticalTextPosition(SwingConstants.CENTER);

                btn.addActionListener(new ActionListener() {
                    //Override
                    public void actionPerformed(ActionEvent e) {
                        handleClick(rr, cc);
                    }
                });

                cells[r][c] = btn;
                gridPanel.add(btn);
            }
        }

        statusLabel.setText("Click a cell. Press R to reset.");

        gridPanel.revalidate();
        gridPanel.repaint();

        // Do layout after panel gets a real size
        SwingUtilities.invokeLater(new Runnable() {
            //Override
            public void run() {
                layoutCellsToMatchBackground();
            }
        });
    }

    // Handle one button click
    private void handleClick(int r, int c) {
        if (gameOver) return;
        if (revealed[r][c]) return;
        if (resetLocked) return;

        if (grid.isBombAtLocation(r, c)) {
            loseGame();
            return;
        }

        revealAreaFrom(r, c);

        if (cellsToReveal <= 0) {
            winGame();
        }
    }

    // BFS flood-fill: reveal 0-region + edge numbers
    private void revealAreaFrom(int startR, int startC) {
        int rows = grid.getNumRows();
        int cols = grid.getNumColumns();

        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{startR, startC});

        while (!q.isEmpty()) {
            int[] cur = q.removeFirst();
            int r = cur[0];
            int c = cur[1];

            if (r < 0 || r >= rows || c < 0 || c >= cols) continue;
            if (revealed[r][c]) continue;
            if (grid.isBombAtLocation(r, c)) continue;

            int count = grid.getCountAtLocation(r, c);

            revealSingle(r, c, count);

            if (count == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        q.add(new int[]{r + dr, c + dc});
                    }
                }
            }
        }
    }

    // Reveal one cell and apply icon/text
    private void revealSingle(int r, int c, int count) {
        revealed[r][c] = true;
        cellsToReveal--;

        JButton btn = cells[r][c];
        btn.setEnabled(false);

        applyEmptyAppearance(btn);
        btn.setText(count > 0 ? String.valueOf(count) : "");
    }

    // Show bombs and game-over dialog
    private void loseGame() {
        gameOver = true;
        revealAll(true);

        statusLabel.setText("Game over. Press R to reset.");

        JOptionPane.showMessageDialog(
                this,
                "You clicked a bomb.\nPress R to reset.",
                "Game Over",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // Show final board and win dialog
    private void winGame() {
        gameOver = true;
        revealAll(false);

        statusLabel.setText("You win. Press R to reset.");

        JOptionPane.showMessageDialog(
                this,
                "All safe cells revealed.\nPress R to reset.",
                "You Win",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // Reveal entire grid at game end
    private void revealAll(boolean showBombsAsBombs) {
        int rows = grid.getNumRows();
        int cols = grid.getNumColumns();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton btn = cells[r][c];
                btn.setEnabled(false);

                if (grid.isBombAtLocation(r, c)) {
                    if (showBombsAsBombs) {
                        applyBombAppearance(btn);
                        btn.setText("");
                    } else {
                        applyEmptyAppearance(btn);
                        btn.setText("B");
                    }
                } else {
                    int count = grid.getCountAtLocation(r, c);
                    applyEmptyAppearance(btn);
                    btn.setText(count == 0 ? "" : String.valueOf(count));
                }
            }
        }

        layoutCellsToMatchBackground();
    }

    // Full reset flow with dialog + Bee Movie gate
    private void resetGame() {
        if (resetLocked) return;
        resetLocked = true;

        showResetDialog();
        tryOpenBeeMovie();
        disableBoardForGate();

        statusLabel.setText("Bee Movie gate active... new game unlocks after 1h 30m 0.042s.");

        new javax.swing.Timer((int) BEE_MOVIE_GATE_MS, new ActionListener() {
            //Override
            public void actionPerformed(ActionEvent e) {
                ((javax.swing.Timer) e.getSource()).stop();
                resetLocked = false;
                startNewGame();
            }
        }).start();
    }

    // Disable all tiles while gate is active
    private void disableBoardForGate() {
        if (cells == null) return;
        int rows = cells.length;
        if (rows == 0) return;
        int cols = cells[0].length;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (cells[r][c] != null) {
                    cells[r][c].setEnabled(false);
                }
            }
        }
    }

    // Reset dialog with optional animation
    private void showResetDialog() {
        final List<ImageIcon> frames = resetFrames;

        if (frames != null && frames.size() > 1) {
            // Ping-pong animation
            final List<ImageIcon> pingPong = buildPingPongFrames(frames);
            final List<ImageIcon> scaled = scaleFramesForDialog(pingPong, RESET_DIALOG_SCALE);

            final JDialog dialog = new JDialog(this, "Reset", true);
            dialog.setLayout(new BorderLayout());

            final JLabel animLabel = new JLabel(scaled.get(0), SwingConstants.CENTER);
            final JLabel textLabel = new JLabel("Reset triggered.", SwingConstants.CENTER);

            JPanel topBox = new JPanel();
            topBox.setLayout(new BoxLayout(topBox, BoxLayout.Y_AXIS));
            topBox.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
            topBox.add(animLabel);
            topBox.add(Box.createVerticalStrut(10));
            topBox.add(textLabel);

            JButton ok = new JButton("OK");
            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            south.add(ok);

            dialog.add(topBox, BorderLayout.NORTH);
            dialog.add(south, BorderLayout.SOUTH);

            final int[] idx = new int[]{0};

            final javax.swing.Timer t =
                    new javax.swing.Timer(RESET_FRAME_DELAY_MS, new ActionListener() {
                        //Override
                        public void actionPerformed(ActionEvent e) {
                            idx[0]++;
                            if (idx[0] >= scaled.size()) idx[0] = 0;
                            animLabel.setIcon(scaled.get(idx[0]));
                        }
                    });

            ok.addActionListener(new ActionListener() {
                //Override
                public void actionPerformed(ActionEvent e) {
                    t.stop();
                    dialog.dispose();
                }
            });

            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                //Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    t.stop();
                }
            });

            dialog.pack();
            dialog.setLocationRelativeTo(this);

            t.start();
            dialog.setVisible(true);
            return;
        }

        // Single-frame dialog
        if (frames != null && frames.size() == 1) {
            ImageIcon scaledOne = scaleSingleForDialog(frames.get(0), RESET_DIALOG_SCALE);

            final JDialog dialog = new JDialog(this, "Reset", true);
            dialog.setLayout(new BorderLayout());

            JLabel imgLabel = new JLabel(scaledOne, SwingConstants.CENTER);
            JLabel textLabel = new JLabel("Reset triggered.", SwingConstants.CENTER);

            JPanel topBox = new JPanel();
            topBox.setLayout(new BoxLayout(topBox, BoxLayout.Y_AXIS));
            topBox.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
            topBox.add(imgLabel);
            topBox.add(Box.createVerticalStrut(10));
            topBox.add(textLabel);

            JButton ok = new JButton("OK");
            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            south.add(ok);

            dialog.add(topBox, BorderLayout.NORTH);
            dialog.add(south, BorderLayout.SOUTH);

            ok.addActionListener(new ActionListener() {
                //Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });

            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            return;
        }

        // Text-only fallback
        JOptionPane.showMessageDialog(
                this,
                "Reset triggered.",
                "Reset",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // Build 0..n-1..1 ping-pong sequence
    private List<ImageIcon> buildPingPongFrames(List<ImageIcon> frames) {
        int n = frames.size();
        List<ImageIcon> out = new ArrayList<>();

        for (int i = 0; i < n; i++) out.add(frames.get(i));
        for (int i = n - 2; i >= 1; i--) out.add(frames.get(i));

        return out;
    }

    // Load numbered frames: baseName1.ext, baseName2.ext, ...
    private List<ImageIcon> loadNumberedResetFrames(String baseName) {
        List<ImageIcon> frames = new ArrayList<>();

        for (int i = 1; i <= 200; i++) {
            File f = resolveFile(
                    baseName + i + ".jpg",
                    baseName + i + ".jpeg",
                    baseName + i + ".png",
                    "src/" + baseName + i + ".jpg",
                    "src/" + baseName + i + ".jpeg",
                    "src/" + baseName + i + ".png"
            );

            if (f == null) break;

            try {
                BufferedImage img = ImageIO.read(f);
                if (img == null) break;

                BufferedImage copy = new BufferedImage(
                        img.getWidth(),
                        img.getHeight(),
                        BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g2 = copy.createGraphics();
                g2.setComposite(AlphaComposite.Src);
                g2.drawImage(img, 0, 0, null);
                g2.dispose();

                frames.add(new ImageIcon(copy));
            } catch (IOException ignored) {
                break;
            }
        }

        return frames.isEmpty() ? null : frames;
    }

    // Scale all frames for dialog
    private List<ImageIcon> scaleFramesForDialog(List<ImageIcon> frames, double scale) {
        List<ImageIcon> out = new ArrayList<>();
        for (int i = 0; i < frames.size(); i++) {
            out.add(scaleSingleForDialog(frames.get(i), scale));
        }
        return out;
    }

    // Scale single icon for dialog
    private ImageIcon scaleSingleForDialog(ImageIcon icon, double scale) {
        if (icon == null) return null;

        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        if (w <= 0 || h <= 0) return icon;

        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));

        Image src = icon.getImage();

        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(src, 0, 0, nw, nh, null);
        g2.dispose();

        return new ImageIcon(dst);
    }

    // Try to open BeeMovie.mp4 in OS default player
    private void tryOpenBeeMovie() {
        if (beeMovieFile == null) return;
        if (!Desktop.isDesktopSupported()) return;

        try {
            Desktop.getDesktop().open(beeMovieFile);
        } catch (IOException ignored) {
            // ignore failure, keep reset flow going
        }
    }

    // Compute per-cell bounds to line up with background grid
    private void layoutCellsToMatchBackground() {
        if (grid == null || cells == null) return;

        int rows = grid.getNumRows();
        int cols = grid.getNumColumns();
        if (rows <= 0 || cols <= 0) return;

        int w = gridPanel.getWidth();
        int h = gridPanel.getHeight();
        if (w <= 0 || h <= 0) return;

        int padX = (int) Math.round(w * OUTER_PAD_FRAC);
        int padY = (int) Math.round(h * OUTER_PAD_FRAC);

        int innerW = Math.max(1, w - 2 * padX);
        int innerH = Math.max(1, h - 2 * padY);

        double baseCell = Math.min(innerW / (double) cols, innerH / (double) rows);
        int gap = (int) Math.round(baseCell * GAP_FRAC_OF_CELL);

        int cellW = (int) Math.floor((innerW - gap * (cols - 1)) / (double) cols);
        int cellH = (int) Math.floor((innerH - gap * (rows - 1)) / (double) rows);
        int cell = Math.max(10, Math.min(cellW, cellH));

        int gridW = cell * cols + gap * (cols - 1);
        int gridH = cell * rows + gap * (rows - 1);

        int startX = (w - gridW) / 2;
        int startY = (h - gridH) / 2;

        tileSize = cell;

        Font cellFont = new Font("Monospaced", Font.BOLD, Math.max(12, tileSize / 2));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
            	
            JButton btn = cells[r][c];
            if (btn == null) continue;

            int x = startX + c * (cell + gap);
            int y = startY + r * (cell + gap);

            btn.setBounds(x, y, cell, cell);
            btn.setFont(cellFont);

            if (!revealed[r][c] && !gameOver) {
                applyHiddenAppearance(btn);
             } else {
                if (grid.isBombAtLocation(r, c) && gameOver) {
                    applyBombAppearance(btn);
                } else {
                    applyEmptyAppearance(btn);
                }
             }
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    // Hidden state: only background image shows
    private void applyHiddenAppearance(JButton btn) {
        btn.setIcon(null);
        btn.setText("");
        btn.setEnabled(!resetLocked && !gameOver);
    }

    // Empty (safe) tile icon
    private void applyEmptyAppearance(JButton btn) {
        ImageIcon icon = getScaledIcon(emptyBase);
        btn.setIcon(icon);
    }

    // Bomb tile icon
    private void applyBombAppearance(JButton btn) {
        ImageIcon icon = getScaledIcon(bombBase);
        btn.setIcon(icon);
    }

    // Scale base image to current tile size
    private ImageIcon getScaledIcon(Image base) {
        if (base == null) return null;
        Image img = base.getScaledInstance(tileSize, tileSize, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    //  File helpers

    // Try each candidate path and return first existing file
    private File resolveFile(String... candidates) {
        for (String name : candidates) {
            File f = new File(name);
            if (f.exists() && f.isFile()) return f;
        }
        return null;
    }

    // Load ImageIcon from first existing path
    private ImageIcon loadIcon(String... candidates) {
        File f = resolveFile(candidates);
        if (f == null) return null;
        return new ImageIcon(f.getAbsolutePath());
    }

    // Load Image from first existing path
    private Image loadImage(String... candidates) {
        ImageIcon icon = loadIcon(candidates);
        if (icon == null) return null;
        return icon.getImage();
    }

    // Replace default Java window icon with a transparent one
    private void setBlankWindowIcon() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        setIconImage(img);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MinesweeperGUI();
            }
        });
    }
}
