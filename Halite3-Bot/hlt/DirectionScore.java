package hlt;


public class DirectionScore implements Comparable<DirectionScore> {
    public int score;
    public Direction dir;

    public DirectionScore(int score, Direction dir){
        this.score = score;
        this.dir = dir;
    }

    public int compareTo(DirectionScore d){
        return d.score - this.score;
    }

    @Override
    public String toString(){
        return score + " " + dir;
    }
}