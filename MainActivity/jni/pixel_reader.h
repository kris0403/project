/*
 * raw_capture_utils.h
 *
 *  Created on: 2014年8月6日
 *      Author: 安然
 */

#ifndef RAW_CAPTURE_UTILS_H_
#define RAW_CAPTURE_UTILS_H_

#include "structures.h"

class pixel_reader{
public:
	virtual inline int getYUV(int x,int y)=0;
	virtual inline int getRGB(int x,int y)=0;
	virtual ~pixel_reader(){}
};

class android_pixel_reader:public pixel_reader{
private:
	int frame_size;
	byte* data;

	static inline int __adjust(double x){
		if(x<0)return 0;
		if(x>255)return 255;
		return (int)x;
	}
	static inline int YUV2RGB(int yuv){
#define __Y (yuv>>16)
#define __U ((yuv>>8)&255)
#define __V (yuv&255)

	return ((__adjust(__Y+1.14*__V))<<16)|
				((__adjust(__Y-0.394*__U-0.581*__V))<<8)|
				((__adjust(__Y+2.032*__U)));
	}

	static inline int RGB2YUV(int rgb){
#define __R (rgb>>16)
#define __G ((rgb>>8)&255)
#define __B (rgb&255)

		return ((__adjust(0.299*__R+0.587*__G+0.114*__B))<<16)|
			    ((__adjust(-0.147*__R-0.289*__G+0.436*__B))<<8)|
			    ((__adjust(0.615*__R-0.515*__G-0.1*__B)));
	}

public:
	int w;
	int h;
	
	android_pixel_reader(int w,int h);

	inline void load_data(byte* data){this->data=data;}
	int getYUV(int x,int y);
	int getUV(int x,int y);
	byte getY(int x,int y);
	int getRGB(int x,int y);
};

int average_getY(int x,int y,android_pixel_reader* reader);
int average_getYUV(int x,int y,android_pixel_reader* reader);

/*
class android_pixel_reader_gaussian:public android_pixel_reader{
public:
	virtual inline int getYUV(int x,int y);
	virtual inline int getUV
*/




#endif /* RAW_CAPTURE_UTILS_H_ */
