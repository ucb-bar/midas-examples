//See LICENSE for license details.

#include "simif.h"

#define INSTRUMENTATIONWIDGET_primary INSTRUMENTATIONWIDGET_1
#define INSTRUMENTATIONWIDGET_other INSTRUMENTATIONWIDGET_0

class InstrumentationWidgetDemo_t: virtual simif_t
{
  // Number of iterations.
  static constexpr unsigned int num_iters = 128;

  // We need this cache for asynchronous testing, since in2 values are generated
  // randomly.
  uint32_t in2_cache[256];

  void print_instrumentation_data() {
    // Macros: https://stackoverflow.com/a/240365
#define _STR1(x)  #x
#define _STR(x)  _STR1(x)
#define _print_queue(queue) printf("(" _STR(queue) ")\t Valid: %d; \t bits: %d.\n", read(INSTRUMENTATIONWIDGET_primary(queue ## _valid)), read(INSTRUMENTATIONWIDGET_primary(queue ## _data)))
    _print_queue(counter);
    _print_queue(in1);
    _print_queue(in2);
    _print_queue(test);
    _print_queue(enable);
    //~ printf("(counter) Valid: %d; \t bits: %d.\n", read(INSTRUMENTATIONWIDGET_primary(counter_valid)), read(INSTRUMENTATIONWIDGET_primary(counter_data)));
#undef _print_queue
#undef _STR
#undef _STR1
  }

  void drain_instrumentation(bool check = false, int max = 16) {
    // If it's already empty, return.
    if (read(INSTRUMENTATIONWIDGET_primary(test_valid)) == 0) {
      return;
    }

    // Block the target from enqueing.
    INSTRUMENTATIONWIDGET_primary(drain_only)(1);
    step(1);

    int count = 0;
    while(1) {
      if (count > max) break;

      INSTRUMENTATIONWIDGET_primary(all_ready)();
      step(1);

      if (read(INSTRUMENTATIONWIDGET_primary(test_valid)) == 0) {
        break;
      }
      if (!check) {
        print_instrumentation_data();
      } else {
        expect(read(INSTRUMENTATIONWIDGET_primary(in1_valid)) != 0, "in1 should be valid");
        expect(read(INSTRUMENTATIONWIDGET_primary(in2_valid)) != 0, "in2 should be valid");
        expect(read(INSTRUMENTATIONWIDGET_primary(test_valid)) != 1, "test should be valid");

        // test is registered, so it is actually test_prev
        int in1 = read(INSTRUMENTATIONWIDGET_primary(in1_data));
        int in2 = read(INSTRUMENTATIONWIDGET_primary(in2_data));
        int test_prev = read(INSTRUMENTATIONWIDGET_primary(test_data));
        if (in1 == 0) {
          // Can't check -1
          continue;
        } else {
          int in1_prev = in1 - 1;
          expect(in2_cache[in1] == in2, "Expect corresponding in2");
          expect(test_prev == (in2_cache[in1_prev] + in1_prev), "Expect correct answer");
        }
      }

      count++;
    }

    // Allow the target to enqueue again.
    INSTRUMENTATIONWIDGET_primary(drain_only)(0);
  }

public:
  void run() {
    target_reset();

    // Forget the 2nd widget for now.
    INSTRUMENTATIONWIDGET_other(blocking)(0);

    // Enable the target.
    poke(io_enable, 0);
    step(5);
    poke(io_enable, 1);
    step(1);

    // Drain before the test starts.
    drain_instrumentation();

    puts("Testing synchronous drain of instrumentation");
    run_test(true);
    puts("Testing asynchronous drain of instrumentation");
    run_test(false);
  }

  // Choose between testing with synchoronous or asynchronous drain.
  void run_test(bool sync_drain = true) {
    uint32_t in1_last = 0;
    uint32_t in2_last = 0;

    for (int i = 0 ; i < 128; i++) {
      uint32_t in1 = i;
      uint32_t in2 = rand_next(256 - i);
      in2_cache[i] = in2;

      if (sync_drain) {
        // Always drain every cycle.
        INSTRUMENTATIONWIDGET_primary(all_ready)();
      } else {
        if (read(INSTRUMENTATIONWIDGET_primary(test_full))) {
          // Test that we can drain asynchronously.
          // We still have to drain when the buffer fills up, or the simulation
          // stops working.
          drain_instrumentation();

          // Re-poke values from last round since that's what the checks below
          // expect.
          poke(io_in1, in1_last);
          poke(io_in2, in2_last);
          step(1);
        }
      }
      // Line up next entries
      poke(io_in1, in1);
      poke(io_in2, in2);
      // Cycle.
      step(1);

      // Read previous iteration's entries.
      if (i > 0) {
        printf("(poke) in1 = %d, in2 = %d\n", in1_last, in2_last);
        printf("(peek) out: %d\n", peek(io_out));
        expect(io_out, in1_last + in2_last);
        if (sync_drain) {
          print_instrumentation_data();
        }
      }

      // Move current values to _last.
      in1_last = in1;
      in2_last = in2;
    }
  }
};
