package medleySimulation;

import java.awt.Color;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;


import java.util.concurrent.CountDownLatch;



public class Swimmer extends Thread {

    public static StadiumGrid stadium; // shared 
    private FinishCounter finish; // shared

    private static final CyclicBarrier[] barriers = new CyclicBarrier[4]; // One barrier for each swim stroke set
    private static CountDownLatch startLatch; // Add latch for synchronization

    static {
        for (int i = 0; i < 4; i++) {
            barriers[i] = new CyclicBarrier(10); // Each barrier will wait for 10 swimmers (one full set)
        }
    }

    GridBlock currentBlock;
    private Random rand;
    private int movingSpeed;

    private PeopleLocation myLocation;
    private int ID; // thread ID 
    private int team; // team ID
    private GridBlock start;

    public enum SwimStroke { 
        Backstroke(1, 2.5, Color.BLACK),
        Breaststroke(2, 2.1, new Color(255, 102, 0)),
        Butterfly(3, 2.55, Color.MAGENTA),
        Freestyle(4, 2.8, Color.RED);
        
        private final double strokeTime;
        private final int order; // in minutes
        private final Color colour;   

        SwimStroke(int order, double sT, Color c) {
            this.strokeTime = sT;
            this.order = order;
            this.colour = c;
        }
    
        public int getOrder() { return order; }
        public Color getColour() { return colour; }
    }  
    private final SwimStroke swimStroke;

    // Updated Constructor with CountDownLatch
    Swimmer(int ID, int t, PeopleLocation loc, FinishCounter f, int speed, SwimStroke s, CountDownLatch startLatch) {
        this.swimStroke = s;
        this.ID = ID;
        this.movingSpeed = speed; // range of speeds for swimmers
        this.myLocation = loc;
        this.team = t;
        this.start = stadium.returnStartingBlock(team);
        this.finish = f;
        this.rand = new Random();
        Swimmer.startLatch = startLatch; // Initialize latch
    }

    // Getter methods...

    public void enterStadium() throws InterruptedException {
        currentBlock = stadium.enterStadium(myLocation);
        sleep(200); // wait a bit at door, look around
    }

    public void goToStartingBlocks() throws InterruptedException {
        int x_st = start.getX();
        int y_st = start.getY();
        while (currentBlock != start) {
            sleep(movingSpeed * 3); // not rushing 
            currentBlock = stadium.moveTowards(currentBlock, x_st, y_st, myLocation); // head toward starting block
        }
        System.out.println("-----------Thread " + this.ID + " at start " + currentBlock.getX() + " " + currentBlock.getY());
    }

    private void dive() throws InterruptedException {
        int x = currentBlock.getX();
        int y = currentBlock.getY();
        currentBlock = stadium.jumpTo(currentBlock, x, y - 2, myLocation);
    }

    private void swimRace() throws InterruptedException {
        int x = currentBlock.getX();
        while (currentBlock.getY() != 0) {
            currentBlock = stadium.moveTowards(currentBlock, x, 0, myLocation);
            sleep((int) (movingSpeed * swimStroke.strokeTime)); // swim
            System.out.println("Thread " + this.ID + " swimming at speed " + movingSpeed);
        }

        while (currentBlock.getY() != (StadiumGrid.start_y - 1)) {
            currentBlock = stadium.moveTowards(currentBlock, x, StadiumGrid.start_y, myLocation);
            sleep((int) (movingSpeed * swimStroke.strokeTime)); // swim
        }
    }

    public void exitPool() throws InterruptedException {
        int bench = stadium.getMaxY() - swimStroke.getOrder(); // they line up
        int lane = currentBlock.getX() + 1; // slightly offset
        currentBlock = stadium.moveTowards(currentBlock, lane, currentBlock.getY(), myLocation);
        while (currentBlock.getY() != bench) {
            currentBlock = stadium.moveTowards(currentBlock, lane, bench, myLocation);
            sleep(movingSpeed * 3); // not rushing 
        }
    }

    public void run() {
        try {
            // Step 1: Enter the stadium and move to the starting blocks
            sleep(movingSpeed + (rand.nextInt(10))); // Delay to simulate different arrival times
            myLocation.setArrived();
            enterStadium();
            goToStartingBlocks();
    
            // Step 2: Wait for all swimmers of the same stroke to arrive at the starting blocks
            System.out.println("Swimmer " + ID + " waiting at start barrier for stroke " + swimStroke.getOrder());
            barriers[3].await(); 
            
            // Step 3: Begin swimming only if it's this swimmer's turn in the relay
            if (swimStroke.getOrder() == 1) { // Backstroke (first stroke) starts immediately
                dive();
                swimRace();
            } else {
                synchronized (finish) { 
                    while (!finish.isPreviousStrokeCompleted(team, swimStroke.getOrder())) {
                        finish.wait(); // Wait until the previous stroke is completed for this team
                    }
                }
                dive();
                swimRace();
            }
    
            // Step 4: Signal that this swimmer has completed their leg of the race
            synchronized (finish) {
                finish.markStrokeCompleted(team, swimStroke.getOrder());
                finish.notifyAll(); // Notify all waiting swimmers
            }
            
            // Step 5: If this is the final swimmer (freestyle), finish the race
            if (swimStroke.getOrder() == 4) {
                finish.finishRace(ID, team);
            } else {
                exitPool(); // Earlier swimmers exit the pool
            }
    
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}    