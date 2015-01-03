#include "debug_api.h"
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <assert.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <stdlib.h>
#include <string.h>

#define read_reg(r) (dev_vaddr[r])
#define write_reg(r, v) (dev_vaddr[r] = v)

debug_api_t::debug_api_t(std::string _design, bool _trace, bool _check_out)
  : design(_design), trace(_trace), check_out(_check_out), t(0), pass(true), fail_t(-1), 
    qin_num(0), qout_num(0), win_num(0), wout_num(0), snaplen(0), snaps(NULL),
    hostlen(0), addrlen(0), memlen(0), taglen(0), tracelen(0)
{
  int fd = open("/dev/mem", O_RDWR|O_SYNC);
  assert(fd != -1);

  int host_prot = PROT_READ | PROT_WRITE;
  int flags = MAP_SHARED;
  uintptr_t pgsize = sysconf(_SC_PAGESIZE);
  assert(dev_paddr % pgsize == 0);

  dev_vaddr = (uintptr_t*)mmap(0, pgsize, host_prot, flags, fd, dev_paddr);
  assert(dev_vaddr != MAP_FAILED);

  // Reset
  write_reg(31, 0); 
  __sync_synchronize();
  // Empty output queues before starting!
  while ((uint32_t) read_reg(0) > 0) {
    uint32_t temp = (uint32_t) read_reg(1);
  }

  // Read mapping files
  read_params(design + "Shim.prm");
  read_io_map(design + ".io.map");
  read_chain_map(design + ".chain.map");

  // Remove snapshot files before getting started
  open_snap(design + ".snap");

  srand(time(NULL));
}

debug_api_t::~debug_api_t() {
  fclose(snaps);

  std::cout << t << " Cycles";
  if (pass) 
    std::cout << " Passed" << std::endl;
  else 
    std::cout << " Failed, first at cycle " << fail_t << std::endl;
}

void debug_api_t::open_snap(std::string filename) {
  if (snaps != NULL) fclose(snaps);
  snaps = fopen(filename.c_str(), "w");
}

void debug_api_t::read_params(std::string filename) {
  std::ifstream file(filename.c_str());
  std::string line;
  if (file) {
    while(getline(file, line)) {
      std::string pair = line.substr(1, line.length()-1);
      size_t colon_idx = pair.find(",");
      if (colon_idx < pair.length()) {
        std::string param = pair.substr(0, colon_idx);
        size_t value = std::stoi(pair.substr(colon_idx+1));
        if (param == "HTIF_WIDTH") hostlen = value;
        else if (param == "MIF_ADDR_BITS") addrlen = value;
        else if (param == "MIF_DATA_BITS") memlen = value;
        else if (param == "MIF_TAG_BITS") taglen = value;
        else if (param == "CMD_BITS") cmdlen = value;
        else if (param == "TRACE_LEN") tracelen = value;
      }
    }
  } else {
    std::cerr << "Cannot open " << filename << std::endl;
    exit(0);
  }
  file.close();
}

void debug_api_t::read_io_map(std::string filename) {
  enum IOType { QIN, QOUT, WIN, WOUT };
  IOType iotype = QIN;
  std::ifstream file(filename.c_str());
  std::string line;
  if (file) {
    while (getline(file, line)) {
      std::istringstream iss(line);
      std::string head;
      iss >> head;
      if (head == "QIN:") iotype = QIN;
      else if (head == "QOUT:") iotype = QOUT;
      else if (head == "WIN:") iotype = WIN;
      else if (head == "WOUT:") iotype = WOUT;
      else {
        size_t width;
        iss >> width;
        size_t n = (width-1)/hostlen + 1;
        switch (iotype) {
          case QIN:
            qin_map[head] = std::vector<size_t>();
            for (int i = 0 ; i < n ; i++) {
              qin_map[head].push_back(qin_num);
              qin_num++;
            }
            break;
          case QOUT:
            qout_map[head] = std::vector<size_t>();
            for (int i = 0 ; i < n ; i++) {
              qout_map[head].push_back(qout_num);
              qout_num++;
            }
            break;
          case WIN:
            win_map[head] = std::vector<size_t>();
            for (int i = 0 ; i < n ; i++) {
              win_map[head].push_back(win_num);
              win_num++;
            }
            break;
          case WOUT:
            wout_map[head] = std::vector<size_t>();
            for (int i = 0 ; i < n ; i++) {
              wout_map[head].push_back(wout_num);
              wout_num++;
            }
            break;
          default:
            break;
        }
      }
    }
    for (int i = 0 ; i < win_num ; i++) {
      poke_map[i] = 0;
    }
    for (int i = 0 ; i < qin_num ; i++) {
      pokeq_map[i] = std::queue<uint32_t>();
    }
    for (int i = 0 ; i < qout_num ; i++) {
      peekq_map[i] = std::queue<uint32_t>();
    }
  } else {
    std::cerr << "Cannot open " << filename << std::endl;
    exit(0);
  }
  file.close();
}

