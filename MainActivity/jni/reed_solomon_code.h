/*
 * reed_solomon.h
 *
 *  Created on: 2014��7��30��
 *      Author: ��Ȼ
 */

#ifndef REED_SOLOMON_H_
#define REED_SOLOMON_H_

#include <assert.h>
#include "FEC.h"
#include "structures.h"

class reed_solomon_code:public block_code<byte>{
private:
	int n,k;
	int npar;
	byte encode_gx[255];
	byte* calc_sigma_mbm(byte* syn,byte* dest, int& res_len);
	bool chien_search(int length,int start,byte wa,byte seki, byte& res1,byte& res2);
	bool chien_search(int length,byte* sigma,int sigma_len,byte* res,int& res_len);
	void do_forney(byte* data,int len,byte* pos, int pos_len, byte* sigma, int sigma_len, byte* omega, int omega_len);
public:
	reed_solomon_code(int _n,int _k);
	byte* encode(byte* input,byte* parity);
	byte* encode(byte* input);
	byte* decode(byte* input);
	~reed_solomon_code();
};

#ifndef NULL
#define NULL   ((void *) 0)
#endif


#endif /* REED_SOLOMON_H_ */
