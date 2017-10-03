
#include <functional>
#include <stdlib.h>
#include <memory.h>
#include "demodulator.h"
#include "pixel_reader.h"
#include "color_comparer.h"
#include "locator.h"
#include "log.h"
#include <time.h>

//static double now_ms(void) {
//
//    struct timespec res;
//    clock_gettime(CLOCK_REALTIME, &res);
//    return 1000.0 * res.tv_sec + (double) res.tv_nsec / 1e6;
//
//}

android_demodulator::android_demodulator(layout_t _layout, android_pixel_reader* _preader):
	preader(_preader),layout(_layout),
	c_locator(android_center_locator(_preader)),
	d_locator(android_distributed_locator(_preader)),
	b_locator(android_center_block_locator(_preader,&d_locator)),
	s_locator(android_symbol_locator(_layout.block_size_x,_layout.block_size_y)),
	frame_num(0),current_pulled_bid(0){



	//initialize arrays
	//demodulator_state.block_state=new block_state_t[layout.frame_size_x*layout.frame_size_y];
	int total_d_locator_num=(layout.frame_size_x+1)*(layout.frame_size_y+1);
	demodulator_state.distributed_locator_bthres=new int[total_d_locator_num]();
	demodulator_state.distributed_locator_state=new int[total_d_locator_num]();
	demodulator_state.distributed_locator_x=new int[total_d_locator_num]();
	demodulator_state.distributed_locator_y=new int[total_d_locator_num]();

	//clear

}

#define GET_LT_LOCATOR_ID(x,y) ((y)*(layout.frame_size_x+1)+(x))
#define GET_RT_LOCATOR_ID(x,y)  ((y)*(layout.frame_size_x+1)+(x)+1)
#define GET_LB_LOCATOR_ID(x,y) (((y)+1)*(layout.frame_size_x+1)+(x))
#define GET_RB_LOCATOR_ID(x,y)  (((y)+1)*(layout.frame_size_x+1)+(x)+1)
#define GET_LOCATOR_ID(x,y) ((y)*(layout.frame_size_x+1)+(x))


bool android_demodulator::pull_block(block_t& result, int& fid,int& bid){
	color_comparer cmp;

	while(current_pulled_bid<layout.frame_size_x*layout.frame_size_y){
		bid=current_pulled_bid;
		int i=current_pulled_bid%layout.frame_size_x;
		int j=current_pulled_bid/layout.frame_size_x;
		current_pulled_bid++;

		int xs[4],ys[4],ss[4];
		ss[0]=demodulator_state.distributed_locator_state[GET_LT_LOCATOR_ID(i,j)];
		xs[0]=demodulator_state.distributed_locator_x[GET_LT_LOCATOR_ID(i,j)];
		ys[0]=demodulator_state.distributed_locator_y[GET_LT_LOCATOR_ID(i,j)];

		ss[1]=demodulator_state.distributed_locator_state[GET_RT_LOCATOR_ID(i,j)];
		xs[1]=demodulator_state.distributed_locator_x[GET_RT_LOCATOR_ID(i,j)];
		ys[1]=demodulator_state.distributed_locator_y[GET_RT_LOCATOR_ID(i,j)];

		ss[2]=demodulator_state.distributed_locator_state[GET_RB_LOCATOR_ID(i,j)];
		xs[2]=demodulator_state.distributed_locator_x[GET_RB_LOCATOR_ID(i,j)];
		ys[2]=demodulator_state.distributed_locator_y[GET_RB_LOCATOR_ID(i,j)];

		ss[3]=demodulator_state.distributed_locator_state[GET_LB_LOCATOR_ID(i,j)];
		xs[3]=demodulator_state.distributed_locator_x[GET_LB_LOCATOR_ID(i,j)];
		ys[3]=demodulator_state.distributed_locator_y[GET_LB_LOCATOR_ID(i,j)];

		if(ss[0]==LOCATE_STATE_LOCATED&&ss[1]==LOCATE_STATE_LOCATED&&
				ss[2]==LOCATE_STATE_LOCATED&&ss[3]==LOCATE_STATE_LOCATED&&
				__locate_symbols(xs,ys,result,&cmp)){
			return true;
		}
	}
	return false;
}