void debug_api_t::read_chain_map(std::string filename) {
  std::ifstream file(filename.c_str());
  std::string line;
  if (file) {
    while(getline(file, line)) {
      std::istringstream iss(line);
      std::string path;
      size_t width;
      iss >> path >> width;
      signals.push_back(path);
      widths.push_back(width);
      snaplen += width;
    }
  } else {
    std::cerr << "Cannot open " << filename << std::endl;
    exit(0);
  }
  file.close();
}

void debug_api_t::poke(uint64_t value) {
  write_reg(0, value);
  __sync_synchronize();
}

bool debug_api_t::peek_ready() {
  return (uint32_t) read_reg(0) != 0;
}

uint64_t debug_api_t::peek() {
  __sync_synchronize();
  while (!peek_ready()) ;
  return (uint32_t) read_reg(1);
}

void debug_api_t::poke_steps(size_t n, bool record) {
  poke(n << (cmdlen+1) | record << cmdlen | STEP);
}

void debug_api_t::poke_all() {
  poke(POKE);
  for (int i = 0 ; i < win_num ; i++) {
    poke(poke_map[i]);
  }
}

void debug_api_t::peek_all() {
  peek_map.clear();
  poke(PEEK);
  for (int i = 0 ; i < wout_num ; i++) {
    peek_map[i] = peek();
  }
}

void debug_api_t::pokeq_all() {
  if (qin_num > 0) poke(POKEQ);
  for (int i = 0 ; i < qin_num ; i++) {
    uint32_t count = (pokeq_map[i].size() < tracelen) ? pokeq_map[i].size() : tracelen;
    poke(count);
    for (int k = 0 ; k < count ; k++) {
      poke(pokeq_map[i].front());
      pokeq_map[i].pop();
    }
  }
}

void debug_api_t::peekq_all() {
  if (qout_num > 0) poke(PEEKQ);
  trace_qout();
}

void debug_api_t::trace_qout() {
  for (int i = 0 ; i < qout_num ; i++) {
    uint32_t count = peek();
    for (int k = 0 ; k < count ; k++) {
      peekq_map[i].push(peek());
    }
  }
}

void debug_api_t::peek_trace() {
  poke(TRACE);
  trace_mem();
}

void debug_api_t::trace_mem() {
  std::vector<uint64_t> waddr;
  std::vector<uint64_t> wdata;
  uint32_t wcount = peek();
  for (int i = 0 ; i < wcount ; i++) {
    uint64_t addr = 0;
    for (int k = 0 ; k < addrlen ; k += hostlen) {
      addr |= peek() << k;
    }
    waddr.push_back(addr);
  }
  for (int i = 0 ; i < wcount ; i++) {
    uint64_t data = 0;
    for (int k = 0 ; k < memlen ; k += hostlen) {
      data |= peek() << k;
    }
    wdata.push_back(data);
  }
  for (int i = 0 ; i < wcount ; i++) {
    uint64_t addr = waddr[i];
    uint64_t data = wdata[i];
    mem_writes[addr] = data;
  }
  waddr.clear();
  wdata.clear();

  uint32_t rcount = peek();
  for (int i = 0 ; i < rcount ; i++) {
    uint64_t addr = 0;
    for (int k = 0 ; k < addrlen ; k += hostlen) {
      addr = (addr << hostlen) | peek();
    }
    uint64_t tag = 0;
    for (int k = 0 ; k < taglen ; k += hostlen) {
      tag = (tag << hostlen) | peek();
    }
    mem_reads[tag] = addr;
  }
}

static inline char* int_to_bin(uint32_t value, size_t size) {
  char* bin = new char[size];
  for(int i = 0 ; i < size ; i++) {
    bin[i] = ((value >> (size-1-i)) & 0x1) + '0';
  }
  return bin;
}

void debug_api_t::read_snap(char *snap) {
  for (size_t offset = 0 ; offset < snaplen ; offset += hostlen) {
    char* value = int_to_bin(peek(), hostlen);
    memcpy(snap+offset, value, (offset+hostlen < snaplen) ? hostlen : snaplen-offset);
    delete[] value; 
  }
}

