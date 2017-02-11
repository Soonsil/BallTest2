package io.github.avantgarde95.balltest.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import io.github.avantgarde95.balltest.model.Circle;
import io.github.avantgarde95.balltest.model.DirectionalPoint;
import io.github.avantgarde95.balltest.model.DirectionalPoint.Direction;
import io.github.avantgarde95.balltest.model.Model;
import io.github.avantgarde95.balltest.model.Point;
import io.github.avantgarde95.balltest.model.Polygon;
import io.github.avantgarde95.balltest.model.TextureModel;
import io.github.avantgarde95.balltest.physics2d.CircleBody2D;
import io.github.avantgarde95.balltest.physics2d.CirclePolygonCollision2D;
import io.github.avantgarde95.balltest.physics2d.PolygonBody2D;
import io.github.avantgarde95.balltest.physics2d.Vector2D;
import io.github.avantgarde95.balltest.util.ArrayOperator;
import io.github.avantgarde95.balltest.util.Debug;
import io.github.avantgarde95.balltest.util.VecOperator;

/**
 * Created by avantgarde on 2017-01-20.
 */

public class MyGLRenderer implements GLSurfaceView.Renderer {
    private Context context; // for using Activity.getAssets()

    private float minDist = 1.0f;
    private float maxDist = 30.0f;

    private float[] eyePos = {0.0f, 0.0f, 4.0f};
    private float[] lookPos = {0.0f, 0.0f, -1.0f};
    private float[] upVec = {0.0f, 1.0f, 0.0f};
    private float[] viewOffset = {0.0f, 0.0f, -1.001f};

    private float[] bgColor = {1.0f, 1.0f, 1.0f, 1.0f};
    private float[] lightPos = {2.0f, 3.0f, 14.0f};

    public float[] viewMatrix = new float[16];
    public float[] viewRotationMatrix = new float[16];
    public float[] viewTranslationMatrix = new float[16];

    private float[] projMatrix = new float[16];

    private Circle ball;
    private CircleBody2D ballBody;
    private CirclePolygonCollision2D ballCollision;

    int rockCount;
    float[][][] rockVertices;
    private Model[] rocks;
    private PolygonBody2D[] rockBodies;
    private TextureModel imagemodel;
    private PolygonBody2D imagebody;

    public MyGLRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // background color
        GLES20.glClearColor(bgColor[0], bgColor[1], bgColor[2], bgColor[3]);

        // init. gl
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // init. view
        Matrix.setLookAtM(
                viewMatrix, 0,
                eyePos[0], eyePos[1], eyePos[2],
                lookPos[0], lookPos[1], lookPos[2],
                upVec[0], upVec[1], upVec[2]
        );

        // init. ball
        float ballRadius = 0.1f;
        ball = new Circle(this, ballRadius);

        ballBody = new CircleBody2D(
                1,
                new Vector2D(-0.25f, 1.0f),
                new Vector2D(0.0f, 0.0f),
                ballRadius
        );

        // init. rocks
        rockVertices = new float[][][]{
                {
                        {0.0f, -0.4f},
                        {-0.1f, -0.5f},
                        {-0.2f, -0.6f},
                        {-0.9f, -0.55f},
                        {-0.1f, -0.7f},
                        {0.0f, -0.4f}
                },
                {
                        {-0.2f, 0.35f},
                        {-0.1f, 0.4f},
                        {0.2f, 0.2f},
                        {0.3f, 0.0f},
                        {0.1f, 0.1f},
                        {-0.2f, 0.35f}
                }
        };

        rockCount = rockVertices.length;
        rocks = new Model[rockCount];
        rockBodies = new PolygonBody2D[rockCount];

        for (int i = 0; i < rockCount; i++) {
            rocks[i] = new Polygon(this, rockVertices[i]);
            rockBodies[i] = new PolygonBody2D(
                    1.0f, new Vector2D(0, 0), new Vector2D(0, 0), rockVertices[i]);
        }

        // init. collision
        ballCollision = new CirclePolygonCollision2D(ballBody);

        imagemodel = new TextureModel(this, 0);
        String filename = "brick1.png";
        imagemodel.setTextureFileName(filename);
        imagemodel.makeShader();
        makeModelByImageFile(imagemodel, filename);
        imagemodel.setMatrix(-0.2f, 0.0f, 0.0f);

        imagebody = new PolygonBody2D(1.0f, new Vector2D(0,0), new Vector2D(0, 0), imagemodel.getBorder());
        System.out.println("-------border-------");

