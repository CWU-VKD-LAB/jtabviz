package src.plots;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Map;
import src.utils.ScreenshotUtils;

public class ShiftedPairedCoordinatesPlot extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private int numPlots;
    private List<Integer> selectedRows;
    private JTable table; // Reference to the table

    public ShiftedPairedCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, int numPlots, List<Integer> selectedRows, String datasetName, JTable table) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.numPlots = numPlots;
        this.selectedRows = selectedRows;
        this.table = table;

        setTitle("Shifted Paired Coordinates");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        ShiftedPairedCoordinatesPanel plotPanel = new ShiftedPairedCoordinatesPanel();
        int plotHeight = 800;
        int plotWidth = numPlots * 250;

        plotPanel.setPreferredSize(new Dimension(plotWidth, plotHeight));

        JScrollPane scrollPane = new JScrollPane(plotPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        scrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        scrollPane.getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScreenshotUtils.captureAndSaveScreenshot(scrollPane, "ShiftedPairedCoordinates", datasetName);
            }
        });

        setFocusable(true);
        requestFocusInWindow();

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(createLegendPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        legendPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        legendPanel.setBackground(Color.WHITE);

        for (Map.Entry<String, Color> entry : classColors.entrySet()) {
            String className = entry.getKey();
            Color color = entry.getValue();
            Shape shape = classShapes.get(className);

            JPanel colorLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            colorLabelPanel.setBackground(Color.WHITE);

            JLabel shapeLabel = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(color);
                    g2.translate(32, 20);
                    g2.scale(2, 2);
                    g2.fill(shape);
                }
            };
            shapeLabel.setPreferredSize(new Dimension(40, 40));

            JLabel label = new JLabel(className);
            label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));

            colorLabelPanel.add(shapeLabel);
            colorLabelPanel.add(label);

            legendPanel.add(colorLabelPanel);
        }

        return legendPanel;
    }

    private class ShiftedPairedCoordinatesPanel extends JPanel {
        private static final int TITLE_PADDING = 20;

        public ShiftedPairedCoordinatesPanel() {
            setBackground(new Color(0, 0, 0, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || attributeNames == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            String title = "Shifted Paired Coordinates Plot";
            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            int titleWidth = fm.stringWidth(title);
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString(title, (getWidth() - titleWidth) / 2, titleHeight);

            g2.setColor(new Color(0xC0C0C0));
            g2.fillRect(0, titleHeight + TITLE_PADDING, getWidth(), getHeight() - titleHeight - TITLE_PADDING);

            int plotWidth = getWidth() / numPlots;
            int plotHeight = getHeight() - titleHeight - TITLE_PADDING - 50;

            for (int i = 0; i < numPlots; i++) {
                int x = i * plotWidth;
                int attrIndex1 = i * 2;
                int attrIndex2 = (i * 2) + 1;
                if (attrIndex2 >= data.size()) {
                    attrIndex2 = attrIndex1;
                }
                drawAxesAndLabels(g2, x, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight, attributeNames.get(attrIndex1), attributeNames.get(attrIndex2));
            }

            // Draw rows in the order they are listed in the table
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i); // Use the current sort order
                drawRow(g2, row, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight);
                drawScatterPlot(g2, row, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight);
            }

            // Draw highlighted rows on top
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                if (selectedRows.contains(row)) {
                    drawHighlightedRow(g2, row, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight);
                }
            }
        }

        private void drawAxesAndLabels(Graphics2D g2, int x, int y, int width, int height, String xLabel, String yLabel) {
            int plotSize = Math.min(width, height) - 40;
            int plotX = x + 40;
            int plotY = y + 20;

            g2.setColor(Color.BLACK);
            g2.drawLine(plotX, plotY, plotX, plotY + plotSize);
            g2.drawLine(plotX, plotY + plotSize, plotX + plotSize, plotY + plotSize);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g2.setColor(Color.BLACK);
            g2.drawString(xLabel, plotX + plotSize / 2, plotY + plotSize + 20);
            g2.drawString(yLabel, plotX - g2.getFontMetrics().stringWidth(yLabel) / 2, plotY - 10);
        }

        private void drawRow(Graphics2D g2, int row, int plotY, int plotWidth, int plotHeight) {
            drawRowHelper(g2, row, plotY, plotWidth, plotHeight, false);
        }

        private void drawHighlightedRow(Graphics2D g2, int row, int plotY, int plotWidth, int plotHeight) {
            drawRowHelper(g2, row, plotY, plotWidth, plotHeight, true);
        }

        private void drawRowHelper(Graphics2D g2, int row, int plotY, int plotWidth, int plotHeight, boolean isHighlighted) {
            int numAttributes = attributeNames.size();
            String classLabel = classLabels.get(row);
            Color color = isHighlighted ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
            Stroke originalStroke = g2.getStroke();
            g2.setStroke(isHighlighted ? new BasicStroke(2) : originalStroke);
        
            for (int i = 0; i < numPlots; i++) {
                int attrIndex1 = i * 2;
                int attrIndex2 = (i * 2) + 1;
                if (attrIndex2 >= numAttributes) {
                    attrIndex2 = attrIndex1;
                }
        
                int plotX1 = i * plotWidth + 40;
                int plotSize = Math.min(plotWidth, plotHeight) - 40;
        
                double normX1 = (data.get(attrIndex1).get(row) - getMin(data.get(attrIndex1))) / (getMax(data.get(attrIndex1)) - getMin(data.get(attrIndex1)));
                double normY1 = (data.get(attrIndex2).get(row) - getMin(data.get(attrIndex2))) / (getMax(data.get(attrIndex2)) - getMin(data.get(attrIndex2)));
        
                int x1 = plotX1 + (int) (plotSize * normX1);
                int y1 = plotY + plotSize - (int) (plotSize * normY1) + 20;
        
                if (i + 1 < numPlots) {
                    int nextAttrIndex1 = (i + 1) * 2;
                    int nextAttrIndex2 = (i + 1) * 2 + 1;
                    if (nextAttrIndex2 >= numAttributes) {
                        nextAttrIndex2 = nextAttrIndex1;
                    }
        
                    int plotX2 = (i + 1) * plotWidth + 40;
        
                    double normX2 = (data.get(nextAttrIndex1).get(row) - getMin(data.get(nextAttrIndex1))) / (getMax(data.get(nextAttrIndex1)) - getMin(data.get(nextAttrIndex1)));
                    double normY2 = (data.get(nextAttrIndex2).get(row) - getMin(data.get(nextAttrIndex2))) / (getMax(data.get(nextAttrIndex2)) - getMin(data.get(nextAttrIndex2)));
        
                    int x2 = plotX2 + (int) (plotSize * normX2);
                    int y2 = plotY + plotSize - (int) (plotSize * normY2) + 20;
                    
                    g2.setColor(color);
                    g2.drawLine(x1, y1, x2, y2);
                }
            }
            g2.setStroke(originalStroke);
        }

        private void drawScatterPlot(Graphics2D g2, int row, int plotY, int plotWidth, int plotHeight) {
            for (int i = 0; i < numPlots; i++) {
                int attrIndex1 = i * 2;
                int attrIndex2 = (i * 2) + 1;
                if (attrIndex2 >= data.size()) {
                    attrIndex2 = attrIndex1;
                }

                int plotX = i * plotWidth + 40;
                int plotSize = Math.min(plotWidth, plotHeight) - 40;

                double normX = (data.get(attrIndex1).get(row) - getMin(data.get(attrIndex1))) / (getMax(data.get(attrIndex1)) - getMin(data.get(attrIndex1)));
                double normY = (data.get(attrIndex2).get(row) - getMin(data.get(attrIndex2))) / (getMax(data.get(attrIndex2)) - getMin(data.get(attrIndex2)));

                int px = plotX + (int) (plotSize * normX);
                int py = plotY + plotSize - (int) (plotSize * normY) + 20;

                String classLabel = classLabels.get(row);
                Color color = selectedRows.contains(row) ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
                Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));

                g2.setColor(color);
                g2.translate(px, py);
                g2.fill(shape);
                g2.translate(-px, -py);
            }
        }

        private double getMin(List<Double> data) {
            return data.stream().min(Double::compare).orElse(Double.NaN);
        }

        private double getMax(List<Double> data) {
            return data.stream().max(Double::compare).orElse(Double.NaN);
        }
    }
}
