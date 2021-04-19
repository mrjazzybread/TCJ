package play;

import gametree.GameNode;
import play.exception.InvalidStrategyException;

import java.util.Iterator;
import java.util.Locale;

/**
 * Bro, you'll never guess what this one does
 */
public class AlwaysDefectStrategy extends Strategy {
    @Override
    public void execute() throws InterruptedException {
        System.err.println("Always Defect Strategy is now active...");
        while (!this.isTreeKnown()) {
            System.err.println("Waiting for game tree to become available.");
            Thread.sleep(1000);
        }

        GameNode finalP1 = null;
        GameNode finalP2 = null;

        while (true) {

            PlayStrategy myStrategy = this.getStrategyRequest();
            if (myStrategy == null) //Game was terminated by an outside event
                break;
            boolean playComplete = false;
            while (!playComplete) {
                if (myStrategy.getFinalP1Node() != -1) {
                    finalP1 = this.tree.getNodeByIndex(myStrategy.getFinalP1Node());
                    if (finalP1 != null)
                        System.out.println("Terminal node in last round as P1: " + finalP1);
                }

                if (myStrategy.getFinalP2Node() != -1) {
                    finalP2 = this.tree.getNodeByIndex(myStrategy.getFinalP2Node());
                    if (finalP2 != null)
                        System.out.println("Terminal node in last round as P2: " + finalP2);
                }

                Iterator<String> keys = myStrategy.keyIterator();
                while (keys.hasNext()) {
                    String k = keys.next();
                    if (k.toLowerCase(Locale.ROOT).contains("defect"))
                        myStrategy.put(k, 1.0);
                    else
                        myStrategy.put(k, 0.0);
                }

                try {
                    this.provideStrategy(myStrategy);
                    playComplete = true;
                } catch (InvalidStrategyException e) {
                    System.err.println("Invalid strategy: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        }
    }

}

