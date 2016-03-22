package com.samsung.smcl.vr.widgets;

import org.gearvrf.GVRMesh;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRTransform;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The geometry represented by {@code BoundingBox} is axis-aligned, i.e., the
 * top and bottom faces are parallel to the X-Z plane, the left and right faces
 * are parallel to the Y-Z plane, and the front and back faces are parallel to
 * the X-Y plane.
 */
public class BoundingBox {
    /**
     * Constructs a bounding box based on the mesh of {@code widget}, and then
     * applies {@code widget's} current transform. The result is a bounding box
     * that is axis-aligned to {@code widget's} parent and is big enough to
     * contain {@code widget's} untransformed bounding box.
     * 
     * @param widget
     *            {@link Widget} to build the bounding box from.
     */
    public BoundingBox(final Widget widget) {
        float[] vertices = getVertices(widget);
        if (vertices != null) {
            loadVertices(vertices);
            transform(this, widget);
        }
    }

    public BoundingBox(final BoundingBox rhs) {
        mVertices = copyVertices(rhs.mVertices);
        mMinCorner = rhs.getMinCorner();
        mMaxCorner = rhs.getMaxCorner();
        mCenter = rhs.getCenter();
        mRadius = rhs.getRadius();
    }

    public Vector3f getMinCorner() {
        return new Vector3f(mMinCorner);
    }

    public Vector3f getMaxCorner() {
        return new Vector3f(mMaxCorner);
    }

    /**
     * @return A copy of the point centered between the
     *         {@linkplain #getMinCorner() minimum} corner and
     *         {@linkplain #getMaxCorner() maximum} corner.
     */
    public Vector3f getCenter() {
        return new Vector3f(mCenter);
    }

    /**
     * @return Absolute value of the distance between the
     *         {@linkplain #getCenter() center} of the bounding box and the
     *         corners.
     */
    public float getRadius() {
        return mRadius;
    }

    /**
     * @return Distance between the Z components of the
     *         {@linkplain #getMinCorner() minimum} and
     *         {@linkplain #getMaxCorner() maximum} corners.
     */
    public float getDepth() {
        return mMaxCorner.z - mMinCorner.z;
    }

    /**
     * @return Distance between the Y components of the
     *         {@linkplain #getMinCorner() minimum} and
     *         {@linkplain #getMaxCorner() maximum} corners.
     */
    public float getHeight() {
        return mMaxCorner.y - mMinCorner.y;
    }

    /**
     * @return Distance between the X components of the
     *         {@linkplain #getMinCorner() minimum} and
     *         {@linkplain #getMaxCorner() maximum} corners.
     */
    public float getWidth() {
        return mMaxCorner.x - mMinCorner.x;
    }

    /**
     * Expands the bounding box as necessary to encompass the minimum and
     * maximum corners of {@code rhs}.
     * 
     * @param rhs
     *            The bounding box to encompass.
     */
    public void expand(final BoundingBox rhs) {
        if (rhs != this) {
            expand(rhs.mMinCorner);
            expand(rhs.mMaxCorner);
        }
    }

    /**
     * Expands the bounding box as necessary to encompass {@code vertex}.
     * 
     * @param vertex
     *            The vertex to encompass.
     */
    public void expand(Vector3f vertex) {
        mMinCorner.min(vertex);
        mMaxCorner.max(vertex);

        Vector3f temp = new Vector3f(mMinCorner);
        mCenter = temp.add(mMaxCorner).mul(.5f);
        temp.set(mMaxCorner);
        mRadius = temp.sub(mMinCorner).length() * .5f;
    }

    /**
     * Expands the bounding box as necessary to encompass the vertex specified
     * by {@code x}, {@code y}, {@code z}.
     * 
     * @param x
     *            X component of the vertex to encompass.
     * @param y
     *            Y component of the vertex to encompass.
     * @param z
     *            Z component of the vertex to encompass.
     */
    public void expand(float x, float y, float z) {
        expand(new Vector3f(x, y, z));
    }

    /**
     * A {@link BoundingBox} is considered valid if the
     * {@linkplain #getMinCorner() minimum} and {@linkplain #getMaxCorner()
     * maximum} corners are set to neither {@linkplain Float#POSITIVE_INFINITY
     * positive} nor {@linkplain Float#NEGATIVE_INFINITY negative} infinity.
     * 
     * @return {@code True} if the {@code BoundingBox} is valid, {@code false}
     *         if it isn't.
     */
    public boolean isValid() {
        return isValid(mMinCorner) && isValid(mMaxCorner);
    }

