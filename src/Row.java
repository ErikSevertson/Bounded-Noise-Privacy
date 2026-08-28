public class Row {
    int frameId;
    int vehicleId;
    double globalX;
    double globalY;
    double vel;
    double acc;
    int laneId;
    double spaceHeadway;
    double timeHeadway;

    public Row(int frameId, int vehicleId, double globalX, double globalY,
               double vel, double acc, int laneId) {
        this.frameId = frameId;
        this.vehicleId = vehicleId;
        this.globalX = globalX;
        this.globalY = globalY;
        this.vel = vel;
        this.acc = acc;
        this.laneId = laneId;
    }

    @Override
    public String toString() {
        return "Row{" +
                "frameId=" + frameId +
                ", vehicleId=" + vehicleId +
                ", globalX=" + globalX +
                ", globalY=" + globalY +
                ", vel=" + vel +
                ", acc=" + acc +
                ", laneId=" + laneId +
                '}';
    }

    public int getFrameId() {
        return frameId;
    }

    public void setFrameId(int frameId) {
        this.frameId = frameId;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public double getGlobalX() {
        return globalX;
    }

    public void setGlobalX(double globalX) {
        this.globalX = globalX;
    }

    public double getGlobalY() {
        return globalY;
    }

    public void setGlobalY(double globalY) {
        this.globalY = globalY;
    }

    public double getVel() {
        return vel;
    }

    public void setVel(double vel) {
        this.vel = vel;
    }

    public double getAcc() {
        return acc;
    }

    public void setAcc(double acc) {
        this.acc = acc;
    }

    public int getLaneId() {
        return laneId;
    }

    public void setLaneId(int laneId) {
        this.laneId = laneId;
    }

    public double getSpaceHeadway() {
        return spaceHeadway;
    }

    public void setSpaceHeadway(double spaceHeadway) {
        this.spaceHeadway = spaceHeadway;
    }

    public double getTimeHeadway() {
        return timeHeadway;
    }

    public void setTimeHeadway(double timeHeadway) {
        this.timeHeadway = timeHeadway;
    }


    

}