        for(int i=0; i<imagemodel.getBorder().length; i++){
            System.out.println(imagemodel.getBorder()[i][0] + " " + imagemodel.getBorder()[i][1]);
        }
        // calc. view matrix
        Matrix.setIdentityM(viewRotationMatrix, 0);
        Matrix.setIdentityM(viewTranslationMatrix, 0);
        Matrix.translateM(viewTranslationMatrix, 0, viewOffset[0], viewOffset[1], viewOffset[2]);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // draw background
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // calc. view
        float[] tempMatrix = new float[16];
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.multiplyMM(tempMatrix, 0, viewRotationMatrix, 0, viewMatrix, 0);
        System.arraycopy(tempMatrix, 0, viewMatrix, 0, 16);
        Matrix.multiplyMM(tempMatrix, 0, viewTranslationMatrix, 0, viewMatrix, 0);
        System.arraycopy(tempMatrix, 0, viewMatrix, 0, 16);

        // physics
        Vector2D gravity = new Vector2D(0.0f, -0.0005f);
        float preserve = 0.3f;

        ballBody.addForce(gravity);
        ballBody.integrateForce(1.0f);

//
        ballCollision.collidePolygon(imagebody, preserve);
        // set matrix from physics
        ball.setMatrix(ballBody.evalMatrix());

        // XXX : These lines are unneeded if rocks' position is always (0, 0)
//        for (int i = 0; i < rockCount; i++) {
//            rocks[i].setMatrix(rockBodies[i].evalMatrix());
//        }

        // draw models
        ball.draw(projMatrix, viewMatrix, lightPos);

