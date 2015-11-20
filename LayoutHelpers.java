package com.samsung.smcl.vr.widgets;

import org.gearvrf.GVRMesh;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTransform;

final class LayoutHelpers {

    /**
     * Calculates the "width", in degrees, of an object from a distance of
     * {@code radius} units. This is also referred to as the "angular diameter"
     * or the "visual angle". This is useful in determining the layout of
     * objects in a circle or ring.
     * <p>
     * This is calculated using the object's
     * {@linkplain #calculateGeometricWidth(GVRSceneObject) geometric width}
     * oriented perpendicularly to the radius. The resulting angular width will
     * then accommodate the object's largest orthogonal dimension.
     * <p>
     * Example: Given an object whose geometric center is positioned on a ring
     * of a specified radius, this method calculates by how many degrees a
     * previous object's trailing bounds and a subsequent object's leading
     * bounds must be separated so that neither one overlaps the given object.
     * 
     * @see <a href="https://en.wikipedia.org/wiki/Angular_diameter">Angular
     *      diameter</a>
     * @see <a href="https://en.wikipedia.org/wiki/Visual_angle">Visual
     *      angle</a>
     * 
     * @param item
     *            Scene object whose angular width to calculate.
     * @param radius
     *            Distance between the origin and {@code item}.
     * @return The angular width of {@code item}, in degrees.
     */
    static float calculateAngularWidth(final GVRSceneObject item, double radius) {
        final float geometricWidth = LayoutHelpers
                .calculateGeometricWidth(item);

        return calculateAngularWidth(geometricWidth, radius);
    }

    /**
     * Calculates the "width", in degrees, of a {@code segment} from a distance
     * of {@code radius} units. This is also referred to as the
     * "angular diameter" or the "visual angle". This is useful in determining
     * the layout of objects in a circle or ring.
     * <p>
     * It is assumed that the {@code segment} is oriented perpendicularly to the
     * radius's origin and parallel to the y-axis.
     * 
     * @see <a href="https://en.wikipedia.org/wiki/Angular_diameter">Angular
     *      diameter</a>
     * @see <a href="https://en.wikipedia.org/wiki/Visual_angle">Visual
     *      angle</a>
     * 
     * @param segment
     *            The segment whose angular width to calculate.
     * 
     * @param radius
     *            Distance between the origin and {@code item}.
     * @return The angular width of {@code item}, in degrees.
     */
    static float calculateAngularWidth(final float segment, double radius) {
        // The item is perpendicular to the center of the origin at *its*
        // center, like a "T". The triangle, then, is between the origin,
        // the item's center, and the "edge" of the item's bounding box. The
        // length of the "opposite" side, therefore, is only half the item's
        // geometric width.
        final double opposite = segment / 2;
        final double tangent = opposite / radius; // The rho is the
                                                  // "adjacent"
                                                  // side
        final double radians = Math.atan(tangent);

        // The previous calculation only gives us half the angular width,
        // since it is reckoned from the item's center to its edge.
        return (float) Math.toDegrees(radians) * 2;
    }

    /**
     * Calculates the "width", in degrees, of a widget from a distance of
     * {@code radius} units.
     * 
     * @see #calculateAngularWidth(GVRSceneObject, double)
     * @param widget
     *            The {@link Widget} whose angular width to calculate.
     * @param radius
     *            Distance between the origin and {@code widget}.
     * @return The angular width of {@code widget}, in degrees.
     */
    static float calculateAngularWidth(final Widget widget, double radius) {
        return calculateAngularWidth(widget.getSceneObject(), radius);
    }

    /**
     * Calculates the length of the edges of {@code item's}
     * {@linkplain GVRMesh#getBoundingBox() bounding box}. These lengths are
     * relative to the object itself, and have the object's scaling applied to
     * it. Rotations are not taken into account.
     * 
     * @param item
     *            The {@link GVRSceneObject} to calculate with width for.
     * @return The dimensions of {@code item}.
     */
    static float[] calculateGeometricDimensions(final GVRSceneObject item) {
        final GVRRenderData renderData = item.getRenderData();
        if (renderData != null) {
            final GVRMesh mesh = renderData.getMesh();
            if (mesh != null) {
                final float[] dimensions = calculateGeometricDimensions(mesh);

                GVRTransform transform = item.getTransform();

                dimensions[0] *= transform.getScaleX();
                dimensions[1] *= transform.getScaleY();
                dimensions[2] *= transform.getScaleZ();

                return dimensions;
            }
        }
        return new float[] {
                -1f, -1f, -1f
        };
    }

