/*
 * pixel_reader.cpp
 */

#include "pixel_reader.h"

#ifndef NULL
#define NULL   ((void *) 0)
#endif

android_pixel_reader::android_pixel_reader(int _w,int _h):w(_w),h(_h),frame_size(_w*_h),data(NULL){}

#define MAKEYUV(y,u,v) (((y)<<16)|((u)<<8)|v)
inline int android_pixel_reader::getYUV(int x,int y){
	int uvp=frame_size+(y>>1)*w;
	int yp=y*w+x;
	int xa=x&0xfffffffe;

	return MAKEYUV(data[yp],data[uvp+xa],data[uvp+xa+1]);
}
inline int android_pixel_reader::getUV(int x,int y){
	int uvp=frame_size+(y>>1)*w;
	int xa=x&0xfffffffe;

	return (data[uvp+xa]<<8)|data[uvp+xa+1];
}
byte android_pixel_reader::getY(int x,int y){
	return data[y*w+x];
}
inline int android_pixel_reader::getRGB(int x,int y){
	return YUV2RGB(getYUV(x,y));
}


int average_getY(int x,int y,android_pixel_reader* reader){
	int l=x-1;
	int r=x+1;
	int t=y-1;
	int b=y+1;

	if(l<0)l=0;
	if(r>=reader->w)r=reader->w-1;
	if(t<0)t=0;
	if(b>=reader->h)b=reader->h-1;

	int tot=0;
	for(int i=l;i<=r;i++)
		for(int j=t;j<=b;j++)
			tot+=reader->getY(i,j);
	return tot/((r-l+1)*(b-t+1));
}

int average_getYUV(int x,int y,android_pixel_reader* reader){
	int l=x-1;
	int r=x+1;
	int t=y-1;
	int b=y+1;

	if(l<0)l=0;
	if(r>=reader->w)r=reader->w-1;
	if(t<0)t=0;
	if(b>=reader->h)b=reader->h-1;

	int totY=0,totU=0,totV=0;
	for(int i=l;i<=r;i++)
		for(int j=t;j<=b;j++){
			int r=reader->getYUV(i,j);
			totY+=(r>>16);
			totU+=((r>>8)&255);
			totV+=(r&255);
		}
	return ((totY<<16)|(totU<<8)|(totV))/((r-l+1)*(b-t+1));
}

