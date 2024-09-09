//M. M. Kuttel 2024 mkuttel@gmail.com
//Class to represent a swim team - which has four swimmers
package medleySimulation;

import medleySimulation.Swimmer.SwimStroke;
import java.awt.Color;

import java.util.concurrent.CountDownLatch;

//M. M. Kuttel 2024 mkuttel@gmail.com
//Class to represent a swim team - which has four swimmers




public class SwimTeam extends Thread {

    public static StadiumGrid stadium; // shared 
    private Swimmer[] swimmers;
    private int teamNo; // team number 

    public static final int sizeOfTeam = 4;
    private CountDownLatch startLatch; // Add latch for synchronization

    // Constructor with CountDownLatch parameter
    SwimTeam(int ID, FinishCounter finish, PeopleLocation[] locArr, CountDownLatch startLatch) {
        this.teamNo = ID;
        this.startLatch = startLatch; // Initialize the latch
        
        swimmers = new Swimmer[sizeOfTeam];
        SwimStroke[] strokes = SwimStroke.values(); // Get all enum constants in SwimStroke

        // Assign the team's color based on the team number
        Color teamColor = strokes[teamNo].getColour();  // This ensures each team gets a unique color
        
        stadium.returnStartingBlock(ID);

        for (int s = 0; s < sizeOfTeam; s++) { // Initialize swimmers in the team
            int swimmerID = teamNo * sizeOfTeam + s;

            // Assign the location with the team's color
            locArr[swimmerID] = new PeopleLocation(swimmerID, teamColor);
            int speed = (int) (Math.random() * 3 + 30); // Random speed in the range of 30-32
            
            // Create a swimmer and pass the team color to PeopleLocation
            swimmers[s] = new Swimmer(swimmerID, teamNo, locArr[swimmerID], finish, speed, strokes[s], startLatch);
        }
    }

    @Override
    public void run() {
        try {
            for (int s = 0; s < sizeOfTeam; s++) { // Start swimmer threads
                swimmers[s].start();
            }

            for (int s = 0; s < sizeOfTeam; s++) {
                swimmers[s].join(); // Wait for swimmers to finish
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
