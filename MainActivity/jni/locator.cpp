#include <math.h>

#include "locator.h"
#include "pixel_reader.h"
#include "log.h"

const int BLACK_MIN_NUM=100;
const double BLACK_MAX_NUM_RATIO=0.5;
const int MAX_ITERATION=5;
const int BTHRES_FLOAT=10;
const int MIN_DIST=5;

int android_center_locator::_sample_random(int xf, int xt,int yf,int yt, int bthres, int& bnum,int& x,int& y){
	bnum=x=y=0;
	for(int i=xf;i<xt;i++)
		for(int j=yf;j<yt;j++){
			int Y=preader->getY(i,j);
			if(Y<bthres){
				bnum++;
				x+=i;
				y+=j;
			}
		}
	if(bnum!=0){
		x/=bnum;
		y/=bnum;
	}
	return (xt-xf)*(yt-yf);
}

#define abs(x) ((x)<0?-(x):(x))
#define dist(x,y) (abs(x)+abs(y))

bool __mean_shift(int& x,int& y,int rad, android_pixel_reader* reader,int& bthres, int iteration=0){
	int W=reader->w;
	int H=reader->h;

	int l=x-rad;
	if(l<0)l=0;
	int r=x+rad;
	if(r>W)r=W;

	int t=y-rad;
	if(t<0)t=0;
	int b=y+rad;
	if(b>H)b=H;

	int xtot=0;
	int ytot=0;
	int tot=0;

	int bcritical=0;
	int wcritical=0;
	int wtot=0;

	int thresh=bthres+BTHRES_FLOAT;
	int thresl=bthres-BTHRES_FLOAT;

	for(int i=l;i<r;i++)
		for(int j=t;j<b;j++){
			int Y=reader->getY(i,j);
			if(Y<bthres){
				tot++;
				xtot+=i;
				ytot+=j;
			}
			else
				wtot++;

			if(Y<bthres&&Y>=thresl)
				bcritical++;
			if(Y>=bthres&&Y<thresh)
				wcritical++;
		}

	if(tot==0)return false;

	xtot/=tot;
	ytot/=tot;

	double adj=(double)bcritical/tot-(double)wcritical/wtot;
	/////////////////////////////////////////////////////////////////
	//logi("mean shift adjust", "%d %d %d %d %d %d",bcritical,wcritical,tot,wtot,bthres,rad);
	/////////////////////////////////////////////////////////////////
	bthres=bthres+(int)(BTHRES_FLOAT*adj);

	x=xtot;
	y=ytot;

	if(dist(x-xtot,y-ytot)<MIN_DIST)
		return true;
	if(iteration>MAX_ITERATION)
		return false;

	return __mean_shift(x,y,rad,reader,bthres,iteration+1);

}
bool __fast_adjust(int &x, int &y, int rad, android_pixel_reader* reader,int bthres){
	if(reader->getY(x,y)>=bthres)return false;

	int l=x;
	int r=x;
	int t=y;
	int b=y;

	while(l>=0&&x-l<rad&&reader->getY(l,y)<bthres)l--;
	while(r<reader->w&&r-x<rad&&reader->getY(r,y)<bthres)r++;
	while(t>=0&&y-t<rad&&reader->getY(x,t)<bthres)t--;
	while(b<reader->h&&b-y<rad&&reader->getY(x,b)<bthres)b++;

	if(x-l>=rad||r-x>=rad||y-t>=rad||b-y>=rad)return false;
	x=(l+r)>>1;
	y=(t+b)>>1;
	return true;
}
bool android_center_locator::_mean_shift(int& x,int& y,int rad,int& bthres,int iteration){
	return __mean_shift(x,y,rad,preader,bthres,iteration);
}

bool android_center_locator::_fast_adjust(int& x,int& y, int rad,int bthres){
	return __fast_adjust(x,y,rad,preader,bthres);
}
#define min(a,b) ((a)<(b)?(a):(b))
android_center_locator::android_center_locator(android_pixel_reader* reader):preader(reader){}
bool android_center_locator::do_full_locate(int& cx, int& cy, int& bthres){
	int W=preader->w;
	int H=preader->h;
	
	int Wpadding=(int)(0.3*W);
	int Hpadding=(int)(0.3*H);

	int MSradius=min(W,H)*0.1;

	int bthreso=bthres;
	int bthresh=(int)(bthres*1.3);
	int bthresl=(int)(bthres*0.8);

	int bnum,baverx,bavery;

	_sample_random(Wpadding,W-Wpadding,Hpadding,H-Hpadding,bthres,bnum,baverx,bavery);

	if(bnum<BLACK_MIN_NUM){
		int oldbnum=bnum;
		_sample_random(Wpadding,W-Wpadding,Hpadding,H-Hpadding,bthresh,bnum,baverx,bavery);

		//logi("bnum","%d %d",oldbnum,bnum);
		if(bnum<BLACK_MIN_NUM)
			return false;
		bthreso=bthresh;
	}
	else if(bnum>BLACK_MAX_NUM_RATIO){
		int t=_sample_random(Wpadding,W-Wpadding,Hpadding,H-Hpadding,bthresl,bnum,baverx,bavery);
		if(bnum>BLACK_MAX_NUM_RATIO*t)
			return false;
		bthreso=bthresl;
	}

	//start mean shift
	

	if(!_mean_shift(baverx,bavery,MSradius,bthreso))
		return false;

	if(!_fast_adjust(baverx,bavery,MSradius,bthreso))
		return false;

	bthres=bthreso;

	cx=baverx;
	cy=bavery;
	return true;
}

