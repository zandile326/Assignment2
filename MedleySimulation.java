package medleySimulation;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CountDownLatch;
import medleySimulation.Swimmer.SwimStroke;

public class MedleySimulation {
    static final int numTeams = 10;

    static int frameX = 300; // frame width
    static int frameY = 600;  // frame height
    static int yLimit = 400;
    static int max = 5;
       private static CountDownLatch latch;
    static int gridX = 50; // number of x grid points
    static int gridY = 120; // number of y grid points

    static SwimTeam[] teams; // array for team threads
    static PeopleLocation[] peopleLocations;  // array to keep track of where people are
    static StadiumView stadiumView; // threaded panel to display stadium
    static StadiumGrid stadiumGrid; // stadium on a discrete grid

    static FinishCounter finishLine; // records who won
    static CounterDisplay counterDisplay; // threaded display of counter


    // Method to setup all the elements of the GUI
    public static void setupGUI(int frameX, int frameY) {
        // Frame initialize and dimensions
        JFrame frame = new JFrame("Swim medley relay animation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(frameX, frameY);

        JPanel g = new JPanel();
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setSize(frameX, frameY);

        stadiumView = new StadiumView(peopleLocations, stadiumGrid);
        stadiumView.setSize(frameX, frameY);
        g.add(stadiumView);

        // add text labels to the panel - this can be extended
        JPanel txt = new JPanel();
        txt.setLayout(new BoxLayout(txt, BoxLayout.LINE_AXIS));
        JLabel winner = new JLabel("");
        txt.add(winner);
        g.add(txt);

        counterDisplay = new CounterDisplay(winner, finishLine); // thread to update score

        // Add start and exit buttons
        JPanel b = new JPanel();
        b.setLayout(new BoxLayout(b, BoxLayout.LINE_AXIS));

        JButton startB = new JButton("Start");
        // add the listener to the jbutton to handle the "pressed" event
        startB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Start viewer thread
                Thread view = new Thread(stadiumView);
                view.start();

                // Start counter thread - for updating results
                Thread results = new Thread(counterDisplay);
                results.start();

                // Start teams, which start swimmers.
                for (int i = 0; i < numTeams; i++) {
                    teams[i].start();
                }

                startB.setEnabled(false); // Disable the start button after starting the race
            }
        });

        JButton endB = new JButton("Quit");
        // add the listener to the jbutton to handle the "pressed" event
        endB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        b.add(startB);
        b.add(endB);
        g.add(b);

        frame.setLocationRelativeTo(null);  // Center window on screen.
        frame.add(g); // add contents to window
        frame.setContentPane(g);
        frame.setVisible(true);
    }


    // Main method - starts it all
	public static void main(String[] args) throws InterruptedException {
		int numSwimmers = numTeams * SwimTeam.sizeOfTeam; // Total number of swimmers
	
		finishLine = new FinishCounter(); // counters for people inside and outside club
		stadiumGrid = new StadiumGrid(gridX, gridY, numTeams, finishLine); // setup stadium with size     
		SwimTeam.stadium = stadiumGrid; // grid shared with class
		Swimmer.stadium = stadiumGrid; // grid shared with class
		peopleLocations = new PeopleLocation[numTeams * SwimTeam.sizeOfTeam]; // four swimmers per team
		teams = new SwimTeam[numTeams];
		
		// Initialize SwimTeam
		for (int i = 0; i < numTeams; i++) {
			teams[i] = new SwimTeam(i, finishLine, peopleLocations);
		}
		
		// Initialize the CountDownLatch with the number of swimmers
		CountDownLatch latch = new CountDownLatch(numSwimmers);
		Swimmer.setLatch(latch); // Set the latch for Swimmer class
		setupGUI(frameX, frameY);
		// Create and start swimmer threads
		for (int i = 0; i < numSwimmers; i++) {
			int teamID = i / SwimTeam.sizeOfTeam; // Determine team ID based on index
			PeopleLocation location = peopleLocations[i]; // Get the location for the swimmer
			SwimStroke stroke = SwimStroke.values()[i % SwimStroke.values().length]; // Assign strokes cyclically or according to your logic
			int speed = (i % 5) + 1; // Example speed, should be set according to your logic
	
			Swimmer swimmer = new Swimmer(i, teamID, location, finishLine, speed, stroke);
			swimmer.start();
		}
	
		// Optionally wait for all swimmers to be in the stadium before proceeding
		//latch.await(); // Wait for all swimmers to enter the stadium
	
		// Proceed with the rest of the simulation if needed
		System.out.println("All swimmers are in the stadium. Starting the race...");
		
		// Setup GUI or other necessary operations
		 // Start Panel thread - for drawing animation
	}
}	