void debug_api_t::record_io() {
  for (iomap_t::iterator it = win_map.begin() ; it != win_map.end() ; it++) {
    std::string signal = it->first;
    std::vector<size_t> ids = it->second;
    uint32_t data = 0;
    for (int i = 0 ; i < ids.size() ; i++) {
      size_t id = ids[i];
      data = (data << hostlen) | ((poke_map.find(id) != poke_map.end()) ? poke_map[id] : 0);
    } 
    fprintf(snaps, "%d %s %x\n", SNAP_POKE, signal.c_str(), data);
  }
  if (check_out) {
    for (iomap_t::iterator it = wout_map.begin() ; it != wout_map.end() ; it++) {
      std::string signal = it->first;
      std::vector<size_t> ids = it->second;
      uint32_t data = 0;
      for (int i = 0 ; i < ids.size() ; i++) {
        size_t id = ids[i];
        assert(peek_map.find(id) != peek_map.end());
        data = (data << hostlen) | peek_map[id];
      } 
      fprintf(snaps, "%d %s %x\n", SNAP_EXPECT, signal.c_str(), data);
    }
  }
  fprintf(snaps, "%d\n", SNAP_FIN);
}

void debug_api_t::record_snap(char *snap) {
  size_t offset = 0;
  for (int i = 0 ; i < signals.size() ; i++) {
    std::string signal = signals[i];
    size_t width = widths[i];
    if (signal != "null") {
      char *bin = new char[width];
      uint32_t value = 0; // TODO: more than 32 bits?
      memcpy(bin, snap+offset, width);
      for (int i = 0 ; i < width ; i++) {
        value = (value << 1) | (bin[i] - '0'); // index?
      }
      fprintf(snaps, "%d %s %x\n", SNAP_POKE, signal.c_str(), value);
      delete[] bin;
    }
    offset += width;
  }
}

void debug_api_t::record_mem() {
  for (map_t::iterator it = mem_writes.begin() ; it != mem_writes.end() ; it++) {
    uint32_t addr = it->first;
    uint32_t data = it->second;
    fprintf(snaps, "%d %x %08x\n", SNAP_WRITE, addr, data);
  }

  for (map_t::iterator it = mem_reads.begin() ; it != mem_reads.end(); it++) {
    uint32_t tag = it->first;
    uint32_t addr = it->second;
    fprintf(snaps, "%d %x %08x\n", SNAP_READ, addr, tag);
  }

  mem_writes.clear();
  mem_reads.clear();
}

void debug_api_t::step(size_t n, bool record) {
  uint64_t target = t + n;
  if (record && t > 0) record_io();
  if (trace) std::cout << "* STEP " << n << " -> " << target << " *" << std::endl;
  if (check_out) fprintf(snaps, "%d %d\n", SNAP_STEP, n);
  poke_all();
  pokeq_all();
  poke_steps(n, record);
  bool fin = false;
  while (!fin) {
    if (peek_ready()) {
      uint32_t resp = peek();
      if (resp == RESP_FIN) fin = true;
      else if (resp == RESP_TRACE) trace_mem();
      else if (resp == RESP_PEEKQ) trace_qout();
    }
  }
  char *snap = new char[snaplen];
  if (record) read_snap(snap);
  peek_all();
  peekq_all();
  if (record) {
    peek_trace();
    record_snap(snap);
    record_mem();
  }
  delete[] snap;
  t += n;
}

void debug_api_t::poke(std::string path, uint64_t value) {
  assert(win_map.find(path) != win_map.end());
  if (trace) std::cout << "* POKE " << path << " <- " << value << " *" << std::endl;
  std::vector<size_t> ids = win_map[path];
  uint64_t mask = (1 << hostlen) - 1;
  for (int i = 0 ; i < ids.size() ; i++) {
    size_t id = ids[ids.size()-1-i];
    size_t shift = hostlen * i;
    uint32_t data = (value >> shift) & mask;
    poke_map[id] = data;
  }
}

uint64_t debug_api_t::peek(std::string path) {
  assert(wout_map.find(path) != wout_map.end());
  uint64_t value = 0;
  std::vector<size_t> ids = wout_map[path];
  for (int i = 0 ; i < ids.size() ; i++) {
    size_t id = ids[ids.size()-1-i];
    assert(peek_map.find(id) != peek_map.end());
    value = value << hostlen | peek_map[id];
  }
  if (trace) std::cout << "* PEEK " << path << " -> " << value << " *" << std::endl;
  return value;
}

