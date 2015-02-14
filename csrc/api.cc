#include "api.h"

size_t API_t::hostlen() const { return HOST_LEN; }
size_t API_t::addrlen() const { return MIF_ADDR_BITS; }
size_t API_t::memlen() const { return MIF_DATA_BITS; }
size_t API_t::taglen() const { return MIF_TAG_BITS; }
size_t API_t::cmdlen() const { return CMD_LEN; }
size_t API_t::tracelen() const { return TRACE_LEN; }
