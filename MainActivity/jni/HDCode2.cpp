#include <jni.h>
#include "structures.h"
#include "locator.h"
#include "decoder.h"
#include "demodulator.h"
#include "log.h"

#define JNICALL
#define JNIEXPORT

#define JNI(x) JNIEXPORT JNICALL Java_anran_hdcode2_NativeBridge_##x
extern "C"{
jboolean JNI(test)(JNIEnv* env, jobject t, jint x1,jint x2,jint x3,jint x4,jint x5,
		jint x6,jint x7,jint x8,jint x9,jint x10,
		jint x11,jint x12,jint x13,jint x14,jint x15){
	return true;
}

static android_demodulator* dem=NULL;
int b_sidelength;

jboolean JNI(startdecode)(JNIEnv* env,jobject t,
		int bx,int by,int fx,int fy,
		int w,int h){
	b_sidelength=bx;
	dem=new android_demodulator(layout_t(0,0,bx,by,fx,fy),new android_pixel_reader(w,h));
	dem->start();
	return true;
}

byte* lock_arr_data=NULL;

jboolean JNI(pushdata)(JNIEnv* env,jobject t,
		jbyteArray src){
	if(lock_arr_data!=NULL)return false;
	byte* src_data=(byte*)(env->GetByteArrayElements(src,0));
	lock_arr_data=src_data;
	dem->push_raw_data(src_data);
}

jboolean JNI(releasedata)(JNIEnv* env, jobject t,
		jbyteArray src){
	if(lock_arr_data==NULL)return false;
	env->ReleaseByteArrayElements(src,(jbyte*)lock_arr_data,0);
	lock_arr_data=NULL;
	return true;
}
reed_solomon_code* coder=NULL;

jint JNI(pulldata)(JNIEnv* env, jobject t,
		jint npar, jintArray destRAW){
	if(dem==NULL)return false;
	if(lock_arr_data==NULL)return false;
	byte raw_data[300];
	block_t res(raw_data);
	int bid,fid;
	while(dem->pull_block(res,fid,bid)){

		int* dest_data2=(int*)(env->GetIntArrayElements(destRAW,0));
		int dest_length2=env->GetArrayLength(destRAW);

		for(int i=0;i<dest_length2;i++)
			dest_data2[i]=res.data[i+7];

		env->ReleaseIntArrayElements(destRAW,dest_data2,0);
		return bid;
	}
	return -1;
}

jboolean JNI(decodeblock)(JNIEnv* env,jobject t,
		jint ltx,jint lty,jint rtx,jint rty,jint lbx,jint lby,jint rbx,jint rby,
		jint b_sidelength,jint npar,
		jint w,jint h,
		jbyteArray src,jintArray dest){
	android_symbol_locator sl(b_sidelength,b_sidelength);
	int xs[4]={ltx,rtx,rbx,lbx};
	int ys[4]={lty,rty,rby,lby};
	sl.register_four_corner(xs,ys);
	android_pixel_reader preader(w,h);

	byte* src_data=(byte*)(env->GetByteArrayElements(src,0));
	int* dest_data=(int*)(env->GetIntArrayElements(dest,0));
	int dest_length=env->GetArrayLength(dest);
	byte raw_dest_data[100];
	preader.load_data(src_data);

	byte raw_data[300];
	block_t b(raw_data);
	parse_color(&sl,&preader,b_sidelength,b);

	reed_solomon_code coder(b_sidelength*b_sidelength/4-2,b_sidelength*b_sidelength/4-2-npar);
	bool res=parse(b,&coder,b_sidelength,raw_dest_data);
	for(int i=0;i<dest_length;i++)
		dest_data[i]=raw_dest_data[i];

	env->ReleaseByteArrayElements(src,(jbyte*)src_data,0);
	env->ReleaseIntArrayElements(dest,(jint*)dest_data,0);
	logi("jni_decode","%d",res);
	return res;
}

jint JNI(getpoints)(JNIEnv* env,jobject t,
		jintArray xs,jintArray ys){
	if(dem==NULL)return 0;
	int res=0;
	int* xs_data=env->GetIntArrayElements(xs,0);
	int* ys_data=env->GetIntArrayElements(ys,0);
	dem->test_get_d_locations(res,xs_data,ys_data);
	env->ReleaseIntArrayElements(xs,xs_data,0);
	env->ReleaseIntArrayElements(ys,ys_data,0);
	return res;
}
}
