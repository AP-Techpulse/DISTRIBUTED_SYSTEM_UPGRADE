import java.rmi.Naming;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * RMIClient — Interactive GUI client for the CSC405 RMI server.
 *
 * Launch up to 4 instances. Each gets its own coloured window.
 * Usage: java RMIClient <ClientName>  e.g. java RMIClient Alice
 *
 * Features:
 *  1. View shared server resources in real time
 *  2. Add a new shared resource
 *  3. Generate a personality profile (calls remote method)
 */
public class RMIClient {

    // Colours for each client window — makes it easy to tell them apart
    private static final Color[] WINDOW_COLORS = {
        new Color(49, 116, 143),   // Client 1 — teal
        new Color(143, 67, 49),    // Client 2 — rust
        new Color(67, 143, 49),    // Client 3 — green
        new Color(116, 49, 143)    // Client 4 — purple
    };

    private final String clientName;
    private ServerInterface server;
    private JTextArea outputArea;
    private Color themeColor;

    public RMIClient(String clientName, Color theme) {
        this.clientName = clientName;
        this.themeColor = theme;
    }

    // ───────────────────────────  Connect to Server  ─────────────────────────

    private boolean connect() {
        try {
            server = (ServerInterface) Naming.lookup("rmi://localhost/RMIServer");
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Could not connect to RMI Server.\nMake sure RMIServer is running first!\n\n" + e.getMessage(),
                "Connection Failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // ───────────────────────────  Swing GUI  ─────────────────────────────────

    private void buildUI() {
        JFrame frame = new JFrame("👤 Client: " + clientName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 620);
        frame.setLocationRelativeTo(null);

        Color dark = new Color(24, 24, 37);
        Color accent = themeColor;

        // ── Top bar ──────────────────────────────────────────────────────────
        JLabel header = new JLabel("  Client: " + clientName + " — Connected to RMI Server", JLabel.LEFT);
        header.setFont(new Font("Monospaced", Font.BOLD, 14));
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setBackground(accent);
        header.setBorder(new EmptyBorder(10, 12, 10, 0));

        // ── Output area ───────────────────────────────────────────────────────
        outputArea = new JTextArea("Welcome, " + clientName + "! Choose an action below.\n\n");
        outputArea.setEditable(false);
        outputArea.setBackground(dark);
        outputArea.setForeground(new Color(205, 214, 244));
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBorder(BorderFactory.createLineBorder(accent, 2));

        // ── Button panel ──────────────────────────────────────────────────────
        JPanel buttons = new JPanel(new GridLayout(1, 3, 10, 0));
        buttons.setBackground(dark);
        buttons.setBorder(new EmptyBorder(8, 8, 8, 8));

        JButton btnResources    = makeBtn("📁 View Resources",    accent);
        JButton btnAddResource  = makeBtn("➕ Add Resource",      new Color(40, 40, 60));
        JButton btnPersonality  = makeBtn("🌟 Personality Test",  accent);

        buttons.add(btnResources);
        buttons.add(btnAddResource);
        buttons.add(btnPersonality);

        // ── Action: View Resources ────────────────────────────────────────────
        btnResources.addActionListener(e -> {
            try {
                List<String> resources = server.getSharedResources();
                StringBuilder sb = new StringBuilder("📁 SHARED RESOURCES (live from server):\n");
                sb.append("─".repeat(40)).append("\n");
                for (String r : resources)
                    sb.append("  ").append(r).append("\n");
                sb.append("─".repeat(40)).append("\n");
                output(sb.toString());
            } catch (Exception ex) {
                output("❌ Error: " + ex.getMessage());
            }
        });

        // ── Action: Add Resource ──────────────────────────────────────────────
        btnAddResource.addActionListener(e -> {
            String res = JOptionPane.showInputDialog(frame,
                "Enter resource name to share:", "Add Shared Resource",
                JOptionPane.PLAIN_MESSAGE);
            if (res != null && !res.trim().isEmpty()) {
                try {
                    server.addSharedResource("📌 " + res.trim() + " [by " + clientName + "]");
                    output("✅ Resource added to server: " + res);
                } catch (Exception ex) {
                    output("❌ Error: " + ex.getMessage());
                }
            }
        });

        // ── Action: Personality Generator ────────────────────────────────────
        btnPersonality.addActionListener(e -> showPersonalityForm(frame));

        // ── Layout ───────────────────────────────────────────────────────────
        JPanel center = new JPanel(new BorderLayout(0, 5));
        center.setBackground(dark);
        center.setBorder(new EmptyBorder(8, 8, 0, 8));
        center.add(scroll, BorderLayout.CENTER);
        center.add(buttons, BorderLayout.SOUTH);

        frame.setBackground(dark);
        frame.add(header, BorderLayout.NORTH);
        frame.add(center, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    // ── Personality input form ────────────────────────────────────────────────

    private void showPersonalityForm(JFrame parent) {
        JDialog dialog = new JDialog(parent, "🌟 Personality Generator", true);
        dialog.setSize(380, 320);
        dialog.setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(30, 30, 46));

        Color fg = new Color(205, 214, 244);

        JTextField tfName   = addFormRow(panel, "Full Name:", fg);
        JTextField tfAge    = addFormRow(panel, "Age:", fg);
        JTextField tfHobby  = addFormRow(panel, "Hobbies:", fg);
        JTextField tfColor  = addFormRow(panel, "Fav Colour:", fg);

        JButton submit = makeBtn("Generate ✨", themeColor);
        JButton cancel = makeBtn("Cancel", new Color(50, 50, 70));

        panel.add(cancel);
        panel.add(submit);

        submit.addActionListener(e -> {
            try {
                int age = Integer.parseInt(tfAge.getText().trim());
                String result = server.generatePersonality(
                    clientName,
                    tfName.getText().trim(),
                    age,
                    tfHobby.getText().trim(),
                    tfColor.getText().trim()
                );
                output(result);
                dialog.dispose();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "Age must be a number!", "Input Error",
                    JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                output("❌ Error: " + ex.getMessage());
                dialog.dispose();
            }
        });

        cancel.addActionListener(e -> dialog.dispose());
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private JTextField addFormRow(JPanel panel, String label, Color fg) {
        JLabel lbl = new JLabel(label);
        lbl.setForeground(fg);
        lbl.setFont(new Font("Dialog", Font.BOLD, 12));
        JTextField field = new JTextField();
        field.setBackground(new Color(24, 24, 37));
        field.setForeground(fg);
        field.setCaretColor(fg);
        panel.add(lbl);
        panel.add(field);
        return field;
    }

    private JButton makeBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Dialog", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(8, 12, 8, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void output(String text) {
        outputArea.append(text + "\n\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    // ─────────────────────────────  Main  ────────────────────────────────────

    public static void main(String[] args) {
        // Get client name from arguments (default to "Client1")
        String name = (args.length > 0) ? args[0] : "Client1";

        // Pick a theme colour based on the client name hash
        int idx = Math.abs(name.hashCode()) % WINDOW_COLORS.length;
        Color theme = WINDOW_COLORS[idx];

        SwingUtilities.invokeLater(() -> {
            RMIClient client = new RMIClient(name, theme);
            if (client.connect())
                client.buildUI();
        });
    }
}
