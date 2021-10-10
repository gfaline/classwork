import random
import time
from van_edem_boas import VanEmdeBoas


def test_speed(n, file):
    veb = VanEmdeBoas(n)

    print("Creating VEB Tree for %d elements..." % n)
    file.write("Creating VEB Tree for %d elements..." % n + '\n')

    # create random numbers
    numbers = set()
    for i in range(n):
        number = random.randint(0, veb.universe_size - 1)
        numbers.add(number)

    total_time = 0
    starttime = time.time()
    for number in numbers:
        VanEmdeBoas.insert(veb, number)
    total_time = (time.time() - starttime)

    ns = float(len(numbers))
    total_time = float(total_time)

    print("Speed per insertion: %f seconds" % (total_time / ns))
    file.write("Speed per insertion: %f seconds" % (total_time / ns) + '\n')

    total_time = 0
    starttime = time.time()
    for number in numbers:
        VanEmdeBoas.member(veb, number)
    total_time = (time.time() - starttime)

    print("Speed per membership query: %f seconds" % (total_time / ns))
    file.write("Speed per membership query: %f seconds" % (total_time / ns) + '\n')

    total_time = 0
    starttime = time.time()
    for number in numbers:
        VanEmdeBoas.predecessor(veb, number)
    total_time = (time.time() - starttime)

    print("Speed per predecessor query: %f seconds" % (total_time / ns))
    file.write("Speed per predecessor query: %f seconds" % (total_time / ns) + '\n')

    total_time = 0
    starttime = time.time()
    for number in numbers:
        VanEmdeBoas.successor(veb, number)
    total_time = (time.time() - starttime)

    print("Speed per successor query: %f seconds" % (total_time / ns))
    file.write("Speed per successor query: %f seconds" % (total_time / ns) + '\n')
    file.write("\n")


f = open("Speed_test_output_2.txt", 'a')
test_speed(16, f)
test_speed(64, f)
test_speed(256, f)
test_speed(65536, f)
test_speed(4096, f)
# This is too big
# test_speed(4294967296, f)
test_speed(2048, f)
test_speed(16384, f)

test_speed(1024, f)
test_speed(5000, f)
test_speed(22003, f)
