package play;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import gametree.GameNode;
import gametree.GameNodeDoesNotExistException;
import play.exception.InvalidStrategyException;

public class GrimTriggerStrategy extends Strategy {

    private List<GameNode> getReversePath(GameNode current) {
        try {
            GameNode n = current.getAncestor();
            List<GameNode> l = getReversePath(n);
            l.add(current);
            return l;
        } catch (GameNodeDoesNotExistException e) {
            List<GameNode> l = new ArrayList<>();
            l.add(current);
            return l;
        }
    }

    private GameNode getAncestor(GameNode n) {
        try {
            return n.getAncestor();
        } catch (GameNodeDoesNotExistException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean defected(GameNode n, boolean isPlayer1) {
        if (!n.isNature() && !n.isRoot()) {
            GameNode ancestor = getAncestor(n);
            assert ancestor != null;
            if (isPlayer1)
                return ancestor.isPlayer2() && n.getLabel().
                        toLowerCase(Locale.ROOT).contains("defect");
            else
                return ancestor.isPlayer1() && n.getLabel().
                        toLowerCase(Locale.ROOT).contains("defect");
        }
        return false;
    }

    private boolean cumputeTrigger(GameNode finalP1, GameNode finalP2) {
        List<GameNode> movesp1 = getReversePath(finalP1);
        List<GameNode> movesp2 = getReversePath(finalP2);
        return movesp1.stream().anyMatch((n) -> defected(n, true)) ||
                movesp2.stream().anyMatch((n) ->
                        defected(n, false));
    }

    @Override
    public void execute() throws InterruptedException {
        System.err.println("Grim Trigger Strategy is now active...");
        while (!this.isTreeKnown()) {
            System.err.println("Waiting for game tree to become available.");
            Thread.sleep(1000);
        }

        boolean trigger;

        while (true) {

            GameNode finalP1 = null;
            GameNode finalP2 = null;

            PlayStrategy myStrategy = this.getStrategyRequest();
            if (myStrategy == null) //Game was terminated by an outside event
                break;

            //trigger = myStrategy.getMaximumNumberOfIterations() > 1000000;
            double beta = myStrategy.probabilityForNextIteration();
            GameNode root = this.tree.getRootNode();
            Iterator<GameNode> nodes = root.getChildren();
            GameNode child1 = nodes.next();
            GameNode child2 = nodes.next();
            GameNode bothcoop, deffect1, bothdeffect;
            if(!child1.getLabel().toLowerCase(Locale.ROOT).contains("cooperate")){
                GameNode aux = child1;
                child1 = child2;
                child2 = aux;
            }
            nodes = child1.getChildren();
            bothcoop = nodes.next();
            deffect1 = nodes.next();
            if(!bothcoop.getLabel().toLowerCase(Locale.ROOT).contains("cooperate")){
                GameNode aux = bothcoop;
                bothcoop = deffect1;
                deffect1 = aux;
            }
            nodes = child2.getChildren();
            bothdeffect = nodes.next();
            if(!bothdeffect.getLabel().toLowerCase(Locale.ROOT).contains("defect"))
                bothdeffect = nodes.next();


            trigger = defect(beta, bothcoop.getPayoffP1(), deffect1.getPayoffP2(), bothdeffect.getPayoffP1());


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

                trigger = trigger || myStrategy.getMaximumNumberOfIterations() <= 1;
                myStrategy.getMaximumNumberOfIterations();
                if (!trigger && finalP1 != null && finalP2 != null)
                    trigger = cumputeTrigger(finalP1, finalP2);

                Iterator<String> iterator = myStrategy.keyIterator();
                List<String> keys = new ArrayList<>();
                while (iterator.hasNext())
                    keys.add(iterator.next());
                if (trigger) {
                    keys.stream().filter((s) -> s.toLowerCase(Locale.ROOT).contains("defect")).forEach((s) -> myStrategy.put(s, 1.0));
                    keys.stream().filter((s) -> s.toLowerCase(Locale.ROOT).contains("cooperate")).forEach((s) -> myStrategy.put(s, 0.0));
                } else {
                    keys.stream().filter((s) -> s.toLowerCase(Locale.ROOT).contains("defect")).forEach((s) -> myStrategy.put(s, 0.0));
                    keys.stream().filter((s) -> s.toLowerCase(Locale.ROOT).contains("cooperate")).forEach((s) -> myStrategy.put(s, 1.0));
                }
                try {
                    this.provideStrategy(myStrategy);
                    playComplete = true;
                } catch (InvalidStrategyException e) {
                    System.err.println("Strategy refused: " + e.getMessage());
                }
            }
        }
    }

    private boolean defect(double beta, double coopPayoff, double deffectPayoff, double bothPlayerDeffectPayoff) {
        return beta != 1 && coopPayoff / (1 - beta) < deffectPayoff + (bothPlayerDeffectPayoff * beta) / (1 - beta);
    }

}