        for (Model rock : rocks) {
            rock.draw(projMatrix, viewMatrix, lightPos);
        }
        imagemodel.draw(projMatrix, viewMatrix, lightPos);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        Matrix.frustumM(
                projMatrix, 0,
                -1, 1,
                -(float) height / width, (float) height / width,
                minDist, maxDist
        );
    }

    public int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public int loadShader(int type, InputStream shaderFile) {
        String shaderCode = null;

        try {
            shaderCode = IOUtils.toString(shaderFile, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return loadShader(type, shaderCode);
    }

    public int loadShaderFromFile(int type, String fileName) {
        try {
            return loadShader(type, context.getAssets().open(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public Bitmap loadImage(String fileName) {
        try {
            Bitmap tmp = BitmapFactory.decodeStream(context.getAssets().open(fileName));
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.preScale(1.0f, -1.0f);
            Bitmap image = Bitmap.createBitmap(tmp, 0, 0, tmp.getWidth(), tmp.getHeight(), matrix, true);
            tmp.recycle();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    public void makeModelByImageFile(TextureModel m, String filename){
        Bitmap bitmap = loadImage(filename);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int []colors = new int[width * height];
        int []below = new int[width];
        int []above = new int[width];
        int transparent = 0xFF0000FF;

        System.out.println("bitmap " + width + " " + height);
        int minWidth = -1, maxWidth = -1;
        int minHeight = height, maxHeight = -1;
        bitmap.getPixels(colors, 0, width, 0, 0 , width, height);
        for (int i=0; i<width; i++){
            below[i] = -1;
            above[i] = -1;
            for(int j=0; j<height; j++){
                int color = colors[i + j * width];
                if(color != transparent){
                    if(minWidth == -1){
                        minWidth = i;
                    }
                    maxWidth = i;

                    if(below[i] == -1){
                        below[i] = j;
                    }
                    above[i] = j;

                    if(minHeight > j){
                        minHeight = j;
                    }
                    if(maxHeight < j){
                        maxHeight = j;
                    }
                }
            }
        }

        ArrayList<Point> aboves = new ArrayList<Point>();
        ArrayList<Point> belows = new ArrayList<Point>();
        ArrayList<DirectionalPoint> points = new ArrayList<DirectionalPoint>();

        aboves.add(new Point(minWidth, above[minWidth]));
        belows.add(new Point(minWidth, below[minWidth]));
        points.add(new DirectionalPoint(new Point(minWidth, above[minWidth]), Direction.above));
        points.add(new DirectionalPoint(new Point(minWidth, below[minWidth]), Direction.below));
        for(int i=minWidth + 2; i<maxWidth; i++){
            if((above[i] - above[i-1]) != (above[i-1] - above[i-2])){
                aboves.add(new Point(i-1, above[i-1]));
                points.add(new DirectionalPoint(new Point(i-1, above[i-1]), Direction.above));
            }
            if((below[i] - below[i-1]) != (below[i-1] - below[i-2])){
                belows.add(new Point(i-1, below[i-1]));
                points.add(new DirectionalPoint(new Point(i-1, below[i-1]), Direction.below));
            }
        }



        aboves.add(new Point(maxWidth - 1, above[maxWidth - 1]));
        belows.add(new Point(maxWidth - 1, below[maxWidth - 1]));
        points.add(new DirectionalPoint(new Point(maxWidth - 1, above[maxWidth - 1]), Direction.above));
        points.add(new DirectionalPoint(new Point(maxWidth - 1, below[maxWidth - 1]), Direction.below));

        System.out.println("pointsize " + points.size());

//        float[] vertices = new float[points.size() * 3];
//        float[] textures = new float[points.size() * 3];
//        for(int i=0; i<points.size(); i++){
//            Point p = new Point(points.get(i).p.x/width, points.get(i).p.y/height);
//            ArrayOperator.insertPoint(vertices, 3*i, p);
//            ArrayOperator.insertPoint(textures, 3*i, p);
//        }

        float[] vertices = new float[(points.size() - 2) * 9];
        float[] textures = new float[(points.size() - 2) * 9];
        for(int i=0; i<vertices.length; i++){
            vertices[i] = -1;
        }
        int [] indexs = new int[]{0,1,2};
        int offset = 0;
        while(indexs[2] < points.size()){
//            System.out.println("hi" + " " +  indexs[2] + " " +  points.size());
            offset = makeTriangles(vertices, offset, indexs, points);
        }
        ArrayOperator.scaleArray(vertices, (float)1/width, 0, 3);
        ArrayOperator.scaleArray(vertices, (float)1/height, 1, 3);
        System.arraycopy(vertices, 0, textures, 0 ,vertices.length);

        float[][] borders = new float[aboves.size() + belows.size()][2];
        for(int i=0; i<aboves.size(); i++){
            Point p = aboves.get(i);
            borders[i][0] = p.x;
            borders[i][1] = p.y;
        }
        for(int i=0; i<belows.size(); i++){
            Point p = belows.get(i);
            borders[aboves.size() + belows.size() - i - 1][0] = p.x;
            borders[aboves.size() + belows.size() - i - 1][1] = p.y;
        }

        int vwidth = maxWidth - minWidth;
        int vheight = maxHeight - minHeight;
        System.out.println(minWidth + " " + maxWidth + " " + width + " " + height);


        ArrayOperator.addArray(vertices, (float)-minWidth/width, 0, 3);
        ArrayOperator.addArray(vertices, (float)-minHeight/height, 1, 3);
        ArrayOperator.scaleArray(vertices, (float)width / vwidth * 2.0f, 0, 3);
        ArrayOperator.scaleArray(vertices, (float)height / vheight * 2.0f, 1, 3);
        ArrayOperator.addArray(vertices, -1.0f);
        ArrayOperator.scaleArray(vertices, (float)vheight / vwidth, 1, 3);
        for(int i=0 ;i<borders.length; i++){
            borders[i][0] = borders[i][0]/width - (float)minWidth/width;
            borders[i][0] *= (float)width / vwidth * 2.0f;
            borders[i][0] -= 1.0f;
            borders[i][1] = borders[i][1]/height - (float)minHeight/height;
            borders[i][1] *= (float)height / vheight * 2.0f;
            borders[i][1] -= 1.0f;
            borders[i][1] *= (float)vheight / vwidth;
        }
        ArrayOperator.addArray(vertices, 1.0f, 2 , 3);
        System.out.println("---texture---");
        Debug.printvert(textures);
        System.out.println("---vertice---");
        Debug.printvert(vertices);

        m.setVertices(vertices);
        m.setTextureCoords(textures);
        m.setBorder(borders);

//        m.setDrawType(GLES20.GL_LINE_STRIP);
        m.makeBuffer();
    }

    public int makeTriangles(float[] vertices, int offset, int[] indexs, ArrayList<DirectionalPoint> points){
//        System.out.println(offset + " " + indexs[0] + " " + indexs[1] + " " + indexs[2] + " " + points.size() + " " +  vertices.length);
        DirectionalPoint a = points.get(indexs[0]);
        DirectionalPoint b = points.get(indexs[1]);
        DirectionalPoint c = points.get(indexs[2]);
//        System.out.println("(" + a.p.x + " " + a.p.y + ") (" + b.p.x + " " + b.p.y + ") (" + c.p.x + " " + c.p.y + ")");


        Point ba = Point.sub(b.p, a.p);
        Point cb = Point.sub(c.p, b.p);
        float[] normal = new float[3];
        VecOperator.cross(ba.pointToVector(), cb.pointToVector(), normal);


        if(b.d == c.d){
            if(normal[2] < 0){
                int offset2 = offset + 9;
                int newc = indexs[2];
                int [] newindexs = new int[]{indexs[1], indexs[2], indexs[2]+1};
                while((normal[2] < 0 && a.d == Direction.above) || (normal[2] > 0 && a.d == Direction.below)) {
                    offset2 = makeTriangles(vertices, offset2, newindexs, points);
                    newc = newindexs[1];
                    c = points.get(newc);
                    cb = Point.sub(c.p, b.p);
                    VecOperator.cross(ba.pointToVector(), cb.pointToVector(), normal);
//                    System.out.println();
                    //조치가 필요
//                    System.out.println(indexs[0] + " " + indexs[1] + " " + newc + " " + normal[2]);
                }
                makeTriangleThreePoints(a.p, b.p, c.p, vertices, offset);
                offset = offset2;

                indexs[1] = newindexs[1];
                indexs[2] = newindexs[2];

                if(b.d != c.d){
                    indexs[0] = newindexs[0];
                }
                return offset;
            }
            else{
                if(vertices[offset + 2] == -1) {
                    makeTriangleThreePoints(a.p, b.p, c.p, vertices, offset);
                }
                int ret = indexs[2];
                indexs[1] = indexs[2];
                indexs[2] = indexs[2] + 1;
                offset = offset + 9;

                return offset;
            }
        }
        else{
            if(vertices[offset + 2] == -1) {
                makeTriangleThreePoints(a.p, b.p, c.p, vertices, offset);
            }
            int ret = indexs[2];
            indexs[0] = indexs[1];
            indexs[1] = indexs[2];
            indexs[2] = indexs[2]+1;
            offset = offset + 9;

            return offset;
        }
    }
    public int makeTriangles(float[] vertices, int offset, int aindex, int bindex, int cindex, ArrayList<DirectionalPoint> points){
        System.out.println(aindex + " " + bindex + " " + cindex + " " + offset + " " + points.size() + " " + vertices.length);

        if(cindex >= points.size()){
            return 0;
        }
        DirectionalPoint a = points.get(aindex);
        DirectionalPoint b = points.get(bindex);
        DirectionalPoint c = points.get(cindex);


        Point ba = Point.sub(b.p, a.p);
        Point cb = Point.sub(c.p, b.p);
        float[] normal = new float[3];
        VecOperator.cross(ba.pointToVector(), cb.pointToVector(), normal);


        if(b.d == c.d){
            if(normal[2] < 0){
                int offset2 = offset;
                int newc = cindex;
                while(normal[2] < 0) {
                    offset2 = offset2 + 9;
                    newc = makeTriangles(vertices, offset2, bindex, cindex, cindex + 1, points);
                    c = points.get(newc);
                    cb = Point.sub(c.p, b.p);
                    VecOperator.cross(ba.pointToVector(), cb.pointToVector(), normal);
                    //조치가 필요
                }
                makeTriangleThreePoints(a.p, b.p, c.p, vertices, offset);
                return newc;

            }
            else{
                if(vertices[offset + 2] != -1) {
                    makeTriangleThreePoints(a.p, b.p, c.p, vertices, offset);
                }
//                System.out.println(aindex + " " + cindex + " " + (cindex + 1) + " " + (offset+9) + " " + points.size() + " " + vertices.length);

                makeTriangles(vertices, offset + 9, aindex, cindex, cindex + 1, points);
                return cindex + 1;
            }
        }
        else{
            if(vertices[offset + 2] != -1) {
                makeTriangleThreePoints(a.p, b.p, c.p, vertices, offset);
            }

            makeTriangles(vertices, offset + 9, bindex, cindex, cindex + 1, points);
            return cindex + 1;
        }
    }

    public void makeTriangleThreePoints(Point a, Point b, Point c, float[] vertices, int offset){
        Point ba = Point.sub(b, a);
        Point cb = Point.sub(c, b);
        float[] normal = new float[3];
        VecOperator.cross(ba.pointToVector(), cb.pointToVector(), normal);
        if(normal[2] > 0){
            ArrayOperator.insertPoint(vertices, offset + 0, a);
            ArrayOperator.insertPoint(vertices, offset + 3, b);
            ArrayOperator.insertPoint(vertices, offset + 6, c);
        }
        else{
            ArrayOperator.insertPoint(vertices, offset + 0, c);
            ArrayOperator.insertPoint(vertices, offset + 3, b);
            ArrayOperator.insertPoint(vertices, offset + 6, a);

        }
    }

}
