package io.github.avantgarde95.balltest.model;

import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import io.github.avantgarde95.balltest.renderer.MyGLRenderer;

/**
 * Created by Jongmin on 2017-01-15.
 */

public class TextureModel extends Model {
    private int mTextureHandle;
    private int mTextureCoorHandle;
    private FloatBuffer mTextureCoorBuffer;

    private String textureFileName = "default.png";
    private int texture = 0;

    protected float textureCoords[];
   
    public TextureModel(MyGLRenderer mRenderer, int texture){
        super(mRenderer);
        setShader("texture-gl2-vshader.glsl", "texture-gl2-fshader.glsl");
        this.texture = texture;
    }
    
    public void setTextureCoords(float[] textureCoords){
        this.textureCoords = new float[textureCoords.length];
        System.arraycopy(textureCoords, 0, this.textureCoords, 0, textureCoords.length);
    }

    public void setTextureFileName(String fileName){
        this.textureFileName = fileName;
    }

    @Override
    public void makeBuffer() {
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        vertexBuffer = byteBuf.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        byteBuf = ByteBuffer.allocateDirect(textureCoords.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mTextureCoorBuffer = byteBuf.asFloatBuffer();
        mTextureCoorBuffer.put(textureCoords);
        mTextureCoorBuffer.position(0);
    }

    @Override
    public void makeShader(){
        // prepare shaders and OpenGL program
        int vShader = renderer.loadShaderFromFile(
                GLES20.GL_VERTEX_SHADER, vertexShader);
        int fShader = renderer.loadShaderFromFile(
                GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        program = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(program, vShader);   // add the vertex shader to program
        GLES20.glAttachShader(program, fShader); // add the fragment shader to program
        GLES20.glLinkProgram(program);                  // create OpenGL program executables


        int[] textureHandles = new int[1];
        GLES20.glGenTextures(1, textureHandles, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + texture);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, renderer.loadImage(textureFileName), 0);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    }

    @Override
    public void draw(float[] projMatrix,
                     float[] viewMatrix, float[] light) {

        GLES20.glUseProgram(program);
        float[] modelViewMatrix = new float[16];
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
//        MatOperator.print(modelViewMatrix);
//        MatOperator.print(projMatrix);
        // uniforms
        int mProjMatrixHandle = GLES20.glGetUniformLocation(program, "uProjMatrix");
        int mModelViewMatrixHandle = GLES20.glGetUniformLocation(program, "uModelViewMatrix");
        int mColorHandle = GLES20.glGetUniformLocation(program, "uColor");

        //mLightHandle = GLES20.glGetUniformLocation(program, "uLight");

        GLES20.glUniformMatrix4fv(mProjMatrixHandle, 1, false, projMatrix, 0);
        GLES20.glUniformMatrix4fv(mModelViewMatrixHandle, 1, false, modelViewMatrix, 0);

        GLES20.glUniform3fv(mColorHandle, 1, color, 0);
//        GLES20.glUniform3fv(mLightHandle, 1, light, 0);

        // attributes
        int mPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        int mNormalHandle = GLES20.glGetAttribLocation(program, "aNormal");


        mTextureHandle = GLES20.glGetUniformLocation(program, "uTextureUnit");
        mTextureCoorHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
        GLES20.glEnableVertexAttribArray(mTextureCoorHandle);
        GLES20.glVertexAttribPointer(
                mTextureCoorHandle, coordsPerVertex,
                GLES20.GL_FLOAT, false,
                vertexStride, mTextureCoorBuffer
        );

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(
                mPositionHandle, coordsPerVertex,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        GLES20.glUniform1i(mTextureHandle, texture);

        // Draw the cube
        GLES20.glDrawArrays(drawType, 0, vertices.length / 3);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mNormalHandle);


        GLES20.glDisableVertexAttribArray(mTextureHandle);

    }
}
