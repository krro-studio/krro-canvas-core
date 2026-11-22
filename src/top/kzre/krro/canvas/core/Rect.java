package top.kzre.krro.canvas.core;

public final class Rect {
    public final double minX, minY, maxX, maxY;

    public Rect(double minX, double minY, double maxX, double maxY) {
        if (minX > maxX || minY > maxY)
            throw new IllegalArgumentException("Invalid rect bounds");
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public boolean contains(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public boolean intersects(Rect other) {
        return !(other.minX > maxX || other.maxX < minX ||
                other.minY > maxY || other.maxY < minY);
    }

    public double midX() {
        return (minX + maxX) * 0.5;
    }

    public double midY() {
        return (minY + maxY) * 0.5;
    }

    public double distToPointSq(double x, double y) {
        double dx = 0, dy = 0;
        if (x < minX) dx = minX - x;
        else if (x > maxX) dx = x - maxX;
        if (y < minY) dy = minY - y;
        else if (y > maxY) dy = y - maxY;
        return dx * dx + dy * dy;
    }

    @Override
    public String toString() {
        return String.format("Rect[%.2f,%.2f,%.2f,%.2f]", minX, minY, maxX, maxY);
    }
}
