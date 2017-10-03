
#ifndef STRUCTURES_H_
#define STRUCTURES_H_

#include <mutex>

#ifndef NULL
#define NULL   ((void *) 0)
#endif

#define DEL(x) if(x!=NULL)delete(x);

typedef unsigned char byte;

typedef byte symbol_t;


struct block_t{
	symbol_t* data;

	block_t(){
		data=NULL;
	}
	block_t(symbol_t* d):data(d){}
	~block_t(){
	}
};


struct layout_t{
	int block_size_x;
	int block_size_y;
	int frame_size_x;
	int frame_size_y;
	int center_block_id;

	layout_t():layout_t(0,0,0,0,0,0){}
	layout_t(int sx,int sy,int bx,int by,int fx,int fy):
		block_size_x(bx),block_size_y(by),
		frame_size_x(fx),frame_size_y(fy),
		center_block_id(frame_size_x*frame_size_y/2){}
};

struct palette_t{
	int data_color_number;
	int functional_color_number;
	uint16_t* data_colors;
	uint16_t* functional_colors;

	palette_t(int data_color_num,int functional_color_num,
			uint16_t* data_colors,uint16_t* functional_colors)
	{
		this->data_color_number=data_color_num;
		this->functional_color_number=functional_color_num;

		this->data_colors=new uint16_t[data_color_num];
		this->functional_colors=new uint16_t[functional_color_num];
		
		memcpy(this->data_colors,data_colors,sizeof(uint16_t)*data_color_num);
		memcpy(this->functional_colors,functional_colors,sizeof(uint16_t)*functional_color_num);
	}
	palette_t(){
		this->data_colors=this->functional_colors=NULL;
		this->data_color_number=this->functional_color_number=0;
	}

	void dispose(){
	//~palette_t(){
		DEL(data_colors);
		DEL(functional_colors);
	}
};



#endif /* STRUCTURES_H_ */
