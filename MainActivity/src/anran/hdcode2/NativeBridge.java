package anran.hdcode2;

public class NativeBridge {

	public static native boolean decodeblock(
			int ltx,int lty,int rtx,int rty,int lbx,int lby,int rbx,int rby,
			int sidelength, int npar, 
			int w, int h, 
			byte[] src, int[] dest);
	public static native boolean test(int x1,int x2,int x3,int x4,int x5,int x6,int x7,int x8,int x9,int x10,int x11,int x12,int x13,int x14,int x15);
	
	public static native boolean startdecode(int bx,int by,int fx,int fy,int w,int h);
	public static native boolean pushdata(byte[] src);
	public static native int pulldata(int npar,int[] destRAW);
	public static native boolean releasedata(byte[] src);
	public static native int getpoints(int[] xs,int[] ys);
	static {
        System.loadLibrary("HDCode2");
    }
}
