import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
 
public class InvestmentOptimizer extends JFrame {
 
    private InputPanel inputPanel;
    private ResultPanel resultPanel;
 
    public InvestmentOptimizer() {
        setTitle("Multi-Stage Investment Allocation - Dynamic Programming Optimizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
 
        inputPanel = new InputPanel();
        resultPanel = new ResultPanel();
         inputPanel.setResultPanel(resultPanel);

 
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, resultPanel);
        split.setDividerLocation(480);
        split.setDividerSize(6);
        split.setBorder(null);
 
        add(split, BorderLayout.CENTER);
        setVisible(true);
    }
 
    public static void main(String[] args) {
        SwingUtilities.invokeLater(InvestmentOptimizer::new);
    }
}
 
// ─── Investment Option ────────────────────────────────────────────────────────
class InvestmentOption {
    public int stage;
    public String name;
    public int cost;
    public int returnValue;
 
    public InvestmentOption(int stage, String name, int cost, int returnValue) {
        this.stage = stage;
        this.name = name;
        this.cost = cost;
        this.returnValue = returnValue;
    }
}
 
// ─── DP Solver ────────────────────────────────────────────────────────────────
// ─── Multi Stage Graph Solver ────────────────────────────────────────────────
class DPSolver {

    public static class StageResult {
        public int stage;
        public int stageBudget;
        public InvestmentOption chosen;
        public int costUsed;
        public int returnGained;
    }

    public static List<StageResult> solve(int stages, int totalBudget,
            int[] stageBudgets, List<InvestmentOption> allOptions) {

        Map<Integer, List<InvestmentOption>> stageMap = new HashMap<>();

        for (InvestmentOption o : allOptions) {
            stageMap.computeIfAbsent(o.stage, k -> new ArrayList<>()).add(o);
        }

        int[][] dp = new int[stages + 1][totalBudget + 1];
        InvestmentOption[][] choice = new InvestmentOption[stages + 1][totalBudget + 1];

        // Multi Stage Graph DP
        for (int s = 1; s <= stages; s++) {
            List<InvestmentOption> options = stageMap.getOrDefault(s, new ArrayList<>());

            for (int b = 0; b <= totalBudget; b++) {

                dp[s][b] = dp[s - 1][b];

                for (InvestmentOption opt : options) {

                    if (opt.cost <= b && opt.cost <= stageBudgets[s - 1]) {

                        int val = dp[s - 1][b - opt.cost] + opt.returnValue;

                        if (val > dp[s][b]) {
                            dp[s][b] = val;
                            choice[s][b] = opt;
                        }
                    }
                }
            }
        }

        // Backtracking
        List<StageResult> results = new ArrayList<>();
        int b = totalBudget;

        for (int s = stages; s >= 1; s--) {

            StageResult sr = new StageResult();
            sr.stage = s;
            sr.stageBudget = stageBudgets[s - 1];

            InvestmentOption chosen = choice[s][b];

            if (chosen != null) {
                sr.chosen = chosen;
                sr.costUsed = chosen.cost;
                sr.returnGained = chosen.returnValue;
                b -= chosen.cost;
            }

            results.add(0, sr);
        }

        return results;
    }
}
 
// ─── Input Panel ──────────────────────────────────────────────────────────────
class InputPanel extends JPanel {
 
    private JSpinner stagesSpinner;
    private JSpinner totalBudgetSpinner;
    private JPanel stageBudgetPanel;
    private JSpinner[] stageBudgetSpinners = new JSpinner[0];
    private DefaultTableModel tableModel;
    private ResultPanel resultPanel;
 
    private static final Color ACCENT   = new Color(25, 118, 210);
    private static final Color BG_LIGHT = new Color(245, 247, 250);
    private static final Color BG_WHITE = Color.WHITE;
    private static final Color BORDER_C = new Color(220, 220, 225);
 
    public InputPanel() {
        setBackground(BG_LIGHT);
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(20, 20, 20, 10));
 