bool android_demodulator::__parse_frame_header(int* xs,int* ys){
	//s_locator.register_four_corner(xs,ys);
	//currently no use
	return true;
}

bool android_demodulator::__locate_center_locator(){
	if(demodulator_state.center_locator_state==LOCATE_STATE_LOCATED){
		if(!c_locator.do_adjust_locate(demodulator_state.center_locator_x,demodulator_state.center_locator_y,
				demodulator_state.center_locator_bthres)){
			demodulator_state.center_locator_state=LOCATE_STATE_NOTLOCATED;
			return false;
		}
		else return true;
	}
	else{
		if(!c_locator.do_full_locate(demodulator_state.center_locator_x,demodulator_state.center_locator_y,
				demodulator_state.center_locator_bthres))
			return false;
		else {
			demodulator_state.center_locator_state=LOCATE_STATE_LOCATED;
			return true;
		}
	}
}


void android_demodulator::__find_adj(int i,int j){
	//find adjacent
	if(i!=0&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i-1,j)]==LOCATE_STATE_NOTLOCATED&&
			i!=layout.frame_size_x&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i+1,j)]==LOCATE_STATE_LOCATED){
		locator_queue[tail++]={i-1,j,i,j,i+1,j};
		demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i-1,j)]=LOCATE_STATE_TOLOCATE;
	}
	if(i!=layout.frame_size_x&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i+1,j)]==LOCATE_STATE_NOTLOCATED&&
			i!=0&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i-1,j)]==LOCATE_STATE_LOCATED){
		locator_queue[tail++]={i+1,j,i,j,i-1,j};
		demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i+1,j)]=LOCATE_STATE_TOLOCATE;
	}
	if(j!=0&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i,j-1)]==LOCATE_STATE_NOTLOCATED&&
			j!=layout.frame_size_y&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i,j+1)]==LOCATE_STATE_LOCATED){
		locator_queue[tail++]={i,j-1,i,j,i,j+1};
		demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i,j-1)]=LOCATE_STATE_TOLOCATE;
	}
	if(j!=layout.frame_size_y&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i,j+1)]==LOCATE_STATE_NOTLOCATED&&
			j!=0&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i,j-1)]==LOCATE_STATE_LOCATED){
		locator_queue[tail++]={i,j+1,i,j,i,j-1};
		demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i,j+1)]=LOCATE_STATE_TOLOCATE;
	}
}

void get_third_point(int x1,int y1,int x2,int y2,int& x3,int& y3){
	x3=2*x2-x1;
	y3=2*y2-y1;
}

