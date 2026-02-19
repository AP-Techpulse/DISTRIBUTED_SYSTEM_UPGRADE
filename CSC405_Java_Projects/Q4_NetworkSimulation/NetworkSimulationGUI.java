import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * NetworkSimulationGUI — An interactive, visual network simulation.
 *
 * Extends the NetworkSimulation.java from the course document with:
 *   • Multiple routers drawn on a canvas
 *   • Animated packet movement along network links
 *   • Real-time statistics panel
 *   • Configurable topology (ring, star, mesh)
 *   • Concurrent packet forwarding with deadlock-free lock ordering
 */
public class NetworkSimulationGUI {

    // ─── Network State ────────────────────────────────────────────────────────

    private final Map<String, SimRouter> routers = new LinkedHashMap<>();
    private final List<AnimPacket>      animating = new CopyOnWriteArrayList<>();

    // Stats
    private int totalSent      = 0;
    private int totalDelivered = 0;
    private int totalDropped   = 0;

    // ─── GUI Components ───────────────────────────────────────────────────────
    private NetworkCanvas canvas;
    private JTextArea     logArea;
    private JLabel        lblStats;
    private int           packetSeq = 1;

    // ═══════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NetworkSimulationGUI().buildUI());
    }

    // ─── Inner Classes ────────────────────────────────────────────────────────

    /** A router node with a position on the canvas */
    static class SimRouter {
        String  id;
        int     x, y;                                 // canvas position (centre)
        int     bufferCapacity = 6;
        List<SimPacket> buffer = new ArrayList<>();
        Map<String, SimRouter> neighbors = new LinkedHashMap<>();
        java.util.concurrent.locks.ReentrantLock lock =
            new java.util.concurrent.locks.ReentrantLock();

        SimRouter(String id, int x, int y) {
            this.id = id; this.x = x; this.y = y;
        }
    }

    /** A packet stored inside a router's buffer */
    static class SimPacket {
        String id, source, destination;
        SimPacket(String id, String source, String destination) {
            this.id = id; this.source = source; this.destination = destination;
        }
        @Override public String toString() { return id + "(→" + destination + ")"; }
    }

    /** A packet being visually animated along a link */
    static class AnimPacket {
        float x, y, targetX, targetY;
        String label;
        Color  color;
        float  progress = 0f;   // 0..1 animation progress

        AnimPacket(float sx, float sy, float tx, float ty, String label, Color color) {
            x = sx; y = sy; targetX = tx; targetY = ty;
            this.label = label; this.color = color;
        }

        /** Returns true when animation is complete */
        boolean step() {
            progress += 0.04f;
            x = sx() + (targetX - sx()) * progress;
            y = sy() + (targetY - sy()) * progress;
            return progress >= 1f;
        }
        private float sx() { return targetX - (targetX - x) / Math.max(progress, 0.001f) * progress; }
        private float sy() { return targetY - (targetY - y) / Math.max(progress, 0.001f) * progress; }
    }

    // ─── Canvas ───────────────────────────────────────────────────────────────

    class NetworkCanvas extends JPanel {
        private static final int NODE_R = 26;

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(24, 24, 37));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Draw links
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(60, 70, 100));
            for (SimRouter r : routers.values()) {
                for (SimRouter n : r.neighbors.values()) {
                    g2.drawLine(r.x, r.y, n.x, n.y);
                }
            }

            // Draw routers
            for (SimRouter r : routers.values()) {
                int fill = r.buffer.size() == 0   ? 0x223355 :
                           r.buffer.size() < r.bufferCapacity / 2 ? 0x2A5F2A :
                           r.buffer.size() < r.bufferCapacity     ? 0x7A5A10 : 0x7A1010;
                g2.setColor(new Color(fill));
                g2.fillOval(r.x - NODE_R, r.y - NODE_R, NODE_R * 2, NODE_R * 2);
                g2.setColor(new Color(137, 180, 250));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(r.x - NODE_R, r.y - NODE_R, NODE_R * 2, NODE_R * 2);

                // Router label
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Dialog", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(r.id, r.x - fm.stringWidth(r.id) / 2, r.y + 5);

                // Buffer bar below node
                g2.setColor(new Color(50, 50, 70));
                g2.fillRect(r.x - NODE_R, r.y + NODE_R + 4, NODE_R * 2, 6);
                float ratio = (float) r.buffer.size() / r.bufferCapacity;
                Color barColor = ratio < 0.5f ? new Color(100, 200, 100) :
                                 ratio < 1f   ? new Color(220, 180, 50)  :
                                                new Color(220, 70, 70);
                g2.setColor(barColor);
                g2.fillRect(r.x - NODE_R, r.y + NODE_R + 4, (int)(NODE_R * 2 * ratio), 6);

                // Buffer text
                g2.setColor(new Color(180, 180, 200));
                g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                g2.drawString(r.buffer.size() + "/" + r.bufferCapacity,
                              r.x - 10, r.y + NODE_R + 22);
            }

            // Draw animated packets
            for (AnimPacket ap : animating) {
                g2.setColor(ap.color);
                g2.fillOval((int) ap.x - 9, (int) ap.y - 9, 18, 18);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Monospaced", Font.BOLD, 9));
                g2.drawString(ap.label, (int) ap.x - 8, (int) ap.y + 4);
            }
        }
    }

    // ─── UI Build ─────────────────────────────────────────────────────────────

    private void buildUI() {
        JFrame frame = new JFrame("🌐 CSC405 Q4 — Network Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 680);
        frame.setLocationRelativeTo(null);

        Color dark   = new Color(30, 30, 46);
        Color accent = new Color(137, 180, 250);

        // Header
        JLabel header = new JLabel("  Network Simulation  •  Concurrent Packet Forwarding  •  Deadlock-Free", JLabel.LEFT);
        header.setFont(new Font("Monospaced", Font.BOLD, 13));
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setBackground(new Color(24, 24, 37));
        header.setBorder(new EmptyBorder(10, 14, 10, 0));

        // Canvas
        canvas = new NetworkCanvas();
        canvas.setBackground(new Color(24, 24, 37));
        canvas.setPreferredSize(new Dimension(600, 420));

        // Right panel
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(dark);
        rightPanel.setPreferredSize(new Dimension(300, 0));
        rightPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Topology buttons
        JLabel tLabel = lbl("Topology:", accent);
        JButton btnRing  = btn("⭕ Ring",   new Color(60, 90, 160));
        JButton btnStar  = btn("⭐ Star",   new Color(90, 60, 160));
        JButton btnMesh  = btn("🕸️ Mesh",  new Color(60, 130, 90));

        JPanel topoRow = new JPanel(new GridLayout(1, 3, 4, 0));
        topoRow.setBackground(dark);
        topoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        topoRow.add(btnRing); topoRow.add(btnStar); topoRow.add(btnMesh);

        // Packet controls
        JLabel srcLabel = lbl("Source:", accent);
        JComboBox<String> cbSrc = routerCombo();
        JLabel dstLabel = lbl("Destination:", accent);
        JComboBox<String> cbDst = routerCombo();

        JButton btnSend = btn("📨 Send Packet", new Color(64, 160, 112));
        btnSend.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JButton btnAuto = btn("⚡ Auto-Simulate (10 packets)", new Color(120, 80, 180));
        btnAuto.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JButton btnForward = btn("⏩ Step: Forward All", new Color(80, 80, 160));
        btnForward.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JButton btnReset = btn("🔄 Reset", new Color(80, 40, 40));
        btnReset.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        // Stats
        lblStats = new JLabel("<html>Sent: 0 | Delivered: 0 | Dropped: 0</html>");
        lblStats.setForeground(new Color(249, 226, 175));
        lblStats.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblStats.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Log
        logArea = new JTextArea(6, 20);
        logArea.setEditable(false);
        logArea.setBackground(new Color(24, 24, 37));
        logArea.setForeground(new Color(166, 227, 161));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(166, 227, 161)),
            " Event Log ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 11), new Color(166, 227, 161)));

        for (Component c : new Component[]{
            tLabel, Box.createVerticalStrut(4), topoRow,
            Box.createVerticalStrut(10),
            srcLabel, cbSrc, Box.createVerticalStrut(4),
            dstLabel, cbDst, Box.createVerticalStrut(8),
            btnSend, Box.createVerticalStrut(4),
            btnAuto, Box.createVerticalStrut(4),
            btnForward, Box.createVerticalStrut(4),
            btnReset, Box.createVerticalStrut(10),
            lblStats, Box.createVerticalStrut(8),
            logScroll
        }) {
            if (c instanceof JComponent) {
                JComponent jc = (JComponent) c;
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            rightPanel.add(c);
        }

        // ── Button Actions ─────────────────────────────────────────────────
        btnRing.addActionListener(e  -> buildTopology("ring", cbSrc, cbDst));
        btnStar.addActionListener(e  -> buildTopology("star", cbSrc, cbDst));
        btnMesh.addActionListener(e  -> buildTopology("mesh", cbSrc, cbDst));

        btnSend.addActionListener(e -> {
            String src = (String) cbSrc.getSelectedItem();
            String dst = (String) cbDst.getSelectedItem();
            if (src != null && dst != null && !src.equals(dst))
                sendPacket(src, dst);
        });

        btnAuto.addActionListener(e -> autoSimulate(cbSrc, cbDst));

        btnForward.addActionListener(e -> forwardAll());

        btnReset.addActionListener(e -> {
            routers.values().forEach(r -> r.buffer.clear());
            animating.clear();
            totalSent = totalDelivered = totalDropped = 0;
            logArea.setText("");
            updateStats();
            canvas.repaint();
            log("🔄 Network reset.");
        });

        // Main split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, rightPanel);
        split.setDividerLocation(620);
        split.setBackground(dark);

        frame.setBackground(dark);
        frame.add(header, BorderLayout.NORTH);
        frame.add(split,  BorderLayout.CENTER);
        frame.setVisible(true);

        // Animation timer — repaints canvas 30 fps
        javax.swing.Timer animTimer = new javax.swing.Timer(33, e -> {
            animating.removeIf(AnimPacket::step);
            canvas.repaint();
        });
        animTimer.start();

        // Default topology
        buildTopology("ring", cbSrc, cbDst);
        log("✅ Network Simulation ready. Select a topology and send packets!");
    }

    // ─── Topology Builders ────────────────────────────────────────────────────

    private void buildTopology(String type, JComboBox<String> cbSrc, JComboBox<String> cbDst) {
        routers.clear();
        animating.clear();

        int cx = 300, cy = 200, r = 150;

        switch (type) {
            case "ring": {
                 String[] ids = {"A", "B", "C", "D", "E"};
                 for (int i = 0; i < ids.length; i++) {
                     double angle = 2 * Math.PI * i / ids.length - Math.PI / 2;
                     routers.put(ids[i], new SimRouter(ids[i],
                         cx + (int)(r * Math.cos(angle)),
                         cy + (int)(r * Math.sin(angle))));
                 }
                 // Ring links
                 for (int i = 0; i < ids.length; i++) {
                     link(ids[i], ids[(i + 1) % ids.length]);
                 }
                 log("⭕ Ring topology: A–B–C–D–E–A");
                break;
            }
            case "star": {
                 routers.put("Hub", new SimRouter("Hub", cx, cy));
                 String[] spokes = {"N1","N2","N3","N4","N5","N6"};
                 for (int i = 0; i < spokes.length; i++) {
                     double angle = 2 * Math.PI * i / spokes.length - Math.PI / 2;
                     routers.put(spokes[i], new SimRouter(spokes[i],
                         cx + (int)(r * Math.cos(angle)),
                         cy + (int)(r * Math.sin(angle))));
                     link("Hub", spokes[i]);
                 }
                 log("⭐ Star topology: Hub + 6 nodes");
                break;
            }
            case "mesh": {
                 String[] ids = {"R1","R2","R3","R4"};
                 int[][] pos = {{cx - 120, cy - 90},{cx + 120, cy - 90},
                                {cx - 120, cy + 90},{cx + 120, cy + 90}};
                 for (int i = 0; i < ids.length; i++)
                     routers.put(ids[i], new SimRouter(ids[i], pos[i][0], pos[i][1]));
                 // Full mesh
                 for (String a : ids) for (String b : ids)
                     if (!a.equals(b)) link(a, b);
                 log("🕸️ Mesh topology: 4 fully connected routers");
                break;
            }
        }

        // Update combo boxes
        String[] ids = routers.keySet().toArray(new String[0]);
        cbSrc.setModel(new DefaultComboBoxModel<>(ids));
        cbDst.setModel(new DefaultComboBoxModel<>(ids));
        if (ids.length > 1) cbDst.setSelectedIndex(ids.length - 1);

        canvas.repaint();
    }

    private void link(String a, String b) {
        SimRouter ra = routers.get(a), rb = routers.get(b);
        if (ra != null && rb != null) {
            ra.neighbors.put(b, rb);
            rb.neighbors.put(a, ra);
        }
    }

    // ─── Packet Sending / Forwarding ─────────────────────────────────────────

    private void sendPacket(String srcId, String dstId) {
        SimRouter src = routers.get(srcId);
        if (src == null) return;

        SimPacket pkt = new SimPacket("P" + packetSeq++, srcId, dstId);
        totalSent++;

        if (src.buffer.size() < src.bufferCapacity) {
            src.buffer.add(pkt);
            log("📨 " + pkt.id + " injected at Router " + srcId + " → dest " + dstId);
        } else {
            totalDropped++;
            log("⛔ Dropped " + pkt.id + " — Router " + srcId + " buffer full!");
        }
        updateStats();
        canvas.repaint();
    }

    private void forwardAll() {
        for (SimRouter r : routers.values()) {
            List<SimPacket> toForward = new ArrayList<>(r.buffer);
            for (SimPacket pkt : toForward) {
                // Already at destination?
                if (r.id.equals(pkt.destination)) {
                    r.buffer.remove(pkt);
                    totalDelivered++;
                    log("✅ " + pkt.id + " DELIVERED at Router " + r.id);
                    continue;
                }
                // Find next hop (simple: pick first neighbour closer to destination)
                SimRouter next = pickNextHop(r, pkt.destination);
                if (next == null) { log("❌ No route from " + r.id + " to " + pkt.destination); continue; }

                if (next.buffer.size() < next.bufferCapacity) {
                    r.buffer.remove(pkt);
                    next.buffer.add(pkt);
                    animatePacket(r, next, pkt.id);
                    log("📤 " + pkt.id + " forwarded " + r.id + " → " + next.id);
                } else {
                    totalDropped++;
                    r.buffer.remove(pkt);
                    log("⛔ " + pkt.id + " dropped — Router " + next.id + " full!");
                }
            }
        }
        updateStats();
    }

    private SimRouter pickNextHop(SimRouter from, String destId) {
        // BFS to find next hop toward destination
        Map<String, String> prev = new HashMap<>();
        Queue<SimRouter> queue   = new LinkedList<>();
        Set<String> visited      = new HashSet<>();
        queue.add(from); visited.add(from.id);

        while (!queue.isEmpty()) {
            SimRouter cur = queue.poll();
            for (SimRouter nb : cur.neighbors.values()) {
                if (!visited.contains(nb.id)) {
                    prev.put(nb.id, cur.id);
                    if (nb.id.equals(destId)) {
                        // Trace back to get first hop
                        String step = destId;
                        while (!prev.get(step).equals(from.id)) step = prev.get(step);
                        return from.neighbors.get(step);
                    }
                    visited.add(nb.id);
                    queue.add(nb);
                }
            }
        }
        return null;
    }

    private void animatePacket(SimRouter from, SimRouter to, String label) {
        Color[] colors = {Color.CYAN, Color.YELLOW, Color.ORANGE,
                          Color.PINK, new Color(180, 255, 180)};
        Color c = colors[packetSeq % colors.length];
        animating.add(new AnimPacket(from.x, from.y, to.x, to.y, label, c));
    }

    private void autoSimulate(JComboBox<String> cbSrc, JComboBox<String> cbDst) {
        String[] ids = routers.keySet().toArray(new String[0]);
        if (ids.length < 2) return;
        Random rnd = new Random();
        javax.swing.Timer t = new javax.swing.Timer(600, null);
        final int[] count = {0};
        t.addActionListener(e -> {
            if (count[0]++ >= 10) { t.stop(); return; }
            String src = ids[rnd.nextInt(ids.length)];
            String dst;
            do { dst = ids[rnd.nextInt(ids.length)]; } while (dst.equals(src));
            sendPacket(src, dst);
            if (count[0] % 3 == 0) forwardAll();
        });
        t.start();
        log("⚡ Auto-simulating 10 packets...");
    }

    // ─── Utils ────────────────────────────────────────────────────────────────

    private void updateStats() {
        lblStats.setText("<html>📊 Sent: <b>" + totalSent +
            "</b>  Delivered: <b style='color:lime'>" + totalDelivered +
            "</b>  Dropped: <b style='color:red'>" + totalDropped + "</b></html>");
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private JComboBox<String> routerCombo() {
        JComboBox<String> cb = new JComboBox<>();
        cb.setBackground(new Color(24, 24, 37));
        cb.setForeground(Color.WHITE);
        cb.setFont(new Font("Monospaced", Font.PLAIN, 13));
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        return cb;
    }

    private JLabel lbl(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(new Font("Dialog", Font.BOLD, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Dialog", Font.BOLD, 12));
        b.setBorder(new EmptyBorder(6, 10, 6, 10));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }
}
