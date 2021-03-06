package board.actors.geneticplayer;

import board.Board;
import board.actors.Actor;
import board.actors.Ghost;
import board.actors.Player;
import board.tiles.BoardNode;
import board.tiles.WalkableNode;

import java.util.*;

/**
 * Created by ahanes on 2/22/15.
 */
public class GeneticAlgorithmPlayer extends Player {
    public BoardNode last;
    public int border;
    private Chromosome decisionTree;

    public GeneticAlgorithmPlayer(Board board) {
        super(board);
        this.border = Board.rng.nextInt(50);
        decisionTree = new Chromosome();
    }

    public GeneticAlgorithmPlayer(Board board, Chromosome t) {
        super(board);
        this.border = Board.rng.nextInt(50);
        decisionTree = t;
    }


    public GeneticAlgorithmPlayer(Board brd, GeneticAlgorithmPlayer a, GeneticAlgorithmPlayer b) {
        super(brd);
        this.border = Board.rng.nextInt(50);
        decisionTree = new Chromosome(a.decisionTree, b.decisionTree);
    }

    public Chromosome getDecisionTree() {
        return decisionTree;
    }

    public void setDecisionTree(Chromosome decisionTree) {
        this.decisionTree = decisionTree;
    }

    public BoardNode rand_move() {
        List<BoardNode> choices = this.getLocation().getNeighbors(this);
        if (choices.size() > 1 && choices.contains(this.last) && this.last != null) {
            choices.remove(this.last); // Don't go backwards
        }
        this.last = this.getLocation();
        Collections.shuffle(choices, Board.rng);
        this.setLocation(choices.get(0));
        return this.getLocation();
    }

    public BoardNode attackMove() {
        List<BoardNode> choices = this.getLocation().getNeighbors(this);
        this.last = this.getLocation();
        Collections.shuffle(choices, Board.rng);
        BoardNode choice = choices.get(0);
        int smallest = this.nearestGhost();
        for (BoardNode b : choices) {
            if (this.nearestGhost(b) < smallest) {
                choice = b;
                smallest = this.nearestGhost(b);
            }
        }
        this.setLocation(choice);
        return this.getLocation();
    }

    public BoardNode safestMove() {
        List<BoardNode> choices = this.getLocation().getNeighbors(this);
        this.last = this.getLocation();
        Collections.shuffle(choices, Board.rng);
        BoardNode choice = choices.get(0);
        int smallest = this.nearestGhost();
        for (BoardNode b : choices) {
            if (this.nearestGhost(b) >= smallest) {
                choice = b;
                smallest = this.nearestGhost(b);
            }
        }
        this.setLocation(choice);
        return this.getLocation();
    }

    public BoardNode energizer_move() {
        List<BoardNode> choices = this.getLocation().getNeighbors(this);
        this.last = this.getLocation();
        Collections.shuffle(choices, Board.rng);
        choices.remove(this.last);
        BoardNode choice = choices.get(0);
        int smallest = this.nearestEnergizer(choice);
        for (BoardNode b : choices) {
            if (this.nearestEnergizer(b) <= smallest && !this.last.equals(b)) {
                choice = b;
                smallest = this.nearestEnergizer(b);
            }
        }
        this.setLocation(choice);
        return this.getLocation();
    }

    public BoardNode chain_move() {
        List<BoardNode> choices = this.getLocation().getNeighbors(this);
        Collections.shuffle(choices, Board.rng);
        BoardNode choice = choices.get(0);
        int dist = 100000;
        for (BoardNode b : choices) {
            if (nearestChain(b) < dist && this.last != null && !this.last.equals(b) && !((WalkableNode)b).hasPowerup()) {
                choice = b;
                dist = nearestChain(b);
            }
        }
        this.last = this.getLocation();
        this.setLocation(choice);
        return this.getLocation();
    }