        add(buildTopSection(), BorderLayout.NORTH);
        add(buildOptionsSection(), BorderLayout.CENTER);
        add(buildPlanButton(), BorderLayout.SOUTH);
    }
 
    public void setResultPanel(ResultPanel rp) {
        this.resultPanel = rp;
    }
 
    private JPanel buildTopSection() {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBackground(BG_LIGHT);
 
        // --- Config card ---
        JPanel configCard = card();
        configCard.setLayout(new GridLayout(1, 2, 16, 0));
 
        stagesSpinner = spinner(3, 1, 10);
        totalBudgetSpinner = spinner(50, 1, 10000);
 
        stagesSpinner.addChangeListener(e -> rebuildStageBudgets());
 
        configCard.add(labeled("Number of stages", stagesSpinner));
        configCard.add(labeled("Total budget", totalBudgetSpinner));
        outer.add(configCard);
        outer.add(Box.createVerticalStrut(12));
 
        // --- Stage budgets card ---
        JPanel sbCard = card();
        sbCard.setLayout(new BorderLayout(0, 8));
        JLabel sbTitle = new JLabel("Budget per stage");
        sbTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sbTitle.setForeground(new Color(80, 80, 90));
        sbCard.add(sbTitle, BorderLayout.NORTH);
 
        stageBudgetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        stageBudgetPanel.setBackground(BG_WHITE);
        sbCard.add(stageBudgetPanel, BorderLayout.CENTER);
 
        outer.add(sbCard);
        outer.add(Box.createVerticalStrut(12));
 
        rebuildStageBudgets();
        return outer;
    }
 
    private void rebuildStageBudgets() {
        int n = (Integer) stagesSpinner.getValue();
        int[] prev = new int[stageBudgetSpinners.length];
        for (int i = 0; i < stageBudgetSpinners.length; i++)
            prev[i] = (Integer) stageBudgetSpinners[i].getValue();
 
        stageBudgetPanel.removeAll();
        stageBudgetSpinners = new JSpinner[n];
        for (int i = 0; i < n; i++) {
            int def = (i < prev.length) ? prev[i] : 20;
            JSpinner sp = spinner(def, 0, 10000);
            stageBudgetSpinners[i] = sp;
            JPanel lp = new JPanel(new BorderLayout(0, 3));
            lp.setBackground(BG_WHITE);
            JLabel lbl = new JLabel("Stage " + (i + 1));
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setForeground(new Color(100, 100, 110));
            lp.add(lbl, BorderLayout.NORTH);
            sp.setPreferredSize(new Dimension(72, 30));
            lp.add(sp, BorderLayout.CENTER);
            stageBudgetPanel.add(lp);
        }
        stageBudgetPanel.revalidate();
        stageBudgetPanel.repaint();
    }
 
    private JPanel buildOptionsSection() {
        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBackground(BG_LIGHT);
 
        JLabel title = new JLabel("Investment options");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(new Color(50, 50, 60));
        outer.add(title, BorderLayout.NORTH);
 
        tableModel = new DefaultTableModel(
                new String[]{"Stage", "Plan Name", "Cost", "Return"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return true; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 || c == 2 || c == 3 ? Integer.class : String.class;
            }
        };
 
        JTable table = new JTable(tableModel);
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setShowGrid(true);
        table.setGridColor(BORDER_C);
        table.setBackground(BG_WHITE);
        table.setSelectionBackground(new Color(210, 230, 255));
        table.setSelectionForeground(Color.BLACK);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(235, 238, 245));
        table.getTableHeader().setForeground(new Color(70, 70, 80));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_C));
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
 
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_C));
        scroll.setBackground(BG_WHITE);
        outer.add(scroll, BorderLayout.CENTER);
 
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnRow.setBackground(BG_LIGHT);
 
        JButton addBtn = linkButton("+ Add option");
        addBtn.addActionListener(e -> tableModel.addRow(new Object[]{1, "", 10, 20}));
 
        JButton delBtn = linkButton("Remove selected");
        delBtn.setForeground(new Color(200, 50, 50));
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) tableModel.removeRow(row);
        });
 
        JButton clrBtn = linkButton("Clear all");
        clrBtn.setForeground(new Color(150, 150, 160));
        clrBtn.addActionListener(e -> tableModel.setRowCount(0));
 
        btnRow.add(addBtn);
        btnRow.add(new JSeparator(SwingConstants.VERTICAL) {{ setPreferredSize(new Dimension(1,16)); }});
        btnRow.add(delBtn);
        btnRow.add(new JSeparator(SwingConstants.VERTICAL) {{ setPreferredSize(new Dimension(1,16)); }});
        btnRow.add(clrBtn);
        outer.add(btnRow, BorderLayout.SOUTH);
 
        return outer;
    }
 
    private JPanel buildPlanButton() {
    JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
    p.setBackground(BG_LIGHT);

    JButton btn = new JButton("Plan");
    btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
    btn.setForeground(Color.WHITE);
    btn.setBackground(ACCENT);
    btn.setFocusPainted(false);
    btn.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
    btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    btn.addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) { btn.setBackground(ACCENT.darker()); }
        public void mouseExited(MouseEvent e)  { btn.setBackground(ACCENT); }
    });
    btn.addActionListener(e -> runPlan());

    // NEW BUTTON
    JButton sampleBtn = new JButton("Load Sample Data");
    sampleBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
    sampleBtn.setForeground(Color.WHITE);
    sampleBtn.setBackground(new Color(76, 175, 80));
    sampleBtn.setFocusPainted(false);
    sampleBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    sampleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    sampleBtn.addActionListener(e -> loadSampleData());

    p.add(btn);
    p.add(Box.createHorizontalStrut(15));
    p.add(sampleBtn);

    return p;
}
 
    private void runPlan() {
        if (resultPanel == null) return;
 
        int stages = (Integer) stagesSpinner.getValue();
        int totalBudget = (Integer) totalBudgetSpinner.getValue();
 
        int[] stageBudgets = new int[stages];
        int sumSB = 0;
        for (int i = 0; i < stages; i++) {
            stageBudgets[i] = (Integer) stageBudgetSpinners[i].getValue();
            sumSB += stageBudgets[i];
        }
 
        if (sumSB > totalBudget) {
            resultPanel.showError("Stage budgets total (" + sumSB + ") exceeds total budget (" + totalBudget + ").");
            return;
        }
 
        List<InvestmentOption> opts = getOptions();
        if (opts.isEmpty()) {
            resultPanel.showError("No valid investment options found. Please add options.");
            return;
        }
 
        List<DPSolver.StageResult> results = DPSolver.solve(stages, totalBudget, stageBudgets, opts);
        resultPanel.showResults(results, totalBudget);
    }

    private void loadSampleData() {

    stagesSpinner.setValue(3);
    totalBudgetSpinner.setValue(100);

    rebuildStageBudgets();

    stageBudgetSpinners[0].setValue(30);
    stageBudgetSpinners[1].setValue(40);
    stageBudgetSpinners[2].setValue(30);

    tableModel.setRowCount(0);

    tableModel.addRow(new Object[]{1, "Stocks A", 10, 15});
    tableModel.addRow(new Object[]{1, "Bonds A", 20, 25});
    tableModel.addRow(new Object[]{1, "Crypto A", 30, 40});

    tableModel.addRow(new Object[]{2, "Stocks B", 15, 20});
    tableModel.addRow(new Object[]{2, "Bonds B", 25, 35});
    tableModel.addRow(new Object[]{2, "Crypto B", 40, 55});

    tableModel.addRow(new Object[]{3, "Stocks C", 10, 12});
    tableModel.addRow(new Object[]{3, "Bonds C", 20, 30});
    tableModel.addRow(new Object[]{3, "Crypto C", 30, 45});
}
 
    private List<InvestmentOption> getOptions() {
        List<InvestmentOption> list = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                Object sv = tableModel.getValueAt(i, 0);
                Object nv = tableModel.getValueAt(i, 1);
                Object cv = tableModel.getValueAt(i, 2);
                Object rv = tableModel.getValueAt(i, 3);
                if (sv == null || nv == null || cv == null || rv == null) continue;
                int stage = Integer.parseInt(sv.toString().trim());
                String name = nv.toString().trim();
                int cost = Integer.parseInt(cv.toString().trim());
                int ret = Integer.parseInt(rv.toString().trim());
                if (name.isEmpty()) continue;
                list.add(new InvestmentOption(stage, name, cost, ret));
            } catch (Exception ignored) {}
        }
        return list;
    }
 
    // ── helpers ──
    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(BG_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_C),
                new EmptyBorder(12, 14, 12, 14)));
        return p;
    }
 
    private JPanel labeled(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(BG_WHITE);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(new Color(100, 100, 110));
        p.add(lbl, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }
 
    private JSpinner spinner(int val, int min, int max) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        s.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);
        s.setBorder(BorderFactory.createLineBorder(BORDER_C));
        return s;
    }
 
    private JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setForeground(ACCENT);
        b.setBackground(null);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
 
