package org.deidentifier.arx.clustering;

public class TassaStatistics {

    /** TODO*/
    private long recordsMoved;
    /** TODO*/
    private long clustersSplit;
    /** TODO*/
    private long clustersMerged;
    /** TODO*/
    private double initialInformationLoss;
    /** TODO*/
    private double finalInformationLoss;
    /** TODO*/
    private int numberOfClusters;
    /** TODO*/
    private long executionTime;
    
    /**
     * Creates a new instance
     */
    TassaStatistics() {
        // Empty by design
    }

    /**
     * TODO
     * @return
     */
    public long getClustersMerged() {
        return clustersMerged;
    }

    /**
     * TODO
     * @return
     */
    public long getClustersSplit() {
        return clustersSplit;
    }

    /**
     * TODO
     * @return
     */
    public long getExecutionTime() {
        return executionTime;
    }

    /**
     * TODO
     * @return
     */
    public double getFinalInformationLoss() {
        return finalInformationLoss;
    }

    /**
     * TODO
     * @return
     */
    public double getInitialInformationLoss() {
        return initialInformationLoss;
    }

    /**
     * TODO
     * @return
     */
    public int getNumberOfClusters() {
        return numberOfClusters;
    }

    /**
     * TODO
     * @return
     */
    public long getRecordsMoved() {
        return recordsMoved;
    }

    /**
     * TODO
     */
    void incClustersMerged() {
        this.clustersMerged++;
    }

    /**
     * TODO
     */
    void incClustersSplit() {
        this.clustersSplit++;
    }

    /**
     * TODO
     */
    void incRecordsMoved() {
        this.recordsMoved++;
    }

    /**
     * TODO
     */
    void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    /**
     * TODO
     */
    void setFinalInformationLoss(double finalInformationLoss) {
        this.finalInformationLoss = finalInformationLoss;
    }

    /**
     * TODO
     */
    void setInitialInformationLoss(double initialInformationLoss) {
        this.initialInformationLoss = initialInformationLoss;
    }

    /**
     * TODO
     */
    void setNumberOfClusters(int numberOfClusters) {
        this.numberOfClusters = numberOfClusters;
    }

    @Override
    public String toString() {
        return "TassaStatistics [recordsMoved=" + recordsMoved + ", clustersSplit=" +
               clustersSplit + ", clustersMerged=" + clustersMerged + ", initialInformationLoss=" +
               initialInformationLoss + ", finalInformationLoss=" + finalInformationLoss +
               ", numberOfClusters=" + numberOfClusters + ", executionTime=" + executionTime + "]";
    }

    /**
     * Merge with another instance
     * @param other
     */
    public void merge(TassaStatistics other) {
        this.finalInformationLoss = other.finalInformationLoss;
        this.recordsMoved += other.recordsMoved;
        this.clustersMerged += other.clustersMerged;
        this.clustersSplit += other.clustersSplit;
        this.numberOfClusters = other.numberOfClusters;
        this.executionTime += other.executionTime;
    }
}
