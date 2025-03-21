package neu.cs6650.serverspring.service;

import io.swagger.client.model.LiftRide;


public class LiftRideEvent {
  private LiftRide liftRide;
  private int resortID;
  private String seasonID;
  private String dayID;
  private int skierID;

  public LiftRideEvent() {}

  public LiftRideEvent(LiftRide liftRide, int resortID, String seasonID, String dayID, int skierID) {
    this.liftRide = liftRide;
    this.resortID = resortID;
    this.seasonID = seasonID;
    this.dayID = dayID;
    this.skierID = skierID;
  }


  public LiftRide getLiftRide() {
    return liftRide;
  }

  public void setLiftRide(LiftRide liftRide) {
    this.liftRide = liftRide;
  }

  public int getResortID() {
    return resortID;
  }

  public void setResortID(int resortID) {
    this.resortID = resortID;
  }

  public String getSeasonID() {
    return seasonID;
  }

  public void setSeasonID(String seasonID) {
    this.seasonID = seasonID;
  }

  public String getDayID() {
    return dayID;
  }

  public void setDayID(String dayID) {
    this.dayID = dayID;
  }

  public int getSkierID() {
    return skierID;
  }

  public void setSkierID(int skierID) {
    this.skierID = skierID;
  }

  @Override
  public String toString() {
    return "LiftRideEvent{" +
        "liftRide=" + liftRide +
        ", resortID=" + resortID +
        ", seasonID='" + seasonID + '\'' +
        ", dayID='" + dayID + '\'' +
        ", skierID=" + skierID +
        '}';
  }
}
