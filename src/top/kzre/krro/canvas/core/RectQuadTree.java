package top.kzre.krro.canvas.core;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 专门用于矩形条目的四叉树，适用于曲线段 AABB 索引。
 * 非线程安全。
 */
public final class RectQuadTree<T> {

    private static final int DEFAULT_CAPACITY = 8;
    private static final double DEFAULT_RANGE = 1e9;
    private static final double EPSILON = 1e-9;

    private Rect bounds;
    private final int capacity;
    private final boolean autoExpand;
    private final List<Entry<T>> entries;
    private RectQuadTree<T> nw, ne, sw, se;
    private boolean divided = false;

    // ========== 构造器 ==========

    public RectQuadTree() {
        this(new Rect(-DEFAULT_RANGE, -DEFAULT_RANGE, DEFAULT_RANGE, DEFAULT_RANGE),
                DEFAULT_CAPACITY, true);
    }

    public RectQuadTree(Rect bounds) {
        this(bounds, DEFAULT_CAPACITY, false);
    }

    public RectQuadTree(Rect bounds, int capacity) {
        this(bounds, capacity, false);
    }

    private RectQuadTree(Rect bounds, int capacity, boolean autoExpand) {
        if (bounds == null) throw new IllegalArgumentException("bounds must not be null");
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.bounds = bounds;
        this.capacity = capacity;
        this.autoExpand = autoExpand;
        this.entries = new ArrayList<>(capacity);
    }

    // ========== 插入 ==========

    public void insert(Rect rect, T value) {
        if (!bounds.intersects(rect)) {
            if (autoExpand) expandBounds(rect);
            else throw new IllegalArgumentException("Rect out of bounds");
        }
        insertInternal(rect, value);
    }

    private void insertInternal(Rect rect, T value) {
        if (!divided) {
            if (entries.size() < capacity) {
                entries.add(new Entry<>(rect, value));
                return;
            }
            subdivide();
        }

        double cx = rect.midX(), cy = rect.midY();
        if (nw.bounds.contains(cx, cy)) {
            nw.insertInternal(rect, value);
        } else if (ne.bounds.contains(cx, cy)) {
            ne.insertInternal(rect, value);
        } else if (sw.bounds.contains(cx, cy)) {
            sw.insertInternal(rect, value);
        } else if (se.bounds.contains(cx, cy)) {
            se.insertInternal(rect, value);
        } else {
            // 矩形跨越多个子节点，存于当前节点
            entries.add(new Entry<>(rect, value));
        }
    }

    private void subdivide() {
        double midX = bounds.midX();
        double midY = bounds.midY();

        nw = new RectQuadTree<>(new Rect(bounds.minX, bounds.minY, midX, midY), capacity, false);
        ne = new RectQuadTree<>(new Rect(midX, bounds.minY, bounds.maxX, midY), capacity, false);
        sw = new RectQuadTree<>(new Rect(bounds.minX, midY, midX, bounds.maxY), capacity, false);
        se = new RectQuadTree<>(new Rect(midX, midY, bounds.maxX, bounds.maxY), capacity, false);

        divided = true;
    }

    // ========== 扩展边界 ==========

    private void expandBounds(Rect rect) {
        double newMinX = Math.min(bounds.minX, rect.minX);
        double newMinY = Math.min(bounds.minY, rect.minY);
        double newMaxX = Math.max(bounds.maxX, rect.maxX);
        double newMaxY = Math.max(bounds.maxY, rect.maxY);

        double marginX = (newMaxX - newMinX) * 0.1;
        double marginY = (newMaxY - newMinY) * 0.1;
        newMinX -= marginX;
        newMinY -= marginY;
        newMaxX += marginX;
        newMaxY += marginY;

        Rect newBounds = new Rect(newMinX, newMinY, newMaxX, newMaxY);

        List<Entry<T>> all = getAllEntries();
        this.bounds = newBounds;
        this.divided = false;
        this.entries.clear();
        this.nw = this.ne = this.sw = this.se = null;
        for (Entry<T> e : all) {
            insertInternal(e.rect, e.value);
        }
    }

    private List<Entry<T>> getAllEntries() {
        List<Entry<T>> result = new ArrayList<>();
        collectEntries(result);
        return result;
    }

    private void collectEntries(List<Entry<T>> collector) {
        if (divided) {
            if (nw != null) nw.collectEntries(collector);
            if (ne != null) ne.collectEntries(collector);
            if (sw != null) sw.collectEntries(collector);
            if (se != null) se.collectEntries(collector);
        }
        collector.addAll(entries);
    }

    // ========== 查询 ==========

    public void query(Rect rect, Consumer<Entry<T>> consumer) {
        if (!bounds.intersects(rect)) return;

        if (divided) {
            nw.query(rect, consumer);
            ne.query(rect, consumer);
            sw.query(rect, consumer);
            se.query(rect, consumer);
        }
        for (Entry<T> e : entries) {
            if (e.rect.intersects(rect)) {
                consumer.accept(e);
            }
        }
    }

