package io.github.avantgarde95.balltest.model;

/**
 * Created by Jongmin on 2017-01-20.
 */

public class DirectionalPoint {
    public Point p;
    public enum Direction{above, below};
    public Direction d;
    public DirectionalPoint(Point p, Direction d){
        this.p = p;
        this.d = d;
    }
}
