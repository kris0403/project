/*
 * decoder.h
 *
 *  Created on: 2014年8月20日
 *      Author: 安然
 */

#ifndef DECODER_H_
#define DECODER_H_

#include <functional>
#include "structures.h"
#include "reed_solomon_code.h"
#include "locator.h"
#include "pixel_reader.h"

bool parse(block_t block, block_code<byte>* coder, int block_size, byte* res);
void parse_color(android_symbol_locator* s_locator, android_pixel_reader* preader, int sidelength,block_t dest);


#endif /* DECODER_H_ */
