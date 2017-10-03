/*
 * FEC.h
 *
 *  Created on: 2014年7月30日
 *      Author: 安然
 */

#ifndef FEC_H_
#define FEC_H_

template<typename T>
class block_code
{
public:
	virtual T* encode(T* input)=0;
	virtual T* decode(T* input)=0;
	virtual ~block_code(){}
};

template<typename T>
class rateless_code_encoder
{
	virtual T* input(T* src,int length)=0;
	virtual T* output(T* dest)=0;
	virtual ~rateless_code_encoder(){}
};

template<typename T>
class rateless_code_decoder
{
	virtual T* input(T* in)=0;
	virtual T* output(T* dest)=0;
	virtual ~rateless_code_decoder(){}
};


#endif /* FEC_H_ */
