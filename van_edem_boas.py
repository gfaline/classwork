from math import floor, sqrt, ceil


class VanEmdeBoas:
    # The element stored in min does not appear in any children trees, it is
    # only stored at min, while element stored at max does.
    min = None
    max = None
    # Summary keeps a log of which children are empty
    summary = None
    clusters = None
    universe_size = None
    # Power of two... or 2 ^ 2 ^ k (size). Also known as even powers of 2

    def __str__(self):
        long_string = "Universe size is " + str(self.universe_size) + ". Max and Min are " + str(self.max) + \
                      " " + str(self.min) + ". Summary: " + str(self.summary) + "Clusters " + str(self.clusters)
        return long_string

    def high(self, x):
        # It will return floor( x/ceil(\sqrt{u}) ),
        # which is basically the cluster index in which the key x is present.
        return floor(x/ceil(sqrt(self.universe_size)))

    def low(self, x):
        # It will return x mod ceil( \sqrt{u} ) which is its position in the cluster.
        return x % ceil(sqrt(self.universe_size))

    def index(self, a, b):
        # a is the cluster index. b is the index inside the cluster
        return a * ceil(sqrt(self.universe_size)) + b
        # return a * floor(sqrt(self.universe_size)) + b

    @staticmethod
    def minimum(vEB):
        if vEB is None:
            return None
        return vEB.min

    @staticmethod
    def maximum(vEB):
        if vEB is None:
            return None
        return vEB.max

    @staticmethod
    def universe_size(vEB):
        return vEB.universe_size

    # cluster of size 2 will be redundant after the addition of min and max values.
    def __init__(self, size):
        self.universe_size = size
        decrease = ceil(sqrt(self.universe_size))
        # self.summary = []
        self.clusters = []
        if size > 2:
            for i in range(decrease):
                # self.summary.append(None)
                self.clusters.append(None)
        self.summary = None

    @staticmethod
    def successor(vEB, key):
        # TODO: I am not sure I am checking for None enough here
        # The SUCCESSOR operation can avoid making a recursive call to determine
        # whether the successor of a value x lies within high.x/. That is because
        # x’s successor lies within its cluster if and only if x is strictly less
        # than the max attribute of its cluster.
        if vEB.universe_size == 2:
            if key == 0 and vEB.max == 1:
                return 1
            return None
        if vEB.min is not None and key < vEB.min:
            return vEB.min
        clusters = vEB.clusters[vEB.high(key)]
        max_low = None
        if clusters is not None:
            max_low = clusters.max
        if max_low is not None and key < vEB.min:
            offset = VanEmdeBoas.successor(clusters, vEB.low(key))
            return vEB.index(vEB.high(key), offset)
        # Not sure what this line does...
        succ_cluster = None
        if vEB.summary is not None:
            succ_cluster = VanEmdeBoas.successor(vEB.summary, vEB.high(key))
        if succ_cluster is None:
            return None
        clusters = vEB.clusters[succ_cluster]
        if clusters is not None:
            offset = VanEmdeBoas.minimum(clusters)
        else:
            offset = 0
        return vEB.index(succ_cluster, offset)

    @staticmethod
    def predecessor(vEB, key):
        if vEB.universe_size == 2:
            if key == 1 and vEB.min == 0:
                return 0
            return None
        if vEB.max is not None and key > vEB.max:
            return vEB.max

        min_low = None
        clusters = vEB.clusters[vEB.high(key)]
        if clusters is not None:
            min_low = VanEmdeBoas.minimum(clusters)
        if min_low is not None and vEB.low(key) > min_low:
            offset = VanEmdeBoas.predecessor(clusters, vEB.low(key))
            if offset is None:
                offset = 0
            return vEB.index(vEB.high(key), offset)
        # Not sure what this line does...
        pred_cluster = None
        if vEB.summary is not None:
            pred_cluster = VanEmdeBoas.predecessor(vEB.summary, vEB.high(key))
        if pred_cluster is None:
            if vEB.min is not None and key > vEB.min:
                return vEB.min
            return None
        offset = VanEmdeBoas.maximum(vEB.clusters[pred_cluster])
        if offset is None:
            offset = 0
        return vEB.index(pred_cluster, offset)

    @staticmethod
    def empty_tree_insert(vEB, key):
        vEB.min = key
        vEB.max = key

    # TODO: I am not sure this actually makes a new tree or cluster properly. There is no init called
    @staticmethod
    def insert(vEB, key):
        # If there is no min, both min and max are set to key
        if vEB.min is None:
            VanEmdeBoas.empty_tree_insert(vEB, key)
            return

        if key < vEB.min:
            temp = vEB.min
            vEB.min = key
            key = temp
        if vEB.universe_size > 2:
            clusters = vEB.clusters[vEB.high(key)]
            if clusters is None:
                vEB.clusters[vEB.high(key)] = VanEmdeBoas(vEB.high(vEB.universe_size))
                clusters = vEB.clusters[vEB.high(key)]
            if vEB.summary is None:
                vEB.summary = VanEmdeBoas(vEB.high(vEB.universe_size))
            if VanEmdeBoas.minimum(clusters) is None:
                VanEmdeBoas.insert(vEB.summary, vEB.high(key))
                VanEmdeBoas.empty_tree_insert(clusters, vEB.low(key))
            else:
                VanEmdeBoas.insert(clusters, vEB.low(key))
        if key > vEB.max:
            vEB.max = key

    @staticmethod
    def delete(vEB, key):
        if vEB.min == vEB.max:
            vEB.min = None
            vEB.max = None
            return None
        if vEB.universe_size == 2:
            if key == 0:
                vEB.min = 1
            else:
                vEB.min = 0
            vEB.max = vEB.min
            return
        if key == vEB.min:
            first_cluster = VanEmdeBoas.minimum(vEB.summary)
            key = vEB.index(first_cluster, VanEmdeBoas.minimum(vEB.clusters[first_cluster]))
            vEB.min = key
        # I'm not sure this line is supposed to be here and not inside that if
        VanEmdeBoas.delete(vEB.clusters[vEB.high(key)], vEB.low(key))
        if VanEmdeBoas.minimum(vEB.clusters[vEB.high(key)]) is None:
            VanEmdeBoas.delete(vEB.summary, vEB.high(key))
            if key == vEB.max:
                summary_max = VanEmdeBoas.maximum(vEB.summary)
                if summary_max is None:
                    vEB.max = vEB.min
                else:
                    vEB.max = vEB.index(summary_max, VanEmdeBoas.maximum(vEB.clusters[summary_max]))
        elif key == vEB.max:
            vEB.max = vEB.index(vEB.high(key), VanEmdeBoas.maximum(vEB.clusters[vEB.high(key)]))

    @staticmethod
    def member(vEB, key):
        if vEB is None:
            return False
        if key == vEB.min or key == vEB.max:
            return True
        if vEB.universe_size == 2:
            return False
        return VanEmdeBoas.member(vEB.clusters[vEB.high(key)], vEB.low(key))


