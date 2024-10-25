package src.utils;

import javax.swing.table.DefaultTableModel;
import java.util.*;

public class PureRegionUtils {
    private String attributeName;
    private double start;
    private double end;
    private String currentClass;
    private int regionCount;
    private double percentageOfClass;
    private double percentageOfDataset;

    public PureRegionUtils(String attributeName, double start, double end, String currentClass,
                      int regionCount, double percentageOfClass, double percentageOfDataset) {
        this.attributeName = attributeName;
        this.start = start;
        this.end = end;
        this.currentClass = currentClass;
        this.regionCount = regionCount;
        this.percentageOfClass = percentageOfClass;
        this.percentageOfDataset = percentageOfDataset;
    }

    public static List<PureRegionUtils> calculatePureRegions(DefaultTableModel tableModel, int thresholdPercentage, int classColumnIndex) {
        if (classColumnIndex == -1) {
            return Collections.emptyList();
        }
    
        List<PureRegionUtils> pureRegions = new ArrayList<>();
        int numColumns = tableModel.getColumnCount();
        int totalRows = tableModel.getRowCount();
    
        Map<String, Integer> classCounts = new HashMap<>();
        for (int row = 0; row < totalRows; row++) {
            String className = tableModel.getValueAt(row, classColumnIndex).toString();
            classCounts.put(className, classCounts.getOrDefault(className, 0) + 1);
        }
    
        for (int col = 0; col < numColumns; col++) {
            if (col == classColumnIndex) continue;
    
            String attributeName = tableModel.getColumnName(col);
            List<Double> values = new ArrayList<>();
            Map<Double, List<Integer>> valueToRowIndicesMap = new HashMap<>();
    
            for (int row = 0; row < totalRows; row++) {
                try {
                    double value = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                    values.add(value);
                    valueToRowIndicesMap.computeIfAbsent(value, k -> new ArrayList<>()).add(row);
                } catch (NumberFormatException e) {
                    // Skip non-numerical values
                }
            }
    
            Collections.sort(values);
    
            for (int start = 0; start < values.size(); start++) {
                String currentClass = null;
                Set<Integer> rowsInWindow = new HashSet<>();
                boolean isPure = true;
    
                for (int end = start + 1; end <= values.size(); end++) {
                    List<Integer> rowIndices = valueToRowIndicesMap.get(values.get(end - 1));
                    for (int rowIndex : rowIndices) {
                        String className = tableModel.getValueAt(rowIndex, classColumnIndex).toString();
                        if (currentClass == null) {
                            currentClass = className;
                        } else if (!currentClass.equals(className)) {
                            isPure = false;
                            break;
                        }
                        rowsInWindow.add(rowIndex);
                    }
    
                    if (!isPure) {
                        break;
                    }
    
                    if (!rowsInWindow.isEmpty()) {
                        int regionCount = rowsInWindow.size();
                        double percentageOfClass = (regionCount / (double) classCounts.get(currentClass)) * 100;
                        double percentageOfDataset = (regionCount / (double) totalRows) * 100;
                        double expandedEnd = values.get(end - 1);
    
                        PureRegionUtils region = new PureRegionUtils(
                                attributeName, values.get(start), expandedEnd,
                                currentClass, regionCount, percentageOfClass, percentageOfDataset
                        );
                        pureRegions.add(region);
                    }
                }
            }
        }
    
        return filterLargestSignificantRegions(pureRegions, thresholdPercentage);
    }

    private static List<PureRegionUtils> filterLargestSignificantRegions(List<PureRegionUtils> pureRegions, int thresholdPercentage) {
        List<PureRegionUtils> filteredRegions = new ArrayList<>();
    
        // Sort by region size (number of cases), then by range size
        pureRegions.sort(Comparator.comparingInt((PureRegionUtils region) -> region.regionCount).reversed()
                .thenComparingDouble(region -> region.end - region.start).reversed());
    
        for (PureRegionUtils regionA : pureRegions) {
            boolean isContained = false;
            for (PureRegionUtils regionB : filteredRegions) {
                // Check if regionA is entirely contained within regionB
                if (regionA.attributeName.equals(regionB.attributeName) &&
                    regionA.currentClass.equals(regionB.currentClass) &&
                    regionA.start >= regionB.start &&
                    regionA.end <= regionB.end &&
                    regionA.regionCount <= regionB.regionCount) {
                    isContained = true;
                    break;
                }
            }
            if (!isContained) {
                filteredRegions.add(regionA);
            }
        }
    
        // Apply the threshold percentage to filter out regions
        double minCoverage = thresholdPercentage;
        filteredRegions.removeIf(region -> region.percentageOfClass < minCoverage && region.percentageOfDataset < minCoverage);
    
        return filteredRegions;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    public String getCurrentClass() {
        return currentClass;
    }

    public int getRegionCount() {
        return regionCount;
    }

    public double getPercentageOfClass() {
        return percentageOfClass;
    }

    public double getPercentageOfDataset() {
        return percentageOfDataset;
    }
}
