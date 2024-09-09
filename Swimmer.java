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
            sleep(movingSpeed + (rand.nextInt(10))); // arriving takes a while
            myLocation.setArrived();
            enterStadium();
            goToStartingBlocks();
			

            // Wait for all swimmers of the same stroke to arrive at the bench
			
				
            barriers[swimStroke.getOrder() - 1].await();
			
			

            // Synchronize start with the first set
            if (swimStroke.getOrder() == 4) {
                // Last swimmer releases the latch
                startLatch.countDown();
            }

            // Wait for the signal to start racing
            startLatch.await();

            dive();
            swimRace();

            if (swimStroke.getOrder() == 4) {
                finish.finishRace(ID, team); // finish line
            } else {
                exitPool(); // if not the last swimmer, leave pool
            }

        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
