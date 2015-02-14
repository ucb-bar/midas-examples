#ifndef __API_H
#define __API_H

#include "simif_zedboard.h"

class API_t: public simif_zedboard_t 
{
public:
  API_t(std::vector<std::string> args, std::string prefix, bool log = true, bool sample_check = true):
    simif_zedboard_t(args, prefix, log, sample_check, false) 
  {
    step_size = 1;
  }
private:
  virtual size_t hostlen() const; 
  virtual size_t addrlen() const; 
  virtual size_t memlen() const; 
  virtual size_t taglen() const; 
  virtual size_t cmdlen() const; 
  virtual size_t tracelen() const; 
};

#endif