    public int nearestChain(BoardNode location) {
        HashSet<BoardNode> ghostPos = new HashSet<BoardNode>();
        for (Actor a : this.board.getActors()) {
            if (a instanceof Ghost) {
                ghostPos.add(a.getLocation());
            }
        }
        Queue<BoardNode> q = new LinkedList<BoardNode>();
        final HashSet<BoardNode> visited = new HashSet<BoardNode>();
        HashMap<BoardNode, BoardNode> tree = new HashMap<BoardNode, BoardNode>();
        BoardNode b = location;
        q.add(b);
        BoardNode last = b;
        while (!q.isEmpty()) {
            b = q.remove();
            tree.put(b, last);
            if (b instanceof WalkableNode && !((WalkableNode) b).hasWalked()) {
                int count = 0;
                while (tree.get(b) != location) {
                    b = tree.get(b);
                    ++count;
                }
                return count;
            }
            last = b;
            for (BoardNode n : b.getNeighbors(this)) {
                if (!visited.contains(n)) {
                    visited.add(n);
                    q.add(n);
                }
            }
        }
        return 300; //Its far away
    }

    @Override
    public BoardNode move() {
        return this.decisionTree.move(this);
    }

    @Override
    public void spawn() {
        super.spawn(board.getPlayerSpawn());
    }

    public boolean inDanger() {
        return nearestGhost() < 11;
    }

    public int nearestGhost() {
        return this.nearestGhost(this.getLocation());
    }

    public int nearestGhost(BoardNode location) {
        HashSet<BoardNode> ghostPos = new HashSet<BoardNode>();
        for (Actor a : this.board.getActors()) {
            if (a instanceof Ghost && a.isActive()) {
                ghostPos.add(a.getLocation());
            }
        }
        Queue<BoardNode> q = new LinkedList<BoardNode>();
        final HashSet<BoardNode> visited = new HashSet<BoardNode>();
        HashMap<BoardNode, BoardNode> tree = new HashMap<BoardNode, BoardNode>();
        BoardNode b = location;
        q.add(b);
        BoardNode last = b;
        while (!q.isEmpty()) {
            b = q.remove();
            tree.put(b, last);
            if (ghostPos.contains(b)) {
                int count = 0;
                while (tree.get(b) != location) {
                    ++count;
                    b = tree.get(b);
                }
                return count;
            }
            last = b;
            for (BoardNode n : b.getNeighbors(this)) {
                if (!visited.contains(n)) {
                    visited.add(n);
                    q.add(n);
                }
            }
        }
        return 3000; //Its far away
    }

    public int nearestEnergizer(BoardNode location) {
        HashSet<BoardNode> ghostPos = new HashSet<BoardNode>();
        Queue<BoardNode> q = new LinkedList<BoardNode>();
        final HashSet<BoardNode> visited = new HashSet<BoardNode>();
        HashMap<BoardNode, BoardNode> tree = new HashMap<BoardNode, BoardNode>();
        BoardNode b = location;
        q.add(b);
        BoardNode last = b;
        while (!q.isEmpty()) {
            b = q.remove();
            tree.put(b, last);
            if (b instanceof WalkableNode && ((WalkableNode) b).hasPowerup()) {
                int count = 0;
                while (tree.get(b) != location) {
                    ++count;
                    b = tree.get(b);
                }
                return count;
            }
            last = b;
            for (BoardNode n : b.getNeighbors(this)) {
                if (!visited.contains(n)) {
                    visited.add(n);
                    q.add(n);
                }
            }
        }
        return 300; //Its far away
    }

    public boolean nearestEnergizerIsSafe(BoardNode location, int t) {
        HashSet<BoardNode> ghostPos = new HashSet<BoardNode>();
        Queue<BoardNode> q = new LinkedList<BoardNode>();
        final HashSet<BoardNode> visited = new HashSet<BoardNode>();
        HashMap<BoardNode, BoardNode> tree = new HashMap<BoardNode, BoardNode>();
        BoardNode b = location;
        q.add(b);
        BoardNode last = b;
        while (!q.isEmpty()) {
            b = q.remove();
            tree.put(b, last);
            if (b instanceof WalkableNode && ((WalkableNode) b).hasPowerup()) {
                while (tree.get(b) != location) {
                    if (nearestGhost(location) > t) {
                        return false;
                    }
                    b = tree.get(b);
                }
                return true;
            }
            last = b;
            for (BoardNode n : b.getNeighbors(this)) {
                if (!visited.contains(n)) {
                    visited.add(n);
                    q.add(n);
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "<";
    }
}