bool android_demodulator::__maintain_locators(){
#define __PUSH_QUEUE(a) locator_queue[tail++]=(a)
#define __POP_QUEUE() locator_queue[head++]
#define __QUEUE_EMPTY() (head==tail)
#define __QUEUE_SIZE() (tail-head)

	head=tail=0;


	//step 1: update all located locators

	for(int i=0;i<layout.frame_size_x+1;i++)
		for(int j=0;j<layout.frame_size_y+1;j++){
			auto lid=GET_LOCATOR_ID(i,j);
			if(demodulator_state.distributed_locator_state[lid]==LOCATE_STATE_LOCATED){
				if(!d_locator.do_locate(demodulator_state.distributed_locator_x[lid],demodulator_state.distributed_locator_y[lid],demodulator_state.distributed_locator_bthres[lid],demodulator_state.shared_sidelength))
					demodulator_state.distributed_locator_state[lid]=LOCATE_STATE_NOTLOCATED;
			}
		}
	//step 1: check all located locators

	int tot=0;
	for(int i=0;i<layout.frame_size_x+1;i++)
		for(int j=0;j<layout.frame_size_y+1;j++){
			if(demodulator_state.distributed_locator_state[GET_LOCATOR_ID(i,j)]==LOCATE_STATE_LOCATED){
				tot++;
				__find_adj(i,j);
			}
		}

	//logi("locate","pre-located: %d %d",tot, __QUEUE_SIZE());

	if(tot<layout.frame_size_x*layout.frame_size_y/2&&__QUEUE_SIZE()==0)
		return false;

	//step 2: recursively locate the locators in the queue

	while(!__QUEUE_EMPTY()){
		auto l=__POP_QUEUE();
		auto lid2=GET_LOCATOR_ID(l.lx2,l.ly2);
		auto lid1=GET_LOCATOR_ID(l.lx1,l.ly1);
		auto lid=GET_LOCATOR_ID(l.tx,l.ty);
		get_third_point(demodulator_state.distributed_locator_x[lid2],demodulator_state.distributed_locator_y[lid2],
				demodulator_state.distributed_locator_x[lid1],demodulator_state.distributed_locator_y[lid1],
				demodulator_state.distributed_locator_x[lid],demodulator_state.distributed_locator_y[lid]);

		demodulator_state.distributed_locator_bthres[lid]=(demodulator_state.distributed_locator_bthres[lid2]+demodulator_state.distributed_locator_bthres[lid1])/2;

		if(d_locator.do_locate(demodulator_state.distributed_locator_x[lid],demodulator_state.distributed_locator_y[lid],
				demodulator_state.distributed_locator_bthres[lid],
				demodulator_state.shared_sidelength,true)){
			demodulator_state.distributed_locator_state[lid]=LOCATE_STATE_LOCATED;
			////logi("locate","success %d %d",l.tx,l.ty);
			__find_adj(l.tx,l.ty);
			if(l.tx!=0&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(l.tx-1,l.ty)]==LOCATE_STATE_LOCATED)
				__find_adj(l.tx-1,l.ty);

			if(l.tx!=layout.frame_size_x&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(l.tx+1,l.ty)]==LOCATE_STATE_LOCATED)
				__find_adj(l.tx+1,l.ty);

			if(l.ty!=0&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(l.tx,l.ty-1)]==LOCATE_STATE_LOCATED)
				__find_adj(l.tx,l.ty-1);

			if(l.ty!=layout.frame_size_y&&demodulator_state.distributed_locator_state[GET_LOCATOR_ID(l.tx,l.ty+1)]==LOCATE_STATE_LOCATED)
				__find_adj(l.tx,l.ty+1);
		}
		else
			demodulator_state.distributed_locator_state[lid]=LOCATE_STATE_NOTLOCATED;
	}

	return true;
}
bool android_demodulator::__locate_center_block(){
	int xs[4],ys[4],bt[4];

	if(!b_locator.do_locate(demodulator_state.center_locator_x,demodulator_state.center_locator_y,
			demodulator_state.center_locator_bthres,layout.block_size_x,layout.block_size_y,
			xs,ys,bt)){
		return false;
	}
	//__parse_frame_header(xs,ys);
	int center_x=layout.center_block_id%layout.frame_size_x,center_y=layout.center_block_id/layout.frame_size_x;

	demodulator_state.distributed_locator_state[GET_LT_LOCATOR_ID(center_x,center_y)]=LOCATE_STATE_LOCATED;
	demodulator_state.distributed_locator_bthres[GET_LT_LOCATOR_ID(center_x,center_y)]=bt[0];
	demodulator_state.distributed_locator_x[GET_LT_LOCATOR_ID(center_x,center_y)]=xs[0];
	demodulator_state.distributed_locator_y[GET_LT_LOCATOR_ID(center_x,center_y)]=ys[0];

	demodulator_state.distributed_locator_state[GET_RT_LOCATOR_ID(center_x,center_y)]=LOCATE_STATE_LOCATED;
	demodulator_state.distributed_locator_bthres[GET_RT_LOCATOR_ID(center_x,center_y)]=bt[1];
	demodulator_state.distributed_locator_x[GET_RT_LOCATOR_ID(center_x,center_y)]=xs[1];
	demodulator_state.distributed_locator_y[GET_RT_LOCATOR_ID(center_x,center_y)]=ys[1];

	demodulator_state.distributed_locator_state[GET_RB_LOCATOR_ID(center_x,center_y)]=LOCATE_STATE_LOCATED;
	demodulator_state.distributed_locator_bthres[GET_RB_LOCATOR_ID(center_x,center_y)]=bt[2];
	demodulator_state.distributed_locator_x[GET_RB_LOCATOR_ID(center_x,center_y)]=xs[2];
	demodulator_state.distributed_locator_y[GET_RB_LOCATOR_ID(center_x,center_y)]=ys[2];

	demodulator_state.distributed_locator_state[GET_LB_LOCATOR_ID(center_x,center_y)]=LOCATE_STATE_LOCATED;
	demodulator_state.distributed_locator_bthres[GET_LB_LOCATOR_ID(center_x,center_y)]=bt[3];
	demodulator_state.distributed_locator_x[GET_LB_LOCATOR_ID(center_x,center_y)]=xs[3];
	demodulator_state.distributed_locator_y[GET_LB_LOCATOR_ID(center_x,center_y)]=ys[3];

	//logi("center_block_pos", "%d %d %d",center_x,center_y, GET_LT_LOCATOR_ID(center_x,center_y));

	//update shared sidelength

	demodulator_state.shared_sidelength=(xs[1]-xs[0])/layout.frame_size_x*2;

	return true;
}
void android_demodulator::__clear_locator_state(){
	for(int i=0;i<(layout.frame_size_x+1)*(layout.frame_size_y+1);i++)
		demodulator_state.distributed_locator_state[i]=LOCATE_STATE_NOTLOCATED;
}

