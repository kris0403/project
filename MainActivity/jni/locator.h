#ifndef LOCATOR_H_
#define LOCATOR_H_

#include "structures.h"
#include "pixel_reader.h"

class android_center_locator{
private:
	android_pixel_reader* preader;

	int _sample_random(int xf, int xt,int yf,int yt, int bthres, int& bnum,int& x,int& y);
	bool _mean_shift(int& x,int& y,int rad,int& bthres,int iteration=0);
	bool _fast_adjust(int& x,int& y, int rad,int bthres);
public:
	android_center_locator(android_pixel_reader* reader);
	bool do_full_locate(int& cx, int& cy, int& bthres);
	bool do_adjust_locate(int& cx, int& cy, int& bthres);
};

class android_distributed_locator{
private:
	android_pixel_reader* preader;

public:
	android_distributed_locator(android_pixel_reader* reader);
	bool do_locate(int& cx,int& cy,int& bthres,int estimated_sidelength,bool force_mean_shift=false);
};

class android_center_block_locator{
private:
	android_pixel_reader* preader;
	android_distributed_locator* d_locator;

public:
	android_center_block_locator(android_pixel_reader* reader,android_distributed_locator* d_locator);
	bool do_locate(int cx,int cy,int c_bthres, int block_size_x, int block_size_y, int* xs,int* ys,int* bthres);
};

class android_symbol_locator{
private:
	int bw,bh;
	int* xs;
	int* ys;
public:
	android_symbol_locator(int _bw,int _bh);
	void register_four_corner(int* xs,int* ys);
	void do_locate(int sx,int sy,int& px,int& py);
};

#endif

