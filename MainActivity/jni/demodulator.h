

#ifndef DEMODULATOR_H_
#define DEMODULATOR_H_

#include <functional>
#include "structures.h"
#include "pixel_reader.h"
#include "locator.h"
#include "color_comparer.h"
#include "queue"

//interfaces:

class demodulator{
public:
	virtual bool pull_block(block_t& result, int& fid, int& bid)=0;
	virtual ~demodulator(){}
};

//concrete:

struct received_block_t{
	block_t content;
	int fid;
	int bid;
	received_block_t(block_t b,int _fid,int _bid):content(b),fid(_fid),bid(_bid){}
};

#define LOCATE_STATE_LOCATED 1
#define LOCATE_STATE_TOLOCATE 2
#define LOCATE_STATE_NOTLOCATED 0

#define STATE_NOTSTARTED 0
#define STATE_CENTERING 1
#define STATE_LOCATING 2
#define STATE_READY 3

#define INIT_BTHRES(x) ((x)=64)

struct demodulator_state_t{
	int state;

	int center_locator_state;
	int center_locator_x,center_locator_y;
	int center_locator_bthres;
	

	//block_state_t* block_state;
	int* distributed_locator_state;
	int* distributed_locator_bthres;
	int* distributed_locator_x, *distributed_locator_y;

	int shared_sidelength;

	demodulator_state_t():state(STATE_NOTSTARTED),distributed_locator_state(NULL),distributed_locator_bthres(NULL), distributed_locator_x(NULL),distributed_locator_y(NULL){
		INIT_BTHRES(center_locator_bthres);
		center_locator_state=LOCATE_STATE_NOTLOCATED;
		center_locator_x=center_locator_y=0;
		shared_sidelength=0;
	}
};

class color_comparer;

class android_demodulator:public demodulator{
private:
	android_pixel_reader* preader;
	int current_pulled_bid;

	demodulator_state_t demodulator_state;

	android_center_locator c_locator;
	android_distributed_locator d_locator;
	android_center_block_locator b_locator;
	android_symbol_locator s_locator;

	layout_t layout;

	bool __parse_frame_header(int* xs,int* ys);
	bool __locate_center_locator();
	bool __maintain_locators();
	bool __locate_center_block();
	void __clear_locator_state();
	void __find_adj(int x,int y);
	bool __locate_symbols(int* xs,int* ys,block_t dest,color_comparer* cmp);

#define __MAX_LOCATOR 500
	struct {
		int tx;
		int ty;
		int lx1;
		int ly1;
		int lx2;
		int ly2;
	}locator_queue[__MAX_LOCATOR];
	int head=0,tail=0;

	int frame_num;

public:
	android_demodulator(layout_t layout, android_pixel_reader* preader);
	bool pull_block(block_t& result, int& fid, int& bid);
	
	void start();

	bool push_raw_data(byte* data);
	//void push_raw_data_async(byte* data, void* context,std::function<void(byte*,void*,bool)> notification){}

	void test_get_d_locations(int& count, int* xs,int* ys);
};

#endif /* DEMODULATOR_H_ */