    public static float[] calculateGeometricDimensions(final GVRMesh mesh) {
        GVRMesh boundingBox = mesh.getBoundingBox();
        final float[] vertices = boundingBox.getVertices();
        final int numVertices = vertices.length / 3;

        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < numVertices; ++i) {
            final int offset = i * 3;
            final float x = vertices[offset];
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);

            final float y = vertices[offset + 1];
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);

            final float z = vertices[offset + 2];
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }

        return new float[] {
                maxX - minX, maxY - minY, maxZ - minZ
        };
    }

    /**
     * @see #calculateGeometricDimensions(GVRSceneObject)
     * @param widget
     * @return
     */
    static float[] calculateGeometricDimensions(final Widget widget) {
        return calculateGeometricDimensions(widget.getSceneObject());
    }

    /**
     * Calculates the length of the edge of {@code item's}
     * {@linkplain GVRMesh#getBoundingBox() bounding box} parallel to the
     * z-axis. This length is relative to the object itself, and has the
     * object's {@linkplain GVRTransform#getScaleZ() z-axis scaling} applied to
     * it. It does not, however, take into account any rotations.
     * <p>
     * Convenience wrapper for
     * {@link #calculateGeometricDimensions(GVRSceneObject)}.
     * 
     * @param item
     *            The {@link GVRSceneObject} to calculate height for.
     * @return The depth of {@code item}.
     */
    static float calculateGeometricDepth(final GVRSceneObject item) {
        return calculateGeometricDimensions(item)[2];
    }

    /**
     * @see #calculateGeometricDepth(GVRSceneObject)
     * @param widget
     * @return
     */
    static float calculateGeometricDepth(final Widget widget) {
        return calculateGeometricDepth(widget.getSceneObject());
    }

    /**
     * Calculates the length of the edge of {@code item's}
     * {@linkplain GVRMesh#getBoundingBox() bounding box} parallel to the
     * y-axis. This length is relative to the object itself, and has the
     * object's {@linkplain GVRTransform#getScaleY() y-axis scaling} applied to
     * it. It does not, however, take into account any rotations.
     * <p>
     * Convenience wrapper for
     * {@link #calculateGeometricDimensions(GVRSceneObject)}.
     * 
     * @param item
     *            The {@link GVRSceneObject} to calculate height for.
     * @return The height of {@code item}.
     */
    static float calculateGeometricHeight(final GVRSceneObject item) {
        return calculateGeometricDimensions(item)[1];
    }

    /**
     * @see #calculateGeometricHeight(GVRSceneObject)
     * @param widget
     * @return
     */
    static float calculateGeometricHeight(final Widget widget) {
        return calculateGeometricHeight(widget.getSceneObject());
    }

    /**
     * Calculates the length of the edge of {@code item's}
     * {@linkplain GVRMesh#getBoundingBox() bounding box} parallel to the
     * x-axis. This length is relative to the object itself, and has the
     * object's {@linkplain GVRTransform#getScaleX() x-axis scaling} applied to
     * it. It does not, however, take into account any rotations.
     * 
     * @param item
     *            The {@link GVRSceneObject} to calculate width for.
     * @return The width of {@code item}.
     */
    static float calculateGeometricWidth(final GVRSceneObject item) {
        GVRMesh boundingBox = item.getRenderData().getMesh().getBoundingBox();
        final float[] vertices = boundingBox.getVertices();
        final int numVertices = vertices.length / 3;
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < numVertices; ++i) {
            final float x = vertices[i * 3];
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
        }
        float width = maxX - minX;
        GVRTransform transform = item.getTransform();
        float xScale = transform.getScaleX();
        return width * xScale;
    }

    /**
     * @see #calculateGeometricWidth(GVRSceneObject)
     * @param widget
     * @return
     */
    static float calculateGeometricWidth(final Widget widget) {
        return calculateGeometricWidth(widget.getSceneObject());
    }

    /**
     * Calculates the angle of an arc.
     * 
     * @param arcLength
     *            Length of the arc.
     * @param radius
     *            Radius of the arc's circle.
     * @return The angle of the arc.
     */
    static float angleOfArc(float arcLength, float radius) {
        return (float) ((arcLength * 180) / (radius * Math.PI));
    }
}