bool android_demodulator::__locate_symbols(int* xs,int* ys,block_t dest,color_comparer* cmp){

	s_locator.register_four_corner(xs,ys);
	//first: locate and read color palette

	//TODO: need to modify when duplex
	int color_count=4;//(1<<(state->sender_current_symbol_capacity-1));
	int colors[4];
	for(int i=0;i<color_count;i++){
		int pos=3+i;
		int x,y;
		s_locator.do_locate(pos%layout.block_size_x,pos/layout.block_size_x, x,y);
		colors[i]=preader->getYUV(x,y);
	}
	cmp->set_palette(colors,color_count);
	//second: locate and read all symbols
	for(int i=3+color_count;i<layout.block_size_x*layout.block_size_y-1;i++){
		int x,y;
		s_locator.do_locate(i%layout.block_size_x,i/layout.block_size_x,x,y);
		dest.data[i]=cmp->find_nearest(preader->getYUV(x,y));
	}

	return true;
}
bool android_demodulator::push_raw_data(byte* data){

	if(demodulator_state.state==STATE_NOTSTARTED)return false;

	preader->load_data(data);
	current_pulled_bid=1e9;
	if(demodulator_state.state==STATE_CENTERING){
		logi("demodulator","centering");

		//first: locate/adjust center locator

		if(!__locate_center_locator())
			return false;


		logi("demodulator","locating half success");

		//second: locate center block
		if(__locate_center_block())
			demodulator_state.state=STATE_LOCATING;
		else
			return false;

		logi("demodulator", "centering success");
	}
	else if(demodulator_state.state==STATE_LOCATING){
		logi("demodulator","locating");

		//third: recursively locate all locators
		if(__maintain_locators()){
			demodulator_state.state=STATE_READY;
		}
		else{
			demodulator_state.center_locator_state=LOCATE_STATE_NOTLOCATED;
			demodulator_state.state=STATE_CENTERING;
			return false;
		}
		logi("demodulator","locating success");
	}
	else if(demodulator_state.state==STATE_READY){
		logi("demodulator","ready");

		if(!__maintain_locators()){
			demodulator_state.center_locator_state=LOCATE_STATE_NOTLOCATED;
			demodulator_state.state=STATE_CENTERING;
			return false;
		}
		current_pulled_bid=0;
		logi("demodulator","ready success");
	}


	frame_num++;
	return true;

}
void android_demodulator::start(){
	demodulator_state.state=STATE_CENTERING;
}


void android_demodulator::test_get_d_locations(int& count, int* xs,int* ys){
	count=0;
	for(int i=0;i<(layout.frame_size_x+1)*(layout.frame_size_y+1);i++){
		if(demodulator_state.distributed_locator_state[i]==LOCATE_STATE_LOCATED){
			xs[count]=demodulator_state.distributed_locator_x[i];
			ys[count]=demodulator_state.distributed_locator_y[i];
			count++;

		}
	}
	logi("get_locations","%d",count);
}