    /**
     * Transforms the vertices of the {@link BoundingBox} by the specified
     * matrix.
     * 
     * @param matrix
     *            The transform matrix to apply.
     * @return A new axis-aligned bounding box for the transformed vertices.
     */
    public BoundingBox transform(Matrix4f matrix) {
        return transform(new BoundingBox(this), matrix);
    }

    /**
     * Transforms the vertices of the {@link BoundingBox} by the local model
     * matrix of {@code widget}.
     * 
     * @param widget
     *            {@link Widget} to get a local model
     *            {@linkplain GVRTransform#getLocalModelMatrix4f() matrix} from.
     * @return A new axis-aligned bounding box for the transformed vertices.
     */
    public BoundingBox transform(final Widget widget) {
        return transform(new BoundingBox(this), widget);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("min: ").append(vec2str(mMinCorner))
                .append(", max: ").append(vec2str(mMaxCorner)).append(", c: ")
                .append(vec2str(mCenter)).append(", r: ").append(mRadius)
                .toString();
    }

    private void clear() {
        mMinCorner.set(Float.POSITIVE_INFINITY);
        mMaxCorner.set(Float.NEGATIVE_INFINITY);
        mCenter.set(0);
        mRadius = 0;
    }

    /**
     * Makes a deep copy of an array of {@link Vector3f}.
     * 
     * @param vertices
     *            Array to copy
     * @return Array of {@code Vector3f}
     */
    static private Vector3f[] copyVertices(Vector3f[] vertices) {
        Vector3f[] copy = new Vector3f[vertices.length];
        for (int i = 0; i < vertices.length; ++i) {
            copy[i] = new Vector3f(vertices[i]);
        }
        return copy;
    }

    static private float[] getVertices(final Widget widget) {
        float[] vertices = null;
        GVRRenderData renderData = widget.getRenderData();
        if (renderData != null) {
            GVRMesh mesh = renderData.getMesh();
            if (mesh != null) {
                GVRMesh boundingBox = mesh.getBoundingBox();
                vertices = boundingBox.getVertices();
            }
        }
        return vertices;
    }

    private boolean isValid(Vector3f v) {
        return v.x != Float.NEGATIVE_INFINITY && v.y != Float.NEGATIVE_INFINITY
                && v.z != Float.NEGATIVE_INFINITY
                && v.x != Float.POSITIVE_INFINITY
                && v.y != Float.POSITIVE_INFINITY
                && v.z != Float.POSITIVE_INFINITY;
    }

    private void loadVertices(float[] vertices) {
        int vCount = vertices.length / 3;
        mVertices = new Vector3f[vCount];
        for (int i = 0; i < vCount; ++i) {
            float x = vertices[(i * 3) + X];
            float y = vertices[(i * 3) + Y];
            float z = vertices[(i * 3) + Z];
            mVertices[i] = new Vector3f(x, y, z);
        }
    }

    /**
     * Apply a transform to {@code boundingBox}.
     * 
     * @param boundingBox
     *            The {@link BoundingBox} to apply the transform to.
     * @param matrix
     *            The transformation matrix to apply.
     * @return The transformed {@code BoundingBox}.
     */
    private BoundingBox transform(BoundingBox boundingBox, Matrix4f matrix) {
        boundingBox.clear();
        for (Vector3f v : boundingBox.mVertices) {
            v.mulPoint(matrix);
            boundingBox.expand(v);
        }
        return boundingBox;
    }

    /**
     * Apply the transform of {@code widget} to {@code boundingBox}.
     * 
     * @param boundingBox
     *            The {@link BoundingBox} to apply the transform to.
     * @param widget
     *            The {@link Widget} to get the transform from.
     * @return The transformed {@code BoundingBox}.
     */
    private BoundingBox transform(BoundingBox boundingBox, final Widget widget) {
        return transform(boundingBox, widget.getTransform()
                .getLocalModelMatrix4f());
    }

    private String vec2str(Vector3f v) {
        float x = v.x;
        float y = v.y;
        float z = v.z;
        return vec2str(x, y, z);
    }

    private String vec2str(float x, float y, float z) {
        return new StringBuilder().append('{').append(x).append(',').append(y)
                .append(',').append(z).append('}').toString();
    }

    private Vector3f[] mVertices;
    private Vector3f mMinCorner = new Vector3f(Float.POSITIVE_INFINITY);
    private Vector3f mMaxCorner = new Vector3f(Float.NEGATIVE_INFINITY);
    private Vector3f mCenter = new Vector3f(0);
    private float mRadius = 0;

    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    @SuppressWarnings("unused")
    private static final String TAG = BoundingBox.class.getSimpleName();
}
