package com.rstest;

import android.app.Activity;
import android.os.Bundle;
import android.support.v8.renderscript.*;
import android.widget.ImageView;

public class MainActivity extends Activity 
{	
	private final int       mWidth = 1920;
	private final int       mHeight = 1080;
	private final int       mSize = mWidth*mHeight;
	private RenderScript    mRS;
	private ScriptC_rgb2yuv mRgb2Yuv;
	private Allocation      mYInAllocation;
	private Allocation      mUInAllocation;
	private Allocation      mVInAllocation;
	private Allocation      mUVInAllocation;
	private Allocation      mYUVInAllocation;
	private Allocation      mYOutAllocation;
	private Allocation      mUOutAllocation;
	private Allocation      mVOutAllocation;
	private Allocation      mUVOutAllocation;
	private Allocation      mYUVOutAllocation;
	
	ImageView snapshotView = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		long time = 0;
		
		mRS = RenderScript.create(this);
		mRgb2Yuv = new ScriptC_rgb2yuv(mRS);
		
		snapshotView = (ImageView) findViewById(R.id.snapshot_view);
		VideoCapture.setImageView(snapshotView);
		VideoCapture.setRenderScript(mRS);
//		time = System.currentTimeMillis();
//		runRgb2YTest();
//		Log.e("RenderScript", "runRgb2YTest time = " + (System.currentTimeMillis()-time));
//		
//		time = System.currentTimeMillis();
//		runRgb2UTest();
//		Log.e("RenderScript", "runRgb2UTest time = " + (System.currentTimeMillis()-time));
//		
//		time = System.currentTimeMillis();
//		runRgb2VTest();
//		Log.e("RenderScript", "runRgb2VTest time = " + (System.currentTimeMillis()-time));
//		
//		time = System.currentTimeMillis();
//		runRgb2UvTest();
//		Log.e("RenderScript", "runRgb2UvTest time = " + (System.currentTimeMillis()-time));
//		
//		time = System.currentTimeMillis();
//		runRgb2YuvTest();
//		Log.e("RenderScript", "runRgb2YuvTest time = " + (System.currentTimeMillis()-time));
//		
//		time = System.currentTimeMillis();
//		runJavaRgb2YuvTest();
//		Log.e("RenderScript", "runJavaRgb2YuvTest time = " + (System.currentTimeMillis()-time));
//		
//		byte[] n21ByteArray = new byte[6*mSize/4];
//		time = System.currentTimeMillis();
//		N21ToRGBA(n21ByteArray);
//		Log.e("RenderScript", "N21ToRGBA time = " + (System.currentTimeMillis()-time));
//		
//		time = System.currentTimeMillis();
//		runJavaYuv2RgbTest(n21ByteArray);
//		Log.e("RenderScript", "runJavaYuv2RgbTest time = " + (System.currentTimeMillis()-time));
		
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		VideoCapture.Stop();
	}
	
	private byte[] runRgb2YTest() {		
		byte[] y_in_buf = new byte[4*mSize];
		byte[] y_out_buf = new byte[mSize];
		Element e_u8_4 = Element.U8_4(mRS);
		Element e_u8 = Element.U8(mRS);
		
		mYInAllocation = Allocation.createSized(mRS, e_u8_4, mSize);
		mYOutAllocation = Allocation.createSized(mRS, e_u8, mSize);
		
		mYInAllocation.copyFrom(y_in_buf);
		mRgb2Yuv.forEach_convert2y(mYInAllocation, mYOutAllocation);
		mYInAllocation.copyTo(y_out_buf);
		
		return y_out_buf;
	}
	
	private byte[] runRgb2UTest() {
		byte[] u_in_buf = new byte[mSize];
		byte[] u_out_buf = new byte[mSize/4];
		Element e_u8_4 = Element.U8_4(mRS);
		Element e_u8 = Element.U8(mRS);

		mUInAllocation = Allocation.createSized(mRS, e_u8_4, mSize/4);
		mUOutAllocation = Allocation.createSized(mRS, e_u8, mSize/4);
		
		mUInAllocation.copyFrom(u_in_buf);
		mRgb2Yuv.forEach_convert2u(mUInAllocation, mUOutAllocation);
		mUInAllocation.copyTo(u_out_buf);
		
		return u_out_buf;
	}
	
	private byte[] runRgb2VTest() {
		byte[] v_in_buf = new byte[mSize];
		byte[] v_out_buf = new byte[mSize/4];
		Element e_u8_4 = Element.U8_4(mRS);
		Element e_u8 = Element.U8(mRS);

		mVInAllocation = Allocation.createSized(mRS, e_u8_4, mSize/4);
		mVOutAllocation = Allocation.createSized(mRS, e_u8, mSize/4);
		
		mVInAllocation.copyFrom(v_in_buf);
		mRgb2Yuv.forEach_convert2v(mVInAllocation, mVOutAllocation);
		mVInAllocation.copyTo(v_out_buf);
		
		return v_out_buf;
	}
	
	private byte[] runRgb2UvTest() {
		byte[] uv_in_buf = new byte[mSize];
		byte[] uv_out_buf = new byte[mSize/2];
		Element e_u8_4 = Element.U8_4(mRS);
		Element e_u8_2 = Element.U8_2(mRS);

		mUVInAllocation = Allocation.createSized(mRS, e_u8_4, mSize/4);
		mUVOutAllocation = Allocation.createSized(mRS, e_u8_2, mSize/4);

		mUVInAllocation.copyFrom(uv_in_buf);
		mRgb2Yuv.forEach_convert2uv(mUVInAllocation, mUVOutAllocation);
		mUVInAllocation.copyTo(uv_out_buf);
		
		return uv_out_buf;
	}
	
	private byte[] runRgb2YuvTest() {
		byte[] yuv_in_buf = new byte[4*mSize];
		byte[] yuv_out_buf = new byte[3*mSize];
		Element e_u8_4 = Element.U8_4(mRS);
		Element e_u8_3 = Element.U8_3(mRS);

		mYUVInAllocation = Allocation.createSized(mRS, e_u8_4, mSize);
		mYUVOutAllocation = Allocation.createSized(mRS, e_u8_3, mSize);

		mYUVInAllocation.copyFrom(yuv_in_buf);
		mRgb2Yuv.forEach_convert2yuv(mYUVInAllocation, mYUVOutAllocation);
		mYUVInAllocation.copyTo(yuv_out_buf);
		
		return yuv_out_buf;
	}
	
	private byte[] runJavaRgb2YuvTest() {
		byte[] yuv_in_buf = new byte[4*mSize];
		byte[] yuv_out_buf = new byte[6*mSize/4];
		int    u_offset = mSize;
		int    v_offset = mSize*5/4;
		int    index = 0;
		byte   R = 0;
		byte   G = 0;
		byte   B = 0;
		byte   Y = 0;
		
		for (int j=0; j<mHeight; j++) {
			for (int i=0; i<mWidth; i++) {
				index = 4*(i+j*mWidth);
				R = yuv_in_buf[index];
				G = yuv_in_buf[index+1];
				B = yuv_in_buf[index+2];
				yuv_out_buf[i+j*mWidth] = (byte)(0.299*R + 0.587*G + 0.114*B); 
				Y = yuv_out_buf[i+j*mWidth];
				if (0 == i%2 && 0 == j%2) {
					yuv_out_buf[u_offset] = (byte)(0.492*(B-Y));
					u_offset++;
					yuv_out_buf[v_offset] = (byte)(0.877*(R-Y));
					v_offset++;
				}
			}
		}

		return yuv_out_buf;		
	}
	
	private byte[] N21ToRGBA(final byte[] n21ByteArray) {
		byte[] rgbaArray = new byte[4*mWidth*mHeight];
	    ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
	    Type.Builder            n21Type;
	    Type.Builder            rgbaType;
	    Allocation              in;
	    Allocation              out;
	    
	    yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(mRS, Element.U8_4(mRS));
	    
	    n21Type = new Type.Builder(mRS, Element.U8(mRS));
	    n21Type.setX(n21ByteArray.length);
	    in = Allocation.createTyped(mRS, n21Type.create());

	    rgbaType = new Type.Builder(mRS, Element.RGBA_8888(mRS));
	    rgbaType.setX(mWidth);
	    rgbaType.setY(mHeight);
	    out = Allocation.createTyped(mRS, rgbaType.create());

	    in.copyFrom(n21ByteArray);
	    yuvToRgbIntrinsic.setInput(in);
	    yuvToRgbIntrinsic.forEach(out);
	    out.copyTo(rgbaArray);
	    
	    return rgbaArray;
	}
	
	private byte[] runJavaYuv2RgbTest(final byte[] yuv420ByteArray) {
		byte[] yuv_out_buf = new byte[4*mSize];
		int    u_offset = mSize;
		int    v_offset = mSize*5/4;
		int    index = 0;
		byte   R = 0;
		byte   G = 0;
		byte   B = 0;
		byte   Y = 0;
		byte   U = 0;
		byte   V = 0;
		
		for (int j=0; j<mHeight; j++) {
			for (int i=0; i<mWidth; i++) {
				Y = yuv420ByteArray[i+j*mWidth];
				if (0 == i%2 && 0 == j%2) {
					U = yuv420ByteArray[u_offset];
					V = yuv420ByteArray[v_offset];
					u_offset++;
					v_offset++;
				}
				
				R = (byte) (Y + 1.140*V);
				G = (byte) (Y - 0.395*U - 0.581*V);
				B = (byte) (Y + 2.032*U);
								
				index = 4*(i+j*mWidth);
				yuv_out_buf[index] = R;
				yuv_out_buf[index+1] = G;
				yuv_out_buf[index+2] = B;
			}
		}

		return yuv_out_buf;		
	}
}
