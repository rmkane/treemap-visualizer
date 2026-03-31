package org.acme.treemap.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class LayoutTest {

    @Test
    void horizontalStripPartitionsWidth() {
        List<TreemapLayout.Rect> rects = TreemapLayout.layoutStrip(0, 0, 100, 50, true, List.of(10.0, 30.0, 60.0));
        assertEquals(3, rects.size());
        assertEquals(0, rects.get(0).x());
        assertEquals(10, rects.get(0).w());
        assertEquals(10, rects.get(1).x());
        assertEquals(30, rects.get(1).w());
        assertEquals(40, rects.get(2).x());
        assertEquals(60, rects.get(2).w());
        assertEquals(50, rects.get(0).h());
    }

    @Test
    void verticalStripPartitionsHeight() {
        List<TreemapLayout.Rect> rects = TreemapLayout.layoutStrip(0, 0, 100, 100, false, List.of(25.0, 75.0));
        assertEquals(0, rects.get(0).y());
        assertEquals(25, rects.get(0).h());
        assertEquals(25, rects.get(1).y());
        assertEquals(75, rects.get(1).h());
        assertEquals(100, rects.get(0).w());
    }

    @Test
    void zeroSumReturnsEmptyRects() {
        List<TreemapLayout.Rect> rects = TreemapLayout.layoutStrip(0, 0, 10, 10, true, List.of(0.0, 0.0));
        assertEquals(2, rects.size());
        assertEquals(0, rects.get(0).w());
    }
}
