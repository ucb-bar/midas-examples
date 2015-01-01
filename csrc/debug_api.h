#ifndef __DEBUG_API_H
#define __DEBUG_API_H

#include <stdint.h>
#include <string>
#include <vector>
#include <map>

typedef std::map< uint32_t, uint32_t > map_t;
typedef std::map< std::string, std::vector<size_t> > iomap_t;

// Constants
enum DEBUG_CMD {
  STEP, POKE, PEEK, POKED, PEEKD, TRACE, MEM,
};

enum SNAP_CMD {
  SNAP_FIN, SNAP_STEP, SNAP_POKE, SNAP_EXPECT, SNAP_WRITE, SNAP_READ,
};

enum STEP_RESP {
  RESP_FIN, RESP_TRACE, RESP_PEEKD,
};

const size_t hostlen = 32;
const size_t addrlen = 32;
const size_t taglen = 5;
const size_t memlen = 32;
const size_t cmdlen = 6;


class debug_api_t
{
  public:
    // debug_api_t(std::string design): debug_api_t(design, true) {}
    debug_api_t(std::string design, bool _trace = true, bool _check_out = true);
    ~debug_api_t();
    virtual void run() = 0;

  private:
    void read_io_map_file(std::string filename);
    void read_chain_map_file(std::string filename);

    void poke(uint64_t value);
    bool peek_ready();
    uint64_t peek();
    void poke_steps(size_t n, bool record = true);
    void poke_all();
    void peek_all();
    void peek_trace();
    void trace_mem();
    void read_snap(char* snap);
    void record_io();
    void record_snap(char *snap);
    void record_mem();

    std::string design;

    std::vector<std::string> signals;
    std::vector<size_t> widths;

    map_t poke_map;
    map_t peek_map;

    map_t mem_writes;
    map_t mem_reads;

    iomap_t din_map;
    iomap_t dout_map;
    iomap_t win_map;
    iomap_t wout_map;

    size_t din_num;
    size_t dout_num;
    size_t win_num;
    size_t wout_num;
    size_t snaplen;

    FILE *snaps;

    bool check_out;
    bool trace; 
    bool pass;
    int64_t fail_t;
    
    volatile uintptr_t* dev_vaddr;
    const static uintptr_t dev_paddr = 0x43C00000;

  protected:
    void open_snap(std::string filename);
    void step(size_t n);
    void poke(std::string path, uint64_t value);
    uint64_t peek(std::string path);
    bool expect(std::string path, uint64_t expected);
    bool expect(bool ok, std::string s);
    void load_mem(std::string filename);
    void write_mem(uint64_t addr, uint64_t data);
    uint64_t read_mem(uint64_t addr);
    uint64_t rand_next(size_t limit); 
    uint64_t t;
};

#endif // __DEBUG_API_H
