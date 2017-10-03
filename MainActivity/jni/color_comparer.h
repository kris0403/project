

#ifndef COLOR_COMPARER_H_
#define COLOR_COMPARER_H_

#include <assert.h>
#include "structures.h"

#ifndef NULL
#define NULL   ((void *) 0)
#endif


class color_comparer{
private:
	int* colors;
	int color_count;
	inline int abs(int x){
		return x>=0?x:-x;
	}
public:
	color_comparer(){
		colors=NULL;
		color_count=0;
	}
	void set_palette(int* colors,int color_count){
		this->colors=colors;
		this->color_count=color_count;
	}
	int find_nearest(int color){
		int color_id = 0;
		int curY = color >> 16, curU = (color >> 8) & 255, curV = color & 255;
		int lowY = 0, lowU = 0, lowV = 0;
		int highY = 0, highU = 0, highV = 0;
		for (int i = 0; i < color_count / 2; i++)
		{
			highY += (colors[i] >> 16);
			highU += ((colors[i] >> 8) & 255);
			highV += (colors[i] & 255);
		}
		for (int i = color_count / 2; i < color_count; i++)
		{
			lowY += (colors[i] >> 16);
			lowU += ((colors[i] >> 8) & 255);
			lowV += (colors[i] & 255);
		}
		highY /= 2;
		highU /= 2;
		highV /= 2;
		lowY /= 2;
		lowU /= 2;
		lowV /= 2;
		if (highY < lowY)
		{
		int temp = highY;
		highY = lowY;
		lowY = temp;
		}
		if (highU < lowU)
		{
		int temp = highU;
		highU = lowU;
		lowU = temp;
		}
		if (highV < lowV)
		{
		int temp = highV;
		highV = lowV;
		lowV = temp;
		}
		if (abs(highY - lowY) < 20 && highY > lowY)
		{
			highY += 20;
			lowY -= 20;
		}
		if (abs(highU - lowU) < 20 && highU > lowU)
		{
			highU += 20;
			lowU -= 20;
		}
		if (abs(highV - lowV) < 20 && highV > lowV)
		{
			highV += 20;
			lowV -= 20;
		}
		int midY = (highY + lowY) / 2;
		float rangeY = highY - midY;
		int midU = (highU + lowU) / 2;
		float rangeU = highU - midU;
		int midV = (highV + lowV) / 2;
		float rangeV = highV - midV;
		if (curY >= midY)
			color_id += 4;
		if (curU >= midU)
			color_id += 1;//previous: color_id += 2
		if (curV >= midV)
			color_id += 2;//previous: color_id += 1
		return color_id;
	}
};




#endif /* COLOR_COMPARER_H_ */
