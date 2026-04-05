package model;

import math.GraphFunction;
import javafx.scene.paint.Color;

public class ShadedRegion {
    public final GraphFunction f1;
    public final GraphFunction f2; 
    public  double a;
    public  double b;
    public final Color color;
    public boolean isRespectToY;

    public ShadedRegion(GraphFunction f1, GraphFunction f2, double a, double b, Color color, boolean isRespectToY) {
        this.f1 = f1;
        this.f2 = f2;
        this.a = Math.min(a, b); 
        this.b = Math.max(a, b);
        this.color = color;
        this.isRespectToY = isRespectToY;
    }
}
