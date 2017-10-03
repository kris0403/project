
#include "decoder.h"
#include "locator.h"
#include "pixel_reader.h"
#include "color_comparer.h"
#include "log.h"

bool parse(block_t block, block_code<byte>* coder, int block_size, byte* res){
	int spb=4,sl=2;
	byte b=0;
	int tmp_block_i=0;
	for(int i=7;i<block_size*block_size-1;i++){
		b=((b<<sl)|block.data[i]);
		if((i-7)%spb==spb-1){
			res[tmp_block_i++]=b;
			b=0;
		}
	}

	if(coder->decode(res)){
		return true;
	}
	else return false;
}

void parse_color(android_symbol_locator* s_locator, android_pixel_reader* preader, int sidelength,block_t dest){
	int color_count=4;
	int colors[4];
	for(int i=0;i<color_count;i++){
		int pos=3+i;
		int x,y;
		s_locator->do_locate(pos%sidelength,pos/sidelength, x,y);
		colors[i]=preader->getYUV(x,y);

	}
	logi("color palette","%X %X %X %X",colors[0],colors[1],colors[2],colors[3]);

	color_comparer cmp;
	cmp.set_palette(colors,color_count);

	//second: locate and read all symbols
	for(int i=3+color_count;i<sidelength*sidelength-1;i++){
		int x,y;
		s_locator->do_locate(i%sidelength,i/sidelength,x,y);
		dest.data[i]=cmp.find_nearest(preader->getYUV(x,y));
	}
	logi("jni before testdata","%d %d %d %d %d",sidelength,dest.data[7],dest.data[8],dest.data[9],dest.data[10]);
}