    public List<Entry<T>> query(Rect rect) {
        List<Entry<T>> result = new ArrayList<>();
        query(rect, result::add);
        return result;
    }

    public List<Entry<T>> queryPoint(double x, double y, double threshold) {
        Rect rect = new Rect(x - threshold, y - threshold, x + threshold, y + threshold);
        List<Entry<T>> result = new ArrayList<>();
        query(rect, result::add);
        return result;
    }

    public List<NearestResult<T>> nearestK(double x, double y, int k, Predicate<Entry<T>> predicate) {
        List<Entry<T>> candidates = new ArrayList<>();
        Rect queryRect = new Rect(x - 1000, y - 1000, x + 1000, y + 1000);
        query(queryRect, e -> {
            if (predicate.test(e)) candidates.add(e);
        });

        PriorityQueue<NearestResult<T>> heap = new PriorityQueue<>(
                (a, b) -> Double.compare(b.distSq, a.distSq)
        );
        for (Entry<T> e : candidates) {
            double dx = x - e.rect.midX();
            double dy = y - e.rect.midY();
            double d = dx * dx + dy * dy;
            if (heap.size() < k) {
                heap.offer(new NearestResult<>(e.value, d, e.rect.midX(), e.rect.midY()));
            } else {
                assert heap.peek() != null;
                if (d < heap.peek().distSq) {
                    heap.poll();
                    heap.offer(new NearestResult<>(e.value, d, e.rect.midX(), e.rect.midY()));
                }
            }
        }
        List<NearestResult<T>> list = new ArrayList<>(heap);
        list.sort(Comparator.comparingDouble(r -> r.distSq));
        return list;
    }

    public List<NearestResult<T>> nearestK(double x, double y, int k) {
        return nearestK(x, y, k, e -> true);
    }

    // ========== 删除 ==========

    /**
     * 删除指定矩形和值的条目。
     * 优先按空间索引删除，失败回退到暴力删除。
     */
    public boolean delete(Rect rect, T value) {
        boolean removed = deleteFast(rect, value);
        if (removed) {
            return true;
        }
        // fallback：暴力遍历删除
        return deleteBruteForce(rect, value);
    }

    /**
     * 快速空间删除
     */
    private boolean deleteFast(Rect rect, T value) {
        if (!bounds.intersects(rect)) {
            return false;
        }

        if (divided) {
            boolean removed = false;
            if (nw != null && nw.bounds.intersects(rect)) {
                removed |= nw.deleteFast(rect, value);
            }
            if (ne != null && ne.bounds.intersects(rect)) {
                removed |= ne.deleteFast(rect, value);
            }
            if (sw != null && sw.bounds.intersects(rect)) {
                removed |= sw.deleteFast(rect, value);
            }
            if (se != null && se.bounds.intersects(rect)) {
                removed |= se.deleteFast(rect, value);
            }
            return removed;
        } else {
            return entries.removeIf(e ->
                    e.value.equals(value) && rectEquals(e.rect, rect)
            );
        }
    }

    /**
     * 暴力删除兜底（精确匹配 rect + value）
     */
    private boolean deleteBruteForce(Rect rect, T value) {
        return deleteIf(e ->
                e.value.equals(value) && rectEquals(e.rect, rect)
        );
    }

    /**
     * 矩形相等比较（带浮点 ε）
     */
    private boolean rectEquals(Rect a, Rect b) {
        return Math.abs(a.minX - b.minX) <= EPSILON
                && Math.abs(a.minY - b.minY) <= EPSILON
                && Math.abs(a.maxX - b.maxX) <= EPSILON
                && Math.abs(a.maxY - b.maxY) <= EPSILON;
    }

    // ========== 条件删除 ==========

    public boolean deleteIf(Predicate<Entry<T>> predicate) {
        if (divided) {
            boolean any = false;
            if (nw != null) any |= nw.deleteIf(predicate);
            if (ne != null) any |= ne.deleteIf(predicate);
            if (sw != null) any |= sw.deleteIf(predicate);
            if (se != null) any |= se.deleteIf(predicate);
            return any;
        } else {
            return entries.removeIf(predicate);
        }
    }

    // ========== 统计 ==========

    public boolean isEmpty() {
        return !divided && entries.isEmpty();
    }

    public int size() {
        if (divided) {
            return (nw != null ? nw.size() : 0) +
                    (ne != null ? ne.size() : 0) +
                    (sw != null ? sw.size() : 0) +
                    (se != null ? se.size() : 0);
        } else {
            return entries.size();
        }
    }

    // ========== 内部类 ==========

    public static class Entry<T> {
        public final Rect rect;
        public final T value;

        Entry(Rect rect, T value) {
            this.rect = rect;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("Entry{rect=%s, value=%s}", rect, value);
        }
    }

    public static class NearestResult<T> {
        public final T value;
        public final double distSq;
        public final double x;
        public final double y;

        NearestResult(T value, double distSq, double x, double y) {
            this.value = value;
            this.distSq = distSq;
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return String.format("NearestResult{value=%s, dist=%.2f, (%.2f, %.2f)}",
                    value, Math.sqrt(distSq), x, y);
        }
    }

}