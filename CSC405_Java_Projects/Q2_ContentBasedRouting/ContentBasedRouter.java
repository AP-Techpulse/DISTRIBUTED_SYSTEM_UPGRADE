import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

/**
 * ContentBasedRouter — Simulates Apache Camel's content-based routing.
 *
 * Camel normally reads files from disk and routes via XML/JSON rules.
 * Here we simulate that behaviour in a Swing GUI so you can see routing
 * decisions happen live without needing Maven or the Camel JAR.
 *
 * Routing Rules (mirrors the Camel RouteBuilder in the document):
 *   XML  + type="important"       → output/important
 *   XML  + other                  → output/xml
 *   JSON + priority="high"        → output/high-priority
 *   JSON + other                  → output/json
 *   Other (txt, csv …)            → output/unsupported
 */
public class ContentBasedRouter {

    // ──────────────────────────  Message Model  ──────────────────────────────

    /**
     * Represents a file/message entering the router (analogous to a Camel Exchange).
     */
    static class Message {
        final String fileName;
        final String fileType;   // xml / json / other
        final String content;    // raw content snippet

        Message(String fileName, String fileType, String content) {
            this.fileName = fileName;
            this.fileType = fileType;
            this.content  = content;
        }
    }

    // ──────────────────────────  Routing Engine  ─────────────────────────────

    /**
     * Routes a message to the correct output folder.
     * This mirrors the Camel .choice() / .when() / .otherwise() DSL.
     *
     * @return [destination, reason] pair
     */
    static String[] route(Message msg) {
        switch (msg.fileType.toLowerCase()) {
            case "xml":
                if (msg.content.contains("type='important'") ||
                    msg.content.contains("type=\"important\"")) {
                    return new String[]{"📂 output/important",
                        "XML with type='important' → routed to important queue"};
                } else {
                    return new String[]{"📂 output/xml",
                        "Regular XML file → routed to xml queue"};
                }
            case "json":
                if (msg.content.contains("\"priority\":\"high\"") ||
                    msg.content.contains("\"priority\": \"high\"")) {
                    return new String[]{"📂 output/high-priority",
                        "JSON with priority=high → routed to high-priority queue"};
                } else {
                    return new String[]{"📂 output/json",
                        "Regular JSON file → routed to json queue"};
                }
            default:
                return new String[]{"📂 output/unsupported",
                    "Unsupported file type → routed to unsupported queue"};
        }
    }

    // ──────────────────────────  Swing GUI  ──────────────────────────────────

    // Counters for each output queue
    private final Map<String, Integer> queueCounts = new LinkedHashMap<>();
    private JTextArea routingLog;
    private JPanel    queuePanel;

