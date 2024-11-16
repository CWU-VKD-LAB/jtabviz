package src.plots;

import javax.swing.*;
import src.utils.ScreenshotUtils;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class LineCoordinatesPlot extends JFrame {
    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private List<Integer> selectedRows;
    private String datasetName;
    private Map<String, Double> axisScales;
    private Map<String, Boolean> axisDirections;
    private Set<String> hiddenClasses;
    private boolean showConnections = true;
    private Map<String, Point> axisPositions;
    private String draggedAxis = null;
    private int curveHeight = 50;

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final int TITLE_PADDING = 20;
    private static final int AXIS_LENGTH = 400;

    public LineCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, 
            Map<String, Color> classColors, Map<String, Shape> classShapes, 
            List<String> classLabels, List<Integer> selectedRows, String datasetName) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.datasetName = datasetName;
        this.axisScales = new HashMap<>();
        this.axisDirections = new HashMap<>();
        this.hiddenClasses = new HashSet<>();
        this.axisPositions = new HashMap<>();

        // Initialize axis properties and positions
        int startX = 100;
        int spacing = 150;
        int fixedY = 300; // Fixed Y position for all axes
        for (int i = 0; i < attributeNames.size(); i++) {
            String attr = attributeNames.get(i);
            axisScales.put(attr, 1.0);
            axisDirections.put(attr, true);
            axisPositions.put(attr, new Point(startX + i * spacing, fixedY));
        }

        setTitle("Line Coordinates Plot");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set up the main panel with vertical layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Create control panel at the top
        JPanel controlPanel = createControlPanel();
        JScrollPane controlScrollPane = new JScrollPane(controlPanel);
        controlScrollPane.setPreferredSize(new Dimension(getWidth(), 150));
        controlScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        controlScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainPanel.add(controlScrollPane, BorderLayout.NORTH);

        // Create main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        // Add the plot panel
        LineCoordinatesPanel plotPanel = new LineCoordinatesPanel();
        JScrollPane plotScrollPane = new JScrollPane(plotPanel);
        plotScrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                 .put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        plotScrollPane.getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScreenshotUtils.captureAndSaveScreenshot(plotScrollPane, "LineCoordinates", datasetName);
            }
        });

        contentPanel.add(plotScrollPane, BorderLayout.CENTER);
        contentPanel.add(createLegendPanel(), BorderLayout.SOUTH);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add curve height control
        JPanel curvePanel = new JPanel(new BorderLayout());
        curvePanel.setBorder(BorderFactory.createTitledBorder("Curve Height"));
        JSlider curveSlider = new JSlider(10, 100, 50);
        curveSlider.addChangeListener(e -> {
            curveHeight = curveSlider.getValue();
            repaint();
        });
        curvePanel.add(curveSlider);
        controlPanel.add(curvePanel);

        // Add connection toggle
        JToggleButton connectionToggle = new JToggleButton("Show Connections", true);
        connectionToggle.addActionListener(e -> {
            showConnections = connectionToggle.isSelected();
            repaint();
        });
        controlPanel.add(connectionToggle);

        // Add controls for each attribute
        for (String attr : attributeNames) {
            JPanel attrPanel = new JPanel();
            attrPanel.setBorder(BorderFactory.createTitledBorder(attr));
            
            // Scale slider
            JPanel scalePanel = new JPanel(new BorderLayout());
            scalePanel.add(new JLabel("Scale:"), BorderLayout.WEST);
            JSlider scaleSlider = new JSlider(0, 200, 100);
            scaleSlider.addChangeListener(e -> {
                axisScales.put(attr, scaleSlider.getValue() / 100.0);
                repaint();
            });
            scalePanel.add(scaleSlider);
            
            // Direction toggle
            JToggleButton directionToggle = new JToggleButton("←");
            directionToggle.addActionListener(e -> {
                axisDirections.put(attr, !directionToggle.isSelected());
                directionToggle.setText(directionToggle.isSelected() ? "→" : "←");
                repaint();
            });
            
            attrPanel.add(scalePanel);
            attrPanel.add(directionToggle);
            controlPanel.add(attrPanel);
        }
        
        return controlPanel;
    }

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        legendPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        legendPanel.setBackground(Color.WHITE);

        for (Map.Entry<String, Color> entry : classColors.entrySet()) {
            String className = entry.getKey();
            Color color = entry.getValue();
            Shape shape = classShapes.get(className);

            JPanel colorLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            colorLabelPanel.setBackground(Color.WHITE);
            colorLabelPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel shapeLabel = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(hiddenClasses.contains(className) ? Color.LIGHT_GRAY : color);
                    g2.translate(20, 20);
                    g2.scale(2, 2);
                    g2.fill(shape);
                }
            };
            shapeLabel.setPreferredSize(new Dimension(40, 40));

            JLabel label = new JLabel(className);
            label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));

            colorLabelPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (hiddenClasses.contains(className)) {
                        hiddenClasses.remove(className);
                    } else {
                        hiddenClasses.add(className);
                    }
                    repaint();
                }
            });

            colorLabelPanel.add(shapeLabel);
            colorLabelPanel.add(label);
            legendPanel.add(colorLabelPanel);
        }

        return legendPanel;
    }

    private class LineCoordinatesPanel extends JPanel {
        public LineCoordinatesPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(800, 600));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    for (String attr : attributeNames) {
                        Point pos = axisPositions.get(attr);
                        if (Math.abs(e.getY() - pos.y) < 10 && 
                            e.getX() >= pos.x && e.getX() <= pos.x + AXIS_LENGTH) {
                            draggedAxis = attr;
                            break;
                        }
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    draggedAxis = null;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedAxis != null) {
                        Point pos = axisPositions.get(draggedAxis);
                        pos.x = e.getX();
                        pos.y = e.getY();
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw title
            g2.setFont(TITLE_FONT);
            String title = "Line Coordinates Plot";
            FontMetrics fm = g2.getFontMetrics();
            int titleWidth = fm.stringWidth(title);
            g2.drawString(title, (getWidth() - titleWidth) / 2, fm.getHeight());

            // Draw horizontal axes
            g2.setFont(AXIS_LABEL_FONT);
            for (String attr : attributeNames) {
                Point pos = axisPositions.get(attr);
                g2.setColor(Color.BLACK);
                double scale = axisScales.get(attr);
                int scaledAxisLength = (int)(AXIS_LENGTH * scale);
                g2.drawLine(pos.x, pos.y, pos.x + scaledAxisLength, pos.y);
                g2.drawString(attr, pos.x + scaledAxisLength/2 - fm.stringWidth(attr)/2, pos.y + 25);
            }

            // Draw data points and connections
            for (int row = 0; row < data.get(0).size(); row++) {
                if (hiddenClasses.contains(classLabels.get(row))) {
                    continue;
                }

                Color classColor = selectedRows.contains(row) ? Color.YELLOW : classColors.get(classLabels.get(row));
                g2.setColor(classColor);

                List<Point2D.Double> points = new ArrayList<>();
                for (int i = 0; i < attributeNames.size(); i++) {
                    String attr = attributeNames.get(i);
                    double value = data.get(i).get(row);
                    double minValue = Collections.min(data.get(i));
                    double maxValue = Collections.max(data.get(i));
                    
                    double normalizedValue = (value - minValue) / (maxValue - minValue);
                    if (!axisDirections.get(attr)) {
                        normalizedValue = 1 - normalizedValue;
                    }
                    
                    double scale = axisScales.get(attr);
                    Point pos = axisPositions.get(attr);
                    double x = pos.x + normalizedValue * scale * AXIS_LENGTH;
                    points.add(new Point2D.Double(x, pos.y));
                }

                // Draw connections between points using Bezier curves
                if (showConnections) {
                    for (int i = 0; i < points.size() - 1; i++) {
                        Point2D.Double p1 = points.get(i);
                        Point2D.Double p2 = points.get(i + 1);
                        
                        // Control points for Bezier curve directly above axes
                        Point2D.Double ctrl1 = new Point2D.Double(
                            p1.x,
                            p1.y - curveHeight
                        );
                        Point2D.Double ctrl2 = new Point2D.Double(
                            p2.x,
                            p2.y - curveHeight
                        );
                        
                        CubicCurve2D curve = new CubicCurve2D.Double(
                            p1.x, p1.y, ctrl1.x, ctrl1.y,
                            ctrl2.x, ctrl2.y, p2.x, p2.y
                        );
                        g2.draw(curve);
                    }
                }

                // Draw points with scaling
                Shape shape = classShapes.get(classLabels.get(row));
                for (Point2D.Double point : points) {
                    AffineTransform transform = new AffineTransform();
                    transform.translate(point.x, point.y);
                    String attr = attributeNames.get(points.indexOf(point));
                    double scale = axisScales.get(attr);
                    transform.scale(scale, scale);
                    Shape scaledShape = transform.createTransformedShape(shape);
                    g2.fill(scaledShape);
                }
            }
        }
    }
} 