bool android_center_locator::do_adjust_locate(int& cx,int& cy,int& bthres){
	int W=preader->w;
	int H=preader->h;
	
	int MSradius=min(W,H)*0.1;

	int bthres0=bthres;
	int cx0=cx;
	int cy0=cy;

	if(preader->getY(cx,cy)<bthres)
		_mean_shift(cx0,cy0,MSradius,bthres0,MAX_ITERATION);
	else
		if(!_mean_shift(cx0,cy0,MSradius,bthres0))
			return false;

	if(!_fast_adjust(cx0,cy0,MSradius,bthres0))
		return false;
	
	bthres=bthres0;
	cx=cx0;
	cy=cy0;
	return true;
}


android_distributed_locator::android_distributed_locator(android_pixel_reader* reader):preader(reader){}
bool android_distributed_locator::do_locate(int& cx,int& cy,int& bthres, int estimated_sidelength,bool force_mean_shift){
	int W=preader->w;
	int H=preader->h;
	if(cx>=W||cy>=H||cx<0||cy<0)return false;
	
	int MSradius=estimated_sidelength;
	
	int bthres0=bthres;
	int cx0=cx;
	int cy0=cy;

	if(force_mean_shift||preader->getY(cx,cy)>=bthres)
		if(!__mean_shift(cx0,cy0,MSradius,preader,bthres0))
			return false;
	if(!__fast_adjust(cx0,cy0,MSradius,preader,bthres0))
		return false;
	
	cx=cx0;
	cy=cy0;
	bthres=bthres0;
	return true;
}

android_center_block_locator::android_center_block_locator(android_pixel_reader* reader, android_distributed_locator* _d_locator):preader(reader),d_locator(_d_locator){}



bool android_center_block_locator::do_locate(int cx,int cy,int c_bthres, int block_size_x,int block_size_y, int* xs,int* ys,int* bthres){
	//currently assume the four direction
	

	int l=cx,r=cx;
	int t=cy,b=cy;

	while(l>=0&&preader->getY(l,cy)<c_bthres)l--;
	while(r<preader->w&&preader->getY(r,cy)<c_bthres)r++;
	while(t>=0&&preader->getY(cx,t)<c_bthres)t--;
	while(b<preader->h&&preader->getY(cx,b)<c_bthres)b++;

	int sidelength_x=(r-l-1)*block_size_x/3;
	int sidelength_y=(b-t-1)*block_size_y/3;
	int s_s_x=sidelength_x/block_size_x;
	int s_s_y=sidelength_y/block_size_y;
	int max_sidelength=(s_s_x>s_s_y?s_s_x:s_s_y);


	//logi("center_block_locator", "%d %d %d %d %d %d %d",cx,cy,c_bthres,block_size_x,block_size_y,sidelength_x,sidelength_y);

	//left-top
	int tx=cx-sidelength_x/2;
	int ty=cy-sidelength_y/2;
	
	int bthres0=c_bthres;

	bool res=d_locator->do_locate(tx,ty,bthres0,max_sidelength,true);

	if(!res)return false;
	///////////////////////////////////////////
	//logi("center_block_locator", "LT success");
    ///////////////////////////////////////////
	xs[0]=tx;
	ys[0]=ty;
	bthres[0]=bthres0;
	
	//right-top
	tx=cx+sidelength_x/2;
	
	bthres0=c_bthres;
	res=d_locator->do_locate(tx,ty,bthres0,max_sidelength,true);
	if(!res)return false;
	////////////////////////////////////////////
	//logi("center_block_locator", "RT success");
    ////////////////////////////////////////////
	xs[1]=tx;
	ys[1]=ty;
	bthres[1]=bthres0;

	//right-bottom
	ty=cy+sidelength_y/2;
	bthres0=c_bthres;
	res=d_locator->do_locate(tx,ty,bthres0,max_sidelength,true);
	if(!res)return false;
	/////////////////////////////////////////////
	//logi("center_block_locator", "RB success");
	/////////////////////////////////////////////
	xs[2]=tx;
	ys[2]=ty;
	bthres[2]=bthres0;

	//left-bottom
	tx=cx-sidelength_x/2;
	bthres0=c_bthres;
	res=d_locator->do_locate(tx,ty,bthres0,max_sidelength,true);
	if(!res)return false;
	/////////////////////////////////////////////
	//logi("center_block_locator", "LB success");
    ////////////////////////////////////////////
	xs[3]=tx;
	ys[3]=ty;
	bthres[3]=bthres0;


	return true;
}

android_symbol_locator::android_symbol_locator(int _bw,int _bh):bw(_bw),bh(_bh),xs(NULL),ys(NULL){}

void android_symbol_locator::register_four_corner(int* xs,int* ys){
	this->xs=xs;
	this->ys=ys;
}
void android_symbol_locator::do_locate(int sx,int sy,int& px,int& py){
	int fmx=bw;
	int fmy=bh;
	int fzx1=sx;
	int fzy1=sy;
	int fzx=fmx-sx;
	int fzy=fmy-sy;


	px=(fzx*fzy*xs[0]+
		fzx1*fzy*xs[1]+
		fzx*fzy1*xs[3]+
		fzx1*fzy1*xs[2])/(fmx*fmy);
	py=(fzx*fzy*ys[0]+
		fzx1*fzy*ys[1]+
		fzx*fzy1*ys[3]+
		fzx1*fzy1*ys[2])/(fmx*fmy);
}