    // Preset demo messages
    private static final Message[] DEMO_MESSAGES = {
        new Message("order_001.xml",    "xml",  "<order type='important'><item>Laptop</item></order>"),
        new Message("order_002.xml",    "xml",  "<order type='regular'><item>Pen</item></order>"),
        new Message("payment_hq.json",  "json", "{\"id\":1,\"priority\":\"high\",\"amount\":5000}"),
        new Message("shipment.json",    "json", "{\"id\":2,\"priority\":\"low\",\"items\":3}"),
        new Message("notes.txt",        "txt",  "Random text file content here"),
        new Message("report.csv",       "csv",  "col1,col2,col3\n1,2,3"),
        new Message("urgent_order.xml", "xml",  "<order type='important'><item>Server</item></order>"),
        new Message("update.json",      "json", "{\"id\":9,\"priority\":\"high\",\"patch\":true}"),
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ContentBasedRouter().buildUI());
    }

    private void buildUI() {
        // Initialise queue counters
        for (String q : new String[]{
            "📂 output/important", "📂 output/xml",
            "📂 output/high-priority", "📂 output/json", "📂 output/unsupported"})
            queueCounts.put(q, 0);

        JFrame frame = new JFrame("🚦 CSC405 Q2 — Content-Based Routing (Apache Camel Simulation)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 640);
        frame.setLocationRelativeTo(null);

        Color dark    = new Color(30, 30, 46);
        Color accent  = new Color(137, 180, 250);

        // ── Header ─────────────────────────────────────────────────────────
        JLabel header = new JLabel("  Apache Camel — Content-Based Routing Simulator", JLabel.LEFT);
        header.setFont(new Font("Monospaced", Font.BOLD, 15));
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setBackground(new Color(24, 24, 37));
        header.setBorder(new EmptyBorder(10, 14, 10, 0));

        // ── Input Panel (left) ─────────────────────────────────────────────
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBackground(dark);
        inputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accent), " 📨 Route a Message ",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 12), accent));
        inputPanel.setPreferredSize(new Dimension(300, 0));

        // File name field
        JLabel lName = label("File Name:", accent);
        JTextField tfName = darkField();

        // File type dropdown
        JLabel lType = label("File Type:", accent);
        JComboBox<String> cbType = new JComboBox<>(new String[]{"xml", "json", "txt", "csv"});
        cbType.setBackground(new Color(24, 24, 37));
        cbType.setForeground(Color.WHITE);
        cbType.setFont(new Font("Monospaced", Font.PLAIN, 13));

        // Content area
        JLabel lContent = label("Content / Payload:", accent);
        JTextArea taContent = new JTextArea(5, 20);
        taContent.setBackground(new Color(24, 24, 37));
        taContent.setForeground(new Color(205, 214, 244));
        taContent.setFont(new Font("Monospaced", Font.PLAIN, 12));
        taContent.setLineWrap(true);
        JScrollPane contentScroll = new JScrollPane(taContent);
        contentScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Update content hint when type changes
        cbType.addActionListener(e -> {
            String type = (String) cbType.getSelectedItem();
            switch (type) {
                case "xml":  taContent.setText("<order type='important'><item>Sample</item></order>"); break;
                case "json": taContent.setText("{\"priority\":\"high\",\"id\":1}"); break;
                default:     taContent.setText("Plain content here");
            }
        });
        taContent.setText("<order type='important'><item>Sample</item></order>");

        JButton btnRoute = btn("▶  Route Message", new Color(64, 160, 112));
        btnRoute.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JButton btnDemo = btn("⚡ Run All Demo Messages", new Color(100, 80, 160));
        btnDemo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JButton btnClear = btn("🗑  Clear Log", new Color(70, 40, 40));
        btnClear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        for (Component c : new Component[]{lName, tfName, lType, cbType,
                                           lContent, contentScroll,
                                           Box.createVerticalStrut(8),
                                           btnRoute, Box.createVerticalStrut(4),
                                           btnDemo,  Box.createVerticalStrut(4),
                                           btnClear}) {
            if (c instanceof JComponent) {
                JComponent jc = (JComponent) c;
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
                jc.setMaximumSize(new Dimension(Integer.MAX_VALUE, jc.getPreferredSize().height + 4));
            }
            inputPanel.add(c);
        }

        // ── Routing Log (centre) ───────────────────────────────────────────
        routingLog = new JTextArea();
        routingLog.setEditable(false);
        routingLog.setBackground(new Color(24, 24, 37));
        routingLog.setForeground(new Color(166, 227, 161));
        routingLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        routingLog.setBorder(new EmptyBorder(5, 8, 5, 8));
        JScrollPane logScroll = new JScrollPane(routingLog);
        logScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(166, 227, 161)),
            " 🔀 Live Routing Log ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 12), new Color(166, 227, 161)));

        // ── Queue Counters (right) ─────────────────────────────────────────
        queuePanel = new JPanel();
        queuePanel.setLayout(new BoxLayout(queuePanel, BoxLayout.Y_AXIS));
        queuePanel.setBackground(dark);
        queuePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(249, 226, 175)),
            " 📊 Queue Counts ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 12), new Color(249, 226, 175)));
        queuePanel.setPreferredSize(new Dimension(230, 0));
        refreshQueuePanel();

        // ── Button Actions ─────────────────────────────────────────────────
        btnRoute.addActionListener(e -> {
            String name    = tfName.getText().trim();
            String type    = (String) cbType.getSelectedItem();
            String content = taContent.getText().trim();
            if (name.isEmpty()) { tfName.setText("message.xml"); return; }
            routeAndLog(new Message(name, type, content), 0);
        });

        btnDemo.addActionListener(e -> {
            appendLog("═".repeat(50));
            appendLog("⚡ Running " + DEMO_MESSAGES.length + " demo messages...");
            appendLog("═".repeat(50));
            // Small delay between messages to make routing look "live"
            javax.swing.Timer timer = new javax.swing.Timer(600, null);
            final int[] idx = {0};
            timer.addActionListener(te -> {
                if (idx[0] < DEMO_MESSAGES.length) {
                    routeAndLog(DEMO_MESSAGES[idx[0]++], 0);
                } else {
                    timer.stop();
                    appendLog("\n✅ All demo messages routed successfully.");
                }
            });
            timer.start();
        });

        btnClear.addActionListener(e -> {
            routingLog.setText("");
            queueCounts.replaceAll((k, v) -> 0);
            refreshQueuePanel();
        });

        // ── Final Layout ───────────────────────────────────────────────────
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            inputPanel, logScroll);
        leftSplit.setDividerLocation(310);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            leftSplit, queuePanel);
        mainSplit.setDividerLocation(650);

        frame.setBackground(dark);
        frame.add(header, BorderLayout.NORTH);
        frame.add(mainSplit, BorderLayout.CENTER);
        frame.setVisible(true);

        appendLog("🚀 Router ready. Submit a message or run all demo messages.");
    }

    // ──────────────────────────  Routing + Log  ───────────────────────────────

    private void routeAndLog(Message msg, int delayMs) {
        String[] result      = route(msg);
        String destination   = result[0];
        String reason        = result[1];

        queueCounts.merge(destination, 1, Integer::sum);

        appendLog(String.format(
            "📄 File    : %s  [%s]\n" +
            "   Content : %.60s%s\n" +
            "   Decision: %s\n" +
            "   → %s\n" +
            "%s",
            msg.fileName, msg.fileType.toUpperCase(),
            msg.content, msg.content.length() > 60 ? "…" : "",
            reason,
            destination,
            "─".repeat(50)
        ));

        refreshQueuePanel();
    }

    private void appendLog(String text) {
        routingLog.append(text + "\n");
        routingLog.setCaretPosition(routingLog.getDocument().getLength());
    }

    private void refreshQueuePanel() {
        queuePanel.removeAll();
        queuePanel.add(Box.createVerticalStrut(8));

        Color[] barColors = {
            new Color(243, 139, 168),   // important  — pink
            new Color(137, 180, 250),   // xml        — blue
            new Color(166, 227, 161),   // high-prio  — green
            new Color(249, 226, 175),   // json       — yellow
            new Color(180, 180, 200),   // unsupported — grey
        };
        int ci = 0;
        for (Map.Entry<String, Integer> e : queueCounts.entrySet()) {
            Color c = barColors[ci++ % barColors.length];
            JLabel lbl = new JLabel("  " + e.getKey().replace("📂 output/","") + ": " + e.getValue());
            lbl.setForeground(c);
            lbl.setFont(new Font("Monospaced", Font.BOLD, 13));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Simple bar
            int barW = Math.min(e.getValue() * 18, 180);
            JPanel bar = new JPanel();
            bar.setBackground(c);
            bar.setPreferredSize(new Dimension(barW + 4, 10));
            bar.setMaximumSize(new Dimension(200, 10));
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);

            queuePanel.add(lbl);
            queuePanel.add(Box.createVerticalStrut(2));
            queuePanel.add(bar);
            queuePanel.add(Box.createVerticalStrut(10));
        }
        queuePanel.revalidate();
        queuePanel.repaint();
    }

    // ──────────────────────────  Helper Builders  ─────────────────────────────

    private JLabel label(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(new Font("Dialog", Font.BOLD, 12));
        return l;
    }

    private JTextField darkField() {
        JTextField f = new JTextField();
        f.setBackground(new Color(24, 24, 37));
        f.setForeground(new Color(205, 214, 244));
        f.setCaretColor(Color.WHITE);
        f.setFont(new Font("Monospaced", Font.PLAIN, 13));
        return f;
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Dialog", Font.BOLD, 12));
        b.setBorder(new EmptyBorder(7, 10, 7, 10));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }
}
