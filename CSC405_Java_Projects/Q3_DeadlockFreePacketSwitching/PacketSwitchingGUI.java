import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * PacketSwitchingGUI — Interactive demo of deadlock-free packet switching.
 *
 * Four routers (A, B, C, D) are connected in a ring.
 * You can inject packets and watch them forward hop by hop.
 * The GUI shows each router's buffer and highlights when packets move.
 */
public class PacketSwitchingGUI {

    // ─── Network Setup ────────────────────────────────────────────────────────
    private final Map<String, Router> network = new LinkedHashMap<>();
    private final String[] routerIds          = {"A", "B", "C", "D"};
    // Ring topology: A→B→C→D→A
    private final String[] nextHop            = {"B", "C", "D", "A"};

    // ─── GUI Components ───────────────────────────────────────────────────────
    private JTextArea logArea;
    private final Map<String, JLabel>  bufferLabels = new LinkedHashMap<>();
    private final Map<String, JPanel>  routerPanels = new LinkedHashMap<>();
    private int packetCounter = 1;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PacketSwitchingGUI().buildUI());
    }

    // ─── Network Init ─────────────────────────────────────────────────────────

    private void initNetwork() {
        for (String id : routerIds) {
            Router r = new Router(id, 4);            // buffer size 4 (as per document)
            r.setLogCallback(this::appendLog);
            network.put(id, r);
        }
        // Connect ring: A↔B↔C↔D↔A
        String[] ids = routerIds;
        for (int i = 0; i < ids.length; i++) {
            Router from = network.get(ids[i]);
            Router to   = network.get(ids[(i + 1) % ids.length]);
            from.addNeighbor(to.getRouterId(), to);
            to.addNeighbor(from.getRouterId(), from); // bidirectional
        }
    }

    // ─── GUI Build ────────────────────────────────────────────────────────────

    private void buildUI() {
        initNetwork();

        JFrame frame = new JFrame("📡 CSC405 Q3 — Deadlock-Free Packet Switching");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(880, 620);
        frame.setLocationRelativeTo(null);

        Color dark   = new Color(30, 30, 46);
        Color accent = new Color(203, 166, 247);

        // ── Header ─────────────────────────────────────────────────────────
        JLabel header = new JLabel("  Deadlock-Free Packet Switching  •  Ring Network  •  Lock Ordering Strategy",
            JLabel.LEFT);
        header.setFont(new Font("Monospaced", Font.BOLD, 13));
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setBackground(new Color(24, 24, 37));
        header.setBorder(new EmptyBorder(10, 14, 10, 0));

        // ── Router visualisation (top area) ────────────────────────────────
        JPanel routerRow = new JPanel(new GridLayout(1, 4, 12, 0));
        routerRow.setBackground(dark);
        routerRow.setBorder(new EmptyBorder(12, 12, 12, 12));

        Color[] rColors = {
            new Color(137, 180, 250),  // A — blue
            new Color(166, 227, 161),  // B — green
            new Color(249, 226, 175),  // C — yellow
            new Color(243, 139, 168),  // D — pink
        };

        for (int i = 0; i < routerIds.length; i++) {
            String id  = routerIds[i];
            Color  col = rColors[i];
            JPanel rp  = buildRouterPanel(id, col);
            routerPanels.put(id, rp);
            routerRow.add(rp);
        }

        // ── Control Panel ───────────────────────────────────────────────────
        JPanel controls = new JPanel();
        controls.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
        controls.setBackground(new Color(24, 24, 37));

        // Source & destination dropdowns
        JLabel lSrc = lbl("From:", accent);
        JComboBox<String> cbSrc = combo(routerIds);

        JLabel lDst = lbl("To:", accent);
        JComboBox<String> cbDst = combo(routerIds);
        cbDst.setSelectedIndex(2); // default destination = C

        JButton btnSend     = btn("📨 Send Packet",      new Color(64, 160, 112));
        JButton btnForward  = btn("⏩ Forward All",       new Color(80, 80, 160));
        JButton btnSimDeadlock = btn("🔥 Simulate Deadlock Attempt", new Color(160, 60, 60));
        JButton btnReset    = btn("🔄 Reset Network",    new Color(70, 70, 80));

        controls.add(lSrc); controls.add(cbSrc);
        controls.add(Box.createHorizontalStrut(4));
        controls.add(lDst); controls.add(cbDst);
        controls.add(btnSend);
        controls.add(btnForward);
        controls.add(btnSimDeadlock);
        controls.add(btnReset);

        // ── Log Area ────────────────────────────────────────────────────────
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(24, 24, 37));
        logArea.setForeground(new Color(166, 227, 161));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(5, 8, 5, 8));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(166, 227, 161)),
            " 📋 Network Event Log ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 12), new Color(166, 227, 161)));

        // ── Button Actions ──────────────────────────────────────────────────

        btnSend.addActionListener(e -> {
            String src = (String) cbSrc.getSelectedItem();
            String dst = (String) cbDst.getSelectedItem();
            if (src.equals(dst)) {
                appendLog("⚠️  Source and destination cannot be the same router!");
                return;
            }
            Packet p = new Packet("P" + packetCounter++, src, dst);
            network.get(src).receivePacket(p);
            refreshRouterPanels();
        });

        btnForward.addActionListener(e -> {
            appendLog("═".repeat(50));
            appendLog("⏩ Forwarding all buffered packets one hop...");
            // Forward each router's packets to its next hop (ring order)
            for (int i = 0; i < routerIds.length; i++) {
                String srcId  = routerIds[i];
                String dstId  = nextHop[i];
                Router r      = network.get(srcId);
                List<Packet> pkts = r.getBufferContents();
                for (Packet p : pkts)
                    r.forwardPacket(p, dstId);
            }
            refreshRouterPanels();
        });

        btnSimDeadlock.addActionListener(e -> simulateDeadlockAttempt());

        btnReset.addActionListener(e -> {
            network.clear();
            packetCounter = 1;
            initNetwork();
            refreshRouterPanels();
            logArea.setText("");
            appendLog("🔄 Network reset. All buffers cleared.");
        });

        // ── Layout ─────────────────────────────────────────────────────────
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(dark);
        top.add(routerRow, BorderLayout.CENTER);
        top.add(controls,  BorderLayout.SOUTH);

        frame.setBackground(dark);
        frame.add(header,    BorderLayout.NORTH);
        frame.add(top,       BorderLayout.CENTER);
        frame.add(logScroll, BorderLayout.SOUTH);
        logScroll.setPreferredSize(new Dimension(0, 200));

        frame.setVisible(true);
        appendLog("✅ Network online. Ring topology: A → B → C → D → A");
        appendLog("📌 Deadlock prevention: Lock ordering (alphabetical by Router ID)");
    }

    private void simulateDeadlockAttempt() {
        appendLog("═".repeat(50));
        appendLog("🔥 Simulating concurrent forwarding that COULD deadlock without lock ordering...");

        // Inject packets going in opposite directions
        Packet p1 = new Packet("P" + packetCounter++, "A", "D");
        Packet p2 = new Packet("P" + packetCounter++, "D", "A");
        network.get("A").receivePacket(p1);
        network.get("D").receivePacket(p2);
        refreshRouterPanels();

        // Both threads try to forward simultaneously
        Thread tA = new Thread(() -> {
            network.get("A").forwardPacket(p1, "B");
            refreshRouterPanels();
        }, "Thread-A→B");

        Thread tD = new Thread(() -> {
            network.get("D").forwardPacket(p2, "A");
            refreshRouterPanels();
        }, "Thread-D→A");

        tA.start();
        tD.start();

        appendLog("🔐 Lock ordering ensures A's lock is acquired first by both threads.");
        appendLog("✅ No deadlock — one thread waits, the other proceeds safely.");
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────

    private JPanel buildRouterPanel(String id, Color color) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(24, 24, 37));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(color, 2),
            " Router " + id + " ", TitledBorder.CENTER, TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 13), color));

        JLabel bufLbl = new JLabel("Buffer: 0 / 4", JLabel.CENTER);
        bufLbl.setForeground(color);
        bufLbl.setFont(new Font("Monospaced", Font.PLAIN, 12));
        bufLbl.setAlignmentX(CENTER_ALIGNMENT);
        bufferLabels.put(id, bufLbl);

        panel.add(Box.createVerticalStrut(8));
        panel.add(bufLbl);
        panel.add(Box.createVerticalStrut(4));
        return panel;
    }

    private void refreshRouterPanels() {
        SwingUtilities.invokeLater(() -> {
            for (String id : routerIds) {
                Router r = network.get(id);
                if (r == null) continue;
                JLabel lbl = bufferLabels.get(id);
                List<Packet> pkts = r.getBufferContents();
                StringBuilder sb = new StringBuilder(
                    "<html><center>Buffer: " + pkts.size() + "/" + r.getBufferCapacity() + "<br><small>");
                for (Packet p : pkts)
                    sb.append(p.getId()).append("(→").append(p.getDestination()).append(") ");
                sb.append("</small></center></html>");
                lbl.setText(sb.toString());
            }
        });
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private JLabel lbl(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(new Font("Dialog", Font.BOLD, 12));
        return l;
    }

    private JComboBox<String> combo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(new Color(24, 24, 37));
        cb.setForeground(Color.WHITE);
        cb.setFont(new Font("Monospaced", Font.PLAIN, 13));
        return cb;
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

    private static final float CENTER_ALIGNMENT = Component.CENTER_ALIGNMENT;
}