void debug_api_t::pokeq(std::string path, uint64_t value) {
  if (trace) std::cout << "* POKEQ " << path << " <- " << value << " *" << std::endl;
  assert(qin_map.find(path) != qin_map.end());
  std::vector<size_t> ids = qin_map[path];
  uint64_t mask = (1 << hostlen) - 1;
  for (int i = 0 ; i < ids.size() ; i++) {
    size_t id = ids[ids.size()-1-i];
    size_t shift = hostlen * i;
    uint32_t data = (value >> shift) & mask;
    assert(pokeq_map.find(id) != pokeq_map.end());
    pokeq_map[id].push(data);
  }
}

uint64_t debug_api_t::peekq(std::string path) {
  assert(qout_map.find(path) != qout_map.end());
  std::vector<size_t> ids = qout_map[path];
  uint64_t value = 0;
  for (int i = 0 ; i < ids.size() ; i++) {
    size_t id = ids[ids.size()-1-i];
    assert(peekq_map.find(id) != peekq_map.end());
    value = value << hostlen | peekq_map[id].front();
    peekq_map[id].pop();
  }
  if (trace) std::cout << "* PEEKQ " << path << " -> " << value << " *" << std::endl;
  return value;
}

bool debug_api_t::peekq_valid(std::string path) {
  assert(qout_map.find(path) != qout_map.end());
  std::vector<size_t> ids = qout_map[path];
  bool valid = true;
  for (int i = 0 ; i < ids.size() ; i++) {
    size_t id = ids[ids.size()-1-i];
    assert(peekq_map.find(id) != peekq_map.end());
    valid &= !peekq_map[id].empty();
  }
  return valid;
}

bool debug_api_t::expect(std::string path, uint64_t expected) {
  uint64_t value = peek(path);
  bool ok = value == expected;
  pass &= ok;
  if (!ok && fail_t < 0) fail_t = t;
  if (trace) std::cout << "* EXPECT " << path << " -> " << value << " == " << expected 
                       << (ok ? " PASS" : " FAIL") << " *" << std::endl;
  return ok;
}

bool debug_api_t::expect(bool ok, std::string s) {
  pass &= ok;
  if (!ok && fail_t < 0) fail_t = t;
  if (trace) std::cout << "* " << s << " " << (ok ? "PASS" : "FAIL") << " *" << std::endl;
  return ok;
}

void debug_api_t::load_mem(std::string filename) {
  std::ifstream file(filename.c_str());
  if (file) { 
    std::string line;
    int i = 0;
    while (std::getline(file, line)) {
      #define parse_nibble(c) ((c) >= 'a' ? (c)-'a'+10 : (c)-'0')
      uint64_t base = (i * line.length()) / 2;
      uint64_t offset = 0;
      for (int k = line.length() - 8 ; k >= 0 ; k -= 8) {
        uint64_t addr = base + offset;
        uint64_t data = 
          (parse_nibble(line[k]) << 28) | (parse_nibble(line[k+1]) << 24) |
          (parse_nibble(line[k+2]) << 20) | (parse_nibble(line[k+3]) << 16) |
          (parse_nibble(line[k+4]) << 12) | (parse_nibble(line[k+5]) << 8) |
          (parse_nibble(line[k+6]) << 4) | parse_nibble(line[k+7]);
        write_mem(base + offset, data);
        offset += 4;
      }
      i += 1;
    }
  } else {
    std::cerr << "Cannot open " << filename << std::endl;
    exit(1);
  }
  file.close();
}

void debug_api_t::write_mem(uint64_t addr, uint64_t data) {
  poke((1 << cmdlen) | MEM);
  uint64_t mask = (1<<hostlen)-1;
  for (int i = (addrlen-1)/hostlen+1 ; i > 0 ; i--) {
    poke((addr >> (hostlen * (i-1))) & mask);
  }
  for (int i = (memlen-1)/hostlen+1 ; i > 0 ; i--) {
    poke((data >> (hostlen * (i-1))) & mask);
  }

  mem_writes[addr] = data;
}

uint64_t debug_api_t::read_mem(uint64_t addr) {
  poke((0 << cmdlen) | MEM);
  uint64_t mask = (1<<hostlen)-1;
  for (int i = (addrlen-1)/hostlen+1 ; i > 0 ; i--) {
    poke((addr >> (hostlen * (i-1))) & mask);
  }
  uint64_t data = 0;
  for (int i = 0 ; i < (memlen-1)/hostlen+1 ; i ++) {
    data |= peek() << (hostlen * i);
  }
  return data;
}

uint64_t debug_api_t::rand_next(size_t limit) {
  return rand() % limit;
}