a = VanEmdeBoas(16)
print(a)
VanEmdeBoas.insert(a, 5)
print(a)
VanEmdeBoas.insert(a, 6)
print(a)
VanEmdeBoas.insert(a, 7)
print(a)
VanEmdeBoas.insert(a, 2)
print(a)
print(VanEmdeBoas.member(a, 2))
print(VanEmdeBoas.member(a, 1))
print(VanEmdeBoas.member(a, 5))
VanEmdeBoas.delete(a, 2)
print(a)
VanEmdeBoas.insert(a, 2)
print(a)
VanEmdeBoas.insert(a, 1)
print(a)
VanEmdeBoas.insert(a, 4)
print(a)
print("NEW TREE")
b = VanEmdeBoas(16)
VanEmdeBoas.insert(b, 0)
VanEmdeBoas.insert(b, 2)
VanEmdeBoas.insert(b, 3)
VanEmdeBoas.insert(b, 4)
VanEmdeBoas.insert(b, 6)
VanEmdeBoas.insert(b, 12)
print(VanEmdeBoas.member(b, 2))
print(b.high(2))
print(b.high(3))
print(b.low(2))
print(b.low(3))
# position in cluster b and its cluster index a
print(b.index(0, 2))
# VanEmdeBoas.insert(b, 14)
# VanEmdeBoas.insert(b, 15)
print("b: ", b)
for cluster in b.clusters:
    print(cluster)