// ─── Result Panel ─────────────────────────────────────────────────────────────
class ResultPanel extends JPanel {
 
    private JPanel content;
    private static final Color BG       = new Color(248, 249, 252);
    private static final Color BG_WHITE = Color.WHITE;
    private static final Color BORDER_C = new Color(220, 220, 225);
    private static final Color ACCENT   = new Color(25, 118, 210);
    private static final Color SUCCESS  = new Color(46, 160, 67);
    private static final Color MUTED    = new Color(110, 110, 120);
 
    public ResultPanel() {
        setBackground(BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 10, 20, 20));
 
        JLabel header = new JLabel("Optimal Investment Plan");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setForeground(new Color(40, 40, 50));
        header.setBorder(new EmptyBorder(0, 0, 14, 0));
        add(header, BorderLayout.NORTH);
 
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
 
        JLabel placeholder = new JLabel("Configure options and click Plan to see results.");
        placeholder.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        placeholder.setForeground(MUTED);
        placeholder.setBorder(new EmptyBorder(10, 0, 0, 0));
        content.add(placeholder);
 
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);
        add(scroll, BorderLayout.CENTER);
    }
 
    public void showError(String msg) {
        content.removeAll();
        JLabel err = new JLabel("<html><span style='color:#c0392b;'>" + msg + "</span></html>");
        err.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        err.setBorder(new EmptyBorder(8, 0, 0, 0));
        content.add(err);
        content.revalidate();
        content.repaint();
    }
 
    public void showResults(List<DPSolver.StageResult> results, int totalBudget) {
        content.removeAll();
 
        int totalReturn = 0, totalCost = 0;
        for (DPSolver.StageResult r : results) {
            totalReturn += r.returnGained;
            totalCost   += r.costUsed;
        }
 
        // Summary metrics
        JPanel metrics = new JPanel(new GridLayout(1, 3, 10, 0));
        metrics.setBackground(BG);
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        metrics.add(metricCard("Total Return", String.valueOf(totalReturn), SUCCESS));
        metrics.add(metricCard("Budget Used", totalCost + " / " + totalBudget, ACCENT));
        metrics.add(metricCard("Budget Remaining", String.valueOf(totalBudget - totalCost), MUTED));
        content.add(metrics);
        content.add(Box.createVerticalStrut(18));
 
        // Stage-by-stage results
        for (DPSolver.StageResult r : results) {
            content.add(stageBlock(r));
            content.add(Box.createVerticalStrut(10));
        }
 
        content.revalidate();
        content.repaint();
    }
 
    private JPanel metricCard(String label, String value, Color valueColor) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(BG_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_C),
                new EmptyBorder(12, 14, 12, 14)));
 
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(MUTED);
 
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 20));
        val.setForeground(valueColor);
 
        p.add(lbl, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }
 
    private JPanel stageBlock(DPSolver.StageResult r) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(BG_WHITE);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 0, BORDER_C),
                        new EmptyBorder(12, 14, 12, 14))));
 
        JLabel stageLabel = new JLabel("Stage " + r.stage + "  ->  Allocated Budget: " + r.stageBudget);
        stageLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        stageLabel.setForeground(new Color(50, 50, 60));
        p.add(stageLabel, BorderLayout.NORTH);
 
        if (r.chosen == null) {
            JLabel none = new JLabel("No suitable investment fits within the allocated budget.");
            none.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            none.setForeground(MUTED);
            p.add(none, BorderLayout.CENTER);
        } else {
            JPanel details = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            details.setBackground(BG_WHITE);
 
            details.add(detailChip("Plan",   r.chosen.name,           new Color(235, 243, 255), ACCENT));
            details.add(Box.createHorizontalStrut(10));
            details.add(detailChip("Cost",   String.valueOf(r.costUsed),    new Color(242, 242, 245), MUTED));
            details.add(Box.createHorizontalStrut(10));
            details.add(detailChip("Return", String.valueOf(r.returnGained), new Color(236, 252, 240), SUCCESS));
 
            p.add(details, BorderLayout.CENTER);
        }
        return p;
    }
 
    private JPanel detailChip(String key, String val, Color bg, Color fg) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setBackground(bg);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_C),
                new EmptyBorder(5, 10, 5, 10)));
 
        JLabel k = new JLabel(key + ": ");
        k.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        k.setForeground(MUTED);
 
        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.BOLD, 12));
        v.setForeground(fg);
 
        p.add(k, BorderLayout.WEST);
        p.add(v, BorderLayout.CENTER);
        return p;
    }
}